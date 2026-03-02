package com.domu.service

import com.domu.dto.CreateFamilyRequest
import com.domu.dto.FamilyResponse
import com.domu.dto.InviteCodeResponse
import com.domu.dto.MemberResponse
import com.domu.model.Family
import com.domu.model.FamilyMember
import com.domu.model.FamilyMemberId
import com.domu.repository.FamilyMemberRepository
import com.domu.repository.FamilyRepository
import com.domu.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class FamilyService(
    private val familyRepository: FamilyRepository,
    private val familyMemberRepository: FamilyMemberRepository,
    private val userRepository: UserRepository,
    private val familyAuthService: FamilyAuthService
) {

    fun create(userId: Long, request: CreateFamilyRequest): FamilyResponse {
        val user = userRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }
        val family = familyRepository.save(
            Family(
                name = request.name,
                inviteCode = generateInviteCode(),
                createdAt = Instant.now()
            )
        )
        val memberId = FamilyMemberId(familyId = family.id, userId = user.id)
        val member = FamilyMember(
            id = memberId,
            family = family,
            user = user,
            role = "ADMIN",
            joinedAt = Instant.now()
        )
        familyMemberRepository.save(member)
        return family.toResponse()
    }

    fun getById(id: Long): Family {
        return familyRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Family not found")
        }
    }

    fun regenerateInviteCode(familyId: Long, userId: Long): InviteCodeResponse {
        familyAuthService.requireAdmin(familyId, userId)
        val family = familyRepository.findById(familyId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Family not found")
        }
        family.inviteCode = generateInviteCode()
        familyRepository.save(family)
        return InviteCodeResponse(inviteCode = family.inviteCode)
    }

    fun join(userId: Long, inviteCode: String): FamilyResponse {
        val user = userRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }
        val family = familyRepository.findByInviteCode(inviteCode)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid invite code")

        if (familyMemberRepository.existsByFamily_IdAndUser_Id(family.id, userId)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Already a member of this family")
        }

        val memberId = FamilyMemberId(familyId = family.id, userId = user.id)
        val member = FamilyMember(
            id = memberId,
            family = family,
            user = user,
            role = "MEMBER",
            joinedAt = Instant.now()
        )
        familyMemberRepository.save(member)
        return family.toResponse()
    }

    fun getMembers(familyId: Long, userId: Long): List<MemberResponse> {
        familyAuthService.requireMember(familyId, userId)
        return familyMemberRepository.findByFamily_Id(familyId).map { member ->
            MemberResponse(
                userId = member.user.id,
                name = member.user.name,
                email = member.user.email,
                avatarPath = member.user.avatarPath,
                role = member.role,
                joinedAt = member.joinedAt.toString()
            )
        }
    }

    fun updateMemberRole(familyId: Long, targetUserId: Long, requestingUserId: Long, role: String) {
        familyAuthService.requireAdmin(familyId, requestingUserId)
        if (role != "ADMIN" && role != "MEMBER") {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role. Must be ADMIN or MEMBER")
        }
        val member = familyMemberRepository.findByFamily_IdAndUser_Id(familyId, targetUserId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found")
        member.role = role
        familyMemberRepository.save(member)
    }

    fun removeMember(familyId: Long, targetUserId: Long, requestingUserId: Long) {
        familyAuthService.requireAdmin(familyId, requestingUserId)
        val member = familyMemberRepository.findByFamily_IdAndUser_Id(familyId, targetUserId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found")
        familyMemberRepository.delete(member)
    }

    private fun generateInviteCode(): String =
        UUID.randomUUID().toString().replace("-", "").substring(0, 8).uppercase()

    private fun Family.toResponse() = FamilyResponse(
        id = id,
        name = name,
        inviteCode = inviteCode,
        createdAt = createdAt.toString()
    )
}
