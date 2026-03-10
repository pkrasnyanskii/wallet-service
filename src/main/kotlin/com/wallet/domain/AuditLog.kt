package com.wallet.domain

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "audit_log")
class AuditLog(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "event_type", nullable = false, length = 50)
    val eventType: String,

    @Column(name = "wallet_id")
    val walletId: UUID? = null,

    @Column(name = "user_id")
    val userId: UUID? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    val payload: Map<String, Any>? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
