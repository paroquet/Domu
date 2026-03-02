package com.domu.service

import com.domu.dto.RegisterRequest
import com.domu.dto.UserResponse
import com.domu.model.User
import com.domu.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    fun register(request: RegisterRequest): UserResponse {
        if (userRepository.findByEmail(request.email) != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Email already in use")
        }
        val user = User(
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password),
            name = request.name,
            createdAt = Instant.now()
        )
        val saved = userRepository.save(user)
        return saved.toResponse()
    }

    fun login(email: String, password: String): User {
        val user = userRepository.findByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password")
        if (!passwordEncoder.matches(password, user.passwordHash)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password")
        }
        return user
    }

    private fun User.toResponse() = UserResponse(
        id = id,
        email = email,
        name = name,
        avatarPath = avatarPath
    )
}
