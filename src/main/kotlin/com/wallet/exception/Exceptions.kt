package com.wallet.exception

import java.math.BigDecimal
import java.util.UUID

class EmailAlreadyExistsException(email: String) :
    RuntimeException("Email уже зарегистрирован: $email")

class WalletNotFoundException(id: UUID) :
    RuntimeException("Кошелёк не найден: $id")

class InsufficientFundsException(balance: BigDecimal, requested: BigDecimal) :
    RuntimeException("Недостаточно средств. Баланс: $balance, запрошено: $requested")

class SameWalletTransferException :
    RuntimeException("Нельзя переводить на тот же кошелёк")
