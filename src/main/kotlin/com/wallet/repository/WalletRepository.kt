package com.wallet.repository

import com.wallet.domain.Wallet
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.util.UUID

interface WalletRepository : JpaRepository<Wallet, UUID> {

    fun findByUserId(userId: UUID): Optional<Wallet>

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    fun findByIdWithLock(id: UUID): Optional<Wallet>
}
