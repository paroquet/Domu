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
 * 用户模块集成测试。
 *
 * 测试 /api/v1/users/me 端点的获取和更新功能。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:sqlite:file:domu_user_test?mode=memory&cache=shared",
    "spring.datasource.driver-class-name=org.sqlite.JDBC",
    "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.jwt.secret=test-secret-key-that-is-at-least-32-chars-long",
    "app.upload.dir=/tmp/domu-test-uploads",
    "app.base-url=http://localhost:8080"
])
class UserIntegrationTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private fun jsonHeaders(cookies: String? = null): HttpHeaders = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
        if (cookies != null) set(HttpHeaders.COOKIE, cookies)
    }

    /** 注册用户，返回 access_token cookie 字符串 */
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

    /** GET 请求 */
    private fun get(url: String, cookie: String) =
        restTemplate.exchange(
            url, HttpMethod.GET,
            HttpEntity<String>(null, jsonHeaders(cookie)),
            String::class.java
        )

    /** PUT 请求 */
    private fun put(url: String, body: String, cookie: String) =
        restTemplate.exchange(
            url, HttpMethod.PUT,
            HttpEntity(body, jsonHeaders(cookie)),
            String::class.java
        )

    // ---------- GET /api/v1/users/me ----------

    @Test
    fun `获取当前用户信息 - 返回用户详情`() {
        val cookie = registerAndGetCookie("getme@test.com", "测试用户")

        val response = get("/api/v1/users/me", cookie)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = objectMapper.readTree(response.body)
        assertThat(body["email"].asText()).isEqualTo("getme@test.com")
        assertThat(body["name"].asText()).isEqualTo("测试用户")
        assertThat(body["id"].asLong()).isGreaterThan(0)
    }

    @Test
    fun `获取当前用户信息 - 未认证返回 4xx`() {
        val response = restTemplate.exchange(
            "/api/v1/users/me", HttpMethod.GET,
            HttpEntity<String>(null, jsonHeaders()),
            String::class.java
        )

        assertThat(response.statusCode.is4xxClientError).isTrue()
    }

    // ---------- PUT /api/v1/users/me ----------

    @Test
    fun `更新用户信息 - 更新用户名成功`() {
        val cookie = registerAndGetCookie("update_name@test.com", "原名")

        val response = put(
            "/api/v1/users/me",
            """{"name":"新名字"}""",
            cookie
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = objectMapper.readTree(response.body)
        assertThat(body["name"].asText()).isEqualTo("新名字")
        assertThat(body["email"].asText()).isEqualTo("update_name@test.com")
    }

    @Test
    fun `更新用户信息 - 更新用户名和头像成功`() {
        val cookie = registerAndGetCookie("update_all@test.com", "原名")

        val response = put(
            "/api/v1/users/me",
            """{"name":"新名字","avatarPath":"/uploads/avatar.png"}""",
            cookie
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = objectMapper.readTree(response.body)
        assertThat(body["name"].asText()).isEqualTo("新名字")
        assertThat(body["avatarPath"].asText()).isEqualTo("/uploads/avatar.png")
    }

    @Test
    fun `更新用户信息 - 未认证返回 4xx`() {
        val response = restTemplate.exchange(
            "/api/v1/users/me", HttpMethod.PUT,
            HttpEntity("""{"name":"新名字"}""", jsonHeaders()),
            String::class.java
        )

        assertThat(response.statusCode.is4xxClientError).isTrue()
    }

    @Test
    fun `更新用户信息 - 多次更新保持一致性`() {
        val cookie = registerAndGetCookie("multi_update@test.com", "初始名")

        // 第一次更新
        put("/api/v1/users/me", """{"name":"第一次更新"}""", cookie)

        // 第二次更新
        put("/api/v1/users/me", """{"name":"第二次更新","avatarPath":"/avatar1.png"}""", cookie)

        // 第三次更新（仅更新名字，头像应保持）
        val response = put("/api/v1/users/me", """{"name":"最终名字"}""", cookie)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = objectMapper.readTree(response.body)
        assertThat(body["name"].asText()).isEqualTo("最终名字")
        assertThat(body["avatarPath"].asText()).isEqualTo("/avatar1.png")
    }

    // ---------- 完整流程测试 ----------

    @Test
    fun `注册后获取用户信息 - 返回注册时的数据`() {
        val cookie = registerAndGetCookie("flow_user@test.com", "流程测试用户")

        val response = get("/api/v1/users/me", cookie)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = objectMapper.readTree(response.body)
        assertThat(body["email"].asText()).isEqualTo("flow_user@test.com")
        assertThat(body["name"].asText()).isEqualTo("流程测试用户")
        assertThat(body["avatarPath"].isNull).isTrue()
    }

    @Test
    fun `更新用户信息后重新获取 - 数据已更新`() {
        val cookie = registerAndGetCookie("verify_update@test.com", "原始名字")

        // 更新用户信息
        put(
            "/api/v1/users/me",
            """{"name":"更新后的名字","avatarPath":"/new-avatar.png"}""",
            cookie
        )

        // 重新获取验证
        val response = get("/api/v1/users/me", cookie)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = objectMapper.readTree(response.body)
        assertThat(body["name"].asText()).isEqualTo("更新后的名字")
        assertThat(body["avatarPath"].asText()).isEqualTo("/new-avatar.png")
    }

    // ---------- 边界条件测试 ----------

    @Test
    fun `更新用户信息 - 支持特殊字符用户名`() {
        val cookie = registerAndGetCookie("special_chars@test.com", "原名")

        val response = put(
            "/api/v1/users/me",
            """{"name":"李明 (管理员)"}""",
            cookie
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = objectMapper.readTree(response.body)
        assertThat(body["name"].asText()).isEqualTo("李明 (管理员)")
    }

    @Test
    fun `更新用户信息 - 支持 emoji 用户名`() {
        val cookie = registerAndGetCookie("emoji_name@test.com", "原名")

        val response = put(
            "/api/v1/users/me",
            """{"name":"小明 🎉"}""",
            cookie
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = objectMapper.readTree(response.body)
        assertThat(body["name"].asText()).isEqualTo("小明 🎉")
    }

    @Test
    fun `更新用户信息 - 幂等性（相同名字多次更新）`() {
        val cookie = registerAndGetCookie("idempotent@test.com", "固定名字")

        // 多次更新相同名字
        put("/api/v1/users/me", """{"name":"固定名字"}""", cookie)
        put("/api/v1/users/me", """{"name":"固定名字"}""", cookie)
        val response = put("/api/v1/users/me", """{"name":"固定名字"}""", cookie)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = objectMapper.readTree(response.body)
        assertThat(body["name"].asText()).isEqualTo("固定名字")
    }

    @Test
    fun `更新用户信息 - 清空头像后保持为空`() {
        val cookie = registerAndGetCookie("clear_avatar@test.com", "测试用户")

        // 设置头像
        put("/api/v1/users/me", """{"name":"测试用户","avatarPath":"/avatar.png"}""", cookie)

        // 仅更新名字（不传 avatarPath），头像应保持
        val response = put("/api/v1/users/me", """{"name":"新名字"}""", cookie)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = objectMapper.readTree(response.body)
        assertThat(body["name"].asText()).isEqualTo("新名字")
        assertThat(body["avatarPath"].asText()).isEqualTo("/avatar.png")
    }
}
