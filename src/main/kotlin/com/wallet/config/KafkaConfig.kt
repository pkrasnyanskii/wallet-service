package com.wallet.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaConfig {

    @Bean
    fun walletTransactionsTopic(): NewTopic =
        TopicBuilder.name("wallet.transactions")
            .partitions(3)
            .replicas(1)
            .build()
}
