package com.wallet.event

import com.wallet.domain.AuditLog
import com.wallet.repository.AuditLogRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class TransactionEventConsumer(
    private val auditLogRepository: AuditLogRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["wallet.transactions"], groupId = "wallet-audit")
    fun handle(event: TransactionEvent) {
        log.info("Audit: transaction {} type={} amount={}", event.transactionId, event.type, event.amount)
        auditLogRepository.save(
            AuditLog(
                eventType = "TRANSACTION_${event.type}",
                walletId = event.walletId,
                userId = event.userId,
                payload = mapOf(
                    "transactionId" to event.transactionId.toString(),
                    "amount" to event.amount.toString(),
                    "balanceAfter" to event.balanceAfter.toString(),
                    "occurredAt" to event.occurredAt.toString()
                )
            )
        )
    }
}
