package com.wallet.controller

import com.wallet.domain.User
import com.wallet.dto.*
import com.wallet.service.WalletService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/wallet")
@Tag(name = "Wallet", description = "Операции с кошельком")
@SecurityRequirement(name = "bearerAuth")
class WalletController(private val walletService: WalletService) {

    @GetMapping("/balance")
    @Operation(summary = "Получить баланс (с кэшированием в Redis)")
    fun getBalance(@AuthenticationPrincipal user: User): BalanceResponse =
        walletService.getBalance(user)

    @PostMapping("/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Пополнить баланс")
    fun deposit(
        @AuthenticationPrincipal user: User,
        @Valid @RequestBody request: DepositRequest
    ): TransactionResponse = walletService.deposit(user, request)

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Перевод с идемпотентностью и оптимистичной блокировкой")
    fun transfer(
        @AuthenticationPrincipal user: User,
        @Valid @RequestBody request: TransferRequest
    ): TransactionResponse = walletService.transfer(user, request)

    @GetMapping("/transactions")
    @Operation(summary = "История транзакций с пагинацией")
    fun getTransactions(
        @AuthenticationPrincipal user: User,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PageResponse<TransactionResponse> = walletService.getTransactions(user, page, size)
}
