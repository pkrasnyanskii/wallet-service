package com.wallet.service

import com.wallet.domain.Transaction
import com.wallet.domain.TransactionType
import com.wallet.domain.User
import com.wallet.dto.*
import com.wallet.event.TransactionEvent
import com.wallet.event.TransactionEventProducer
import com.wallet.exception.*
import com.wallet.repository.TransactionRepository
import com.wallet.repository.WalletRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Duration
import java.util.UUID

@Service
class WalletService(
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
    private val eventProducer: TransactionEventProducer,
    private val redisTemplate: RedisTemplate<String, Any>
) {

    private fun cacheKey(walletId: UUID) = "wallet:balance:$walletId"

    fun getBalance(user: User): BalanceResponse {
        val wallet = walletRepository.findByUserId(user.id)
            .orElseThrow { WalletNotFoundException(user.id) }

        val cacheKey = cacheKey(wallet.id)
        val cached = redisTemplate.opsForValue().get(cacheKey) as? BigDecimal
        if (cached != null) {
            return BalanceResponse(wallet.id, cached, wallet.currency, cached = true)
        }
        redisTemplate.opsForValue().set(cacheKey, wallet.balance, Duration.ofMinutes(5))
        return BalanceResponse(wallet.id, wallet.balance, wallet.currency)
    }

    @Transactional
    fun deposit(user: User, request: DepositRequest): TransactionResponse {
        val wallet = walletRepository.findByUserId(user.id)
            .orElseThrow { WalletNotFoundException(user.id) }

        wallet.balance = wallet.balance.add(request.amount)
        walletRepository.save(wallet)

        val tx = transactionRepository.save(
            Transaction(
                wallet = wallet,
                type = TransactionType.DEPOSIT,
                amount = request.amount,
                balanceAfter = wallet.balance
            )
        )

        redisTemplate.delete(cacheKey(wallet.id))
        eventProducer.publish(TransactionEvent(
            transactionId = tx.id,
            walletId = wallet.id,
            userId = user.id,
            type = TransactionType.DEPOSIT,
            amount = request.amount,
            balanceAfter = wallet.balance
        ))
        return tx.toResponse()
    }

    @Retryable(
        retryFor = [ObjectOptimisticLockingFailureException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 100)
    )
    @Transactional
    fun transfer(user: User, request: TransferRequest): TransactionResponse {
        transactionRepository.findByIdempotencyKey(request.idempotencyKey).ifPresent {
            return it.toResponse()
        }

        val fromWallet = walletRepository.findByUserId(user.id)
            .orElseThrow { WalletNotFoundException(user.id) }
        val toWallet = walletRepository.findByIdWithLock(request.toWalletId)
            .orElseThrow { WalletNotFoundException(request.toWalletId) }

        if (fromWallet.id == toWallet.id) throw SameWalletTransferException()
        if (fromWallet.balance < request.amount) throw InsufficientFundsException(fromWallet.balance, request.amount)

        fromWallet.balance = fromWallet.balance.subtract(request.amount)
        toWallet.balance = toWallet.balance.add(request.amount)
        walletRepository.saveAll(listOf(fromWallet, toWallet))

        val outTx = transactionRepository.save(
            Transaction(
                wallet = fromWallet,
                counterpartWallet = toWallet,
                type = TransactionType.TRANSFER_OUT,
                amount = request.amount,
                balanceAfter = fromWallet.balance,
                idempotencyKey = request.idempotencyKey,
                description = request.description
            )
        )
        transactionRepository.save(
            Transaction(
                wallet = toWallet,
                counterpartWallet = fromWallet,
                type = TransactionType.TRANSFER_IN,
                amount = request.amount,
                balanceAfter = toWallet.balance,
                description = request.description
            )
        )

        redisTemplate.delete(listOf(cacheKey(fromWallet.id), cacheKey(toWallet.id)))
        eventProducer.publish(TransactionEvent(
            transactionId = outTx.id,
            walletId = fromWallet.id,
            userId = user.id,
            type = TransactionType.TRANSFER_OUT,
            amount = request.amount,
            balanceAfter = fromWallet.balance,
            counterpartWalletId = toWallet.id
        ))
        return outTx.toResponse()
    }

    fun getTransactions(user: User, page: Int, size: Int): PageResponse<TransactionResponse> {
        val wallet = walletRepository.findByUserId(user.id)
            .orElseThrow { WalletNotFoundException(user.id) }
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        val txPage = transactionRepository.findByWalletId(wallet.id, pageable)
        return PageResponse(
            content = txPage.content.map { it.toResponse() },
            page = txPage.number,
            size = txPage.size,
            totalElements = txPage.totalElements,
            totalPages = txPage.totalPages
        )
    }

    private fun Transaction.toResponse() = TransactionResponse(
        id = id,
        type = type,
        amount = amount,
        balanceAfter = balanceAfter,
        counterpartWalletId = counterpartWallet?.id,
        description = description,
        createdAt = createdAt
    )
}
