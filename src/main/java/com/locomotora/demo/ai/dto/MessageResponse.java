package com.locomotora.demo.ai.dto;

public record MessageResponse(String id, String role, String content, String model, String createdAt) {
}
