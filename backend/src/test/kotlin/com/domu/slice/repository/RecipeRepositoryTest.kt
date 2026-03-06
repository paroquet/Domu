package com.domu.slice.repository

import com.domu.model.Family
import com.domu.model.Recipe
import com.domu.model.User
import com.domu.repository.RecipeRepository
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
class RecipeRepositoryTest {

    @Autowired
    private lateinit var recipeRepository: RecipeRepository

    @Autowired
    private lateinit var entityManager: TestEntityManager

    private lateinit var testUser: User
    private lateinit var testFamily1: Family
    private lateinit var testFamily2: Family

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
    }

    private fun saveRecipe(
        title: String,
        family: Family,
        shareToken: String? = null,
        createdAt: Instant = Instant.now()
    ): Recipe {
        val recipe = Recipe(
            title = title,
            description = "描述",
            ingredients = "[]",
            steps = "[]",
            author = testUser,
            family = family,
            shareToken = shareToken,
            createdAt = createdAt,
            updatedAt = createdAt
        )
        return entityManager.persistAndFlush(recipe)
    }

    // ---------- findByFamily_IdOrderByCreatedAtDesc ----------

    @Test
    fun `findByFamily_IdOrderByCreatedAtDesc - 返回指定家庭的菜谱`() {
        val now = Instant.now()
        saveRecipe("红烧肉", testFamily1, createdAt = now.minusSeconds(100))
        saveRecipe("糖醋排骨", testFamily1, createdAt = now)
        saveRecipe("其他家庭菜谱", testFamily2)

        val result = recipeRepository.findByFamily_IdOrderByCreatedAtDesc(testFamily1.id)

        assertThat(result).hasSize(2)
        // 按 createdAt 降序，最新的在前
        assertThat(result[0].title).isEqualTo("糖醋排骨")
        assertThat(result[1].title).isEqualTo("红烧肉")
    }

    @Test
    fun `findByFamily_IdOrderByCreatedAtDesc - 不返回其他家庭的菜谱`() {
        saveRecipe("家庭1菜谱", testFamily1)
        saveRecipe("家庭2菜谱", testFamily2)

        val result = recipeRepository.findByFamily_IdOrderByCreatedAtDesc(testFamily1.id)

        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("家庭1菜谱")
    }

    @Test
    fun `findByFamily_IdOrderByCreatedAtDesc - 家庭无菜谱时返回空列表`() {
        val result = recipeRepository.findByFamily_IdOrderByCreatedAtDesc(testFamily1.id)

        assertThat(result).isEmpty()
    }

    // ---------- findByShareToken ----------

    @Test
    fun `findByShareToken - 存在时返回菜谱`() {
        saveRecipe("分享菜谱", testFamily1, shareToken = "share-token-123")

        val result = recipeRepository.findByShareToken("share-token-123")

        assertThat(result).isNotNull
        assertThat(result!!.title).isEqualTo("分享菜谱")
    }

    @Test
    fun `findByShareToken - 不存在时返回 null`() {
        val result = recipeRepository.findByShareToken("non-existent-token")

        assertThat(result).isNull()
    }

    @Test
    fun `findByShareToken - 只返回精确匹配`() {
        saveRecipe("菜谱A", testFamily1, shareToken = "token-abc")
        saveRecipe("菜谱B", testFamily1, shareToken = "token-xyz")

        val result = recipeRepository.findByShareToken("token-abc")

        assertThat(result).isNotNull
        assertThat(result!!.title).isEqualTo("菜谱A")
    }

    // ---------- save ----------

    @Test
    fun `save - 成功保存菜谱并生成 ID`() {
        val recipe = Recipe(
            title = "新菜谱",
            description = "测试描述",
            ingredients = "[{\"name\":\"食材\",\"amount\":\"100\",\"unit\":\"克\"}]",
            steps = "[{\"order\":1,\"description\":\"步骤1\"}]",
            author = testUser,
            family = testFamily1
        )

        val saved = recipeRepository.save(recipe)
        entityManager.flush()

        assertThat(saved.id).isGreaterThan(0)
        assertThat(saved.title).isEqualTo("新菜谱")
        assertThat(saved.createdAt).isNotNull
    }

    @Test
    fun `save - shareToken 唯一约束生效`() {
        saveRecipe("菜谱1", testFamily1, shareToken = "unique-token")

        org.junit.jupiter.api.assertThrows<Exception> {
            saveRecipe("菜谱2", testFamily1, shareToken = "unique-token")
        }
    }

    // ---------- delete ----------

    @Test
    fun `delete - 成功删除菜谱`() {
        val recipe = saveRecipe("待删除菜谱", testFamily1)
        val recipeId = recipe.id

        recipeRepository.delete(recipe)
        entityManager.flush()

        val found = recipeRepository.findById(recipeId)
        assertThat(found).isEmpty
    }

    // ---------- findById ----------

    @Test
    fun `findById - ID 存在时返回菜谱`() {
        val recipe = saveRecipe("查询测试", testFamily1)

        val found = recipeRepository.findById(recipe.id)

        assertThat(found).isPresent
        assertThat(found.get().title).isEqualTo("查询测试")
    }

    @Test
    fun `findById - ID 不存在时返回空`() {
        val found = recipeRepository.findById(999L)

        assertThat(found).isEmpty
    }
}
