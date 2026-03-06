package com.domu.controller

import com.domu.config.AppProperties
import com.domu.dto.CreateRecipeRequest
import com.domu.dto.RecipeResponse
import com.domu.dto.ShareResponse
import com.domu.dto.UpdateRecipeRequest
import com.domu.service.RecipeService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/recipes")
class RecipeController(
    private val recipeService: RecipeService,
    private val appProperties: AppProperties
) {

    @GetMapping
    fun listRecipes(@RequestParam familyId: Long): ResponseEntity<List<RecipeResponse>> {
        val userId = getCurrentUserId()
        return ResponseEntity.ok(recipeService.list(familyId, userId))
    }

    @PostMapping
    fun createRecipe(@RequestBody request: CreateRecipeRequest): ResponseEntity<RecipeResponse> {
        val userId = getCurrentUserId()
        return ResponseEntity.status(HttpStatus.CREATED).body(recipeService.create(request, userId))
    }

    @GetMapping("/shared/{token}")
    fun getSharedRecipe(@PathVariable token: String): ResponseEntity<RecipeResponse> {
        return ResponseEntity.ok(recipeService.getByShareToken(token))
    }

    @GetMapping("/{id}")
    fun getRecipe(@PathVariable id: Long): ResponseEntity<RecipeResponse> {
        val userId = getCurrentUserId()
        return ResponseEntity.ok(recipeService.getById(id, userId))
    }

    @PutMapping("/{id}")
    fun updateRecipe(
        @PathVariable id: Long,
        @RequestBody request: UpdateRecipeRequest
    ): ResponseEntity<RecipeResponse> {
        val userId = getCurrentUserId()
        return ResponseEntity.ok(recipeService.update(id, request, userId))
    }

    @DeleteMapping("/{id}")
    fun deleteRecipe(@PathVariable id: Long): ResponseEntity<Void> {
        val userId = getCurrentUserId()
        recipeService.delete(id, userId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/share")
    fun shareRecipe(@PathVariable id: Long): ResponseEntity<ShareResponse> {
        val userId = getCurrentUserId()
        return ResponseEntity.ok(recipeService.share(id, userId, appProperties.baseUrl))
    }

    private fun getCurrentUserId(): Long {
        return SecurityContextHolder.getContext().authentication.name.toLong()
    }
}
