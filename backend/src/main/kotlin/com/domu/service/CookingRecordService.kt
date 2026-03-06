package com.domu.service

import com.domu.dto.CookingRecordResponse
import com.domu.dto.CreateCookingRecordRequest
import com.domu.dto.UpdateCookingRecordRequest
import com.domu.model.CookingRecord
import com.domu.repository.CookingRecordRepository
import com.domu.repository.FamilyRepository
import com.domu.repository.RecipeRepository
import com.domu.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@Service
@Transactional
class CookingRecordService(
    private val cookingRecordRepository: CookingRecordRepository,
    private val recipeRepository: RecipeRepository,
    private val userRepository: UserRepository,
    private val familyRepository: FamilyRepository,
    private val familyAuthService: FamilyAuthService,
    private val objectMapper: ObjectMapper
) {

    fun list(familyId: Long, recipeId: Long?, userId: Long): List<CookingRecordResponse> {
        familyAuthService.requireMember(familyId, userId)
        val records = if (recipeId != null) {
            cookingRecordRepository.findByFamily_IdAndRecipe_IdOrderByCreatedAtDesc(familyId, recipeId)
        } else {
            cookingRecordRepository.findByFamily_IdOrderByCreatedAtDesc(familyId)
        }
        return records.map { it.toResponse() }
    }

    fun create(request: CreateCookingRecordRequest, userId: Long): CookingRecordResponse {
        familyAuthService.requireMember(request.familyId, userId)
        val user = userRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }
        val recipe = recipeRepository.findById(request.recipeId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found")
        }
        val family = familyRepository.findById(request.familyId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Family not found")
        }
        val record = CookingRecord(
            recipe = recipe,
            user = user,
            family = family,
            cookedAt = Instant.parse(request.cookedAt),
            notes = request.notes,
            images = objectMapper.writeValueAsString(request.images),
            createdAt = Instant.now()
        )
        return cookingRecordRepository.save(record).toResponse()
    }

    fun getById(id: Long, userId: Long): CookingRecordResponse {
        val record = findById(id)
        familyAuthService.requireMember(record.family.id, userId)
        return record.toResponse()
    }

    fun update(id: Long, request: UpdateCookingRecordRequest, userId: Long): CookingRecordResponse {
        val record = findById(id)
        familyAuthService.requireMember(record.family.id, userId)
        record.cookedAt = Instant.parse(request.cookedAt)
        record.notes = request.notes
        record.images = objectMapper.writeValueAsString(request.images)
        return cookingRecordRepository.save(record).toResponse()
    }

    fun delete(id: Long, userId: Long) {
        val record = findById(id)
        familyAuthService.requireMember(record.family.id, userId)
        cookingRecordRepository.delete(record)
    }

    private fun findById(id: Long): CookingRecord {
        return cookingRecordRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Cooking record not found")
        }
    }

    private fun CookingRecord.toResponse(): CookingRecordResponse {
        val imagesList: List<String> = try {
            objectMapper.readValue(images)
        } catch (e: Exception) {
            emptyList()
        }
        return CookingRecordResponse(
            id = id,
            recipeId = recipe.id,
            recipeTitle = recipe.title,
            userId = user.id,
            userName = user.name,
            familyId = family.id,
            cookedAt = cookedAt.toString(),
            notes = notes,
            images = imagesList,
            createdAt = createdAt.toString()
        )
    }
}
