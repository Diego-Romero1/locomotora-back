package com.locomotora.demo.auth.dto;

public record AuthResponse(String token, UserResponse user) {
}
