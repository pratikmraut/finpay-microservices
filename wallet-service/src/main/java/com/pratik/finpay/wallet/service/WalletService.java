package com.pratik.finpay.wallet.service;

import com.pratik.finpay.common.enums.WalletStatus;
import com.pratik.finpay.common.util.MoneyUtils;
import com.pratik.finpay.wallet.dto.request.CreateWalletRequest;
import com.pratik.finpay.wallet.dto.request.UpdateWalletStatusRequest;
import com.pratik.finpay.wallet.dto.request.WalletOperationRequest;
import com.pratik.finpay.wallet.dto.response.WalletBalanceResponse;
import com.pratik.finpay.wallet.dto.response.WalletResponse;
import com.pratik.finpay.wallet.entity.Wallet;
import com.pratik.finpay.wallet.exception.BusinessException;
import com.pratik.finpay.wallet.exception.ResourceNotFoundException;
import com.pratik.finpay.wallet.repository.WalletRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WalletService {

    private static final String WALLET_CACHE = "walletDetails";

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Transactional
    public WalletResponse createWallet(CreateWalletRequest request) {
        BigDecimal initialBalance = MoneyUtils.normalize(request.initialBalance());
        String currency = normalizeCurrencyOrDefault(request.currency());

        Wallet wallet = new Wallet(
                generateWalletNumber(),
                request.userId(),
                initialBalance,
                currency,
                WalletStatus.ACTIVE
        );

        return WalletResponse.from(walletRepository.save(wallet));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = WALLET_CACHE, key = "'wallet:details:' + #walletId")
    public WalletResponse getWallet(Long walletId) {
        return WalletResponse.from(findWallet(walletId));
    }

    @Transactional(readOnly = true)
    public WalletBalanceResponse getBalance(Long walletId) {
        return WalletBalanceResponse.from(findWallet(walletId));
    }

    @Transactional
    @CacheEvict(cacheNames = WALLET_CACHE, key = "'wallet:details:' + #walletId")
    public WalletResponse updateStatus(Long walletId, UpdateWalletStatusRequest request) {
        Wallet wallet = findWallet(walletId);
        wallet.changeStatus(request.status());
        return WalletResponse.from(walletRepository.save(wallet));
    }

    @Transactional
    @CacheEvict(cacheNames = WALLET_CACHE, key = "'wallet:details:' + #walletId")
    public WalletBalanceResponse debitWallet(Long walletId, WalletOperationRequest request) {
        Wallet wallet = findWallet(walletId);
        BigDecimal amount = validateOperation(wallet, request);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("INSUFFICIENT_BALANCE", "Wallet does not have sufficient balance");
        }

        wallet.debit(amount);
        return WalletBalanceResponse.from(walletRepository.save(wallet));
    }

    @Transactional
    @CacheEvict(cacheNames = WALLET_CACHE, key = "'wallet:details:' + #walletId")
    public WalletBalanceResponse creditWallet(Long walletId, WalletOperationRequest request) {
        Wallet wallet = findWallet(walletId);
        BigDecimal amount = validateOperation(wallet, request);

        wallet.credit(amount);
        return WalletBalanceResponse.from(walletRepository.save(wallet));
    }

    private Wallet findWallet(Long walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }

    private BigDecimal validateOperation(Wallet wallet, WalletOperationRequest request) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BusinessException("WALLET_INACTIVE", "Wallet must be ACTIVE");
        }

        String currency = MoneyUtils.normalizeCurrency(request.currency());
        if (!MoneyUtils.isSupportedCurrency(currency) || !wallet.getCurrency().equals(currency)) {
            throw new BusinessException("UNSUPPORTED_CURRENCY", "Currency must be INR for v1");
        }

        return MoneyUtils.requirePositive(request.amount(), "amount");
    }

    private String normalizeCurrencyOrDefault(String currency) {
        if (currency == null || currency.isBlank()) {
            return MoneyUtils.DEFAULT_CURRENCY;
        }
        String normalizedCurrency = MoneyUtils.normalizeCurrency(currency);
        if (!MoneyUtils.isSupportedCurrency(normalizedCurrency)) {
            throw new BusinessException("UNSUPPORTED_CURRENCY", "Currency must be INR for v1");
        }
        return normalizedCurrency;
    }

    private String generateWalletNumber() {
        String candidate;
        do {
            candidate = "WALLET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (walletRepository.existsByWalletNumber(candidate));
        return candidate;
    }
}
