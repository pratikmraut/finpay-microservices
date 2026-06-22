package com.pratik.finpay.wallet.repository;

import com.pratik.finpay.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    boolean existsByWalletNumber(String walletNumber);
}
