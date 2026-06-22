package com.pratik.finpay.wallet.service;

import com.pratik.finpay.common.enums.WalletStatus;
import com.pratik.finpay.wallet.dto.request.CreateWalletRequest;
import com.pratik.finpay.wallet.dto.request.UpdateWalletStatusRequest;
import com.pratik.finpay.wallet.dto.request.WalletOperationRequest;
import com.pratik.finpay.wallet.dto.response.WalletBalanceResponse;
import com.pratik.finpay.wallet.dto.response.WalletResponse;
import com.pratik.finpay.wallet.entity.Wallet;
import com.pratik.finpay.wallet.exception.BusinessException;
import com.pratik.finpay.wallet.exception.ResourceNotFoundException;
import com.pratik.finpay.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(walletRepository);
    }

    @Test
    void createWalletShouldCreateActiveWallet() {
        when(walletRepository.existsByWalletNumber(anyString())).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> withIdentity(invocation.getArgument(0)));

        WalletResponse response = walletService.createWallet(new CreateWalletRequest(
                1L,
                new BigDecimal("5000.00"),
                "INR"
        ));

        assertEquals(1L, response.walletId());
        assertEquals(1L, response.userId());
        assertEquals(new BigDecimal("5000.00"), response.balance());
        assertEquals("INR", response.currency());
        assertEquals(WalletStatus.ACTIVE, response.status());
    }

    @Test
    void getWalletNotFoundShouldFail() {
        when(walletRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> walletService.getWallet(99L));
    }

    @Test
    void debitShouldSubtractAmount() {
        Wallet wallet = wallet(new BigDecimal("1000.00"), WalletStatus.ACTIVE);
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(wallet)).thenReturn(wallet);

        WalletBalanceResponse response = walletService.debitWallet(1L, operation("250.00"));

        assertEquals(new BigDecimal("750.00"), response.balance());
        verify(walletRepository).save(wallet);
    }

    @Test
    void debitInsufficientBalanceShouldFail() {
        Wallet wallet = wallet(new BigDecimal("100.00"), WalletStatus.ACTIVE);
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> walletService.debitWallet(1L, operation("250.00"))
        );

        assertEquals("INSUFFICIENT_BALANCE", exception.getErrorCode());
    }

    @Test
    void creditShouldAddAmount() {
        Wallet wallet = wallet(new BigDecimal("1000.00"), WalletStatus.ACTIVE);
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(wallet)).thenReturn(wallet);

        WalletBalanceResponse response = walletService.creditWallet(1L, operation("250.00"));

        assertEquals(new BigDecimal("1250.00"), response.balance());
        verify(walletRepository).save(wallet);
    }

    @Test
    void inactiveWalletDebitShouldFail() {
        Wallet wallet = wallet(new BigDecimal("1000.00"), WalletStatus.INACTIVE);
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> walletService.debitWallet(1L, operation("250.00"))
        );

        assertEquals("WALLET_INACTIVE", exception.getErrorCode());
    }

    @Test
    void updateStatusShouldChangeWalletStatus() {
        Wallet wallet = wallet(new BigDecimal("1000.00"), WalletStatus.ACTIVE);
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(wallet)).thenReturn(wallet);

        WalletResponse response = walletService.updateStatus(1L, new UpdateWalletStatusRequest(WalletStatus.INACTIVE));

        assertEquals(WalletStatus.INACTIVE, response.status());
    }

    private Wallet wallet(BigDecimal balance, WalletStatus status) {
        Wallet wallet = new Wallet("WALLET-TEST", 1L, balance, "INR", status);
        return withIdentity(wallet);
    }

    private Wallet withIdentity(Wallet wallet) {
        ReflectionTestUtils.setField(wallet, "id", 1L);
        ReflectionTestUtils.setField(wallet, "createdAt", Instant.parse("2026-06-21T00:00:00Z"));
        ReflectionTestUtils.setField(wallet, "updatedAt", Instant.parse("2026-06-21T00:00:00Z"));
        return wallet;
    }

    private WalletOperationRequest operation(String amount) {
        return new WalletOperationRequest(new BigDecimal(amount), "INR", "PAY-TEST");
    }
}
