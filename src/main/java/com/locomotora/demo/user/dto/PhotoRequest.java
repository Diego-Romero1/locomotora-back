package com.locomotora.demo.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Foto de perfil como data URI o URL publica. La app la manda recortada a 400 px en JPEG
 * (unos 30-60 KB en base64); el limite deja margen.
 */
public record PhotoRequest(@NotBlank @Size(max = 400_000) String profilePhotoUrl) {
}
