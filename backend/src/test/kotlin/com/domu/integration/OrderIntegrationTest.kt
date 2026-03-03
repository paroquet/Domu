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
 * 订单模块集成测试。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:sqlite:file:domu_order_test?mode=memory&cache=shared",
    "spring.datasource.driver-class-name=org.sqlite.JDBC",
    "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.jwt.secret=test-secret-key-that-is-at-least-32-chars-long",
    "app.upload.dir=/tmp/domu-test-uploads",
    "app.base-url=http://localhost:8080"
])
class OrderIntegrationTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private fun jsonHeaders(cookies: String? = null): HttpHeaders = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
        if (cookies != null) set(HttpHeaders.COOKIE, cookies)
    }

    private fun registerAndGetCookieWithId(email: String, name: String): Pair<String, Long> {
        val response = restTemplate.postForEntity(
            "/api/v1/auth/register",
            HttpEntity(
                """{"email":"$email","password":"pass1234","name":"$name"}""",
                jsonHeaders()
            ),
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val cookie = response.headers["Set-Cookie"]!!
            .first { it.startsWith("access_token=") }
            .substringBefore(";")
        val userId = objectMapper.readTree(response.body)["id"].asLong()
        return Pair(cookie, userId)
    }

    private fun createFamilyAndGetIdWithInviteCode(cookie: String, name: String): Pair<Long, String> {
        val response = restTemplate.postForEntity(
            "/api/v1/families",
            HttpEntity("""{"name":"$name"}""", jsonHeaders(cookie)),
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val body = objectMapper.readTree(response.body)
        return Pair(body["id"].asLong(), body["inviteCode"].asText())
    }

    private fun joinFamily(cookie: String, inviteCode: String) {
        val response = restTemplate.postForEntity(
            "/api/v1/families/join",
            HttpEntity("""{"inviteCode":"$inviteCode"}""", jsonHeaders(cookie)),
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    private fun createRecipe(cookie: String, familyId: Long, title: String, ingredients: String = "[]"): Long {
        val response = restTemplate.postForEntity(
            "/api/v1/recipes",
            HttpEntity(
                """{"title":"$title","ingredients":$ingredients,"steps":[],"familyId":$familyId}""",
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

    // ---------- 创建订单 ----------

    @Test
    fun `创建订单 - 成功返回 201`() {
        val (cookie, userId) = registerAndGetCookieWithId("order_create@test.com", "创建者")
        val (familyId, _) = createFamilyAndGetIdWithInviteCode(cookie, "订单家庭")
        val recipeId = createRecipe(cookie, familyId, "红烧肉")

        val response = post(
            "/api/v1/orders",
            """
                {
                    "familyId": $familyId,
                    "orderedForId": $userId,
                    "recipeId": $recipeId,
                    "plannedDate": "2026-03-03"
                }
            """.trimIndent(),
            cookie
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val body = objectMapper.readTree(response.body)
        assertThat(body["status"].asText()).isEqualTo("PENDING")
        assertThat(body["recipeTitle"].asText()).isEqualTo("红烧肉")
        assertThat(body["plannedDate"].asText()).isEqualTo("2026-03-03")
    }

    @Test
    fun `创建订单 - 菜谱不存在返回 404`() {
        val (cookie, userId) = registerAndGetCookieWithId("order_nofound@test.com", "用户")
        val (familyId, _) = createFamilyAndGetIdWithInviteCode(cookie, "测试家庭")

        val response = post(
            "/api/v1/orders",
            """{"familyId": $familyId, "orderedForId": $userId, "recipeId": 9999, "plannedDate": "2026-03-03"}""",
            cookie
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    // ---------- 列出订单 ----------

    @Test
    fun `列出订单 - 返回家庭所有订单`() {
        val (cookie, userId) = registerAndGetCookieWithId("order_list@test.com", "用户")
        val (familyId, _) = createFamilyAndGetIdWithInviteCode(cookie, "列表家庭")
        val recipeId = createRecipe(cookie, familyId, "测试菜谱")

        // 创建两个订单
        post(
            "/api/v1/orders",
            """{"familyId": $familyId, "orderedForId": $userId, "recipeId": $recipeId, "plannedDate": "2026-03-03"}""",
            cookie
        )
        post(
            "/api/v1/orders",
            """{"familyId": $familyId, "orderedForId": $userId, "recipeId": $recipeId, "plannedDate": "2026-03-04"}""",
            cookie
        )

        val response = get("/api/v1/orders?familyId=$familyId", cookie)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val orders = objectMapper.readTree(response.body)
        assertThat(orders.size()).isEqualTo(2)
    }

    @Test
    fun `列出订单 - 按日期筛选`() {
        val (cookie, userId) = registerAndGetCookieWithId("order_date@test.com", "用户")
        val (familyId, _) = createFamilyAndGetIdWithInviteCode(cookie, "日期家庭")
        val recipeId = createRecipe(cookie, familyId, "测试菜谱")

        post(
            "/api/v1/orders",
            """{"familyId": $familyId, "orderedForId": $userId, "recipeId": $recipeId, "plannedDate": "2026-03-03"}""",
            cookie
        )
        post(
            "/api/v1/orders",
            """{"familyId": $familyId, "orderedForId": $userId, "recipeId": $recipeId, "plannedDate": "2026-03-04"}""",
            cookie
        )

        val response = get("/api/v1/orders?familyId=$familyId&date=2026-03-03", cookie)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val orders = objectMapper.readTree(response.body)
        assertThat(orders.size()).isEqualTo(1)
        assertThat(orders[0]["plannedDate"].asText()).isEqualTo("2026-03-03")
    }

    // ---------- 更新订单状态 ----------

    @Test
    fun `更新订单状态 - 标记为完成`() {
        val (cookie, userId) = registerAndGetCookieWithId("order_done@test.com", "用户")
        val (familyId, _) = createFamilyAndGetIdWithInviteCode(cookie, "完成家庭")
        val recipeId = createRecipe(cookie, familyId, "测试菜谱")

        val createResp = post(
            "/api/v1/orders",
            """{"familyId": $familyId, "orderedForId": $userId, "recipeId": $recipeId, "plannedDate": "2026-03-03"}""",
            cookie
        )
        val orderId = objectMapper.readTree(createResp.body)["id"].asLong()

        val updateResp = put(
            "/api/v1/orders/$orderId/status",
            """{"status": "DONE"}""",
            cookie
        )

        assertThat(updateResp.statusCode).isEqualTo(HttpStatus.OK)
        val body = objectMapper.readTree(updateResp.body)
        assertThat(body["status"].asText()).isEqualTo("DONE")
    }

    @Test
    fun `更新订单状态 - 取消订单`() {
        val (cookie, userId) = registerAndGetCookieWithId("order_cancel@test.com", "用户")
        val (familyId, _) = createFamilyAndGetIdWithInviteCode(cookie, "取消家庭")
        val recipeId = createRecipe(cookie, familyId, "测试菜谱")

        val createResp = post(
            "/api/v1/orders",
            """{"familyId": $familyId, "orderedForId": $userId, "recipeId": $recipeId, "plannedDate": "2026-03-03"}""",
            cookie
        )
        val orderId = objectMapper.readTree(createResp.body)["id"].asLong()

        val updateResp = put(
            "/api/v1/orders/$orderId/status",
            """{"status": "CANCELLED"}""",
            cookie
        )

        assertThat(updateResp.statusCode).isEqualTo(HttpStatus.OK)
        val body = objectMapper.readTree(updateResp.body)
        assertThat(body["status"].asText()).isEqualTo("CANCELLED")
    }

    @Test
    fun `更新订单状态 - 无效状态返回 400`() {
        val (cookie, userId) = registerAndGetCookieWithId("order_invalid@test.com", "用户")
        val (familyId, _) = createFamilyAndGetIdWithInviteCode(cookie, "无效家庭")
        val recipeId = createRecipe(cookie, familyId, "测试菜谱")

        val createResp = post(
            "/api/v1/orders",
            """{"familyId": $familyId, "orderedForId": $userId, "recipeId": $recipeId, "plannedDate": "2026-03-03"}""",
            cookie
        )
        val orderId = objectMapper.readTree(createResp.body)["id"].asLong()

        val updateResp = put(
            "/api/v1/orders/$orderId/status",
            """{"status": "INVALID_STATUS"}""",
            cookie
        )

        assertThat(updateResp.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    // ---------- 删除订单 ----------

    @Test
    fun `删除订单 - 成功返回 204`() {
        val (cookie, userId) = registerAndGetCookieWithId("order_delete@test.com", "用户")
        val (familyId, _) = createFamilyAndGetIdWithInviteCode(cookie, "删除家庭")
        val recipeId = createRecipe(cookie, familyId, "测试菜谱")

        val createResp = post(
            "/api/v1/orders",
            """{"familyId": $familyId, "orderedForId": $userId, "recipeId": $recipeId, "plannedDate": "2026-03-03"}""",
            cookie
        )
        val orderId = objectMapper.readTree(createResp.body)["id"].asLong()

        val deleteResp = delete("/api/v1/orders/$orderId", cookie)

        assertThat(deleteResp.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    // ---------- 买菜计划 ----------

    @Test
    fun `买菜计划 - 聚合多个订单的食材`() {
        val (cookie, userId) = registerAndGetCookieWithId("order_shopping@test.com", "用户")
        val (familyId, _) = createFamilyAndGetIdWithInviteCode(cookie, "买菜家庭")

        // 创建两个菜谱，有重叠的食材
        val recipe1 = createRecipe(
            cookie, familyId, "红烧肉",
            """[{"name":"猪肉","amount":"500","unit":"克"},{"name":"酱油","amount":"2","unit":"勺"}]"""
        )
        val recipe2 = createRecipe(
            cookie, familyId, "糖醋排骨",
            """[{"name":"猪肉","amount":"300","unit":"克"},{"name":"白糖","amount":"1","unit":"勺"}]"""
        )

        // 为同一天创建两个订单
        post(
            "/api/v1/orders",
            """{"familyId": $familyId, "orderedForId": $userId, "recipeId": $recipe1, "plannedDate": "2026-03-03"}""",
            cookie
        )
        post(
            "/api/v1/orders",
            """{"familyId": $familyId, "orderedForId": $userId, "recipeId": $recipe2, "plannedDate": "2026-03-03"}""",
            cookie
        )

        val response = get("/api/v1/orders/shopping-plan?familyId=$familyId&date=2026-03-03", cookie)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val items = objectMapper.readTree(response.body)
        assertThat(items.size()).isEqualTo(3) // 猪肉、酱油、白糖

        // 猪肉应该合并：500 + 300 = 800
        val pork = items.find { it["name"].asText() == "猪肉" }
        assertThat(pork).isNotNull
        assertThat(pork!!["amount"].asDouble()).isEqualTo(800.0)
    }

    @Test
    fun `买菜计划 - 排除已取消的订单`() {
        val (cookie, userId) = registerAndGetCookieWithId("order_exclude@test.com", "用户")
        val (familyId, _) = createFamilyAndGetIdWithInviteCode(cookie, "排除家庭")

        val recipe = createRecipe(
            cookie, familyId, "测试菜谱",
            """[{"name":"牛肉","amount":"200","unit":"克"}]"""
        )

        // 创建一个订单然后取消
        val createResp = post(
            "/api/v1/orders",
            """{"familyId": $familyId, "orderedForId": $userId, "recipeId": $recipe, "plannedDate": "2026-03-03"}""",
            cookie
        )
        val orderId = objectMapper.readTree(createResp.body)["id"].asLong()
        put("/api/v1/orders/$orderId/status", """{"status": "CANCELLED"}""", cookie)

        val response = get("/api/v1/orders/shopping-plan?familyId=$familyId&date=2026-03-03", cookie)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val items = objectMapper.readTree(response.body)
        assertThat(items.size()).isEqualTo(0) // 取消的订单不计入
    }

    @Test
    fun `买菜计划 - 非成员返回 403`() {
        val (adminCookie, _) = registerAndGetCookieWithId("order_admin@test.com", "管理员")
        val (familyId, _) = createFamilyAndGetIdWithInviteCode(adminCookie, "私有家庭")

        val (outsiderCookie, _) = registerAndGetCookieWithId("order_outsider@test.com", "外部用户")

        val response = get("/api/v1/orders/shopping-plan?familyId=$familyId&date=2026-03-03", outsiderCookie)

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    // ---------- 多成员场景 ----------

    @Test
    fun `多成员场景 - 为其他成员点菜`() {
        val (adminCookie, adminId) = registerAndGetCookieWithId("order_multi_admin@test.com", "管理员")
        val (familyId, inviteCode) = createFamilyAndGetIdWithInviteCode(adminCookie, "多成员家庭")

        val (memberCookie, memberId) = registerAndGetCookieWithId("order_multi_member@test.com", "成员")
        joinFamily(memberCookie, inviteCode)

        val recipeId = createRecipe(adminCookie, familyId, "测试菜谱")

        // 管理员为成员点菜
        val response = post(
            "/api/v1/orders",
            """{"familyId": $familyId, "orderedForId": $memberId, "recipeId": $recipeId, "plannedDate": "2026-03-03"}""",
            adminCookie
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val body = objectMapper.readTree(response.body)
        assertThat(body["orderedById"].asLong()).isEqualTo(adminId)
        assertThat(body["orderedForId"].asLong()).isEqualTo(memberId)
        assertThat(body["orderedByName"].asText()).isEqualTo("管理员")
        assertThat(body["orderedForName"].asText()).isEqualTo("成员")
    }
}
