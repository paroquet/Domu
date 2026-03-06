package com.domu.dto

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class FamilySummaryResponse(
    val id: Long,
    val name: String
)

data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val avatarPath: String?,
    val families: List<FamilySummaryResponse> = emptyList()
)

data class TokenResponse(
    val user: UserResponse
)
