package com.locomotora.demo.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.locomotora.demo.common.ApiException;

@Service
public class JwtService {
    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationMinutes;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${locomotora.jwt.secret}") String secret,
            @Value("${locomotora.jwt.expiration-minutes}") long expirationMinutes
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationMinutes = expirationMinutes;
    }

    public String createToken(UUID userId, String email) {
        try {
            ObjectNode header = objectMapper.createObjectNode();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            Instant now = Instant.now();
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("sub", userId.toString());
            payload.put("email", email);
            payload.put("iat", now.getEpochSecond());
            payload.put("exp", now.plusSeconds(expirationMinutes * 60).getEpochSecond());

            String unsigned = encode(objectMapper.writeValueAsBytes(header)) + "." + encode(objectMapper.writeValueAsBytes(payload));
            return unsigned + "." + sign(unsigned);
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create token");
        }
    }

    public UUID validateAndGetUserId(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid token");
            }
            String unsigned = parts[0] + "." + parts[1];
            if (!constantTimeEquals(sign(unsigned), parts[2])) {
                throw new IllegalArgumentException("Invalid signature");
            }
            JsonNode payload = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            if (payload.path("exp").asLong(0) < Instant.now().getEpochSecond()) {
                throw new IllegalArgumentException("Expired token");
            }
            return UUID.fromString(payload.path("sub").asText());
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
    }

    private String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sign(String unsigned) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return encode(mac.doFinal(unsigned.getBytes(StandardCharsets.UTF_8)));
    }

    private boolean constantTimeEquals(String left, String right) {
        return java.security.MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }
}
