package com.domu.controller

import com.domu.dto.CookingRecordResponse
import com.domu.dto.CreateCookingRecordRequest
import com.domu.dto.UpdateCookingRecordRequest
import com.domu.service.CookingRecordService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/cooking-records")
class CookingRecordController(private val cookingRecordService: CookingRecordService) {

    @GetMapping
    fun listRecords(
        @RequestParam familyId: Long,
        @RequestParam(required = false) recipeId: Long?
    ): ResponseEntity<List<CookingRecordResponse>> {
        val userId = getCurrentUserId()
        return ResponseEntity.ok(cookingRecordService.list(familyId, recipeId, userId))
    }

    @PostMapping
    fun createRecord(@RequestBody request: CreateCookingRecordRequest): ResponseEntity<CookingRecordResponse> {
        val userId = getCurrentUserId()
        return ResponseEntity.status(HttpStatus.CREATED).body(cookingRecordService.create(request, userId))
    }

    @GetMapping("/{id}")
    fun getRecord(@PathVariable id: Long): ResponseEntity<CookingRecordResponse> {
        val userId = getCurrentUserId()
        return ResponseEntity.ok(cookingRecordService.getById(id, userId))
    }

    @PutMapping("/{id}")
    fun updateRecord(
        @PathVariable id: Long,
        @RequestBody request: UpdateCookingRecordRequest
    ): ResponseEntity<CookingRecordResponse> {
        val userId = getCurrentUserId()
        return ResponseEntity.ok(cookingRecordService.update(id, request, userId))
    }

    @DeleteMapping("/{id}")
    fun deleteRecord(@PathVariable id: Long): ResponseEntity<Void> {
        val userId = getCurrentUserId()
        cookingRecordService.delete(id, userId)
        return ResponseEntity.noContent().build()
    }

    private fun getCurrentUserId(): Long {
        return SecurityContextHolder.getContext().authentication.name.toLong()
    }
}
