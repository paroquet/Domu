package com.domu.unit.service

import com.domu.model.User
import com.domu.repository.UserRepository
import com.domu.service.UserService
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
import org.springframework.web.server.ResponseStatusException
import java.util.Optional

@Suppress("NonAsciiCharacters")
@ExtendWith(MockKExtension::class)
class UserServiceTest {

    @MockK
    private lateinit var userRepository: UserRepository

    @InjectMockKs
    private lateinit var userService: UserService

    // ---------- getMe ----------

    @Test
    fun `getMe - 用户存在时返回用户信息`() {
        val user = User(id = 1L, email = "test@example.com", passwordHash = "hashed", name = "张三", avatarPath = "/avatar.png")

        every { userRepository.findById(1L) } returns Optional.of(user)

        val result = userService.getMe(1L)

        assertThat(result.id).isEqualTo(1L)
        assertThat(result.email).isEqualTo("test@example.com")
        assertThat(result.name).isEqualTo("张三")
        assertThat(result.avatarPath).isEqualTo("/avatar.png")
    }

    @Test
    fun `getMe - 用户不存在时抛出 404 NOT_FOUND`() {
        every { userRepository.findById(999L) } returns Optional.empty()

        val ex = assertThrows<ResponseStatusException> {
            userService.getMe(999L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    // ---------- updateMe ----------

    @Test
    fun `updateMe - 更新用户名和头像成功`() {
        val user = User(id = 1L, email = "test@example.com", passwordHash = "hashed", name = "原名", avatarPath = null)
        val updatedUser = User(id = 1L, email = "test@example.com", passwordHash = "hashed", name = "新名", avatarPath = "/new-avatar.png")

        every { userRepository.findById(1L) } returns Optional.of(user)
        every { userRepository.save(any()) } returns updatedUser

        val result = userService.updateMe(1L, "新名", "/new-avatar.png")

        assertThat(result.name).isEqualTo("新名")
        assertThat(result.avatarPath).isEqualTo("/new-avatar.png")
        verify(exactly = 1) { userRepository.save(any()) }
    }

    @Test
    fun `updateMe - 仅更新用户名，头像保持不变`() {
        val user = User(id = 1L, email = "test@example.com", passwordHash = "hashed", name = "原名", avatarPath = "/old-avatar.png")
        val savedUser = User(id = 1L, email = "test@example.com", passwordHash = "hashed", name = "新名", avatarPath = "/old-avatar.png")

        every { userRepository.findById(1L) } returns Optional.of(user)
        every { userRepository.save(any()) } returns savedUser

        val result = userService.updateMe(1L, "新名", null)

        assertThat(result.name).isEqualTo("新名")
        assertThat(result.avatarPath).isEqualTo("/old-avatar.png")
    }

    @Test
    fun `updateMe - 用户不存在时抛出 404 NOT_FOUND`() {
        every { userRepository.findById(999L) } returns Optional.empty()

        val ex = assertThrows<ResponseStatusException> {
            userService.updateMe(999L, "新名", null)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        verify(exactly = 0) { userRepository.save(any()) }
    }
}
