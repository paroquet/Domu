package com.domu.slice.controller

import com.domu.config.AppConfig
import com.domu.config.SecurityConfig
import com.domu.dto.RegisterRequest
import com.domu.dto.LoginRequest
import com.domu.dto.UserResponse
import com.domu.model.User
import com.domu.repository.UserRepository
import com.domu.security.JwtAuthenticationFilter
import com.domu.security.JwtTokenProvider
import com.domu.security.UserDetailsServiceImpl
import com.domu.service.AuthService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.web.server.ResponseStatusException
import java.util.Optional

@WebMvcTest(controllers = [com.domu.controller.AuthController::class])
@Import(SecurityConfig::class, JwtTokenProvider::class, JwtAuthenticationFilter::class,
        UserDetailsServiceImpl::class, AppConfig::class)
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var authService: AuthService

    // UserDetailsServiceImpl 依赖 UserRepository，需要 mock
    @MockkBean
    private lateinit var userRepository: UserRepository

    // ---------- POST /api/v1/auth/register ----------

    @Test
    fun `POST register - 注册成功返回 201 并携带 JWT Cookie`() {
        val userResponse = UserResponse(id = 1L, email = "new@test.com", name = "张三", avatarPath = null)
        every { authService.register(any()) } returns userResponse

        mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"new@test.com","password":"pass123","name":"张三"}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { value(1) }
            jsonPath("$.email") { value("new@test.com") }
            header { exists("Set-Cookie") }
        }
    }

    @Test
    fun `POST register - 邮箱已存在返回 409`() {
        every { authService.register(any()) } throws
            ResponseStatusException(HttpStatus.CONFLICT, "Email already in use")

        mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"exist@test.com","password":"pass123","name":"李四"}"""
        }.andExpect {
            status { isConflict() }
        }
    }

    // ---------- POST /api/v1/auth/login ----------

    @Test
    fun `POST login - 登录成功返回 200 并携带 JWT Cookie`() {
        val user = User(id = 1L, email = "test@test.com", passwordHash = "h", name = "张三")
        every { authService.login("test@test.com", "pass123") } returns user

        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"test@test.com","password":"pass123"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.email") { value("test@test.com") }
            header { exists("Set-Cookie") }
        }
    }

    @Test
    fun `POST login - 凭据错误返回 401`() {
        every { authService.login("test@test.com", "wrong") } throws
            ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password")

        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"test@test.com","password":"wrong"}"""
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    // ---------- POST /api/v1/auth/logout ----------

    @Test
    fun `POST logout - 返回 204 并清除 Cookie`() {
        mockMvc.post("/api/v1/auth/logout").andExpect {
            status { isNoContent() }
            // 清除 Cookie：Max-Age=0
            header { string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")) }
        }
    }
}
