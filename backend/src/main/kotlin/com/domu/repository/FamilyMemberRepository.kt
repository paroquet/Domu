package com.domu.repository

import com.domu.model.FamilyMember
import com.domu.model.FamilyMemberId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface FamilyMemberRepository : JpaRepository<FamilyMember, FamilyMemberId> {
    fun findByFamily_Id(familyId: Long): List<FamilyMember>
    fun findByUser_Id(userId: Long): List<FamilyMember>
    fun findByFamily_IdAndUser_Id(familyId: Long, userId: Long): FamilyMember?
    fun existsByFamily_IdAndUser_Id(familyId: Long, userId: Long): Boolean

    @Query("SELECT fm FROM FamilyMember fm JOIN FETCH fm.family WHERE fm.user.id = :userId")
    fun findByUser_IdWithFamily(userId: Long): List<FamilyMember>
}
