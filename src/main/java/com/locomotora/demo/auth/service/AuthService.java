package com.locomotora.demo.auth.service;

import com.locomotora.demo.auth.dto.AuthResponse;
import com.locomotora.demo.auth.dto.GoogleLoginRequest;
import com.locomotora.demo.auth.dto.LoginRequest;
import com.locomotora.demo.auth.dto.RegisterRequest;
import com.locomotora.demo.auth.dto.UserResponse;
import com.locomotora.demo.auth.google.GoogleTokenVerifier;
import com.locomotora.demo.auth.google.GoogleTokenVerifier.GoogleIdentity;
import com.locomotora.demo.auth.repository.AuthRepository;
import com.locomotora.demo.auth.repository.AuthRepository.AccountRow;
import com.locomotora.demo.auth.repository.AuthRepository.LoginRow;
import com.locomotora.demo.auth.repository.AuthRepository.UserRow;
import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.security.JwtService;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthService(
            AuthRepository authRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            GoogleTokenVerifier googleTokenVerifier
    ) {
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    public AuthResponse register(RegisterRequest request) {
        try {
            UserRow user = authRepository.createUser(request, passwordEncoder.encode(request.password()));
            authRepository.createProfile(user.id());
            return toResponse(user);
        } catch (DuplicateKeyException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");
        }
    }

    public AuthResponse login(LoginRequest request) {
        LoginRow row = authRepository.findActiveLoginByEmail(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (row.passwordHash() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "This account signs in with Google");
        }
        if (!passwordEncoder.matches(request.password(), row.passwordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return toResponse(new UserRow(row.id(), row.email(), row.name()));
    }

    /**
     * Entra o crea la cuenta con Google: primero por la cuenta de Google ya vinculada; si no, por el email
     * (verificado por Google), vinculandola; si no existe, crea la cuenta con su perfil vacio (pasa por el
     * onboarding como un registro normal).
     */
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        if (!googleTokenVerifier.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Google sign-in is not configured");
        }
        GoogleIdentity identity = googleTokenVerifier.verify(request.idToken());

        Optional<UserRow> existing = findGoogleAccount(identity);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }
        try {
            return toResponse(authRepository.createGoogleUser(
                    identity.email(), identity.name(), identity.subject(), identity.pictureUrl()));
        } catch (DuplicateKeyException exception) {
            // Dos peticiones a la vez para la misma cuenta: la otra ya la creo.
            return findGoogleAccount(identity)
                    .map(this::toResponse)
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Email is already registered"));
        }
    }

    private Optional<UserRow> findGoogleAccount(GoogleIdentity identity) {
        Optional<AccountRow> linked = authRepository.findAccountByGoogleSubject(identity.subject());
        if (linked.isPresent()) {
            return Optional.of(requireActive(linked.get()).toUser());
        }
        Optional<AccountRow> byEmail = authRepository.findAccountByEmail(identity.email());
        if (byEmail.isEmpty()) {
            return Optional.empty();
        }
        AccountRow account = requireActive(byEmail.get());
        if (!authRepository.linkGoogleSubject(account.id(), identity.subject())) {
            throw new ApiException(HttpStatus.CONFLICT, "This email is linked to another Google account");
        }
        return Optional.of(account.toUser());
    }

    private AccountRow requireActive(AccountRow account) {
        if (!account.isActive()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Account is not active");
        }
        return account;
    }

    private AuthResponse toResponse(UserRow user) {
        return new AuthResponse(
                jwtService.createToken(user.id(), user.email()),
                new UserResponse(user.id().toString(), user.name(), user.email())
        );
    }
}
