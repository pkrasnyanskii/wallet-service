package com.wallet.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "wallets")
class Wallet(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, precision = 19, scale = 4)
    var balance: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, length = 3)
    val currency: String = "RUB",

    @Version
    var version: Long = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @OneToMany(mappedBy = "wallet", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val transactions: MutableList<Transaction> = mutableListOf()
)
