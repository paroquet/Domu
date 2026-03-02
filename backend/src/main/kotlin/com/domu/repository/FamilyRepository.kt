package com.domu.repository

import com.domu.model.Family
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FamilyRepository : JpaRepository<Family, Long> {
    fun findByInviteCode(code: String): Family?
}
