package com.domu.unit.service

import com.domu.dto.CreateRecipeRequest
import com.domu.dto.IngredientDto
import com.domu.dto.StepDto
import com.domu.dto.UpdateRecipeRequest
import com.domu.model.Family
import com.domu.model.FamilyMember
import com.domu.model.FamilyMemberId
import com.domu.model.Recipe
import com.domu.model.User
import com.domu.repository.FamilyRepository
import com.domu.repository.RecipeRepository
import com.domu.repository.UserRepository
import com.domu.service.FamilyAuthService
import com.domu.service.RecipeService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.Optional

@Suppress("NonAsciiCharacters")
@ExtendWith(MockKExtension::class)
class RecipeServiceTest {

    @MockK
    private lateinit var recipeRepository: RecipeRepository

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var familyRepository: FamilyRepository

    @MockK
    private lateinit var familyAuthService: FamilyAuthService

    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private lateinit var recipeService: RecipeService

    private val testUser = User(id = 1L, email = "test@test.com", passwordHash = "h", name = "测试用户")
    private val testFamily = Family(id = 10L, name = "测试家庭", inviteCode = "ABC12345")
    private val testFamilyMember = FamilyMember(
        id = FamilyMemberId(10L, 1L),
        family = testFamily,
        user = testUser,
        role = "MEMBER"
    )

    @BeforeEach
    fun setUp() {
        recipeService = RecipeService(
            recipeRepository, userRepository, familyRepository, familyAuthService, objectMapper
        )
    }

    private fun createTestRecipe(
        id: Long = 1L,
        title: String = "红烧肉",
        shareToken: String? = null
    ): Recipe {
        val ingredients = objectMapper.writeValueAsString(
            listOf(IngredientDto("猪肉", "500", "克"), IngredientDto("酱油", "2", "勺"))
        )
        val steps = objectMapper.writeValueAsString(
            listOf(StepDto(1, "切肉"), StepDto(2, "炒制"))
        )
        return Recipe(
            id = id,
            title = title,
            description = "经典菜品",
            ingredients = ingredients,
            steps = steps,
            coverImagePath = "/cover.jpg",
            author = testUser,
            family = testFamily,
            shareToken = shareToken,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    // ---------- list ----------

    @Test
    fun `list - 成员可以查看家庭菜谱列表`() {
        val recipes = listOf(createTestRecipe(1L, "红烧肉"), createTestRecipe(2L, "糖醋排骨"))

        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember
        every { recipeRepository.findByFamily_IdOrderByCreatedAtDesc(10L) } returns recipes

        val result = recipeService.list(10L, 1L)

        assertThat(result).hasSize(2)
        assertThat(result[0].title).isEqualTo("红烧肉")
        assertThat(result[1].title).isEqualTo("糖醋排骨")
    }

    @Test
    fun `list - 非成员无法查看菜谱列表`() {
        every { familyAuthService.requireMember(10L, 99L) } throws
            ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member")

        val ex = assertThrows<ResponseStatusException> {
            recipeService.list(10L, 99L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    // ---------- create ----------

    @Test
    fun `create - 成员成功创建菜谱`() {
        val request = CreateRecipeRequest(
            title = "宫保鸡丁",
            description = "四川名菜",
            ingredients = listOf(IngredientDto("鸡肉", "300", "克")),
            steps = listOf(StepDto(1, "切丁")),
            coverImagePath = "/cover.jpg",
            familyId = 10L
        )
        val savedRecipe = createTestRecipe(5L, "宫保鸡丁")

        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember
        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { familyRepository.findById(10L) } returns Optional.of(testFamily)
        every { recipeRepository.save(any()) } returns savedRecipe

        val result = recipeService.create(request, 1L)

        assertThat(result.title).isEqualTo("宫保鸡丁")
        assertThat(result.familyId).isEqualTo(10L)
        assertThat(result.authorId).isEqualTo(1L)
        verify(exactly = 1) { recipeRepository.save(any()) }
    }

    @Test
    fun `create - 用户不存在时抛出 404`() {
        val request = CreateRecipeRequest(
            title = "测试菜谱",
            ingredients = emptyList(),
            steps = emptyList(),
            familyId = 10L
        )

        every { familyAuthService.requireMember(10L, 999L) } returns testFamilyMember
        every { userRepository.findById(999L) } returns Optional.empty()

        val ex = assertThrows<ResponseStatusException> {
            recipeService.create(request, 999L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `create - 家庭不存在时抛出 404`() {
        val request = CreateRecipeRequest(
            title = "测试菜谱",
            ingredients = emptyList(),
            steps = emptyList(),
            familyId = 999L
        )

        every { familyAuthService.requireMember(999L, 1L) } returns testFamilyMember
        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { familyRepository.findById(999L) } returns Optional.empty()

        val ex = assertThrows<ResponseStatusException> {
            recipeService.create(request, 1L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    // ---------- getById ----------

    @Test
    fun `getById - 成员可以获取菜谱详情`() {
        val recipe = createTestRecipe(1L, "红烧肉")

        every { recipeRepository.findById(1L) } returns Optional.of(recipe)
        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember

        val result = recipeService.getById(1L, 1L)

        assertThat(result.id).isEqualTo(1L)
        assertThat(result.title).isEqualTo("红烧肉")
        assertThat(result.ingredients).hasSize(2)
        assertThat(result.steps).hasSize(2)
    }

    @Test
    fun `getById - 菜谱不存在时抛出 404`() {
        every { recipeRepository.findById(999L) } returns Optional.empty()

        val ex = assertThrows<ResponseStatusException> {
            recipeService.getById(999L, 1L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    // ---------- update ----------

    @Test
    fun `update - 成员可以更新菜谱`() {
        val existingRecipe = createTestRecipe(1L, "红烧肉")
        val updateRequest = UpdateRecipeRequest(
            title = "红烧五花肉",
            description = "更新后的描述",
            ingredients = listOf(IngredientDto("五花肉", "600", "克")),
            steps = listOf(StepDto(1, "切块"), StepDto(2, "焯水"), StepDto(3, "红烧")),
            coverImagePath = "/new-cover.jpg"
        )

        every { recipeRepository.findById(1L) } returns Optional.of(existingRecipe)
        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember
        every { recipeRepository.save(any()) } answers { firstArg() }

        val result = recipeService.update(1L, updateRequest, 1L)

        assertThat(result.title).isEqualTo("红烧五花肉")
        assertThat(result.ingredients).hasSize(1)
        assertThat(result.steps).hasSize(3)
    }

    @Test
    fun `update - 非成员无法更新菜谱`() {
        val recipe = createTestRecipe(1L, "红烧肉")
        val updateRequest = UpdateRecipeRequest(
            title = "新标题",
            ingredients = emptyList(),
            steps = emptyList()
        )

        every { recipeRepository.findById(1L) } returns Optional.of(recipe)
        every { familyAuthService.requireMember(10L, 99L) } throws
            ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member")

        val ex = assertThrows<ResponseStatusException> {
            recipeService.update(1L, updateRequest, 99L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    // ---------- delete ----------

    @Test
    fun `delete - 成员可以删除菜谱`() {
        val recipe = createTestRecipe(1L, "红烧肉")

        every { recipeRepository.findById(1L) } returns Optional.of(recipe)
        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember
        justRun { recipeRepository.delete(recipe) }

        recipeService.delete(1L, 1L)

        verify(exactly = 1) { recipeRepository.delete(recipe) }
    }

    @Test
    fun `delete - 菜谱不存在时抛出 404`() {
        every { recipeRepository.findById(999L) } returns Optional.empty()

        val ex = assertThrows<ResponseStatusException> {
            recipeService.delete(999L, 1L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    // ---------- share ----------

    @Test
    fun `share - 首次分享生成新 token`() {
        val recipe = createTestRecipe(1L, "红烧肉", shareToken = null)

        every { recipeRepository.findById(1L) } returns Optional.of(recipe)
        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember
        every { recipeRepository.save(any()) } answers { firstArg() }

        val result = recipeService.share(1L, 1L, "http://localhost:8080")

        assertThat(result.shareToken).isNotEmpty
        assertThat(result.shareUrl).contains("/api/v1/recipes/shared/")
    }

    @Test
    fun `share - 已有 token 时复用`() {
        val existingToken = "existing-token-123"
        val recipe = createTestRecipe(1L, "红烧肉", shareToken = existingToken)

        every { recipeRepository.findById(1L) } returns Optional.of(recipe)
        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember

        val result = recipeService.share(1L, 1L, "http://localhost:8080")

        assertThat(result.shareToken).isEqualTo(existingToken)
        verify(exactly = 0) { recipeRepository.save(any()) }
    }

    // ---------- getByShareToken ----------

    @Test
    fun `getByShareToken - 有效 token 返回菜谱`() {
        val recipe = createTestRecipe(1L, "红烧肉", shareToken = "valid-token")

        every { recipeRepository.findByShareToken("valid-token") } returns recipe

        val result = recipeService.getByShareToken("valid-token")

        assertThat(result.title).isEqualTo("红烧肉")
        assertThat(result.shareToken).isEqualTo("valid-token")
    }

    @Test
    fun `getByShareToken - 无效 token 抛出 404`() {
        every { recipeRepository.findByShareToken("invalid-token") } returns null

        val ex = assertThrows<ResponseStatusException> {
            recipeService.getByShareToken("invalid-token")
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }
}
