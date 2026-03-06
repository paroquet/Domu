package com.domu.slice.repository

import com.domu.model.Family
import com.domu.model.Order
import com.domu.model.Recipe
import com.domu.model.User
import com.domu.repository.OrderRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.TestPropertySource
import java.time.Instant
import java.time.LocalDate

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:sqlite::memory:",
    "spring.datasource.driver-class-name=org.sqlite.JDBC",
    "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
])
class OrderRepositoryTest {

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var entityManager: TestEntityManager

    private lateinit var testUser1: User
    private lateinit var testUser2: User
    private lateinit var testFamily1: Family
    private lateinit var testFamily2: Family
    private lateinit var testRecipe: Recipe

    @BeforeEach
    fun setUp() {
        testUser1 = entityManager.persistAndFlush(
            User(email = "user1@test.com", passwordHash = "h", name = "用户1")
        )
        testUser2 = entityManager.persistAndFlush(
            User(email = "user2@test.com", passwordHash = "h", name = "用户2")
        )
        testFamily1 = entityManager.persistAndFlush(
            Family(name = "家庭1", inviteCode = "FAMILY01")
        )
        testFamily2 = entityManager.persistAndFlush(
            Family(name = "家庭2", inviteCode = "FAMILY02")
        )
        testRecipe = entityManager.persistAndFlush(
            Recipe(
                title = "红烧肉",
                ingredients = "[]",
                steps = "[]",
                author = testUser1,
                family = testFamily1
            )
        )
    }

    private fun saveOrder(
        family: Family,
        recipe: Recipe = testRecipe,
        plannedDate: LocalDate = LocalDate.of(2026, 3, 3),
        status: String = "PENDING",
        createdAt: Instant = Instant.now()
    ): Order {
        val order = Order(
            family = family,
            orderedBy = testUser1,
            orderedFor = testUser2,
            recipe = recipe,
            plannedDate = plannedDate,
            status = status,
            createdAt = createdAt
        )
        return entityManager.persistAndFlush(order)
    }

    // ---------- findByFamily_IdOrderByCreatedAtDesc ----------

    @Test
    fun `findByFamily_IdOrderByCreatedAtDesc - 返回指定家庭的订单`() {
        val now = Instant.now()
        saveOrder(testFamily1, createdAt = now.minusSeconds(100))
        saveOrder(testFamily1, createdAt = now)

        val result = orderRepository.findByFamily_IdOrderByCreatedAtDesc(testFamily1.id)

        assertThat(result).hasSize(2)
        // 按 createdAt 降序
        assertThat(result[0].createdAt).isAfterOrEqualTo(result[1].createdAt)
    }

    @Test
    fun `findByFamily_IdOrderByCreatedAtDesc - 不返回其他家庭的订单`() {
        saveOrder(testFamily1)
        // 创建另一个家庭的菜谱和订单
        val recipe2 = entityManager.persistAndFlush(
            Recipe(title = "糖醋排骨", ingredients = "[]", steps = "[]", author = testUser1, family = testFamily2)
        )
        saveOrder(testFamily2, recipe = recipe2)

        val result = orderRepository.findByFamily_IdOrderByCreatedAtDesc(testFamily1.id)

        assertThat(result).hasSize(1)
    }

    @Test
    fun `findByFamily_IdOrderByCreatedAtDesc - 家庭无订单时返回空列表`() {
        val result = orderRepository.findByFamily_IdOrderByCreatedAtDesc(testFamily1.id)

        assertThat(result).isEmpty()
    }

    // ---------- findByFamily_IdAndPlannedDate ----------

    @Test
    fun `findByFamily_IdAndPlannedDate - 返回指定日期的订单`() {
        val date1 = LocalDate.of(2026, 3, 3)
        val date2 = LocalDate.of(2026, 3, 4)
        saveOrder(testFamily1, plannedDate = date1)
        saveOrder(testFamily1, plannedDate = date1)
        saveOrder(testFamily1, plannedDate = date2)

        val result = orderRepository.findByFamily_IdAndPlannedDate(testFamily1.id, date1)

        assertThat(result).hasSize(2)
        assertThat(result.all { it.plannedDate == date1 }).isTrue()
    }

    @Test
    fun `findByFamily_IdAndPlannedDate - 不同日期返回不同结果`() {
        val date1 = LocalDate.of(2026, 3, 3)
        val date2 = LocalDate.of(2026, 3, 4)
        saveOrder(testFamily1, plannedDate = date1)
        saveOrder(testFamily1, plannedDate = date2)

        val result1 = orderRepository.findByFamily_IdAndPlannedDate(testFamily1.id, date1)
        val result2 = orderRepository.findByFamily_IdAndPlannedDate(testFamily1.id, date2)

        assertThat(result1).hasSize(1)
        assertThat(result2).hasSize(1)
    }

    @Test
    fun `findByFamily_IdAndPlannedDate - 无订单返回空列表`() {
        val date = LocalDate.of(2026, 3, 3)

        val result = orderRepository.findByFamily_IdAndPlannedDate(testFamily1.id, date)

        assertThat(result).isEmpty()
    }

    // ---------- findByFamily_IdAndPlannedDateAndStatusNot ----------

    @Test
    fun `findByFamily_IdAndPlannedDateAndStatusNot - 排除指定状态的订单`() {
        val date = LocalDate.of(2026, 3, 3)
        saveOrder(testFamily1, plannedDate = date, status = "PENDING")
        saveOrder(testFamily1, plannedDate = date, status = "DONE")
        saveOrder(testFamily1, plannedDate = date, status = "CANCELLED")

        val result = orderRepository.findByFamily_IdAndPlannedDateAndStatusNot(testFamily1.id, date, "CANCELLED")

        assertThat(result).hasSize(2)
        assertThat(result.none { it.status == "CANCELLED" }).isTrue()
    }

    @Test
    fun `findByFamily_IdAndPlannedDateAndStatusNot - 所有订单都是排除状态时返回空`() {
        val date = LocalDate.of(2026, 3, 3)
        saveOrder(testFamily1, plannedDate = date, status = "CANCELLED")
        saveOrder(testFamily1, plannedDate = date, status = "CANCELLED")

        val result = orderRepository.findByFamily_IdAndPlannedDateAndStatusNot(testFamily1.id, date, "CANCELLED")

        assertThat(result).isEmpty()
    }

    @Test
    fun `findByFamily_IdAndPlannedDateAndStatusNot - 用于买菜计划排除已取消订单`() {
        val date = LocalDate.of(2026, 3, 3)
        saveOrder(testFamily1, plannedDate = date, status = "PENDING")
        saveOrder(testFamily1, plannedDate = date, status = "DONE")
        saveOrder(testFamily1, plannedDate = date, status = "CANCELLED")

        val result = orderRepository.findByFamily_IdAndPlannedDateAndStatusNot(testFamily1.id, date, "CANCELLED")

        // PENDING 和 DONE 都应该被包含在买菜计划中
        assertThat(result).hasSize(2)
        assertThat(result.map { it.status }).containsExactlyInAnyOrder("PENDING", "DONE")
    }

    // ---------- save ----------

    @Test
    fun `save - 成功保存订单并生成 ID`() {
        val order = Order(
            family = testFamily1,
            orderedBy = testUser1,
            orderedFor = testUser2,
            recipe = testRecipe,
            plannedDate = LocalDate.of(2026, 3, 3),
            status = "PENDING"
        )

        val saved = orderRepository.save(order)
        entityManager.flush()

        assertThat(saved.id).isGreaterThan(0)
        assertThat(saved.status).isEqualTo("PENDING")
        assertThat(saved.createdAt).isNotNull
    }

    // ---------- delete ----------

    @Test
    fun `delete - 成功删除订单`() {
        val order = saveOrder(testFamily1)
        val orderId = order.id

        orderRepository.delete(order)
        entityManager.flush()

        val found = orderRepository.findById(orderId)
        assertThat(found).isEmpty
    }

    // ---------- findById ----------

    @Test
    fun `findById - ID 存在时返回订单`() {
        val order = saveOrder(testFamily1)

        val found = orderRepository.findById(order.id)

        assertThat(found).isPresent
        assertThat(found.get().recipe.title).isEqualTo("红烧肉")
    }

    @Test
    fun `findById - ID 不存在时返回空`() {
        val found = orderRepository.findById(999L)

        assertThat(found).isEmpty
    }
}
