package com.domu.integration

import com.domu.dto.UserResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = [
    // 使用 SQLite 共享缓存命名内存数据库：file: 协议 + mode=memory + cache=shared
    // 连接池中所有连接共享同一个内存数据库，避免每条连接拥有独立内存库导致跨请求数据不可见
    "spring.datasource.url=jdbc:sqlite:file:domu_auth_test?mode=memory&cache=shared",
    "spring.datasource.driver-class-name=org.sqlite.JDBC",
    "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.jwt.secret=test-secret-key-that-is-at-least-32-chars-long",
    "app.upload.dir=/tmp/domu-test-uploads",
    "app.base-url=http://localhost:8080"
])
class AuthIntegrationTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    // TestRestTemplate 默认不跟随重定向，也不抛异常，适合断言 HTTP 状态

    private fun jsonHeaders(): HttpHeaders = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
    }

    private fun post(url: String, body: String) =
        restTemplate.postForEntity(url, HttpEntity(body, jsonHeaders()), String::class.java)

    // ---------- register ----------

    @Test
    fun `POST register - 新邮箱注册成功返回 201 和用户信息`() {
        val response = post(
            "/api/v1/auth/register",
            """{"email":"reg_test@example.com","password":"pass1234","name":"注册用户"}"""
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body).contains("reg_test@example.com")
        // 响应头中包含 Set-Cookie
        assertThat(response.headers["Set-Cookie"]).anyMatch { it.startsWith("access_token=") }
        assertThat(response.headers["Set-Cookie"]).anyMatch { it.startsWith("refresh_token=") }
        // Cookie 为 HttpOnly
        assertThat(response.headers["Set-Cookie"]).anyMatch { it.contains("HttpOnly") }
    }

    @Test
    fun `POST register - 重复邮箱返回 409`() {
        val body = """{"email":"dup@example.com","password":"pass1234","name":"重复用户"}"""
        post("/api/v1/auth/register", body)           // 第一次注册

        val response = post("/api/v1/auth/register", body)  // 重复注册

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    }

    // ---------- login ----------

    @Test
    fun `POST login - 正确凭据登录返回 200 和 JWT Cookie`() {
        // 先注册
        post(
            "/api/v1/auth/register",
            """{"email":"login_ok@example.com","password":"pass1234","name":"登录用户"}"""
        )

        // 再登录
        val response = post(
            "/api/v1/auth/login",
            """{"email":"login_ok@example.com","password":"pass1234"}"""
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).contains("login_ok@example.com")
        assertThat(response.headers["Set-Cookie"]).anyMatch { it.startsWith("access_token=") }
    }

    @Test
    fun `POST login - 错误密码返回 401`() {
        post(
            "/api/v1/auth/register",
            """{"email":"login_fail@example.com","password":"correct","name":"测试用户"}"""
        )

        val response = post(
            "/api/v1/auth/login",
            """{"email":"login_fail@example.com","password":"wrong_password"}"""
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `POST login - 不存在的邮箱返回 401`() {
        val response = post(
            "/api/v1/auth/login",
            """{"email":"ghost@example.com","password":"any"}"""
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    // ---------- logout ----------

    @Test
    fun `POST logout - 返回 204 并清除 Cookie（Max-Age=0）`() {
        val response = post("/api/v1/auth/logout", "")

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        // 验证 Cookie 被清除（Max-Age=0）
        val setCookieHeaders = response.headers["Set-Cookie"] ?: emptyList()
        assertThat(setCookieHeaders).anyMatch {
            it.contains("access_token") && it.contains("Max-Age=0")
        }
    }

    // ---------- register → login 完整流程 ----------

    @Test
    fun `注册后立即登录 - 返回完整的用户信息`() {
        // 注册
        val registerResponse = post(
            "/api/v1/auth/register",
            """{"email":"flow@example.com","password":"flowpass","name":"流程用户"}"""
        )
        assertThat(registerResponse.statusCode).isEqualTo(HttpStatus.CREATED)

        // 使用注册时返回的信息登录
        val loginResponse = post(
            "/api/v1/auth/login",
            """{"email":"flow@example.com","password":"flowpass"}"""
        )
        assertThat(loginResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(loginResponse.body).contains("flow@example.com")
        assertThat(loginResponse.body).contains("流程用户")
    }
}
