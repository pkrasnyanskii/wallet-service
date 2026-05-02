package com.wallet.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.wallet.dto.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class WalletIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    companion object {
        @Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
            withDatabaseName("walletdb")
            withUsername("wallet")
            withPassword("wallet_secret")
        }

        @Container
        val redis = GenericContainer<Nothing>("redis:7-alpine").apply {
            withExposedPorts(6379)
        }

        @Container
        val kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.0"))

        @JvmStatic
        @DynamicPropertySource
        fun overrideProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.data.redis.host", redis::getHost)
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
            registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers)
        }
    }

    @Test
    fun `register, login, deposit and check balance flow`() {
        val email = "test-${UUID.randomUUID()}@example.com"

        // Регистрация
        val registerResult = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                RegisterRequest(email = email, password = "password123", fullName = "Тест Пользователь")
            )
        }.andExpect { status { isCreated() } }
            .andReturn()

        val token = objectMapper.readValue(
            registerResult.response.contentAsString, AuthResponse::class.java
        ).token

        // Проверка баланса до пополнения
        mockMvc.get("/api/wallet/balance") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.balance") { value("0.0000") }
        }

        // Пополнение
        mockMvc.post("/api/wallet/deposit") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(DepositRequest(amount = BigDecimal("1000.00")))
        }.andExpect { status { isCreated() } }

        // Баланс после пополнения
        mockMvc.get("/api/wallet/balance") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.balance") { value("1000.0000") }
        }
    }

    @Test
    fun `transfer between two wallets with idempotency`() {
        fun registerAndGetToken(email: String): String {
            val result = mockMvc.post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    RegisterRequest(email = email, password = "password123", fullName = "User")
                )
            }.andReturn()
            return objectMapper.readValue(result.response.contentAsString, AuthResponse::class.java).token
        }

        val token1 = registerAndGetToken("sender-${UUID.randomUUID()}@example.com")
        val token2 = registerAndGetToken("receiver-${UUID.randomUUID()}@example.com")

        // Пополняем кошелёк отправителя
        mockMvc.post("/api/wallet/deposit") {
            header("Authorization", "Bearer $token1")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(DepositRequest(amount = BigDecimal("500.00")))
        }.andExpect { status { isCreated() } }

        // Получаем ID кошелька получателя
        val balanceResult = mockMvc.get("/api/wallet/balance") {
            header("Authorization", "Bearer $token2")
        }.andReturn()
        val toWalletId = objectMapper.readTree(balanceResult.response.contentAsString)
            .get("walletId").asText()

        val idempotencyKey = UUID.randomUUID().toString()

        // Перевод
        mockMvc.post("/api/wallet/transfer") {
            header("Authorization", "Bearer $token1")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                TransferRequest(
                    toWalletId = UUID.fromString(toWalletId),
                    amount = BigDecimal("200.00"),
                    idempotencyKey = idempotencyKey
                )
            )
        }.andExpect { status { isCreated() } }

        // Повторный запрос с тем же ключом — должен вернуть тот же результат (идемпотентность)
        mockMvc.post("/api/wallet/transfer") {
            header("Authorization", "Bearer $token1")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                TransferRequest(
                    toWalletId = UUID.fromString(toWalletId),
                    amount = BigDecimal("200.00"),
                    idempotencyKey = idempotencyKey
                )
            )
        }.andExpect { status { isCreated() } }

        // Баланс отправителя должен быть 300 (не 100), т.к. дублирование предотвращено
        mockMvc.get("/api/wallet/balance") {
            header("Authorization", "Bearer $token1")
        }.andExpect {
            status { isOk() }
            jsonPath("$.balance") { value("300.0000") }
        }
    }

    @Test
    fun `deposit with invalid amount returns 400`() {
        val result = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                RegisterRequest(
                    email = "invalid-${UUID.randomUUID()}@example.com",
                    password = "password123",
                    fullName = "User"
                )
            )
        }.andReturn()
        val token = objectMapper.readValue(result.response.contentAsString, AuthResponse::class.java).token

        mockMvc.post("/api/wallet/deposit") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"amount": -100}"""
        }.andExpect { status { isBadRequest() } }
    }
}
