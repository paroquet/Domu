package com.domu.unit.service

import com.domu.dto.RegisterRequest
import com.domu.model.User
import com.domu.repository.UserRepository
import com.domu.service.AuthService
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.Optional

@Suppress("NonAsciiCharacters")
@ExtendWith(MockKExtension::class)
class AuthServiceTest {

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var passwordEncoder: PasswordEncoder

    @InjectMockKs
    private lateinit var authService: AuthService

    // ---------- register ----------

    @Test
    fun `register - 邮箱未注册时成功创建用户`() {
        val request = RegisterRequest(email = "test@example.com", password = "pass123", name = "张三")
        val savedUser = User(id = 1L, email = "test@example.com", passwordHash = "hashed", name = "张三")

        every { userRepository.findByEmail("test@example.com") } returns null
        every { passwordEncoder.encode("pass123") } returns "hashed"
        every { userRepository.save(any()) } returns savedUser

        val result = authService.register(request)

        assertThat(result.id).isEqualTo(1L)
        assertThat(result.email).isEqualTo("test@example.com")
        assertThat(result.name).isEqualTo("张三")
        verify(exactly = 1) { userRepository.save(any()) }
    }

    @Test
    fun `register - 邮箱已存在时抛出 409 CONFLICT`() {
        val request = RegisterRequest(email = "exist@example.com", password = "pass123", name = "李四")
        val existingUser = User(id = 2L, email = "exist@example.com", passwordHash = "hashed", name = "李四")

        every { userRepository.findByEmail("exist@example.com") } returns existingUser

        val ex = assertThrows<ResponseStatusException> {
            authService.register(request)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.CONFLICT)
        verify(exactly = 0) { userRepository.save(any()) }
    }

    // ---------- login ----------

    @Test
    fun `login - 邮箱和密码正确时返回 User 实体`() {
        val user = User(id = 1L, email = "test@example.com", passwordHash = "hashed", name = "张三")

        every { userRepository.findByEmail("test@example.com") } returns user
        every { passwordEncoder.matches("pass123", "hashed") } returns true

        val result = authService.login("test@example.com", "pass123")

        assertThat(result.id).isEqualTo(1L)
        assertThat(result.email).isEqualTo("test@example.com")
    }

    @Test
    fun `login - 邮箱不存在时抛出 401 UNAUTHORIZED`() {
        every { userRepository.findByEmail("nobody@example.com") } returns null

        val ex = assertThrows<ResponseStatusException> {
            authService.login("nobody@example.com", "any")
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `login - 密码错误时抛出 401 UNAUTHORIZED`() {
        val user = User(id = 1L, email = "test@example.com", passwordHash = "hashed", name = "张三")

        every { userRepository.findByEmail("test@example.com") } returns user
        every { passwordEncoder.matches("wrong", "hashed") } returns false

        val ex = assertThrows<ResponseStatusException> {
            authService.login("test@example.com", "wrong")
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }
}
