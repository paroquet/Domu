package com.domu.slice.controller

import com.domu.config.AppConfig
import com.domu.config.SecurityConfig
import com.domu.controller.UserController
import com.domu.dto.UserResponse
import com.domu.model.User
import com.domu.repository.UserRepository
import com.domu.security.JwtAuthenticationFilter
import com.domu.security.JwtTokenProvider
import com.domu.security.UserDetailsServiceImpl
import com.domu.service.UserService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import org.springframework.web.server.ResponseStatusException
import java.util.Optional

@WebMvcTest(controllers = [UserController::class])
@Import(SecurityConfig::class, JwtTokenProvider::class, JwtAuthenticationFilter::class,
        UserDetailsServiceImpl::class, AppConfig::class)
class UserControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockkBean
    private lateinit var userService: UserService

    @MockkBean
    private lateinit var userRepository: UserRepository

    private val userId = 1L
    private lateinit var authCookie: Cookie

    private val authUser = User(id = 1L, email = "user@test.com", passwordHash = "h", name = "测试用户")

    @BeforeEach
    fun setUp() {
        val token = jwtTokenProvider.createAccessToken(userId)
        authCookie = Cookie("access_token", token)
        every { userRepository.findById(userId) } returns Optional.of(authUser)
    }

    private val userResponse = UserResponse(
        id = 1L, email = "user@test.com", name = "测试用户", avatarPath = null
    )

    // ---------- GET /api/v1/users/me ----------

    @Test
    fun `GET users me - 返回当前用户信息`() {
        every { userService.getMe(userId) } returns userResponse

        mockMvc.get("/api/v1/users/me") {
            cookie(authCookie)
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(1) }
            jsonPath("$.email") { value("user@test.com") }
            jsonPath("$.name") { value("测试用户") }
        }
    }

    @Test
    fun `GET users me - 未认证返回 4xx`() {
        mockMvc.get("/api/v1/users/me") {
        }.andExpect {
            status { is4xxClientError() }
        }
    }

    @Test
    fun `GET users me - 用户不存在返回 404`() {
        every { userService.getMe(userId) } throws
            ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

        mockMvc.get("/api/v1/users/me") {
            cookie(authCookie)
        }.andExpect {
            status { isNotFound() }
        }
    }

    // ---------- PUT /api/v1/users/me ----------

    @Test
    fun `PUT users me - 更新用户信息成功`() {
        val updatedResponse = UserResponse(
            id = 1L, email = "user@test.com", name = "新名字", avatarPath = "/avatar.png"
        )
        every { userService.updateMe(userId, "新名字", "/avatar.png") } returns updatedResponse

        mockMvc.put("/api/v1/users/me") {
            cookie(authCookie)
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"新名字","avatarPath":"/avatar.png"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("新名字") }
            jsonPath("$.avatarPath") { value("/avatar.png") }
        }
    }

    @Test
    fun `PUT users me - 仅更新用户名`() {
        val updatedResponse = UserResponse(
            id = 1L, email = "user@test.com", name = "新名字", avatarPath = null
        )
        every { userService.updateMe(userId, "新名字", null) } returns updatedResponse

        mockMvc.put("/api/v1/users/me") {
            cookie(authCookie)
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"新名字"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("新名字") }
        }
    }

    @Test
    fun `PUT users me - 未认证返回 4xx`() {
        mockMvc.put("/api/v1/users/me") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"新名字"}"""
        }.andExpect {
            status { is4xxClientError() }
        }
    }

    @Test
    fun `PUT users me - 用户不存在返回 404`() {
        every { userService.updateMe(userId, "新名字", null) } throws
            ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

        mockMvc.put("/api/v1/users/me") {
            cookie(authCookie)
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"新名字"}"""
        }.andExpect {
            status { isNotFound() }
        }
    }
}
