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
 * 做菜记录模块集成测试。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:sqlite:file:domu_cooking_test?mode=memory&cache=shared",
    "spring.datasource.driver-class-name=org.sqlite.JDBC",
    "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.jwt.secret=test-secret-key-that-is-at-least-32-chars-long",
    "app.upload.dir=/tmp/domu-test-uploads",
    "app.base-url=http://localhost:8080"
])
class CookingRecordIntegrationTest {

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

    private fun createRecipe(cookie: String, familyId: Long, title: String): Long {
        val response = restTemplate.postForEntity(
            "/api/v1/recipes",
            HttpEntity(
                """{"title":"$title","ingredients":[],"steps":[],"familyId":$familyId}""",
                jsonHeaders(cookie)
            ),
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

    private fun post(url: String, body: String, cookie: String) =
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

    // ---------- 创建做菜记录 ----------

    @Test
    fun `创建做菜记录 - 成功返回 201`() {
        val cookie = registerAndGetCookie("cooking_create@test.com", "创建者")
        val familyId = createFamily(cookie, "测试家庭")
        val recipeId = createRecipe(cookie, familyId, "红烧肉")

        val response = post(
            "/api/v1/cooking-records",
            """
                {
                    "recipeId": $recipeId,
                    "familyId": $familyId,
                    "cookedAt": "2026-03-01T12:00:00Z",
                    "notes": "第一次做，味道不错",
                    "images": ["/img1.jpg"]
                }
            """.trimIndent(),
            cookie
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val body = objectMapper.readTree(response.body)
        assertThat(body["recipeId"].asLong()).isEqualTo(recipeId)
        assertThat(body["recipeTitle"].asText()).isEqualTo("红烧肉")
        assertThat(body["notes"].asText()).isEqualTo("第一次做，味道不错")
        assertThat(body["images"].size()).isEqualTo(1)
    }

    @Test
    fun `创建做菜记录 - 非成员返回 403`() {
        val adminCookie = registerAndGetCookie("cooking_admin@test.com", "管理员")
        val familyId = createFamily(adminCookie, "私有家庭")
        val recipeId = createRecipe(adminCookie, familyId, "私有菜谱")

        val outsiderCookie = registerAndGetCookie("cooking_outsider@test.com", "外部用户")

        val response = post(
            "/api/v1/cooking-records",
            """{"recipeId": $recipeId, "familyId": $familyId, "cookedAt": "2026-03-01T12:00:00Z"}""",
            outsiderCookie
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `创建做菜记录 - 菜谱不存在返回 404`() {
        val cookie = registerAndGetCookie("cooking_notfound@test.com", "用户")
        val familyId = createFamily(cookie, "测试家庭")

        val response = post(
            "/api/v1/cooking-records",
            """{"recipeId": 9999, "familyId": $familyId, "cookedAt": "2026-03-01T12:00:00Z"}""",
            cookie
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    // ---------- 列出做菜记录 ----------

    @Test
    fun `列出做菜记录 - 返回家庭所有记录`() {
        val cookie = registerAndGetCookie("cooking_list@test.com", "用户")
        val familyId = createFamily(cookie, "记录家庭")
        val recipeId = createRecipe(cookie, familyId, "测试菜谱")

        // 创建两条记录
        post(
            "/api/v1/cooking-records",
            """{"recipeId": $recipeId, "familyId": $familyId, "cookedAt": "2026-03-01T12:00:00Z", "notes": "记录1"}""",
            cookie
        )
        post(
            "/api/v1/cooking-records",
            """{"recipeId": $recipeId, "familyId": $familyId, "cookedAt": "2026-03-02T12:00:00Z", "notes": "记录2"}""",
            cookie
        )

        val response = get("/api/v1/cooking-records?familyId=$familyId", cookie)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val records = objectMapper.readTree(response.body)
        assertThat(records.size()).isEqualTo(2)
    }

    @Test
    fun `列出做菜记录 - 按菜谱筛选`() {
        val cookie = registerAndGetCookie("cooking_filter@test.com", "用户")
        val familyId = createFamily(cookie, "筛选家庭")
        val recipe1Id = createRecipe(cookie, familyId, "红烧肉")
        val recipe2Id = createRecipe(cookie, familyId, "糖醋排骨")

        // 红烧肉两条记录
        post(
            "/api/v1/cooking-records",
            """{"recipeId": $recipe1Id, "familyId": $familyId, "cookedAt": "2026-03-01T12:00:00Z"}""",
            cookie
        )
        post(
            "/api/v1/cooking-records",
            """{"recipeId": $recipe1Id, "familyId": $familyId, "cookedAt": "2026-03-02T12:00:00Z"}""",
            cookie
        )
        // 糖醋排骨一条记录
        post(
            "/api/v1/cooking-records",
            """{"recipeId": $recipe2Id, "familyId": $familyId, "cookedAt": "2026-03-03T12:00:00Z"}""",
            cookie
        )

        val response = get("/api/v1/cooking-records?familyId=$familyId&recipeId=$recipe1Id", cookie)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val records = objectMapper.readTree(response.body)
        assertThat(records.size()).isEqualTo(2)
        assertThat(records.all { it["recipeId"].asLong() == recipe1Id }).isTrue()
    }

    // ---------- 获取做菜记录详情 ----------

    @Test
    fun `获取做菜记录详情 - 返回完整信息`() {
        val cookie = registerAndGetCookie("cooking_detail@test.com", "用户")
        val familyId = createFamily(cookie, "详情家庭")
        val recipeId = createRecipe(cookie, familyId, "宫保鸡丁")

        val createResp = post(
            "/api/v1/cooking-records",
            """
                {
                    "recipeId": $recipeId,
                    "familyId": $familyId,
                    "cookedAt": "2026-03-15T18:30:00Z",
                    "notes": "详细的做菜心得",
                    "images": ["/img1.jpg", "/img2.jpg"]
                }
            """.trimIndent(),
            cookie
        )
        val recordId = objectMapper.readTree(createResp.body)["id"].asLong()

        val response = get("/api/v1/cooking-records/$recordId", cookie)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = objectMapper.readTree(response.body)
        assertThat(body["recipeTitle"].asText()).isEqualTo("宫保鸡丁")
        assertThat(body["notes"].asText()).isEqualTo("详细的做菜心得")
        assertThat(body["images"].size()).isEqualTo(2)
        assertThat(body["cookedAt"].asText()).isEqualTo("2026-03-15T18:30:00Z")
    }

    @Test
    fun `获取做菜记录详情 - 记录不存在返回 404`() {
        val cookie = registerAndGetCookie("cooking_404@test.com", "用户")
        createFamily(cookie, "404家庭")

        val response = get("/api/v1/cooking-records/99999", cookie)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    // ---------- 更新做菜记录 ----------

    @Test
    fun `更新做菜记录 - 成功更新内容`() {
        val cookie = registerAndGetCookie("cooking_update@test.com", "用户")
        val familyId = createFamily(cookie, "更新家庭")
        val recipeId = createRecipe(cookie, familyId, "更新菜谱")

        val createResp = post(
            "/api/v1/cooking-records",
            """{"recipeId": $recipeId, "familyId": $familyId, "cookedAt": "2026-03-01T12:00:00Z", "notes": "原始心得"}""",
            cookie
        )
        val recordId = objectMapper.readTree(createResp.body)["id"].asLong()

        val updateResp = put(
            "/api/v1/cooking-records/$recordId",
            """
                {
                    "cookedAt": "2026-03-02T15:00:00Z",
                    "notes": "更新后的心得",
                    "images": ["/new-img.jpg"]
                }
            """.trimIndent(),
            cookie
        )

        assertThat(updateResp.statusCode).isEqualTo(HttpStatus.OK)
        val body = objectMapper.readTree(updateResp.body)
        assertThat(body["notes"].asText()).isEqualTo("更新后的心得")
        assertThat(body["images"].size()).isEqualTo(1)
    }

    // ---------- 删除做菜记录 ----------

    @Test
    fun `删除做菜记录 - 成功返回 204`() {
        val cookie = registerAndGetCookie("cooking_delete@test.com", "用户")
        val familyId = createFamily(cookie, "删除家庭")
        val recipeId = createRecipe(cookie, familyId, "删除菜谱")

        val createResp = post(
            "/api/v1/cooking-records",
            """{"recipeId": $recipeId, "familyId": $familyId, "cookedAt": "2026-03-01T12:00:00Z"}""",
            cookie
        )
        val recordId = objectMapper.readTree(createResp.body)["id"].asLong()

        val deleteResp = delete("/api/v1/cooking-records/$recordId", cookie)

        assertThat(deleteResp.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

        // 验证已删除
        val getResp = get("/api/v1/cooking-records/$recordId", cookie)
        assertThat(getResp.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    // ---------- 完整流程 ----------

    @Test
    fun `完整流程 - 创建、查看、更新、删除做菜记录`() {
        val cookie = registerAndGetCookie("cooking_flow@test.com", "流程用户")
        val familyId = createFamily(cookie, "流程家庭")
        val recipeId = createRecipe(cookie, familyId, "流程菜谱")

        // 1. 创建
        val createResp = post(
            "/api/v1/cooking-records",
            """{"recipeId": $recipeId, "familyId": $familyId, "cookedAt": "2026-03-01T12:00:00Z", "notes": "创建"}""",
            cookie
        )
        assertThat(createResp.statusCode).isEqualTo(HttpStatus.CREATED)
        val recordId = objectMapper.readTree(createResp.body)["id"].asLong()

        // 2. 查看详情
        val getResp = get("/api/v1/cooking-records/$recordId", cookie)
        assertThat(getResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(objectMapper.readTree(getResp.body)["notes"].asText()).isEqualTo("创建")

        // 3. 查看列表
        val listResp = get("/api/v1/cooking-records?familyId=$familyId", cookie)
        assertThat(listResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(objectMapper.readTree(listResp.body).size()).isEqualTo(1)

        // 4. 更新
        val updateResp = put(
            "/api/v1/cooking-records/$recordId",
            """{"cookedAt": "2026-03-02T12:00:00Z", "notes": "更新"}""",
            cookie
        )
        assertThat(updateResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(objectMapper.readTree(updateResp.body)["notes"].asText()).isEqualTo("更新")

        // 5. 删除
        val deleteResp = delete("/api/v1/cooking-records/$recordId", cookie)
        assertThat(deleteResp.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

        // 6. 确认删除
        val verifyResp = get("/api/v1/cooking-records/$recordId", cookie)
        assertThat(verifyResp.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    // ---------- 多用户场景 ----------

    @Test
    fun `多用户场景 - 同家庭成员可以查看彼此的做菜记录`() {
        // 用户1创建家庭和菜谱
        val user1Cookie = registerAndGetCookie("cooking_user1@test.com", "用户1")
        val familyId = createFamily(user1Cookie, "共享家庭")
        val recipeId = createRecipe(user1Cookie, familyId, "共享菜谱")

        // 用户1创建做菜记录
        post(
            "/api/v1/cooking-records",
            """{"recipeId": $recipeId, "familyId": $familyId, "cookedAt": "2026-03-01T12:00:00Z", "notes": "用户1的记录"}""",
            user1Cookie
        )

        // 用户2注册并加入家庭
        val user2Cookie = registerAndGetCookie("cooking_user2@test.com", "用户2")

        // 获取邀请码
        val inviteResp = restTemplate.postForEntity(
            "/api/v1/families/$familyId/invite-code",
            HttpEntity<String>(null, jsonHeaders(user1Cookie)),
            String::class.java
        )
        val inviteCode = objectMapper.readTree(inviteResp.body)["inviteCode"].asText()

        // 用户2加入家庭
        restTemplate.postForEntity(
            "/api/v1/families/join",
            HttpEntity("""{"inviteCode":"$inviteCode"}""", jsonHeaders(user2Cookie)),
            String::class.java
        )

        // 用户2查看做菜记录
        val listResp = get("/api/v1/cooking-records?familyId=$familyId", user2Cookie)

        assertThat(listResp.statusCode).isEqualTo(HttpStatus.OK)
        val records = objectMapper.readTree(listResp.body)
        assertThat(records.size()).isEqualTo(1)
        assertThat(records[0]["notes"].asText()).isEqualTo("用户1的记录")
    }
}
