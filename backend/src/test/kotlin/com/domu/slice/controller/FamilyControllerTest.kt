package com.domu.slice.controller

import com.domu.config.AppConfig
import com.domu.config.SecurityConfig
import com.domu.dto.FamilyResponse
import com.domu.dto.InviteCodeResponse
import com.domu.dto.MemberResponse
import com.domu.model.User
import com.domu.repository.UserRepository
import com.domu.security.JwtAuthenticationFilter
import com.domu.security.JwtTokenProvider
import com.domu.security.UserDetailsServiceImpl
import com.domu.service.FamilyService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.web.server.ResponseStatusException
import java.util.Optional

@WebMvcTest(controllers = [com.domu.controller.FamilyController::class])
@Import(SecurityConfig::class, JwtTokenProvider::class, JwtAuthenticationFilter::class,
        UserDetailsServiceImpl::class, AppConfig::class)
class FamilyControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockkBean
    private lateinit var familyService: FamilyService

    @MockkBean
    private lateinit var userRepository: UserRepository

    private val userId = 1L
    private lateinit var authCookie: Cookie

    // 登录用户对应的 User entity，供 UserDetailsServiceImpl 使用
    private val authUser = User(id = 1L, email = "admin@test.com", passwordHash = "h", name = "管理员")

    @BeforeEach
    fun setUp() {
        val token = jwtTokenProvider.createAccessToken(userId)
        authCookie = Cookie("access_token", token)
        every { userRepository.findById(userId) } returns Optional.of(authUser)
    }

    private val familyResponse = FamilyResponse(
        id = 10L, name = "测试家庭", inviteCode = "ABCD1234",
        createdAt = "2026-01-01T00:00:00Z"
    )

    // ---------- POST /api/v1/families ----------

    @Test
    fun `POST families - 创建家庭成功返回 201`() {
        every { familyService.create(userId, any()) } returns familyResponse

        mockMvc.post("/api/v1/families") {
            cookie(authCookie)
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"测试家庭"}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { value(10) }
            jsonPath("$.name") { value("测试家庭") }
            jsonPath("$.inviteCode") { value("ABCD1234") }
        }
    }

    @Test
    fun `POST families - 未认证返回 4xx（SecurityConfig 无 EntryPoint 时为 403）`() {
        // SecurityConfig 未配置 AuthenticationEntryPoint，Spring Security 默认对未认证请求返回 403
        mockMvc.post("/api/v1/families") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"测试家庭"}"""
        }.andExpect {
            status { is4xxClientError() }
        }
    }

    // ---------- GET /api/v1/families/{id} ----------

    @Test
    fun `GET families id - 返回家庭信息`() {
        every { familyService.getById(10L) } returns
            com.domu.model.Family(id = 10L, name = "测试家庭", inviteCode = "ABCD1234")

        mockMvc.get("/api/v1/families/10") {
            cookie(authCookie)
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(10) }
            jsonPath("$.name") { value("测试家庭") }
        }
    }

    // ---------- POST /api/v1/families/{id}/invite-code ----------

    @Test
    fun `POST families id invite-code - 管理员重新生成邀请码`() {
        every { familyService.regenerateInviteCode(10L, userId) } returns
            InviteCodeResponse(inviteCode = "NEWCODE1")

        mockMvc.post("/api/v1/families/10/invite-code") {
            cookie(authCookie)
        }.andExpect {
            status { isOk() }
            jsonPath("$.inviteCode") { value("NEWCODE1") }
        }
    }

    @Test
    fun `POST families id invite-code - 非管理员返回 403`() {
        every { familyService.regenerateInviteCode(10L, userId) } throws
            ResponseStatusException(HttpStatus.FORBIDDEN, "Admin required")

        mockMvc.post("/api/v1/families/10/invite-code") {
            cookie(authCookie)
        }.andExpect {
            status { isForbidden() }
        }
    }

    // ---------- POST /api/v1/families/join ----------

    @Test
    fun `POST families join - 通过邀请码加入家庭`() {
        every { familyService.join(userId, "ABCD1234") } returns familyResponse

        mockMvc.post("/api/v1/families/join") {
            cookie(authCookie)
            contentType = MediaType.APPLICATION_JSON
            content = """{"inviteCode":"ABCD1234"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(10) }
        }
    }

    @Test
    fun `POST families join - 无效邀请码返回 404`() {
        every { familyService.join(userId, "INVALID") } throws
            ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid invite code")

        mockMvc.post("/api/v1/families/join") {
            cookie(authCookie)
            contentType = MediaType.APPLICATION_JSON
            content = """{"inviteCode":"INVALID"}"""
        }.andExpect {
            status { isNotFound() }
        }
    }

    // ---------- GET /api/v1/families/{id}/members ----------

    @Test
    fun `GET families id members - 返回成员列表`() {
        val members = listOf(
            MemberResponse(userId = 1L, name = "管理员", email = "admin@test.com",
                avatarPath = null, role = "ADMIN", joinedAt = "2026-01-01T00:00:00Z"),
            MemberResponse(userId = 2L, name = "成员A", email = "a@test.com",
                avatarPath = null, role = "MEMBER", joinedAt = "2026-01-02T00:00:00Z")
        )
        every { familyService.getMembers(10L, userId) } returns members

        mockMvc.get("/api/v1/families/10/members") {
            cookie(authCookie)
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[0].role") { value("ADMIN") }
            jsonPath("$[1].name") { value("成员A") }
        }
    }

    // ---------- PUT /api/v1/families/{id}/members/{uid}/role ----------

    @Test
    fun `PUT families id members uid role - 修改角色成功返回 204`() {
        justRun { familyService.updateMemberRole(10L, 2L, userId, "ADMIN") }

        mockMvc.put("/api/v1/families/10/members/2/role") {
            cookie(authCookie)
            contentType = MediaType.APPLICATION_JSON
            content = """{"role":"ADMIN"}"""
        }.andExpect {
            status { isNoContent() }
        }
    }

    // ---------- DELETE /api/v1/families/{id}/members/{uid} ----------

    @Test
    fun `DELETE families id members uid - 踢出成员成功返回 204`() {
        justRun { familyService.removeMember(10L, 2L, userId) }

        mockMvc.delete("/api/v1/families/10/members/2") {
            cookie(authCookie)
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `DELETE families id members uid - 非管理员返回 403`() {
        every { familyService.removeMember(10L, 2L, userId) } throws
            ResponseStatusException(HttpStatus.FORBIDDEN, "Admin required")

        mockMvc.delete("/api/v1/families/10/members/2") {
            cookie(authCookie)
        }.andExpect {
            status { isForbidden() }
        }
    }
}
