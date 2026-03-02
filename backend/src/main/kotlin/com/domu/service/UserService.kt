package com.domu.service

import com.domu.dto.UserResponse
import com.domu.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class UserService(private val userRepository: UserRepository) {

    fun getMe(userId: Long): UserResponse {
        val user = userRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }
        return UserResponse(
            id = user.id,
            email = user.email,
            name = user.name,
            avatarPath = user.avatarPath
        )
    }

    fun updateMe(userId: Long, name: String, avatarPath: String?): UserResponse {
        val user = userRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }
        user.name = name
        if (avatarPath != null) {
            user.avatarPath = avatarPath
        }
        val saved = userRepository.save(user)
        return UserResponse(
            id = saved.id,
            email = saved.email,
            name = saved.name,
            avatarPath = saved.avatarPath
        )
    }
}
