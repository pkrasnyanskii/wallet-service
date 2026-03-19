package com.wallet.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:Email(message = "Некорректный email")
    @field:NotBlank(message = "Email обязателен")
    @Schema(example = "user@example.com")
    val email: String,

    @field:NotBlank(message = "Пароль обязателен")
    @field:Size(min = 8, message = "Пароль минимум 8 символов")
    @Schema(example = "securepassword")
    val password: String,

    @field:NotBlank(message = "Имя обязательно")
    @Schema(example = "Иван Иванов")
    val fullName: String
)

data class LoginRequest(
    @field:Email
    @field:NotBlank
    @Schema(example = "user@example.com")
    val email: String,

    @field:NotBlank
    @Schema(example = "securepassword")
    val password: String
)

data class AuthResponse(
    val token: String,
    val tokenType: String = "Bearer"
)
