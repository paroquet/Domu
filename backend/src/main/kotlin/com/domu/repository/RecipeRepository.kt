package com.domu.repository

import com.domu.model.Recipe
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RecipeRepository : JpaRepository<Recipe, Long> {
    fun findByFamily_IdOrderByCreatedAtDesc(familyId: Long): List<Recipe>
    fun findByShareToken(token: String): Recipe?
}
