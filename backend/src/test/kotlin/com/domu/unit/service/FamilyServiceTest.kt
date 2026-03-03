package com.domu.unit.service

import com.domu.dto.CreateFamilyRequest
import com.domu.model.Family
import com.domu.model.FamilyMember
import com.domu.model.FamilyMemberId
import com.domu.model.User
import com.domu.repository.FamilyMemberRepository
import com.domu.repository.FamilyRepository
import com.domu.repository.UserRepository
import com.domu.service.FamilyAuthService
import com.domu.service.FamilyService
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.Optional

@ExtendWith(MockKExtension::class)
class FamilyServiceTest {

    @MockK
    private lateinit var familyRepository: FamilyRepository

    @MockK
    private lateinit var familyMemberRepository: FamilyMemberRepository

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var familyAuthService: FamilyAuthService

    @InjectMockKs
    private lateinit var familyService: FamilyService

    private val testUser = User(id = 1L, email = "admin@test.com", passwordHash = "h", name = "管理员")
    private val testFamily = Family(id = 10L, name = "测试家庭", inviteCode = "ABCD1234")

    // ---------- create ----------

    @Test
    fun `create - 创建者自动成为 ADMIN 成员`() {
        val memberSlot = slot<FamilyMember>()

        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { familyRepository.save(any()) } returns testFamily
        every { familyMemberRepository.save(capture(memberSlot)) } returns mockk()

        val result = familyService.create(1L, CreateFamilyRequest(name = "测试家庭"))

        assertThat(result.id).isEqualTo(10L)
        assertThat(result.name).isEqualTo("测试家庭")
        assertThat(memberSlot.captured.role).isEqualTo("ADMIN")
        assertThat(memberSlot.captured.id.userId).isEqualTo(1L)
    }

    @Test
    fun `create - 用户不存在时抛出 404 NOT_FOUND`() {
        every { userRepository.findById(99L) } returns Optional.empty()

        val ex = assertThrows<ResponseStatusException> {
            familyService.create(99L, CreateFamilyRequest(name = "家庭X"))
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        verify(exactly = 0) { familyRepository.save(any()) }
    }

    // ---------- join ----------

    @Test
    fun `join - 有效邀请码加入家庭，角色为 MEMBER`() {
        val memberSlot = slot<FamilyMember>()

        every { userRepository.findById(2L) } returns Optional.of(
            User(id = 2L, email = "member@test.com", passwordHash = "h", name = "成员")
        )
        every { familyRepository.findByInviteCode("ABCD1234") } returns testFamily
        every { familyMemberRepository.existsByFamily_IdAndUser_Id(10L, 2L) } returns false
        every { familyMemberRepository.save(capture(memberSlot)) } returns mockk()

        val result = familyService.join(2L, "ABCD1234")

        assertThat(result.id).isEqualTo(10L)
        assertThat(memberSlot.captured.role).isEqualTo("MEMBER")
    }

    @Test
    fun `join - 无效邀请码时抛出 404 NOT_FOUND`() {
        every { userRepository.findById(2L) } returns Optional.of(
            User(id = 2L, email = "m@test.com", passwordHash = "h", name = "成员")
        )
        every { familyRepository.findByInviteCode("INVALID") } returns null

        val ex = assertThrows<ResponseStatusException> {
            familyService.join(2L, "INVALID")
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `join - 已是家庭成员时抛出 409 CONFLICT`() {
        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { familyRepository.findByInviteCode("ABCD1234") } returns testFamily
        every { familyMemberRepository.existsByFamily_IdAndUser_Id(10L, 1L) } returns true

        val ex = assertThrows<ResponseStatusException> {
            familyService.join(1L, "ABCD1234")
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.CONFLICT)
        verify(exactly = 0) { familyMemberRepository.save(any()) }
    }

    // ---------- getMembers ----------

    @Test
    fun `getMembers - 非成员调用时 requireMember 抛出 403`() {
        every { familyAuthService.requireMember(10L, 99L) } throws
            ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this family")

        val ex = assertThrows<ResponseStatusException> {
            familyService.getMembers(10L, 99L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `getMembers - 成员可以获取成员列表`() {
        val member = FamilyMember(
            id = FamilyMemberId(10L, 1L),
            family = testFamily,
            user = testUser,
            role = "ADMIN"
        )
        every { familyAuthService.requireMember(10L, 1L) } returns member
        every { familyMemberRepository.findByFamily_Id(10L) } returns listOf(member)

        val result = familyService.getMembers(10L, 1L)

        assertThat(result).hasSize(1)
        assertThat(result[0].role).isEqualTo("ADMIN")
        assertThat(result[0].userId).isEqualTo(1L)
    }

    // ---------- updateMemberRole ----------

    @Test
    fun `updateMemberRole - 非法角色值时抛出 400 BAD_REQUEST`() {
        val adminMember = FamilyMember(
            id = FamilyMemberId(10L, 1L), family = testFamily, user = testUser, role = "ADMIN"
        )
        every { familyAuthService.requireAdmin(10L, 1L) } returns adminMember

        val ex = assertThrows<ResponseStatusException> {
            familyService.updateMemberRole(10L, 2L, 1L, "SUPERUSER")
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `updateMemberRole - 目标成员不存在时抛出 404 NOT_FOUND`() {
        val adminMember = FamilyMember(
            id = FamilyMemberId(10L, 1L), family = testFamily, user = testUser, role = "ADMIN"
        )
        every { familyAuthService.requireAdmin(10L, 1L) } returns adminMember
        every { familyMemberRepository.findByFamily_IdAndUser_Id(10L, 99L) } returns null

        val ex = assertThrows<ResponseStatusException> {
            familyService.updateMemberRole(10L, 99L, 1L, "MEMBER")
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    // ---------- removeMember ----------

    @Test
    fun `removeMember - 成功删除目标成员`() {
        val adminMember = FamilyMember(
            id = FamilyMemberId(10L, 1L), family = testFamily, user = testUser, role = "ADMIN"
        )
        val targetMember = FamilyMember(
            id = FamilyMemberId(10L, 2L),
            family = testFamily,
            user = User(id = 2L, email = "m@test.com", passwordHash = "h", name = "成员"),
            role = "MEMBER"
        )
        every { familyAuthService.requireAdmin(10L, 1L) } returns adminMember
        every { familyMemberRepository.findByFamily_IdAndUser_Id(10L, 2L) } returns targetMember
        every { familyMemberRepository.delete(targetMember) } returns Unit

        familyService.removeMember(10L, 2L, 1L)

        verify(exactly = 1) { familyMemberRepository.delete(targetMember) }
    }

    @Test
    fun `removeMember - 目标成员不存在时抛出 404 NOT_FOUND`() {
        val adminMember = FamilyMember(
            id = FamilyMemberId(10L, 1L), family = testFamily, user = testUser, role = "ADMIN"
        )
        every { familyAuthService.requireAdmin(10L, 1L) } returns adminMember
        every { familyMemberRepository.findByFamily_IdAndUser_Id(10L, 99L) } returns null

        val ex = assertThrows<ResponseStatusException> {
            familyService.removeMember(10L, 99L, 1L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        verify(exactly = 0) { familyMemberRepository.delete(any()) }
    }
}
