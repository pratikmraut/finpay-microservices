package com.pratik.finpay.wallet.repository;

import com.pratik.finpay.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    boolean existsByWalletNumber(String walletNumber);

    Optional<Wallet> findByUserId(Long userId);
}
