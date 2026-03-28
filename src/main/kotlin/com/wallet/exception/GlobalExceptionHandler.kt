package com.wallet.exception

import org.springframework.http.*
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(EmailAlreadyExistsException::class)
    fun handleEmailExists(ex: EmailAlreadyExistsException): ProblemDetail =
        problem(HttpStatus.CONFLICT, "Email уже занят", ex.message)

    @ExceptionHandler(WalletNotFoundException::class)
    fun handleWalletNotFound(ex: WalletNotFoundException): ProblemDetail =
        problem(HttpStatus.NOT_FOUND, "Кошелёк не найден", ex.message)

    @ExceptionHandler(InsufficientFundsException::class)
    fun handleInsufficientFunds(ex: InsufficientFundsException): ProblemDetail =
        problem(HttpStatus.UNPROCESSABLE_ENTITY, "Недостаточно средств", ex.message)

    @ExceptionHandler(SameWalletTransferException::class)
    fun handleSameWallet(ex: SameWalletTransferException): ProblemDetail =
        problem(HttpStatus.BAD_REQUEST, "Некорректный перевод", ex.message)

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(ex: BadCredentialsException): ProblemDetail =
        problem(HttpStatus.UNAUTHORIZED, "Неверные учётные данные", "Проверьте email и пароль")

    @ExceptionHandler(ObjectOptimisticLockingFailureException::class)
    fun handleOptimisticLock(ex: ObjectOptimisticLockingFailureException): ProblemDetail =
        problem(HttpStatus.CONFLICT, "Конфликт данных", "Операция выполняется, попробуйте позже")

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any> {
        val errors = ex.bindingResult.allErrors.associate {
            (if (it is FieldError) it.field else it.objectName) to (it.defaultMessage ?: "invalid")
        }
        val pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Ошибка валидации")
        pd.title = "Validation Failed"
        pd.setProperty("errors", errors)
        return ResponseEntity(pd, HttpStatus.BAD_REQUEST)
    }

    private fun problem(status: HttpStatus, title: String, detail: String?): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail ?: title).also { it.title = title }
}
