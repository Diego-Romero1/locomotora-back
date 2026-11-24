package com.locomotora.demo.auth.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.locomotora.demo.auth.google.GoogleTokenVerifier.GoogleIdentity;
import com.locomotora.demo.common.ApiException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** Tokens firmados con una clave propia en lugar de la de Google; el resto de las reglas es el real. */
class GoogleTokenVerifierTest {
    private static final String CLIENT_ID = "web-client.apps.googleusercontent.com";
    private static KeyPair keys;
    private static KeyPair otherKeys;

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keys = generator.generateKeyPair();
        otherKeys = generator.generateKeyPair();
    }

    private GoogleTokenVerifier verifier(String... clientIds) {
        return new GoogleTokenVerifier(List.of(clientIds),
                NimbusJwtDecoder.withPublicKey((RSAPublicKey) keys.getPublic()).build());
    }

    private static String token(KeyPair signingKeys, Consumer<JWTClaimsSet.Builder> customize) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer("https://accounts.google.com")
                .audience(CLIENT_ID)
                .subject("google-123")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(3600)))
                .claim("email", "Diego@Example.com")
                .claim("email_verified", true)
                .claim("name", "Diego Romero")
                .claim("picture", "https://lh3.googleusercontent.com/a/photo");
        customize.accept(claims);
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims.build());
        jwt.sign(new RSASSASigner(signingKeys.getPrivate()));
        return jwt.serialize();
    }

    private static String token(Consumer<JWTClaimsSet.Builder> customize) throws Exception {
        return token(keys, customize);
    }

    private void assertRejected(GoogleTokenVerifier verifier, String token) {
        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).status()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void aValidTokenGivesTheGoogleIdentityWithTheEmailNormalized() throws Exception {
        GoogleIdentity identity = verifier(CLIENT_ID).verify(token(claims -> {}));

        assertThat(identity).isEqualTo(new GoogleIdentity(
                "google-123", "diego@example.com", "Diego Romero", "https://lh3.googleusercontent.com/a/photo"));
    }

    @Test
    void theIssuerWithoutSchemeIsAlsoGoogle() throws Exception {
        assertThat(verifier(CLIENT_ID).verify(token(claims -> claims.issuer("accounts.google.com"))).subject())
                .isEqualTo("google-123");
    }

    @Test
    void anyOfTheConfiguredClientIdsIsAccepted() throws Exception {
        assertThat(verifier("android-client", CLIENT_ID).verify(token(claims -> {})).subject()).isEqualTo("google-123");
    }

    @Test
    void withoutNameItFallsBackToGivenNameAndThenToTheEmail() throws Exception {
        GoogleTokenVerifier verifier = verifier(CLIENT_ID);
        assertThat(verifier.verify(token(claims -> claims.claim("name", null).claim("given_name", "Diego"))).name())
                .isEqualTo("Diego");
        assertThat(verifier.verify(token(claims -> claims.claim("name", null))).name()).isEqualTo("diego");
    }

    @Test
    void aTokenForAnotherAppIsRejected() throws Exception {
        assertRejected(verifier(CLIENT_ID), token(claims -> claims.audience("other-app.apps.googleusercontent.com")));
    }

    @Test
    void aTokenFromAnotherIssuerIsRejected() throws Exception {
        assertRejected(verifier(CLIENT_ID), token(claims -> claims.issuer("https://evil.example.com")));
    }

    @Test
    void anExpiredTokenIsRejected() throws Exception {
        Instant past = Instant.now().minusSeconds(7200);
        assertRejected(verifier(CLIENT_ID), token(claims -> claims
                .issueTime(Date.from(past))
                .expirationTime(Date.from(past.plusSeconds(3600)))));
    }

    @Test
    void aTokenSignedWithAnotherKeyIsRejected() throws Exception {
        assertRejected(verifier(CLIENT_ID), token(otherKeys, claims -> {}));
    }

    @Test
    void anUnverifiedEmailIsRejected() throws Exception {
        assertRejected(verifier(CLIENT_ID), token(claims -> claims.claim("email_verified", false)));
    }

    @Test
    void garbageIsRejected() {
        assertRejected(verifier(CLIENT_ID), "not-a-token");
    }

    @Test
    void withoutClientIdsItIsNotConfigured() {
        assertThat(new GoogleTokenVerifier(" , ").isConfigured()).isFalse();
        assertThat(new GoogleTokenVerifier(CLIENT_ID).isConfigured()).isTrue();
    }
}
