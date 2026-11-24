package com.locomotora.demo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.locomotora.demo.auth.dto.AuthResponse;
import com.locomotora.demo.auth.dto.GoogleLoginRequest;
import com.locomotora.demo.auth.dto.LoginRequest;
import com.locomotora.demo.auth.google.GoogleTokenVerifier;
import com.locomotora.demo.auth.google.GoogleTokenVerifier.GoogleIdentity;
import com.locomotora.demo.auth.repository.AuthRepository;
import com.locomotora.demo.auth.repository.AuthRepository.AccountRow;
import com.locomotora.demo.auth.repository.AuthRepository.LoginRow;
import com.locomotora.demo.auth.repository.AuthRepository.UserRow;
import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.security.JwtService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceGoogleTest {
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final GoogleIdentity IDENTITY =
            new GoogleIdentity("google-123", "diego@example.com", "Diego", "https://photo");
    private static final GoogleLoginRequest REQUEST = new GoogleLoginRequest("id-token");

    @Mock
    private AuthRepository repository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private GoogleTokenVerifier verifier;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(repository, passwordEncoder, jwtService, verifier);
    }

    private void verifiedToken() {
        rejectedAfterVerification();
        when(jwtService.createToken(any(), anyString())).thenReturn("jwt");
    }

    /** Token valido, pero la cuenta no puede entrar (no se llega a crear el JWT). */
    private void rejectedAfterVerification() {
        when(verifier.isConfigured()).thenReturn(true);
        when(verifier.verify("id-token")).thenReturn(IDENTITY);
    }

    private static AccountRow account(String status, String googleSubject) {
        return new AccountRow(USER_ID, "diego@example.com", "Diego R", status, googleSubject);
    }

    private static HttpStatus statusOf(Throwable error) {
        return ((ApiException) error).status();
    }

    @Test
    void anAlreadyLinkedAccountSignsIn() {
        verifiedToken();
        when(repository.findAccountByGoogleSubject("google-123")).thenReturn(Optional.of(account("ACTIVE", "google-123")));

        AuthResponse response = service.loginWithGoogle(REQUEST);

        assertThat(response.token()).isEqualTo("jwt");
        assertThat(response.user().name()).isEqualTo("Diego R");
        verify(repository, never()).createGoogleUser(any(), any(), any(), any());
    }

    @Test
    void anAccountWithThePasswordAndTheSameEmailGetsLinked() {
        verifiedToken();
        when(repository.findAccountByGoogleSubject("google-123")).thenReturn(Optional.empty());
        when(repository.findAccountByEmail("diego@example.com")).thenReturn(Optional.of(account("ACTIVE", null)));
        when(repository.linkGoogleSubject(USER_ID, "google-123")).thenReturn(true);

        assertThat(service.loginWithGoogle(REQUEST).user().id()).isEqualTo(USER_ID.toString());
        verify(repository, never()).createGoogleUser(any(), any(), any(), any());
    }

    @Test
    void anEmailLinkedToAnotherGoogleAccountIsAConflict() {
        rejectedAfterVerification();
        when(repository.findAccountByGoogleSubject("google-123")).thenReturn(Optional.empty());
        when(repository.findAccountByEmail("diego@example.com")).thenReturn(Optional.of(account("ACTIVE", "google-999")));
        when(repository.linkGoogleSubject(USER_ID, "google-123")).thenReturn(false);

        assertThatThrownBy(() -> service.loginWithGoogle(REQUEST))
                .satisfies(error -> assertThat(statusOf(error)).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void aNewGoogleUserGetsAnAccountWithNameAndPhoto() {
        verifiedToken();
        when(repository.findAccountByGoogleSubject("google-123")).thenReturn(Optional.empty());
        when(repository.findAccountByEmail("diego@example.com")).thenReturn(Optional.empty());
        when(repository.createGoogleUser("diego@example.com", "Diego", "google-123", "https://photo"))
                .thenReturn(new UserRow(USER_ID, "diego@example.com", "Diego"));

        AuthResponse response = service.loginWithGoogle(REQUEST);

        assertThat(response.user().name()).isEqualTo("Diego");
        assertThat(response.token()).isEqualTo("jwt");
    }

    @Test
    void whenTwoRequestsCreateTheSameAccountTheSecondOneSignsIn() {
        verifiedToken();
        when(repository.findAccountByGoogleSubject("google-123"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(account("ACTIVE", "google-123")));
        when(repository.findAccountByEmail("diego@example.com")).thenReturn(Optional.empty());
        when(repository.createGoogleUser(any(), any(), any(), any())).thenThrow(new DuplicateKeyException("users_email_key"));

        assertThat(service.loginWithGoogle(REQUEST).user().id()).isEqualTo(USER_ID.toString());
    }

    @Test
    void aDisabledAccountCannotSignIn() {
        rejectedAfterVerification();
        when(repository.findAccountByGoogleSubject("google-123")).thenReturn(Optional.of(account("DISABLED", "google-123")));

        assertThatThrownBy(() -> service.loginWithGoogle(REQUEST))
                .satisfies(error -> assertThat(statusOf(error)).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void withoutConfigurationItAnswers503AndDoesNotTouchTheToken() {
        when(verifier.isConfigured()).thenReturn(false);

        assertThatThrownBy(() -> service.loginWithGoogle(REQUEST))
                .satisfies(error -> assertThat(statusOf(error)).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        verify(verifier, never()).verify(any());
    }

    @Test
    void aGoogleOnlyAccountCannotSignInWithAPassword() {
        when(repository.findActiveLoginByEmail("diego@example.com"))
                .thenReturn(Optional.of(new LoginRow(USER_ID, "diego@example.com", "Diego", null)));

        assertThatThrownBy(() -> service.login(new LoginRequest("diego@example.com", "whatever1")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Google");
        verify(passwordEncoder, never()).matches(any(), any());
    }
}
