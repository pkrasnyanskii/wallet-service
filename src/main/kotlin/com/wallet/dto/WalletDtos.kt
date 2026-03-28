package com.wallet.dto

import com.wallet.domain.TransactionType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class DepositRequest(
    @field:NotNull(message = "Сумма обязательна")
    @field:DecimalMin(value = "0.01", message = "Сумма должна быть больше 0")
    @field:Digits(integer = 15, fraction = 4)
    @Schema(example = "1000.00")
    val amount: BigDecimal
)

data class TransferRequest(
    @field:NotNull(message = "ID кошелька получателя обязателен")
    @Schema(description = "UUID кошелька получателя")
    val toWalletId: UUID,

    @field:NotNull(message = "Сумма обязательна")
    @field:DecimalMin(value = "0.01", message = "Сумма должна быть больше 0")
    @field:Digits(integer = 15, fraction = 4)
    @Schema(example = "500.00")
    val amount: BigDecimal,

    @field:NotBlank(message = "Idempotency key обязателен")
    @field:Size(max = 255)
    @Schema(description = "Уникальный ключ для предотвращения дублирования", example = "unique-transfer-key-123")
    val idempotencyKey: String,

    @field:Size(max = 500)
    val description: String? = null
)

data class BalanceResponse(
    val walletId: UUID,
    val balance: BigDecimal,
    val currency: String,
    @Schema(description = "Из Redis кэша или БД")
    val cached: Boolean = false
)

data class TransactionResponse(
    val id: UUID,
    val type: TransactionType,
    val amount: BigDecimal,
    val balanceAfter: BigDecimal,
    val counterpartWalletId: UUID?,
    val description: String?,
    val createdAt: OffsetDateTime
)

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
