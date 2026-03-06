package com.domu.unit.service

import com.domu.dto.CreateOrderRequest
import com.domu.dto.IngredientDto
import com.domu.model.Family
import com.domu.model.FamilyMember
import com.domu.model.FamilyMemberId
import com.domu.model.Order
import com.domu.model.Recipe
import com.domu.model.User
import com.domu.repository.FamilyRepository
import com.domu.repository.OrderRepository
import com.domu.repository.RecipeRepository
import com.domu.repository.UserRepository
import com.domu.service.FamilyAuthService
import com.domu.service.OrderService
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
import java.time.LocalDate
import java.util.Optional

@Suppress("NonAsciiCharacters")
@ExtendWith(MockKExtension::class)
class OrderServiceTest {

    @MockK
    private lateinit var orderRepository: OrderRepository

    @MockK
    private lateinit var familyRepository: FamilyRepository

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var recipeRepository: RecipeRepository

    @MockK
    private lateinit var familyAuthService: FamilyAuthService

    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private lateinit var orderService: OrderService

    private val testUser1 = User(id = 1L, email = "user1@test.com", passwordHash = "h", name = "用户1")
    private val testUser2 = User(id = 2L, email = "user2@test.com", passwordHash = "h", name = "用户2")
    private val testFamily = Family(id = 10L, name = "测试家庭", inviteCode = "ABC12345")
    private val testFamilyMember = FamilyMember(
        id = FamilyMemberId(10L, 1L),
        family = testFamily,
        user = testUser1,
        role = "MEMBER"
    )

    private fun createTestRecipe(id: Long, title: String, ingredients: String): Recipe {
        return Recipe(
            id = id,
            title = title,
            ingredients = ingredients,
            steps = "[]",
            author = testUser1,
            family = testFamily
        )
    }

    private fun createTestOrder(
        id: Long,
        recipe: Recipe,
        plannedDate: LocalDate = LocalDate.of(2026, 3, 3),
        status: String = "PENDING"
    ): Order {
        return Order(
            id = id,
            family = testFamily,
            orderedBy = testUser1,
            orderedFor = testUser2,
            recipe = recipe,
            plannedDate = plannedDate,
            status = status,
            createdAt = Instant.now()
        )
    }

    @BeforeEach
    fun setUp() {
        orderService = OrderService(
            orderRepository, familyRepository, userRepository,
            recipeRepository, familyAuthService, objectMapper
        )
    }

    // ---------- list ----------

    @Test
    fun `list - 查询家庭所有订单`() {
        val recipe = createTestRecipe(1L, "红烧肉", "[]")
        val orders = listOf(
            createTestOrder(1L, recipe),
            createTestOrder(2L, recipe)
        )

        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember
        every { orderRepository.findByFamily_IdOrderByCreatedAtDesc(10L) } returns orders

        val result = orderService.list(10L, null, 1L)

        assertThat(result).hasSize(2)
    }

    @Test
    fun `list - 按日期筛选订单`() {
        val recipe = createTestRecipe(1L, "红烧肉", "[]")
        val targetDate = LocalDate.of(2026, 3, 3)
        val orders = listOf(createTestOrder(1L, recipe, targetDate))

        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember
        every { orderRepository.findByFamily_IdAndPlannedDate(10L, targetDate) } returns orders

        val result = orderService.list(10L, targetDate, 1L)

        assertThat(result).hasSize(1)
        assertThat(result[0].plannedDate).isEqualTo("2026-03-03")
    }

    @Test
    fun `list - 非成员无法查看订单`() {
        every { familyAuthService.requireMember(10L, 99L) } throws
            ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member")

        val ex = assertThrows<ResponseStatusException> {
            orderService.list(10L, null, 99L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    // ---------- create ----------

    @Test
    fun `create - 成功创建订单`() {
        val recipe = createTestRecipe(1L, "红烧肉", "[]")
        val request = CreateOrderRequest(
            familyId = 10L,
            orderedForId = 2L,
            recipeId = 1L,
            plannedDate = "2026-03-03"
        )
        val savedOrder = createTestOrder(5L, recipe)

        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember
        every { userRepository.findById(1L) } returns Optional.of(testUser1)
        every { userRepository.findById(2L) } returns Optional.of(testUser2)
        every { recipeRepository.findById(1L) } returns Optional.of(recipe)
        every { familyRepository.findById(10L) } returns Optional.of(testFamily)
        every { orderRepository.save(any()) } returns savedOrder

        val result = orderService.create(request, 1L)

        assertThat(result.id).isEqualTo(5L)
        assertThat(result.status).isEqualTo("PENDING")
        assertThat(result.recipeTitle).isEqualTo("红烧肉")
        verify(exactly = 1) { orderRepository.save(any()) }
    }

    @Test
    fun `create - 下单用户不存在时抛出 404`() {
        val request = CreateOrderRequest(
            familyId = 10L,
            orderedForId = 2L,
            recipeId = 1L,
            plannedDate = "2026-03-03"
        )

        every { familyAuthService.requireMember(10L, 999L) } returns testFamilyMember
        every { userRepository.findById(999L) } returns Optional.empty()

        val ex = assertThrows<ResponseStatusException> {
            orderService.create(request, 999L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `create - 被点菜用户不存在时抛出 404`() {
        val request = CreateOrderRequest(
            familyId = 10L,
            orderedForId = 999L,
            recipeId = 1L,
            plannedDate = "2026-03-03"
        )

        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember
        every { userRepository.findById(1L) } returns Optional.of(testUser1)
        every { userRepository.findById(999L) } returns Optional.empty()

        val ex = assertThrows<ResponseStatusException> {
            orderService.create(request, 1L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `create - 菜谱不存在时抛出 404`() {
        val request = CreateOrderRequest(
            familyId = 10L,
            orderedForId = 2L,
            recipeId = 999L,
            plannedDate = "2026-03-03"
        )

        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember
        every { userRepository.findById(1L) } returns Optional.of(testUser1)
        every { userRepository.findById(2L) } returns Optional.of(testUser2)
        every { recipeRepository.findById(999L) } returns Optional.empty()

        val ex = assertThrows<ResponseStatusException> {
            orderService.create(request, 1L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    // ---------- updateStatus ----------

    @Test
    fun `updateStatus - 更新状态为 DONE`() {
        val recipe = createTestRecipe(1L, "红烧肉", "[]")
        val order = createTestOrder(1L, recipe, status = "PENDING")

        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember
        every { orderRepository.save(any()) } answers { firstArg() }

        val result = orderService.updateStatus(1L, "DONE", 1L)

        assertThat(result.status).isEqualTo("DONE")
    }

    @Test
    fun `updateStatus - 更新状态为 CANCELLED`() {
        val recipe = createTestRecipe(1L, "红烧肉", "[]")
        val order = createTestOrder(1L, recipe, status = "PENDING")

        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember
        every { orderRepository.save(any()) } answers { firstArg() }

        val result = orderService.updateStatus(1L, "CANCELLED", 1L)

        assertThat(result.status).isEqualTo("CANCELLED")
    }

    @Test
    fun `updateStatus - 无效状态抛出 400 BAD_REQUEST`() {
        val recipe = createTestRecipe(1L, "红烧肉", "[]")
        val order = createTestOrder(1L, recipe)

        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember

        val ex = assertThrows<ResponseStatusException> {
            orderService.updateStatus(1L, "INVALID", 1L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `updateStatus - 订单不存在时抛出 404`() {
        every { orderRepository.findById(999L) } returns Optional.empty()

        val ex = assertThrows<ResponseStatusException> {
            orderService.updateStatus(999L, "DONE", 1L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    // ---------- delete ----------

    @Test
    fun `delete - 成功删除订单`() {
        val recipe = createTestRecipe(1L, "红烧肉", "[]")
        val order = createTestOrder(1L, recipe)

        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember
        justRun { orderRepository.delete(order) }

        orderService.delete(1L, 1L)

        verify(exactly = 1) { orderRepository.delete(order) }
    }

    @Test
    fun `delete - 订单不存在时抛出 404`() {
        every { orderRepository.findById(999L) } returns Optional.empty()

        val ex = assertThrows<ResponseStatusException> {
            orderService.delete(999L, 1L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    // ---------- getShoppingPlan ----------

    @Test
    fun `getShoppingPlan - 聚合多个订单的食材`() {
        val ingredients1 = objectMapper.writeValueAsString(
            listOf(IngredientDto("猪肉", "500", "克"), IngredientDto("酱油", "2", "勺"))
        )
        val ingredients2 = objectMapper.writeValueAsString(
            listOf(IngredientDto("猪肉", "300", "克"), IngredientDto("白糖", "1", "勺"))
        )
        val recipe1 = createTestRecipe(1L, "红烧肉", ingredients1)
        val recipe2 = createTestRecipe(2L, "糖醋排骨", ingredients2)

        val date = LocalDate.of(2026, 3, 3)
        val orders = listOf(
            createTestOrder(1L, recipe1, date, "PENDING"),
            createTestOrder(2L, recipe2, date, "PENDING")
        )

        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember
        every { orderRepository.findByFamily_IdAndPlannedDateAndStatusNot(10L, date, "CANCELLED") } returns orders

        val result = orderService.getShoppingPlan(10L, date, 1L)

        assertThat(result).hasSize(3)
        // 猪肉应该合并：500 + 300 = 800
        val pork = result.find { it.name == "猪肉" }
        assertThat(pork).isNotNull
        assertThat(pork!!.amount).isEqualTo(800.0)
        assertThat(pork.unit).isEqualTo("克")

        // 结果按名称排序
        assertThat(result.map { it.name }).isSorted
    }

    @Test
    fun `getShoppingPlan - 排除已取消的订单`() {
        val ingredients = objectMapper.writeValueAsString(
            listOf(IngredientDto("牛肉", "200", "克"))
        )
        val recipe = createTestRecipe(1L, "牛肉面", ingredients)
        val date = LocalDate.of(2026, 3, 3)
        val orders = listOf(createTestOrder(1L, recipe, date, "PENDING"))

        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember
        every { orderRepository.findByFamily_IdAndPlannedDateAndStatusNot(10L, date, "CANCELLED") } returns orders

        val result = orderService.getShoppingPlan(10L, date, 1L)

        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("牛肉")
    }

    @Test
    fun `getShoppingPlan - 空订单返回空列表`() {
        val date = LocalDate.of(2026, 3, 3)

        every { familyAuthService.requireMember(10L, 1L) } returns testFamilyMember
        every { orderRepository.findByFamily_IdAndPlannedDateAndStatusNot(10L, date, "CANCELLED") } returns emptyList()

        val result = orderService.getShoppingPlan(10L, date, 1L)

        assertThat(result).isEmpty()
    }

    @Test
    fun `getShoppingPlan - 非成员无法查看买菜计划`() {
        val date = LocalDate.of(2026, 3, 3)

        every { familyAuthService.requireMember(10L, 99L) } throws
            ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member")

        val ex = assertThrows<ResponseStatusException> {
            orderService.getShoppingPlan(10L, date, 99L)
        }
        assertThat(ex.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }
}
