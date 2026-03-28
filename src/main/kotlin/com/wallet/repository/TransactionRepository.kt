package com.wallet.repository

import com.wallet.domain.Transaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface TransactionRepository : JpaRepository<Transaction, UUID> {
    fun findByWalletId(walletId: UUID, pageable: Pageable): Page<Transaction>
    fun findByIdempotencyKey(idempotencyKey: String): Optional<Transaction>
}
