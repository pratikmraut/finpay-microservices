package com.pratik.finpay.auth.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pratik.finpay.auth.entity.Role;
import com.pratik.finpay.auth.entity.User;
import com.pratik.finpay.common.security.JwtConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final String secret;
    private final long expirationSeconds;
    private final ObjectMapper objectMapper;

    public JwtService(
            @Value("${finpay.jwt.secret}") String secret,
            @Value("${finpay.jwt.expiration-minutes}") long expirationMinutes,
            ObjectMapper objectMapper
    ) {
        this.secret = secret;
        this.expirationSeconds = expirationMinutes * 60;
        this.objectMapper = objectMapper;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationSeconds);

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.getEmail());
        payload.put(JwtConstants.CLAIM_USER_ID, user.getId());
        payload.put(JwtConstants.CLAIM_ROLE, user.getRole().name());
        payload.put("iss", JwtConstants.ISSUER);
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String unsignedToken = encodeJson(header) + "." + encodeJson(payload);
        return unsignedToken + "." + sign(unsignedToken);
    }

    public AuthenticatedUser validateToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            throw new IllegalArgumentException("Invalid JWT signature");
        }

        Map<String, Object> claims = decodePayload(parts[1]);
        validateIssuer(claims);
        validateExpiry(claims);

        String email = String.valueOf(claims.get("sub"));
        Long userId = ((Number) claims.get(JwtConstants.CLAIM_USER_ID)).longValue();
        Role role = Role.valueOf(String.valueOf(claims.get(JwtConstants.CLAIM_ROLE)));
        return new AuthenticatedUser(userId, email, role);
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(value);
            return BASE64_URL_ENCODER.encodeToString(json);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to encode JWT", ex);
        }
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
            byte[] signature = mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8));
            return BASE64_URL_ENCODER.encodeToString(signature);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign JWT", ex);
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