package com.domu.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource

/**
 * 菜谱模块集成测试。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:sqlite:file:domu_recipe_test?mode=memory&cache=shared",
    "spring.datasource.driver-class-name=org.sqlite.JDBC",
    "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.jwt.secret=test-secret-key-that-is-at-least-32-chars-long",
    "app.upload.dir=/tmp/domu-test-uploads",
    "app.base-url=http://localhost:8080"
])
class RecipeIntegrationTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private fun jsonHeaders(cookies: String? = null): HttpHeaders = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
        if (cookies != null) set(HttpHeaders.COOKIE, cookies)
    }

    private fun registerAndGetCookie(email: String, name: String): String {
        val response = restTemplate.postForEntity(
            "/api/v1/auth/register",
            HttpEntity(
                """{"email":"$email","password":"pass1234","name":"$name"}""",
                jsonHeaders()
            ),
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        return response.headers["Set-Cookie"]!!
            .first { it.startsWith("access_token=") }
            .substringBefore(";")
    }

    private fun createFamily(cookie: String, name: String): Long {
        val response = restTemplate.postForEntity(
            "/api/v1/families",
            HttpEntity("""{"name":"$name"}""", jsonHeaders(cookie)),
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        return objectMapper.readTree(response.body)["id"].asLong()
    }

    private fun get(url: String, cookie: String) =
        restTemplate.exchange(
            url, HttpMethod.GET,
            HttpEntity<String>(null, jsonHeaders(cookie)),
            String::class.java
        )

    private fun post(url: String, body: String, cookie: String? = null) =
        restTemplate.postForEntity(
            url,
            HttpEntity(body, jsonHeaders(cookie)),
            String::class.java
        )

    private fun put(url: String, body: String, cookie: String) =
        restTemplate.exchange(
            url, HttpMethod.PUT,
            HttpEntity(body, jsonHeaders(cookie)),
            String::class.java
        )

    private fun delete(url: String, cookie: String) =
        restTemplate.exchange(
            url, HttpMethod.DELETE,
            HttpEntity<String>(null, jsonHeaders(cookie)),
            String::class.java
        )

    // ---------- 创建菜谱 ----------

    @Test
    fun `创建菜谱 - 成功返回 201`() {
        val cookie = registerAndGetCookie("recipe_create@test.com", "创建者")
        val familyId = createFamily(cookie, "测试家庭")

        val response = post(
            "/api/v1/recipes",
            """
                {
                    "title": "红烧肉",
                    "description": "经典菜品",
                    "ingredients": [{"name": "猪肉", "amount": "500", "unit": "克"}],
                    "steps": [{"order": 1, "description": "切块"}],
                    "familyId": $familyId
                }
            """.trimIndent(),
            cookie
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val body = objectMapper.readTree(response.body)
        assertThat(body["title"].asText()).isEqualTo("红烧肉")
        assertThat(body["familyId"].asLong()).isEqualTo(familyId)
        assertThat(body["ingredients"].size()).isEqualTo(1)
    }

    @Test
    fun `创建菜谱 - 非成员返回 403`() {
        val adminCookie = registerAndGetCookie("recipe_admin@test.com", "管理员")
        val familyId = createFamily(adminCookie, "私有家庭")

        val outsiderCookie = registerAndGetCookie("recipe_outsider@test.com", "外部用户")

        val response = post(
            "/api/v1/recipes",
            """{"title": "测试", "ingredients": [], "steps": [], "familyId": $familyId}""",
            outsiderCookie
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    // ---------- 列出菜谱 ----------

    @Test
    fun `列出菜谱 - 返回家庭所有菜谱`() {
        val cookie = registerAndGetCookie("recipe_list@test.com", "用户")
        val familyId = createFamily(cookie, "菜谱家庭")

        // 创建两个菜谱
        post(
            "/api/v1/recipes",
            """{"title": "红烧肉", "ingredients": [], "steps": [], "familyId": $familyId}""",
            cookie
        )
        post(
            "/api/v1/recipes",
            """{"title": "糖醋排骨", "ingredients": [], "steps": [], "familyId": $familyId}""",
            cookie
        )

        val response = get("/api/v1/recipes?familyId=$familyId", cookie)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val recipes = objectMapper.readTree(response.body)
        assertThat(recipes.size()).isEqualTo(2)
    }

    // ---------- 获取菜谱详情 ----------

    @Test
    fun `获取菜谱详情 - 返回完整信息`() {
        val cookie = registerAndGetCookie("recipe_detail@test.com", "用户")
        val familyId = createFamily(cookie, "详情家庭")

        val createResp = post(
            "/api/v1/recipes",
            """
                {
                    "title": "宫保鸡丁",
                    "description": "四川名菜",
                    "ingredients": [{"name": "鸡肉", "amount": "300", "unit": "克"}],
                    "steps": [{"order": 1, "description": "切丁"}, {"order": 2, "description": "炒制"}],
                    "familyId": $familyId
                }
            """.trimIndent(),
            cookie
        )
        val recipeId = objectMapper.readTree(createResp.body)["id"].asLong()

        val response = get("/api/v1/recipes/$recipeId", cookie)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = objectMapper.readTree(response.body)
        assertThat(body["title"].asText()).isEqualTo("宫保鸡丁")
        assertThat(body["description"].asText()).isEqualTo("四川名菜")
        assertThat(body["ingredients"].size()).isEqualTo(1)
        assertThat(body["steps"].size()).isEqualTo(2)
    }

    // ---------- 更新菜谱 ----------

    @Test
    fun `更新菜谱 - 成功更新内容`() {
        val cookie = registerAndGetCookie("recipe_update@test.com", "用户")
        val familyId = createFamily(cookie, "更新家庭")

        val createResp = post(
            "/api/v1/recipes",
            """{"title": "原标题", "ingredients": [], "steps": [], "familyId": $familyId}""",
            cookie
        )
        val recipeId = objectMapper.readTree(createResp.body)["id"].asLong()

        val updateResp = put(
            "/api/v1/recipes/$recipeId",
            """
                {
                    "title": "新标题",
                    "description": "新描述",
                    "ingredients": [{"name": "新食材", "amount": "100", "unit": "克"}],
                    "steps": [{"order": 1, "description": "新步骤"}]
                }
            """.trimIndent(),
            cookie
        )

        assertThat(updateResp.statusCode).isEqualTo(HttpStatus.OK)
        val body = objectMapper.readTree(updateResp.body)
        assertThat(body["title"].asText()).isEqualTo("新标题")
        assertThat(body["description"].asText()).isEqualTo("新描述")
    }

    // ---------- 删除菜谱 ----------

    @Test
    fun `删除菜谱 - 成功返回 204`() {
        val cookie = registerAndGetCookie("recipe_delete@test.com", "用户")
        val familyId = createFamily(cookie, "删除家庭")

        val createResp = post(
            "/api/v1/recipes",
            """{"title": "待删除", "ingredients": [], "steps": [], "familyId": $familyId}""",
            cookie
        )
        val recipeId = objectMapper.readTree(createResp.body)["id"].asLong()

        val deleteResp = delete("/api/v1/recipes/$recipeId", cookie)

        assertThat(deleteResp.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

        // 验证已删除
        val getResp = get("/api/v1/recipes/$recipeId", cookie)
        assertThat(getResp.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    // ---------- 分享菜谱 ----------

    @Test
    fun `分享菜谱 - 生成分享链接`() {
        val cookie = registerAndGetCookie("recipe_share@test.com", "用户")
        val familyId = createFamily(cookie, "分享家庭")

        val createResp = post(
            "/api/v1/recipes",
            """{"title": "分享菜谱", "ingredients": [], "steps": [], "familyId": $familyId}""",
            cookie
        )
        val recipeId = objectMapper.readTree(createResp.body)["id"].asLong()

        val shareResp = post("/api/v1/recipes/$recipeId/share", "", cookie)

        assertThat(shareResp.statusCode).isEqualTo(HttpStatus.OK)
        val body = objectMapper.readTree(shareResp.body)
        assertThat(body["shareToken"].asText()).isNotEmpty
        assertThat(body["shareUrl"].asText()).contains("/api/v1/recipes/shared/")
    }

    @Test
    fun `分享菜谱 - 公开访问无需认证`() {
        val cookie = registerAndGetCookie("recipe_public@test.com", "用户")
        val familyId = createFamily(cookie, "公开家庭")

        val createResp = post(
            "/api/v1/recipes",
            """{"title": "公开菜谱", "ingredients": [], "steps": [], "familyId": $familyId}""",
            cookie
        )
        val recipeId = objectMapper.readTree(createResp.body)["id"].asLong()

        val shareResp = post("/api/v1/recipes/$recipeId/share", "", cookie)
        val shareToken = objectMapper.readTree(shareResp.body)["shareToken"].asText()

        // 无需认证访问
        val publicResp = restTemplate.getForEntity(
            "/api/v1/recipes/shared/$shareToken",
            String::class.java
        )

        assertThat(publicResp.statusCode).isEqualTo(HttpStatus.OK)
        val body = objectMapper.readTree(publicResp.body)
        assertThat(body["title"].asText()).isEqualTo("公开菜谱")
    }

    // ---------- 完整流程 ----------

    @Test
    fun `完整流程 - 创建、更新、分享、删除`() {
        val cookie = registerAndGetCookie("recipe_flow@test.com", "流程用户")
        val familyId = createFamily(cookie, "流程家庭")

        // 1. 创建
        val createResp = post(
            "/api/v1/recipes",
            """{"title": "流程菜谱", "ingredients": [], "steps": [], "familyId": $familyId}""",
            cookie
        )
        assertThat(createResp.statusCode).isEqualTo(HttpStatus.CREATED)
        val recipeId = objectMapper.readTree(createResp.body)["id"].asLong()

        // 2. 更新
        val updateResp = put(
            "/api/v1/recipes/$recipeId",
            """{"title": "更新后", "ingredients": [], "steps": []}""",
            cookie
        )
        assertThat(updateResp.statusCode).isEqualTo(HttpStatus.OK)

        // 3. 分享
        val shareResp = post("/api/v1/recipes/$recipeId/share", "", cookie)
        assertThat(shareResp.statusCode).isEqualTo(HttpStatus.OK)

        // 4. 删除
        val deleteResp = delete("/api/v1/recipes/$recipeId", cookie)
        assertThat(deleteResp.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }
}
