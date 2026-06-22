package com.pratik.finpay.payment.client;

import com.pratik.finpay.common.dto.ApiResponse;
import com.pratik.finpay.common.dto.ErrorResponse;
import com.pratik.finpay.payment.exception.WalletClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Component
public class RestWalletClient implements WalletClient {

    private static final ParameterizedTypeReference<ApiResponse<WalletBalanceResponse>> WALLET_BALANCE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestTemplate restTemplate;
    private final String walletServiceBaseUrl;
    private final String internalServiceToken;

    public RestWalletClient(
            RestTemplate restTemplate,
            @Value("${finpay.wallet-service.base-url}") String walletServiceBaseUrl,
            @Value("${finpay.internal-service-token}") String internalServiceToken
    ) {
        this.restTemplate = restTemplate;
        this.walletServiceBaseUrl = walletServiceBaseUrl;
        this.internalServiceToken = internalServiceToken;
    }

    @Override
    public WalletBalanceResponse debitWallet(Long walletId, BigDecimal amount, String currency, String reference) {
        return callWallet(walletId, amount, currency, reference, "debit");
    }

    @Override
    public WalletBalanceResponse creditWallet(Long walletId, BigDecimal amount, String currency, String reference) {
        return callWallet(walletId, amount, currency, reference, "credit");
    }

    private WalletBalanceResponse callWallet(
            Long walletId,
            BigDecimal amount,
            String currency,
            String reference,
            String operation
    ) {
        String url = walletServiceBaseUrl + "/internal/v1/wallets/" + walletId + "/" + operation;
        WalletOperationRequest request = new WalletOperationRequest(amount, currency, reference);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-INTERNAL-SERVICE-TOKEN", internalServiceToken);

        try {
            ResponseEntity<ApiResponse<WalletBalanceResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    WALLET_BALANCE_TYPE
            );
            ApiResponse<WalletBalanceResponse> body = response.getBody();
            if (body == null || body.data() == null) {
                throw new WalletClientException("WALLET_SERVICE_ERROR", "Wallet service returned empty response");
            }
            return body.data();
        } catch (HttpStatusCodeException ex) {
            throw new WalletClientException(resolveErrorCode(ex), resolveErrorMessage(ex));
        }
    }

    private String resolveErrorCode(HttpStatusCodeException ex) {
        ErrorResponse error = parseError(ex);
        return error != null ? error.errorCode() : "WALLET_SERVICE_ERROR";
    }

    private String resolveErrorMessage(HttpStatusCodeException ex) {
        ErrorResponse error = parseError(ex);
        return error != null ? error.message() : "Wallet service call failed";
    }

    private ErrorResponse parseError(HttpStatusCodeException ex) {
        try {
            return RestTemplateErrorParser.parse(ex.getResponseBodyAsString());
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
