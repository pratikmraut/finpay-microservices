package com.pratik.finpay.wallet.service;

import com.pratik.finpay.wallet.dto.request.UpdateWalletStatusRequest;
import com.pratik.finpay.wallet.dto.request.WalletOperationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WalletCacheAnnotationTest {

    @Test
    void getWalletShouldUseWalletDetailsCache() throws Exception {
        Cacheable cacheable = WalletService.class
                .getMethod("getWallet", Long.class)
                .getAnnotation(Cacheable.class);

        assertNotNull(cacheable);
        assertArrayEquals(new String[]{"walletDetails"}, cacheable.cacheNames());
        assertEquals("'wallet:details:' + #walletId", cacheable.key());
    }

    @Test
    void debitCreditAndStatusUpdateShouldEvictWalletDetailsCache() throws Exception {
        assertEvictsWalletDetails("debitWallet", WalletOperationRequest.class);
        assertEvictsWalletDetails("creditWallet", WalletOperationRequest.class);
        assertEvictsWalletDetails("updateStatus", UpdateWalletStatusRequest.class);
    }

    private void assertEvictsWalletDetails(String methodName, Class<?> requestType) throws Exception {
        CacheEvict cacheEvict = WalletService.class
                .getMethod(methodName, Long.class, requestType)
                .getAnnotation(CacheEvict.class);

        assertNotNull(cacheEvict);
        assertArrayEquals(new String[]{"walletDetails"}, cacheEvict.cacheNames());
        assertEquals("'wallet:details:' + #walletId", cacheEvict.key());
    }
}
