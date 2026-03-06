package com.domu.unit.service

import com.domu.model.Family
import com.domu.model.FamilyMember
import com.domu.model.FamilyMemberId
import com.domu.model.User
import com.domu.repository.FamilyMemberRepository
import com.domu.service.FamilyAuthService
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

@ExtendWith(MockKExtension::class)
class FamilyAuthServiceTest {

    @MockK
    private lateinit var familyMemberRepository: FamilyMemberRepository

    @InjectMockKs
    private lateinit var familyAuthService: FamilyAuthService

    private fun mockMember(role: String): FamilyMember {
        val family = Family(id = 1L, name = "家庭A", inviteCode = "ABCD1234")
        val user = User(id = 10L, email = "u@test.com", passwordHash = "h", name = "用户A")
        return FamilyMember(id = FamilyMemberId(1L, 10L), family = family, user = user, role = role)
    }

    // ---------- requireMember ----------

    @Test
    fun `requireMember - 存在时返回 FamilyMember`() {
        val member = mockMember("MEMBER")
        every { familyMemberRepository.findByFamily_IdAndUser_Id(1L, 10L) } returns member

        val result = familyAuthService.requireMember(1L, 10L)

        assertThat(result.role).isEqualTo("MEMBER")
    }

    @Test
    fun `requireMember - 不存在时抛出 403 FORBIDDEN`() {
        every { familyMemberRepository.findByFamily_IdAndUser_Id(1L, 99L) } returns null

        val ex = assertThrows<ResponseStatusException> {
            familyAuthService.requireMember(1L, 99L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    // ---------- requireAdmin ----------

    @Test
    fun `requireAdmin - ADMIN 角色时正常返回`() {
        val member = mockMember("ADMIN")
        every { familyMemberRepository.findByFamily_IdAndUser_Id(1L, 10L) } returns member

        val result = familyAuthService.requireAdmin(1L, 10L)

        assertThat(result.role).isEqualTo("ADMIN")
    }

    @Test
    fun `requireAdmin - MEMBER 角色时抛出 403 FORBIDDEN`() {
        val member = mockMember("MEMBER")
        every { familyMemberRepository.findByFamily_IdAndUser_Id(1L, 10L) } returns member

        val ex = assertThrows<ResponseStatusException> {
            familyAuthService.requireAdmin(1L, 10L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    // ---------- isMember ----------

    @Test
    fun `isMember - 存在时返回 true`() {
        every { familyMemberRepository.existsByFamily_IdAndUser_Id(1L, 10L) } returns true
        assertThat(familyAuthService.isMember(1L, 10L)).isTrue()
    }

    @Test
    fun `isMember - 不存在时返回 false`() {
        every { familyMemberRepository.existsByFamily_IdAndUser_Id(1L, 99L) } returns false
        assertThat(familyAuthService.isMember(1L, 99L)).isFalse()
    }
}
