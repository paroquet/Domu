package com.domu.repository

import com.domu.model.CookingRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CookingRecordRepository : JpaRepository<CookingRecord, Long> {
    fun findByFamily_IdOrderByCreatedAtDesc(familyId: Long): List<CookingRecord>
    fun findByFamily_IdAndRecipe_IdOrderByCreatedAtDesc(familyId: Long, recipeId: Long): List<CookingRecord>
}
