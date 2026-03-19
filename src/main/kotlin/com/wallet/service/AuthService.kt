package com.wallet.service

import com.wallet.config.JwtService
import com.wallet.domain.User
import com.wallet.domain.Wallet
import com.wallet.dto.AuthResponse
import com.wallet.dto.LoginRequest
import com.wallet.dto.RegisterRequest
import com.wallet.exception.EmailAlreadyExistsException
import com.wallet.repository.UserRepository
import com.wallet.repository.WalletRepository
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val walletRepository: WalletRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val authenticationManager: AuthenticationManager
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails =
        userRepository.findByEmail(username)
            .orElseThrow { UsernameNotFoundException("Пользователь не найден: $username") }

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw EmailAlreadyExistsException(request.email)
        }
        val user = userRepository.save(
            User(
                email = request.email,
                password = passwordEncoder.encode(request.password),
                fullName = request.fullName
            )
        )
        walletRepository.save(Wallet(user = user))
        return AuthResponse(token = jwtService.generateToken(user))
    }

    fun login(request: LoginRequest): AuthResponse {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.password)
        )
        val user = userRepository.findByEmail(request.email).get()
        return AuthResponse(token = jwtService.generateToken(user))
    }
}
