package com.pratik.finpay.wallet.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pratik.finpay.common.security.JwtConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final String secret;
    private final ObjectMapper objectMapper;

    public JwtService(@Value("${finpay.jwt.secret}") String secret, ObjectMapper objectMapper) {
        this.secret = secret;
        this.objectMapper = objectMapper;
    }

    public AuthenticatedUser validateToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(unsignedToken), parts[2])) {
            throw new IllegalArgumentException("Invalid JWT signature");
        }

        Map<String, Object> claims = decodePayload(parts[1]);
        validateIssuer(claims);
        validateExpiry(claims);

        Long userId = ((Number) claims.get(JwtConstants.CLAIM_USER_ID)).longValue();
        String email = String.valueOf(claims.get("sub"));
        String role = String.valueOf(claims.get(JwtConstants.CLAIM_ROLE));
        return new AuthenticatedUser(userId, email, role);
    }

    private Map<String, Object> decodePayload(String payload) {
        try {
            byte[] json = BASE64_URL_DECODER.decode(payload);
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid JWT payload", ex);
        }
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to validate JWT", ex);
        }
    }

    private void validateIssuer(Map<String, Object> claims) {
        if (!JwtConstants.ISSUER.equals(claims.get("iss"))) {
            throw new IllegalArgumentException("Invalid JWT issuer");
        }
    }

    private void validateExpiry(Map<String, Object> claims) {
        Object exp = claims.get("exp");
        if (!(exp instanceof Number expirationEpochSeconds)) {
            throw new IllegalArgumentException("Invalid JWT expiry");
        }
        if (Instant.now().getEpochSecond() >= expirationEpochSeconds.longValue()) {
            throw new IllegalArgumentException("JWT is expired");
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        if (leftBytes.length != rightBytes.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < leftBytes.length; i++) {
            result |= leftBytes[i] ^ rightBytes[i];
        }
        return result == 0;
    }
}
