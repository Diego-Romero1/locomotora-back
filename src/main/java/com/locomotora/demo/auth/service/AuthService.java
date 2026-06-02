package com.locomotora.demo.auth.service;

import com.locomotora.demo.auth.dto.AuthResponse;
import com.locomotora.demo.auth.dto.LoginRequest;
import com.locomotora.demo.auth.dto.RegisterRequest;
import com.locomotora.demo.auth.dto.UserResponse;
import com.locomotora.demo.auth.repository.AuthRepository;
import com.locomotora.demo.auth.repository.AuthRepository.LoginRow;
import com.locomotora.demo.auth.repository.AuthRepository.UserRow;
import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.security.JwtService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AuthRepository authRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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
        if (!passwordEncoder.matches(request.password(), row.passwordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return toResponse(new UserRow(row.id(), row.email(), row.name()));
    }

    private AuthResponse toResponse(UserRow user) {
        return new AuthResponse(
                jwtService.createToken(user.id(), user.email()),
                new UserResponse(user.id().toString(), user.name(), user.email())
        );
    }
}
