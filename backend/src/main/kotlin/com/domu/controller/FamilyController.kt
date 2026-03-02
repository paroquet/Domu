package com.domu.controller

import com.domu.dto.*
import com.domu.service.FamilyService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/families")
class FamilyController(private val familyService: FamilyService) {

    @PostMapping
    fun createFamily(@RequestBody request: CreateFamilyRequest): ResponseEntity<FamilyResponse> {
        val userId = getCurrentUserId()
        return ResponseEntity.status(HttpStatus.CREATED).body(familyService.create(userId, request))
    }

    @GetMapping("/{id}")
    fun getFamily(@PathVariable id: Long): ResponseEntity<FamilyResponse> {
        val userId = getCurrentUserId()
        val family = familyService.getById(id)
        return ResponseEntity.ok(
            FamilyResponse(
                id = family.id,
                name = family.name,
                inviteCode = family.inviteCode,
                createdAt = family.createdAt.toString()
            )
        )
    }

    @PostMapping("/{id}/invite-code")
    fun regenerateInviteCode(@PathVariable id: Long): ResponseEntity<InviteCodeResponse> {
        val userId = getCurrentUserId()
        return ResponseEntity.ok(familyService.regenerateInviteCode(id, userId))
    }

    @PostMapping("/join")
    fun joinFamily(@RequestBody request: JoinFamilyRequest): ResponseEntity<FamilyResponse> {
        val userId = getCurrentUserId()
        return ResponseEntity.ok(familyService.join(userId, request.inviteCode))
    }

    @GetMapping("/{id}/members")
    fun getMembers(@PathVariable id: Long): ResponseEntity<List<MemberResponse>> {
        val userId = getCurrentUserId()
        return ResponseEntity.ok(familyService.getMembers(id, userId))
    }

    @PutMapping("/{id}/members/{uid}/role")
    fun updateMemberRole(
        @PathVariable id: Long,
        @PathVariable uid: Long,
        @RequestBody request: UpdateRoleRequest
    ): ResponseEntity<Void> {
        val userId = getCurrentUserId()
        familyService.updateMemberRole(id, uid, userId, request.role)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{id}/members/{uid}")
    fun removeMember(
        @PathVariable id: Long,
        @PathVariable uid: Long
    ): ResponseEntity<Void> {
        val userId = getCurrentUserId()
        familyService.removeMember(id, uid, userId)
        return ResponseEntity.noContent().build()
    }

    private fun getCurrentUserId(): Long {
        return SecurityContextHolder.getContext().authentication.name.toLong()
    }
}
