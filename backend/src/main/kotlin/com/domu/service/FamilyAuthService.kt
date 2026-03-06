package com.domu.service

import com.domu.model.FamilyMember
import com.domu.repository.FamilyMemberRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class FamilyAuthService(private val familyMemberRepository: FamilyMemberRepository) {

    fun requireMember(familyId: Long, userId: Long): FamilyMember {
        return familyMemberRepository.findByFamily_IdAndUser_Id(familyId, userId)
            ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this family")
    }

    fun requireAdmin(familyId: Long, userId: Long): FamilyMember {
        val member = requireMember(familyId, userId)
        if (member.role != "ADMIN") throw ResponseStatusException(HttpStatus.FORBIDDEN, "Admin required")
        return member
    }

    fun isMember(familyId: Long, userId: Long): Boolean =
        familyMemberRepository.existsByFamily_IdAndUser_Id(familyId, userId)
}
