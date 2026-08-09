package com.pratik.finpay.notification.service;

import java.util.Optional;

public interface WalletLookupClient {

    Optional<Long> findWalletIdForUser(Long userId);
}
