package com.domu.dto

data class IngredientDto(
    val name: String,
    val amount: String,
    val unit: String
)

data class StepDto(
    val order: Int,
    val description: String,
    val imagePath: String? = null
)

data class CreateRecipeRequest(
    val title: String,
    val description: String? = null,
    val ingredients: List<IngredientDto>,
    val steps: List<StepDto>,
    val coverImagePath: String? = null,
    val familyId: Long
)

data class UpdateRecipeRequest(
    val title: String,
    val description: String? = null,
    val ingredients: List<IngredientDto>,
    val steps: List<StepDto>,
    val coverImagePath: String? = null
)

data class RecipeResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val ingredients: List<IngredientDto>,
    val steps: List<StepDto>,
    val coverImagePath: String?,
    val authorId: Long,
    val authorName: String,
    val familyId: Long,
    val shareToken: String?,
    val shareUrl: String?,
    val createdAt: String,
    val updatedAt: String
)

data class ShareResponse(
    val shareToken: String,
    val shareUrl: String
)
