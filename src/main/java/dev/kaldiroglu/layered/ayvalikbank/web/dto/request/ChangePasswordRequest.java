package dev.kaldiroglu.layered.ayvalikbank.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(@NotBlank String newPassword) {}
