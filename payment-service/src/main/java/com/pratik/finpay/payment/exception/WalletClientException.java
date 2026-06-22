package com.pratik.finpay.payment.exception;

public class WalletClientException extends RuntimeException {

    private final String errorCode;

    public WalletClientException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
