package com.wallet.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

enum class TransactionType {
    DEPOSIT, WITHDRAWAL, TRANSFER_OUT, TRANSFER_IN
}

@Entity
@Table(name = "transactions")
class Transaction(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    val wallet: Wallet,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counterpart_wallet_id")
    val counterpartWallet: Wallet? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: TransactionType,

    @Column(nullable = false, precision = 19, scale = 4)
    val amount: BigDecimal,

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    val balanceAfter: BigDecimal,

    @Column(name = "idempotency_key")
    val idempotencyKey: String? = null,

    @Column(length = 500)
    val description: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
