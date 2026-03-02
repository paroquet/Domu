package com.domu.service

import com.domu.dto.CreateRecipeRequest
import com.domu.dto.IngredientDto
import com.domu.dto.RecipeResponse
import com.domu.dto.ShareResponse
import com.domu.dto.StepDto
import com.domu.dto.UpdateRecipeRequest
import com.domu.model.Recipe
import com.domu.repository.FamilyRepository
import com.domu.repository.RecipeRepository
import com.domu.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class RecipeService(
    private val recipeRepository: RecipeRepository,
    private val userRepository: UserRepository,
    private val familyRepository: FamilyRepository,
    private val familyAuthService: FamilyAuthService,
    private val objectMapper: ObjectMapper
) {

    fun list(familyId: Long, userId: Long): List<RecipeResponse> {
        familyAuthService.requireMember(familyId, userId)
        return recipeRepository.findByFamily_IdOrderByCreatedAtDesc(familyId)
            .map { it.toResponse() }
    }

    fun create(request: CreateRecipeRequest, userId: Long): RecipeResponse {
        familyAuthService.requireMember(request.familyId, userId)
        val user = userRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }
        val family = familyRepository.findById(request.familyId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Family not found")
        }
        val now = Instant.now()
        val recipe = Recipe(
            title = request.title,
            description = request.description,
            ingredients = objectMapper.writeValueAsString(request.ingredients),
            steps = objectMapper.writeValueAsString(request.steps),
            coverImagePath = request.coverImagePath,
            author = user,
            family = family,
            createdAt = now,
            updatedAt = now
        )
        return recipeRepository.save(recipe).toResponse()
    }

    fun getById(id: Long, userId: Long): RecipeResponse {
        val recipe = findRecipeById(id)
        familyAuthService.requireMember(recipe.family.id, userId)
        return recipe.toResponse()
    }

    fun update(id: Long, request: UpdateRecipeRequest, userId: Long): RecipeResponse {
        val recipe = findRecipeById(id)
        familyAuthService.requireMember(recipe.family.id, userId)
        recipe.title = request.title
        recipe.description = request.description
        recipe.ingredients = objectMapper.writeValueAsString(request.ingredients)
        recipe.steps = objectMapper.writeValueAsString(request.steps)
        recipe.coverImagePath = request.coverImagePath
        recipe.updatedAt = Instant.now()
        return recipeRepository.save(recipe).toResponse()
    }

    fun delete(id: Long, userId: Long) {
        val recipe = findRecipeById(id)
        familyAuthService.requireMember(recipe.family.id, userId)
        recipeRepository.delete(recipe)
    }

    fun share(id: Long, userId: Long, baseUrl: String): ShareResponse {
        val recipe = findRecipeById(id)
        familyAuthService.requireMember(recipe.family.id, userId)
        val token = recipe.shareToken ?: UUID.randomUUID().toString().also {
            recipe.shareToken = it
            recipeRepository.save(recipe)
        }
        val shareUrl = "$baseUrl/api/v1/recipes/shared/$token"
        return ShareResponse(shareToken = token, shareUrl = shareUrl)
    }

    fun getByShareToken(token: String): RecipeResponse {
        val recipe = recipeRepository.findByShareToken(token)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found")
        return recipe.toResponse()
    }

    private fun findRecipeById(id: Long): Recipe {
        return recipeRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found")
        }
    }

    private fun Recipe.toResponse(): RecipeResponse {
        val ingredientsList: List<IngredientDto> = try {
            objectMapper.readValue(ingredients)
        } catch (e: Exception) {
            emptyList()
        }
        val stepsList: List<StepDto> = try {
            objectMapper.readValue(steps)
        } catch (e: Exception) {
            emptyList()
        }
        val token = shareToken
        return RecipeResponse(
            id = id,
            title = title,
            description = description,
            ingredients = ingredientsList,
            steps = stepsList,
            coverImagePath = coverImagePath,
            authorId = author.id,
            authorName = author.name,
            familyId = family.id,
            shareToken = token,
            shareUrl = token?.let { "/api/v1/recipes/shared/$it" },
            createdAt = createdAt.toString(),
            updatedAt = updatedAt.toString()
        )
    }
}
