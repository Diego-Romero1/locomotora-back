package com.locomotora.demo.auth.google;

import com.locomotora.demo.common.ApiException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Verifica el ID token que la app obtiene de Google: firma (claves publicas de Google, que Nimbus guarda en
 * cache), emisor, vencimiento y audiencia (nuestro Web client ID). Sin client IDs configurados, el login con
 * Google queda apagado.
 */
@Component
public class GoogleTokenVerifier {
    static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");
    private static final int MAX_NAME_LENGTH = 160;

    private final List<String> clientIds;
    private final JwtDecoder decoder;

    @Autowired
    public GoogleTokenVerifier(@Value("${locomotora.google.client-ids:}") String clientIds) {
        this(parseClientIds(clientIds), NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build());
    }

    GoogleTokenVerifier(List<String> clientIds, JwtDecoder decoder) {
        this.clientIds = List.copyOf(clientIds);
        this.decoder = decoder;
        if (decoder instanceof NimbusJwtDecoder nimbus) {
            nimbus.setJwtValidator(validator(this.clientIds));
        }
    }

    public boolean isConfigured() {
        return !clientIds.isEmpty();
    }

    public GoogleIdentity verify(String idToken) {
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid Google token");
        }
        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        if (subject == null || subject.isBlank() || email == null || email.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid Google token");
        }
        // Solo se confia en el email si Google lo verifico: con eso se vincula a una cuenta existente.
        if (!"true".equalsIgnoreCase(String.valueOf(jwt.getClaims().get("email_verified")))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Google email is not verified");
        }
        String normalizedEmail = email.trim().toLowerCase();
        return new GoogleIdentity(subject, normalizedEmail, displayName(jwt, normalizedEmail), jwt.getClaimAsString("picture"));
    }

    static OAuth2TokenValidator<Jwt> validator(List<String> clientIds) {
        OAuth2TokenValidator<Jwt> issuer = jwt -> GOOGLE_ISSUERS.contains(String.valueOf(jwt.getClaims().get("iss")))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Unexpected issuer", null));
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience() != null && jwt.getAudience().stream().anyMatch(clientIds::contains)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Unexpected audience", null));
        return new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), issuer, audience);
    }

    private static String displayName(Jwt jwt, String email) {
        String name = firstNonBlank(jwt.getClaimAsString("name"), jwt.getClaimAsString("given_name"), email.split("@")[0]);
        name = name.trim();
        return name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
    }

    private static String firstNonBlank(String... values) {
        return Arrays.stream(values).filter(value -> value != null && !value.isBlank()).findFirst().orElse("");
    }

    private static List<String> parseClientIds(String raw) {
        return Arrays.stream(raw.split(",")).map(String::trim).filter(value -> !value.isEmpty()).toList();
    }

    public record GoogleIdentity(String subject, String email, String name, String pictureUrl) {
    }
}
