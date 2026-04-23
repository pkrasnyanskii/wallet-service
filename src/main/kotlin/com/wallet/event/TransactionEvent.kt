package com.wallet.event

import com.wallet.domain.TransactionType
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class TransactionEvent(
    val transactionId: UUID,
    val walletId: UUID,
    val userId: UUID,
    val type: TransactionType,
    val amount: BigDecimal,
    val balanceAfter: BigDecimal,
    val counterpartWalletId: UUID? = null,
    val occurredAt: OffsetDateTime = OffsetDateTime.now()
)
