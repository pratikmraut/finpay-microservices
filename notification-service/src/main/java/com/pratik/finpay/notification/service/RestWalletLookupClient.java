package com.pratik.finpay.notification.service;

import com.pratik.finpay.common.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Component
public class RestWalletLookupClient implements WalletLookupClient {

    private static final ParameterizedTypeReference<ApiResponse<WalletLookupResponse>> WALLET_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestTemplate restTemplate;
    private final String walletServiceBaseUrl;
    private final String internalServiceToken;

    public RestWalletLookupClient(
            RestTemplate restTemplate,
            @Value("${finpay.wallet-service.base-url}") String walletServiceBaseUrl,
            @Value("${finpay.internal-service-token}") String internalServiceToken
    ) {
        this.restTemplate = restTemplate;
        this.walletServiceBaseUrl = walletServiceBaseUrl;
        this.internalServiceToken = internalServiceToken;
    }

    @Override
    public Optional<Long> findWalletIdForUser(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-INTERNAL-SERVICE-TOKEN", internalServiceToken);

        try {
            ResponseEntity<ApiResponse<WalletLookupResponse>> response = restTemplate.exchange(
                    walletServiceBaseUrl + "/internal/v1/wallets/user/" + userId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    WALLET_RESPONSE_TYPE
            );
            ApiResponse<WalletLookupResponse> body = response.getBody();
            if (body == null || body.data() == null || body.data().walletId() == null) {
                throw new IllegalStateException("Wallet service returned an empty response");
            }
            return Optional.of(body.data().walletId());
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (RestClientException ex) {
            throw new IllegalStateException("Unable to resolve the current user's wallet", ex);
        }
    }

    private record WalletLookupResponse(Long walletId) {
    }
}
