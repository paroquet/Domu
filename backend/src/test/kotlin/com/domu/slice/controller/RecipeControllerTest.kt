package com.domu.slice.controller

import com.domu.config.AppConfig
import com.domu.config.SecurityConfig
import com.domu.controller.RecipeController
import com.domu.dto.IngredientDto
import com.domu.dto.RecipeResponse
import com.domu.dto.ShareResponse
import com.domu.dto.StepDto
import com.domu.model.User
import com.domu.repository.UserRepository
import com.domu.security.JwtAuthenticationFilter
import com.domu.security.JwtTokenProvider
import com.domu.security.UserDetailsServiceImpl
import com.domu.service.RecipeService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.web.server.ResponseStatusException
import java.util.Optional

@WebMvcTest(controllers = [RecipeController::class])
@Import(SecurityConfig::class, JwtTokenProvider::class, JwtAuthenticationFilter::class,
        UserDetailsServiceImpl::class, AppConfig::class)
@TestPropertySource(properties = [
    "app.jwt.secret=test-secret-key-that-is-at-least-32-chars-long",
    "app.base-url=http://localhost:8080",
    "app.upload-dir=/tmp/test-uploads"
])
class RecipeControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockkBean
    private lateinit var recipeService: RecipeService

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

    private val recipeResponse = RecipeResponse(
        id = 1L,
        title = "红烧肉",
        description = "经典菜品",
        ingredients = listOf(IngredientDto("猪肉", "500", "克")),
        steps = listOf(StepDto(1, "切块")),
        coverImagePath = "/cover.jpg",
        authorId = 1L,
        authorName = "测试用户",
        familyId = 10L,
        shareToken = null,
        shareUrl = null,
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z"
    )

    // ---------- GET /api/v1/recipes ----------

    @Test
    fun `GET recipes - 列出家庭菜谱`() {
        every { recipeService.list(10L, userId) } returns listOf(recipeResponse)

        mockMvc.get("/api/v1/recipes?familyId=10") {
            cookie(authCookie)
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].title") { value("红烧肉") }
        }
    }

    @Test
    fun `GET recipes - 未认证返回 4xx`() {
        mockMvc.get("/api/v1/recipes?familyId=10") {
        }.andExpect {
            status { is4xxClientError() }
        }
    }

    @Test
    fun `GET recipes - 非成员返回 403`() {
        every { recipeService.list(10L, userId) } throws
            ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member")

        mockMvc.get("/api/v1/recipes?familyId=10") {
            cookie(authCookie)
        }.andExpect {
            status { isForbidden() }
        }
    }

    // ---------- POST /api/v1/recipes ----------

    @Test
    fun `POST recipes - 创建菜谱成功返回 201`() {
        every { recipeService.create(any(), userId) } returns recipeResponse

        mockMvc.post("/api/v1/recipes") {
            cookie(authCookie)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "title": "红烧肉",
                    "description": "经典菜品",
                    "ingredients": [{"name": "猪肉", "amount": "500", "unit": "克"}],
                    "steps": [{"order": 1, "description": "切块"}],
                    "familyId": 10
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.title") { value("红烧肉") }
            jsonPath("$.familyId") { value(10) }
        }
    }

    @Test
    fun `POST recipes - 未认证返回 4xx`() {
        mockMvc.post("/api/v1/recipes") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"title": "测试", "ingredients": [], "steps": [], "familyId": 10}"""
        }.andExpect {
            status { is4xxClientError() }
        }
    }

    // ---------- GET /api/v1/recipes/{id} ----------

    @Test
    fun `GET recipes id - 获取菜谱详情`() {
        every { recipeService.getById(1L, userId) } returns recipeResponse

        mockMvc.get("/api/v1/recipes/1") {
            cookie(authCookie)
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(1) }
            jsonPath("$.title") { value("红烧肉") }
            jsonPath("$.ingredients.length()") { value(1) }
        }
    }

    @Test
    fun `GET recipes id - 菜谱不存在返回 404`() {
        every { recipeService.getById(999L, userId) } throws
            ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found")

        mockMvc.get("/api/v1/recipes/999") {
            cookie(authCookie)
        }.andExpect {
            status { isNotFound() }
        }
    }

    // ---------- PUT /api/v1/recipes/{id} ----------

    @Test
    fun `PUT recipes id - 更新菜谱成功`() {
        val updatedResponse = recipeResponse.copy(title = "红烧五花肉")
        every { recipeService.update(1L, any(), userId) } returns updatedResponse

        mockMvc.put("/api/v1/recipes/1") {
            cookie(authCookie)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "title": "红烧五花肉",
                    "ingredients": [{"name": "五花肉", "amount": "600", "unit": "克"}],
                    "steps": [{"order": 1, "description": "切块"}]
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.title") { value("红烧五花肉") }
        }
    }

    @Test
    fun `PUT recipes id - 非成员返回 403`() {
        every { recipeService.update(1L, any(), userId) } throws
            ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member")

        mockMvc.put("/api/v1/recipes/1") {
            cookie(authCookie)
            contentType = MediaType.APPLICATION_JSON
            content = """{"title": "新标题", "ingredients": [], "steps": []}"""
        }.andExpect {
            status { isForbidden() }
        }
    }

    // ---------- DELETE /api/v1/recipes/{id} ----------

    @Test
    fun `DELETE recipes id - 删除菜谱成功返回 204`() {
        justRun { recipeService.delete(1L, userId) }

        mockMvc.delete("/api/v1/recipes/1") {
            cookie(authCookie)
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `DELETE recipes id - 菜谱不存在返回 404`() {
        every { recipeService.delete(999L, userId) } throws
            ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found")

        mockMvc.delete("/api/v1/recipes/999") {
            cookie(authCookie)
        }.andExpect {
            status { isNotFound() }
        }
    }

    // ---------- POST /api/v1/recipes/{id}/share ----------

    @Test
    fun `POST recipes id share - 生成分享链接`() {
        val shareResponse = ShareResponse(
            shareToken = "abc123",
            shareUrl = "http://localhost:8080/api/v1/recipes/shared/abc123"
        )
        every { recipeService.share(1L, userId, "http://localhost:8080") } returns shareResponse

        mockMvc.post("/api/v1/recipes/1/share") {
            cookie(authCookie)
        }.andExpect {
            status { isOk() }
            jsonPath("$.shareToken") { value("abc123") }
            jsonPath("$.shareUrl") { value("http://localhost:8080/api/v1/recipes/shared/abc123") }
        }
    }

    // ---------- GET /api/v1/recipes/shared/{token} ----------

    @Test
    fun `GET recipes shared token - 公开访问无需认证`() {
        val sharedRecipe = recipeResponse.copy(shareToken = "valid-token")
        every { recipeService.getByShareToken("valid-token") } returns sharedRecipe

        mockMvc.get("/api/v1/recipes/shared/valid-token") {
            // 无需 Cookie
        }.andExpect {
            status { isOk() }
            jsonPath("$.title") { value("红烧肉") }
        }
    }

    @Test
    fun `GET recipes shared token - 无效 token 返回 404`() {
        every { recipeService.getByShareToken("invalid") } throws
            ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found")

        mockMvc.get("/api/v1/recipes/shared/invalid") {
        }.andExpect {
            status { isNotFound() }
        }
    }
}
