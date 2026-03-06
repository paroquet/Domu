package com.domu.slice.repository

import com.domu.model.CookingRecord
import com.domu.model.Family
import com.domu.model.Recipe
import com.domu.model.User
import com.domu.repository.CookingRecordRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.TestPropertySource
import java.time.Instant

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:sqlite::memory:",
    "spring.datasource.driver-class-name=org.sqlite.JDBC",
    "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
])
class CookingRecordRepositoryTest {

    @Autowired
    private lateinit var cookingRecordRepository: CookingRecordRepository

    @Autowired
    private lateinit var entityManager: TestEntityManager

    private lateinit var testUser: User
    private lateinit var testFamily1: Family
    private lateinit var testFamily2: Family
    private lateinit var testRecipe1: Recipe
    private lateinit var testRecipe2: Recipe

    @BeforeEach
    fun setUp() {
        testUser = entityManager.persistAndFlush(
            User(email = "test@test.com", passwordHash = "h", name = "测试用户")
        )
        testFamily1 = entityManager.persistAndFlush(
            Family(name = "家庭1", inviteCode = "FAMILY01")
        )
        testFamily2 = entityManager.persistAndFlush(
            Family(name = "家庭2", inviteCode = "FAMILY02")
        )
        testRecipe1 = entityManager.persistAndFlush(
            Recipe(
                title = "红烧肉",
                description = "经典菜品",
                ingredients = "[]",
                steps = "[]",
                author = testUser,
                family = testFamily1
            )
        )
        testRecipe2 = entityManager.persistAndFlush(
            Recipe(
                title = "糖醋排骨",
                description = "另一道菜",
                ingredients = "[]",
                steps = "[]",
                author = testUser,
                family = testFamily1
            )
        )
    }

    private fun saveRecord(
        recipe: Recipe,
        family: Family,
        notes: String? = null,
        createdAt: Instant = Instant.now()
    ): CookingRecord {
        val record = CookingRecord(
            recipe = recipe,
            user = testUser,
            family = family,
            cookedAt = Instant.now(),
            notes = notes,
            images = "[]",
            createdAt = createdAt
        )
        return entityManager.persistAndFlush(record)
    }

    // ---------- findByFamily_IdOrderByCreatedAtDesc ----------

    @Test
    fun `findByFamily_IdOrderByCreatedAtDesc - 返回指定家庭的做菜记录`() {
        val now = Instant.now()
        saveRecord(testRecipe1, testFamily1, "记录1", now.minusSeconds(100))
        saveRecord(testRecipe2, testFamily1, "记录2", now)

        val result = cookingRecordRepository.findByFamily_IdOrderByCreatedAtDesc(testFamily1.id)

        assertThat(result).hasSize(2)
        // 按 createdAt 降序，最新的在前
        assertThat(result[0].notes).isEqualTo("记录2")
        assertThat(result[1].notes).isEqualTo("记录1")
    }

    @Test
    fun `findByFamily_IdOrderByCreatedAtDesc - 不返回其他家庭的记录`() {
        saveRecord(testRecipe1, testFamily1, "家庭1的记录")

        // 创建家庭2的菜谱和记录
        val family2Recipe = entityManager.persistAndFlush(
            Recipe(
                title = "家庭2菜谱",
                ingredients = "[]",
                steps = "[]",
                author = testUser,
                family = testFamily2
            )
        )
        saveRecord(family2Recipe, testFamily2, "家庭2的记录")

        val result = cookingRecordRepository.findByFamily_IdOrderByCreatedAtDesc(testFamily1.id)

        assertThat(result).hasSize(1)
        assertThat(result[0].notes).isEqualTo("家庭1的记录")
    }

    @Test
    fun `findByFamily_IdOrderByCreatedAtDesc - 家庭无记录时返回空列表`() {
        val result = cookingRecordRepository.findByFamily_IdOrderByCreatedAtDesc(testFamily1.id)

        assertThat(result).isEmpty()
    }

    // ---------- findByFamily_IdAndRecipe_IdOrderByCreatedAtDesc ----------

    @Test
    fun `findByFamily_IdAndRecipe_IdOrderByCreatedAtDesc - 按菜谱筛选记录`() {
        val now = Instant.now()
        saveRecord(testRecipe1, testFamily1, "红烧肉记录1", now.minusSeconds(100))
        saveRecord(testRecipe1, testFamily1, "红烧肉记录2", now)
        saveRecord(testRecipe2, testFamily1, "糖醋排骨记录")

        val result = cookingRecordRepository.findByFamily_IdAndRecipe_IdOrderByCreatedAtDesc(
            testFamily1.id, testRecipe1.id
        )

        assertThat(result).hasSize(2)
        assertThat(result[0].notes).isEqualTo("红烧肉记录2")
        assertThat(result[1].notes).isEqualTo("红烧肉记录1")
        assertThat(result.all { it.recipe.id == testRecipe1.id }).isTrue()
    }

    @Test
    fun `findByFamily_IdAndRecipe_IdOrderByCreatedAtDesc - 菜谱无记录时返回空列表`() {
        saveRecord(testRecipe2, testFamily1, "糖醋排骨记录")

        val result = cookingRecordRepository.findByFamily_IdAndRecipe_IdOrderByCreatedAtDesc(
            testFamily1.id, testRecipe1.id
        )

        assertThat(result).isEmpty()
    }

    // ---------- save ----------

    @Test
    fun `save - 成功保存做菜记录并生成 ID`() {
        val record = CookingRecord(
            recipe = testRecipe1,
            user = testUser,
            family = testFamily1,
            cookedAt = Instant.now(),
            notes = "测试记录",
            images = """["/img1.jpg", "/img2.jpg"]"""
        )

        val saved = cookingRecordRepository.save(record)
        entityManager.flush()

        assertThat(saved.id).isGreaterThan(0)
        assertThat(saved.notes).isEqualTo("测试记录")
        assertThat(saved.images).contains("/img1.jpg")
        assertThat(saved.createdAt).isNotNull
    }

    @Test
    fun `save - 可保存空心得`() {
        val record = CookingRecord(
            recipe = testRecipe1,
            user = testUser,
            family = testFamily1,
            cookedAt = Instant.now(),
            notes = null,
            images = "[]"
        )

        val saved = cookingRecordRepository.save(record)
        entityManager.flush()

        assertThat(saved.id).isGreaterThan(0)
        assertThat(saved.notes).isNull()
    }

    // ---------- delete ----------

    @Test
    fun `delete - 成功删除做菜记录`() {
        val record = saveRecord(testRecipe1, testFamily1, "待删除记录")
        val recordId = record.id

        cookingRecordRepository.delete(record)
        entityManager.flush()

        val found = cookingRecordRepository.findById(recordId)
        assertThat(found).isEmpty
    }

    // ---------- findById ----------

    @Test
    fun `findById - ID 存在时返回记录`() {
        val record = saveRecord(testRecipe1, testFamily1, "查询测试")

        val found = cookingRecordRepository.findById(record.id)

        assertThat(found).isPresent
        assertThat(found.get().notes).isEqualTo("查询测试")
    }

    @Test
    fun `findById - ID 不存在时返回空`() {
        val found = cookingRecordRepository.findById(999L)

        assertThat(found).isEmpty
    }

    // ---------- 关联查询 ----------

    @Test
    fun `findById - 能正确加载关联的 Recipe`() {
        val record = saveRecord(testRecipe1, testFamily1, "关联测试")
        entityManager.clear() // 清除缓存，强制从数据库加载

        val found = cookingRecordRepository.findById(record.id)

        assertThat(found).isPresent
        assertThat(found.get().recipe.title).isEqualTo("红烧肉")
    }

    @Test
    fun `findById - 能正确加载关联的 User`() {
        val record = saveRecord(testRecipe1, testFamily1, "用户关联测试")
        entityManager.clear()

        val found = cookingRecordRepository.findById(record.id)

        assertThat(found).isPresent
        assertThat(found.get().user.name).isEqualTo("测试用户")
    }

    @Test
    fun `findById - 能正确加载关联的 Family`() {
        val record = saveRecord(testRecipe1, testFamily1, "家庭关联测试")
        entityManager.clear()

        val found = cookingRecordRepository.findById(record.id)

        assertThat(found).isPresent
        assertThat(found.get().family.name).isEqualTo("家庭1")
    }

    // ---------- 时间字段 ----------

    @Test
    fun `时间字段 - cookedAt 正确存储和读取`() {
        val cookedAt = Instant.parse("2026-03-15T10:30:00Z")
        val record = CookingRecord(
            recipe = testRecipe1,
            user = testUser,
            family = testFamily1,
            cookedAt = cookedAt,
            notes = "时间测试",
            images = "[]"
        )

        val saved = cookingRecordRepository.save(record)
        entityManager.flush()
        entityManager.clear()

        val found = cookingRecordRepository.findById(saved.id)
        assertThat(found).isPresent
        assertThat(found.get().cookedAt).isEqualTo(cookedAt)
    }
}
