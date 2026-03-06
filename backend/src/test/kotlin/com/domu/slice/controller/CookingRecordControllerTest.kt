package com.domu.slice.controller

import com.domu.config.AppConfig
import com.domu.config.SecurityConfig
import com.domu.controller.CookingRecordController
import com.domu.dto.CookingRecordResponse
import com.domu.model.User
import com.domu.repository.UserRepository
import com.domu.security.JwtAuthenticationFilter
import com.domu.security.JwtTokenProvider
import com.domu.security.UserDetailsServiceImpl
import com.domu.service.CookingRecordService
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
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.web.server.ResponseStatusException
import java.util.Optional

@WebMvcTest(controllers = [CookingRecordController::class])
@Import(SecurityConfig::class, JwtTokenProvider::class, JwtAuthenticationFilter::class,
        UserDetailsServiceImpl::class, AppConfig::class)
@TestPropertySource(properties = [
    "app.jwt.secret=test-secret-key-that-is-at-least-32-chars-long",
    "app.base-url=http://localhost:8080",
    "app.upload-dir=/tmp/test-uploads"
])
class CookingRecordControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockkBean
    private lateinit var cookingRecordService: CookingRecordService

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

    private val recordResponse = CookingRecordResponse(
        id = 1L,
        recipeId = 100L,
        recipeTitle = "红烧肉",
        userId = 1L,
        userName = "测试用户",
        familyId = 10L,
        cookedAt = "2026-01-01T12:00:00Z",
        notes = "这次做得很好",
        images = listOf("/img1.jpg"),
        createdAt = "2026-01-01T12:30:00Z"
    )

    // ---------- GET /api/v1/cooking-records ----------

    @Test
    fun `GET cooking-records - 列出家庭做菜记录`() {
        every { cookingRecordService.list(10L, null, userId) } returns listOf(recordResponse)

        mockMvc.get("/api/v1/cooking-records?familyId=10") {
            cookie(authCookie)
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].recipeTitle") { value("红烧肉") }
            jsonPath("$[0].notes") { value("这次做得很好") }
        }
    }

    @Test
    fun `GET cooking-records - 按菜谱筛选`() {
        every { cookingRecordService.list(10L, 100L, userId) } returns listOf(recordResponse)

        mockMvc.get("/api/v1/cooking-records?familyId=10&recipeId=100") {
            cookie(authCookie)
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].recipeId") { value(100) }
        }
    }

    @Test
    fun `GET cooking-records - 未认证返回 4xx`() {
        mockMvc.get("/api/v1/cooking-records?familyId=10") {
        }.andExpect {
            status { is4xxClientError() }
        }
    }

    @Test
    fun `GET cooking-records - 非成员返回 403`() {
        every { cookingRecordService.list(10L, null, userId) } throws
            ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member")

        mockMvc.get("/api/v1/cooking-records?familyId=10") {
            cookie(authCookie)
        }.andExpect {
            status { isForbidden() }
        }
    }

    // ---------- POST /api/v1/cooking-records ----------

    @Test
    fun `POST cooking-records - 创建记录成功返回 201`() {
        every { cookingRecordService.create(any(), userId) } returns recordResponse

        mockMvc.post("/api/v1/cooking-records") {
            cookie(authCookie)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "recipeId": 100,
                    "familyId": 10,
                    "cookedAt": "2026-01-01T12:00:00Z",
                    "notes": "这次做得很好",
                    "images": ["/img1.jpg"]
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.recipeId") { value(100) }
            jsonPath("$.notes") { value("这次做得很好") }
        }
    }

    @Test
    fun `POST cooking-records - 未认证返回 4xx`() {
        mockMvc.post("/api/v1/cooking-records") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"recipeId": 100, "familyId": 10, "cookedAt": "2026-01-01T12:00:00Z"}"""
        }.andExpect {
            status { is4xxClientError() }
        }
    }

    @Test
    fun `POST cooking-records - 菜谱不存在返回 404`() {
        every { cookingRecordService.create(any(), userId) } throws
            ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found")

        mockMvc.post("/api/v1/cooking-records") {
            cookie(authCookie)
            contentType = MediaType.APPLICATION_JSON
            content = """{"recipeId": 999, "familyId": 10, "cookedAt": "2026-01-01T12:00:00Z"}"""
        }.andExpect {
            status { isNotFound() }
        }
    }

    // ---------- GET /api/v1/cooking-records/{id} ----------

    @Test
    fun `GET cooking-records id - 获取记录详情`() {
        every { cookingRecordService.getById(1L, userId) } returns recordResponse

        mockMvc.get("/api/v1/cooking-records/1") {
            cookie(authCookie)
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(1) }
            jsonPath("$.recipeTitle") { value("红烧肉") }
            jsonPath("$.userName") { value("测试用户") }
            jsonPath("$.images.length()") { value(1) }
        }
    }

    @Test
    fun `GET cooking-records id - 记录不存在返回 404`() {
        every { cookingRecordService.getById(999L, userId) } throws
            ResponseStatusException(HttpStatus.NOT_FOUND, "Cooking record not found")

        mockMvc.get("/api/v1/cooking-records/999") {
            cookie(authCookie)
        }.andExpect {
            status { isNotFound() }
        }
    }

    // ---------- PUT /api/v1/cooking-records/{id} ----------

    @Test
    fun `PUT cooking-records id - 更新记录成功`() {
        val updatedResponse = recordResponse.copy(notes = "更新后的心得")
        every { cookingRecordService.update(1L, any(), userId) } returns updatedResponse

        mockMvc.put("/api/v1/cooking-records/1") {
            cookie(authCookie)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "cookedAt": "2026-01-02T15:00:00Z",
                    "notes": "更新后的心得",
                    "images": []
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.notes") { value("更新后的心得") }
        }
    }

    @Test
    fun `PUT cooking-records id - 非成员返回 403`() {
        every { cookingRecordService.update(1L, any(), userId) } throws
            ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member")

        mockMvc.put("/api/v1/cooking-records/1") {
            cookie(authCookie)
            contentType = MediaType.APPLICATION_JSON
            content = """{"cookedAt": "2026-01-02T15:00:00Z"}"""
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `PUT cooking-records id - 记录不存在返回 404`() {
        every { cookingRecordService.update(999L, any(), userId) } throws
            ResponseStatusException(HttpStatus.NOT_FOUND, "Cooking record not found")

        mockMvc.put("/api/v1/cooking-records/999") {
            cookie(authCookie)
            contentType = MediaType.APPLICATION_JSON
            content = """{"cookedAt": "2026-01-02T15:00:00Z"}"""
        }.andExpect {
            status { isNotFound() }
        }
    }

    // ---------- DELETE /api/v1/cooking-records/{id} ----------

    @Test
    fun `DELETE cooking-records id - 删除记录成功返回 204`() {
        justRun { cookingRecordService.delete(1L, userId) }

        mockMvc.delete("/api/v1/cooking-records/1") {
            cookie(authCookie)
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `DELETE cooking-records id - 记录不存在返回 404`() {
        every { cookingRecordService.delete(999L, userId) } throws
            ResponseStatusException(HttpStatus.NOT_FOUND, "Cooking record not found")

        mockMvc.delete("/api/v1/cooking-records/999") {
            cookie(authCookie)
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `DELETE cooking-records id - 非成员返回 403`() {
        every { cookingRecordService.delete(1L, userId) } throws
            ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member")

        mockMvc.delete("/api/v1/cooking-records/1") {
            cookie(authCookie)
        }.andExpect {
            status { isForbidden() }
        }
    }
}
