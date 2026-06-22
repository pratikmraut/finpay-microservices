package com.pratik.finpay.payment.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pratik.finpay.common.dto.ErrorResponse;

final class RestTemplateErrorParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private RestTemplateErrorParser() {
    }

    static ErrorResponse parse(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, ErrorResponse.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to parse wallet-service error response", ex);
        }
    }
}
