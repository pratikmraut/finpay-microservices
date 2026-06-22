package com.pratik.finpay.common.security;

public final class JwtConstants {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String ISSUER = "finpay-auth-service";
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_ROLE = "role";
    public static final String TOKEN_TYPE = "Bearer";

    private JwtConstants() {
    }
}