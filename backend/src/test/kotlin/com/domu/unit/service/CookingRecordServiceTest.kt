package com.domu.unit.service

import com.domu.dto.CreateCookingRecordRequest
import com.domu.dto.UpdateCookingRecordRequest
import com.domu.model.CookingRecord
import com.domu.model.Family
import com.domu.model.FamilyMember
import com.domu.model.FamilyMemberId
import com.domu.model.Recipe
import com.domu.model.User
import com.domu.repository.CookingRecordRepository
import com.domu.repository.FamilyRepository
import com.domu.repository.RecipeRepository
import com.domu.repository.UserRepository
import com.domu.service.CookingRecordService
import com.domu.service.FamilyAuthService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.*
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
class CookingRecordServiceTest {

    @MockK
    private lateinit var cookingRecordRepository: CookingRecordRepository

    @MockK
    private lateinit var recipeRepository: RecipeRepository

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var familyRepository: FamilyRepository

    @MockK
    private lateinit var familyAuthService: FamilyAuthService

    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private lateinit var cookingRecordService: CookingRecordService

    private val testUser = User(id = 1L, email = "test@test.com", passwordHash = "h", name = "测试用户")
    private val testFamily = Family(id = 10L, name = "测试家庭", inviteCode = "ABC12345")
    private val testRecipe = Recipe(
        id = 100L,
        title = "红烧肉",
        description = "经典菜品",
        ingredients = "[]",
        steps = "[]",
        author = testUser,
        family = testFamily
    )
    private val testFamilyMember = FamilyMember(
        id = FamilyMemberId(10L, 1L),
        family = testFamily,
        user = testUser,
        role = "MEMBER"
    )

    @BeforeEach
    fun setUp() {
        cookingRecordService = CookingRecordService(
            cookingRecordRepository, recipeRepository, userRepository, familyRepository, familyAuthService, objectMapper
        )
    }

    private fun createTestRecord(
        id: Long = 1L,
        notes: String? = "这次做得很好",
        images: List<String> = emptyList()
    ): CookingRecord {
        return CookingRecord(
            id = id,
            recipe = testRecipe,
            user = testUser,
            family = testFamily,
            cookedAt = Instant.parse("2026-01-01T12:00:00Z"),
            notes = notes,
            images = objectMapper.writeValueAsString(images),
            createdAt = Instant.now()
        )
    }

    // ---------- list ----------

    @Test
    fun `list - 成员可以查看家庭做菜记录列表`() {
        val records = listOf(createTestRecord(1L, "记录1"), createTestRecord(2L, "记录2"))

        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember
        every { cookingRecordRepository.findByFamily_IdOrderByCreatedAtDesc(10L) } returns records

        val result = cookingRecordService.list(10L, null, 1L)

        assertThat(result).hasSize(2)
        assertThat(result[0].notes).isEqualTo("记录1")
        assertThat(result[1].notes).isEqualTo("记录2")
    }

    @Test
    fun `list - 按菜谱筛选记录`() {
        val records = listOf(createTestRecord(1L, "红烧肉记录"))

        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember
        every { cookingRecordRepository.findByFamily_IdAndRecipe_IdOrderByCreatedAtDesc(10L, 100L) } returns records

        val result = cookingRecordService.list(10L, 100L, 1L)

        assertThat(result).hasSize(1)
        assertThat(result[0].recipeId).isEqualTo(100L)
    }

    @Test
    fun `list - 非成员无法查看做菜记录`() {
        every { familyAuthService.requireMember(10L, 99L) } throws
            ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member")

        val ex = assertThrows<ResponseStatusException> {
            cookingRecordService.list(10L, null, 99L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    // ---------- create ----------

    @Test
    fun `create - 成员成功创建做菜记录`() {
        val request = CreateCookingRecordRequest(
            recipeId = 100L,
            familyId = 10L,
            cookedAt = "2026-01-01T12:00:00Z",
            notes = "第一次做，味道不错",
            images = listOf("/img1.jpg", "/img2.jpg")
        )
        val savedRecord = createTestRecord(5L, "第一次做，味道不错", listOf("/img1.jpg", "/img2.jpg"))

        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember
        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { recipeRepository.findById(100L) } returns Optional.of(testRecipe)
        every { familyRepository.findById(10L) } returns Optional.of(testFamily)
        every { cookingRecordRepository.save(any()) } returns savedRecord

        val result = cookingRecordService.create(request, 1L)

        assertThat(result.notes).isEqualTo("第一次做，味道不错")
        assertThat(result.recipeId).isEqualTo(100L)
        assertThat(result.familyId).isEqualTo(10L)
        assertThat(result.images).hasSize(2)
        verify(exactly = 1) { cookingRecordRepository.save(any()) }
    }

    @Test
    fun `create - 用户不存在时抛出 404`() {
        val request = CreateCookingRecordRequest(
            recipeId = 100L,
            familyId = 10L,
            cookedAt = "2026-01-01T12:00:00Z"
        )

        every { familyAuthService.requireMember(10L, 999L) } returns testFamilyMember
        every { userRepository.findById(999L) } returns Optional.empty()

        val ex = assertThrows<ResponseStatusException> {
            cookingRecordService.create(request, 999L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `create - 菜谱不存在时抛出 404`() {
        val request = CreateCookingRecordRequest(
            recipeId = 999L,
            familyId = 10L,
            cookedAt = "2026-01-01T12:00:00Z"
        )

        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember
        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { recipeRepository.findById(999L) } returns Optional.empty()

        val ex = assertThrows<ResponseStatusException> {
            cookingRecordService.create(request, 1L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `create - 家庭不存在时抛出 404`() {
        val request = CreateCookingRecordRequest(
            recipeId = 100L,
            familyId = 999L,
            cookedAt = "2026-01-01T12:00:00Z"
        )

        every { familyAuthService.requireMember(999L, 1L) } returns testFamilyMember
        every { userRepository.findById(1L) } returns Optional.of(testUser)
        every { recipeRepository.findById(100L) } returns Optional.of(testRecipe)
        every { familyRepository.findById(999L) } returns Optional.empty()

        val ex = assertThrows<ResponseStatusException> {
            cookingRecordService.create(request, 1L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    // ---------- getById ----------

    @Test
    fun `getById - 成员可以获取做菜记录详情`() {
        val record = createTestRecord(1L, "测试记录")

        every { cookingRecordRepository.findById(1L) } returns Optional.of(record)
        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember

        val result = cookingRecordService.getById(1L, 1L)

        assertThat(result.id).isEqualTo(1L)
        assertThat(result.notes).isEqualTo("测试记录")
        assertThat(result.recipeTitle).isEqualTo("红烧肉")
    }

    @Test
    fun `getById - 记录不存在时抛出 404`() {
        every { cookingRecordRepository.findById(999L) } returns Optional.empty()

        val ex = assertThrows<ResponseStatusException> {
            cookingRecordService.getById(999L, 1L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `getById - 非成员无法获取记录详情`() {
        val record = createTestRecord(1L)

        every { cookingRecordRepository.findById(1L) } returns Optional.of(record)
        every { familyAuthService.requireMember(10L, 99L) } throws
            ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member")

        val ex = assertThrows<ResponseStatusException> {
            cookingRecordService.getById(1L, 99L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    // ---------- update ----------

    @Test
    fun `update - 成员可以更新做菜记录`() {
        val existingRecord = createTestRecord(1L, "原始心得")
        val updateRequest = UpdateCookingRecordRequest(
            cookedAt = "2026-01-02T15:00:00Z",
            notes = "更新后的心得",
            images = listOf("/new-img.jpg")
        )

        every { cookingRecordRepository.findById(1L) } returns Optional.of(existingRecord)
        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember
        every { cookingRecordRepository.save(any()) } answers { firstArg() }

        val result = cookingRecordService.update(1L, updateRequest, 1L)

        assertThat(result.notes).isEqualTo("更新后的心得")
        assertThat(result.images).hasSize(1)
    }

    @Test
    fun `update - 记录不存在时抛出 404`() {
        val updateRequest = UpdateCookingRecordRequest(
            cookedAt = "2026-01-02T15:00:00Z"
        )

        every { cookingRecordRepository.findById(999L) } returns Optional.empty()

        val ex = assertThrows<ResponseStatusException> {
            cookingRecordService.update(999L, updateRequest, 1L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `update - 非成员无法更新记录`() {
        val record = createTestRecord(1L)
        val updateRequest = UpdateCookingRecordRequest(
            cookedAt = "2026-01-02T15:00:00Z"
        )

        every { cookingRecordRepository.findById(1L) } returns Optional.of(record)
        every { familyAuthService.requireMember(10L, 99L) } throws
            ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member")

        val ex = assertThrows<ResponseStatusException> {
            cookingRecordService.update(1L, updateRequest, 99L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    // ---------- delete ----------

    @Test
    fun `delete - 成员可以删除做菜记录`() {
        val record = createTestRecord(1L)

        every { cookingRecordRepository.findById(1L) } returns Optional.of(record)
        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember
        justRun { cookingRecordRepository.delete(record) }

        cookingRecordService.delete(1L, 1L)

        verify(exactly = 1) { cookingRecordRepository.delete(record) }
    }

    @Test
    fun `delete - 记录不存在时抛出 404`() {
        every { cookingRecordRepository.findById(999L) } returns Optional.empty()

        val ex = assertThrows<ResponseStatusException> {
            cookingRecordService.delete(999L, 1L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `delete - 非成员无法删除记录`() {
        val record = createTestRecord(1L)

        every { cookingRecordRepository.findById(1L) } returns Optional.of(record)
        every { familyAuthService.requireMember(10L, 99L) } throws
            ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member")

        val ex = assertThrows<ResponseStatusException> {
            cookingRecordService.delete(1L, 99L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    // ---------- toResponse images 解析 ----------

    @Test
    fun `toResponse - 正确解析图片列表`() {
        val record = createTestRecord(1L, "测试", listOf("/img1.jpg", "/img2.jpg", "/img3.jpg"))

        every { cookingRecordRepository.findById(1L) } returns Optional.of(record)
        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember

        val result = cookingRecordService.getById(1L, 1L)

        assertThat(result.images).containsExactly("/img1.jpg", "/img2.jpg", "/img3.jpg")
    }

    @Test
    fun `toResponse - 空图片列表正确处理`() {
        val record = createTestRecord(1L, "测试", emptyList())

        every { cookingRecordRepository.findById(1L) } returns Optional.of(record)
        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember

        val result = cookingRecordService.getById(1L, 1L)

        assertThat(result.images).isEmpty()
    }
}
