package com.domu.controller

import com.domu.dto.UserResponse
import com.domu.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

data class UpdateMeRequest(val name: String, val avatarPath: String? = null)

@RestController
@RequestMapping("/api/v1/users")
class UserController(private val userService: UserService) {

    @GetMapping("/me")
    fun getMe(): ResponseEntity<UserResponse> {
        val userId = getCurrentUserId()
        return ResponseEntity.ok(userService.getMe(userId))
    }

    @PutMapping("/me")
    fun updateMe(@RequestBody request: UpdateMeRequest): ResponseEntity<UserResponse> {
        val userId = getCurrentUserId()
        return ResponseEntity.ok(userService.updateMe(userId, request.name, request.avatarPath))
    }

    private fun getCurrentUserId(): Long {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication.name.toLong()
    }
}
