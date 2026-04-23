package com.wallet.event

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class TransactionEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, TransactionEvent>
) {
    fun publish(event: TransactionEvent) {
        kafkaTemplate.send("wallet.transactions", event.walletId.toString(), event)
    }
}
