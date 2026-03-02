package com.domu.dto

data class CreateCookingRecordRequest(
    val recipeId: Long,
    val familyId: Long,
    val cookedAt: String,
    val notes: String? = null,
    val images: List<String> = emptyList()
)

data class UpdateCookingRecordRequest(
    val cookedAt: String,
    val notes: String? = null,
    val images: List<String> = emptyList()
)

data class CookingRecordResponse(
    val id: Long,
    val recipeId: Long,
    val recipeTitle: String,
    val userId: Long,
    val userName: String,
    val familyId: Long,
    val cookedAt: String,
    val notes: String?,
    val images: List<String>,
    val createdAt: String
)
