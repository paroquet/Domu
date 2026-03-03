package com.domu.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource

/**
 * 家庭模块集成测试。
 *
 * 由于集成测试共享一个 SQLite 内存数据库（同一个 Spring Context），
 * 每个测试方法注册不同邮箱以避免冲突，或使用 @DirtiesContext 重置状态。
 * 这里使用邮箱唯一化策略（更高效）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = [
    // 使用 SQLite 共享缓存命名内存数据库：file: 协议 + mode=memory + cache=shared
    // 连接池中所有连接共享同一个内存数据库，避免每条连接拥有独立内存库导致跨请求数据不可见
    "spring.datasource.url=jdbc:sqlite:file:domu_family_test?mode=memory&cache=shared",
    "spring.datasource.driver-class-name=org.sqlite.JDBC",
    "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.jwt.secret=test-secret-key-that-is-at-least-32-chars-long",
    "app.upload.dir=/tmp/domu-test-uploads",
    "app.base-url=http://localhost:8080"
])
class FamilyIntegrationTest {

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
            .substringBefore(";")   // "access_token=<jwt>"
    }

    /** GET 请求 */
    private fun get(url: String, cookie: String) =
        restTemplate.exchange(
            url, HttpMethod.GET,
            HttpEntity<String>(null, jsonHeaders(cookie)),
            String::class.java
        )

    /** POST 请求 */
    private fun post(url: String, body: String, cookie: String? = null) =
        restTemplate.postForEntity(
            url,
            HttpEntity(body, jsonHeaders(cookie)),
            String::class.java
        )

    /** PUT 请求 */
    private fun put(url: String, body: String, cookie: String) =
        restTemplate.exchange(
            url, HttpMethod.PUT,
            HttpEntity(body, jsonHeaders(cookie)),
            String::class.java
        )

    /** DELETE 请求 */
    private fun delete(url: String, cookie: String) =
        restTemplate.exchange(
            url, HttpMethod.DELETE,
            HttpEntity<String>(null, jsonHeaders(cookie)),
            String::class.java
        )

    // ---------- 创建家庭 ----------

    @Test
    fun `创建家庭 - 创建者成为管理员`() {
        val cookie = registerAndGetCookie("creator1@test.com", "创建者")

        val response = post("/api/v1/families", """{"name":"幸福家庭"}""", cookie)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val body = objectMapper.readTree(response.body)
        assertThat(body["name"].asText()).isEqualTo("幸福家庭")
        assertThat(body["inviteCode"].asText()).hasSize(8)
    }

    @Test
    fun `创建家庭 - 未认证返回 4xx`() {
        // SecurityConfig 未配置 AuthenticationEntryPoint，Spring Security 默认返回 403
        val response = post("/api/v1/families", """{"name":"未授权家庭"}""")

        assertThat(response.statusCode.is4xxClientError).isTrue()
    }

    // ---------- 获取家庭信息 ----------

    @Test
    fun `获取家庭详情 - 已认证用户可以访问`() {
        val cookie = registerAndGetCookie("getfamily@test.com", "用户")
        val createResp = post("/api/v1/families", """{"name":"查询家庭"}""", cookie)
        val familyId = objectMapper.readTree(createResp.body)["id"].asLong()

        val response = get("/api/v1/families/$familyId", cookie)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(objectMapper.readTree(response.body)["name"].asText()).isEqualTo("查询家庭")
    }

    // ---------- 邀请码加入家庭 ----------

    @Test
    fun `用邀请码加入家庭 - 成功加入`() {
        val adminCookie = registerAndGetCookie("admin_join@test.com", "管理员")
        val createResp = post("/api/v1/families", """{"name":"可加入家庭"}""", adminCookie)
        val inviteCode = objectMapper.readTree(createResp.body)["inviteCode"].asText()

        val memberCookie = registerAndGetCookie("joiner@test.com", "新成员")
        val joinResp = post("/api/v1/families/join", """{"inviteCode":"$inviteCode"}""", memberCookie)

        assertThat(joinResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(objectMapper.readTree(joinResp.body)["name"].asText()).isEqualTo("可加入家庭")
    }

    @Test
    fun `用邀请码加入家庭 - 无效邀请码返回 404`() {
        val cookie = registerAndGetCookie("invalid_join@test.com", "用户")

        val response = post("/api/v1/families/join", """{"inviteCode":"INVALID1"}""", cookie)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `用邀请码加入家庭 - 重复加入返回 409`() {
        val adminCookie = registerAndGetCookie("admin_dup@test.com", "管理员")
        val createResp = post("/api/v1/families", """{"name":"重复加入测试"}""", adminCookie)
        val inviteCode = objectMapper.readTree(createResp.body)["inviteCode"].asText()

        val memberCookie = registerAndGetCookie("dup_joiner@test.com", "重复加入者")
        post("/api/v1/families/join", """{"inviteCode":"$inviteCode"}""", memberCookie)  // 第一次

        val response = post("/api/v1/families/join", """{"inviteCode":"$inviteCode"}""", memberCookie)  // 第二次

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    }

    // ---------- 查看成员列表 ----------

    @Test
    fun `查看成员列表 - 家庭成员可以访问`() {
        val adminCookie = registerAndGetCookie("admin_members@test.com", "管理员")
        val createResp = post("/api/v1/families", """{"name":"成员查询家庭"}""", adminCookie)
        val familyId = objectMapper.readTree(createResp.body)["id"].asLong()
        val inviteCode = objectMapper.readTree(createResp.body)["inviteCode"].asText()

        val memberCookie = registerAndGetCookie("member_list@test.com", "成员A")
        post("/api/v1/families/join", """{"inviteCode":"$inviteCode"}""", memberCookie)

        val response = get("/api/v1/families/$familyId/members", adminCookie)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val members = objectMapper.readTree(response.body)
        assertThat(members.size()).isEqualTo(2)
        assertThat(members.map { it["role"].asText() }).containsAnyOf("ADMIN", "MEMBER")
    }

    @Test
    fun `查看成员列表 - 非家庭成员返回 403`() {
        val adminCookie = registerAndGetCookie("admin_secret@test.com", "管理员")
        val createResp = post("/api/v1/families", """{"name":"秘密家庭"}""", adminCookie)
        val familyId = objectMapper.readTree(createResp.body)["id"].asLong()

        val outsiderCookie = registerAndGetCookie("outsider@test.com", "局外人")

        val response = get("/api/v1/families/$familyId/members", outsiderCookie)

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    // ---------- 修改成员角色 ----------

    @Test
    fun `修改成员角色 - 管理员可以提升成员为管理员`() {
        val adminCookie = registerAndGetCookie("admin_role@test.com", "管理员")
        val createResp = post("/api/v1/families", """{"name":"角色测试家庭"}""", adminCookie)
        val familyId = objectMapper.readTree(createResp.body)["id"].asLong()
        val inviteCode = objectMapper.readTree(createResp.body)["inviteCode"].asText()

        val memberCookie = registerAndGetCookie("member_role@test.com", "普通成员")
        val joinResp = post("/api/v1/families/join", """{"inviteCode":"$inviteCode"}""", memberCookie)
        val memberId = objectMapper.readTree(joinResp.body).let {
            // 通过成员列表获取 memberId
            val members = objectMapper.readTree(
                get("/api/v1/families/$familyId/members", adminCookie).body
            )
            members.first { it["email"].asText() == "member_role@test.com" }["userId"].asLong()
        }

        val response = put(
            "/api/v1/families/$familyId/members/$memberId/role",
            """{"role":"ADMIN"}""",
            adminCookie
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

        // 验证角色已更新
        val members = objectMapper.readTree(
            get("/api/v1/families/$familyId/members", adminCookie).body
        )
        val updatedMember = members.first { it["userId"].asLong() == memberId }
        assertThat(updatedMember["role"].asText()).isEqualTo("ADMIN")
    }

    // ---------- 踢出成员 ----------

    @Test
    fun `踢出成员 - 管理员成功移除成员`() {
        val adminCookie = registerAndGetCookie("admin_kick@test.com", "管理员")
        val createResp = post("/api/v1/families", """{"name":"踢人家庭"}""", adminCookie)
        val familyId = objectMapper.readTree(createResp.body)["id"].asLong()
        val inviteCode = objectMapper.readTree(createResp.body)["inviteCode"].asText()

        val memberCookie = registerAndGetCookie("to_be_kicked@test.com", "待踢成员")
        post("/api/v1/families/join", """{"inviteCode":"$inviteCode"}""", memberCookie)

        // 获取待踢成员 ID
        val members = objectMapper.readTree(
            get("/api/v1/families/$familyId/members", adminCookie).body
        )
        val targetId = members.first { it["email"].asText() == "to_be_kicked@test.com" }["userId"].asLong()

        val response = delete("/api/v1/families/$familyId/members/$targetId", adminCookie)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

        // 验证成员已被移除
        val afterMembers = objectMapper.readTree(
            get("/api/v1/families/$familyId/members", adminCookie).body
        )
        assertThat(afterMembers.size()).isEqualTo(1)
        assertThat(afterMembers[0]["email"].asText()).isEqualTo("admin_kick@test.com")
    }

    @Test
    fun `踢出成员 - 非管理员返回 403`() {
        val adminCookie = registerAndGetCookie("admin_notkick@test.com", "管理员")
        val createResp = post("/api/v1/families", """{"name":"权限测试家庭"}""", adminCookie)
        val familyId = objectMapper.readTree(createResp.body)["id"].asLong()
        val inviteCode = objectMapper.readTree(createResp.body)["inviteCode"].asText()

        val member1Cookie = registerAndGetCookie("member_notkick1@test.com", "成员1")
        post("/api/v1/families/join", """{"inviteCode":"$inviteCode"}""", member1Cookie)

        val member2Cookie = registerAndGetCookie("member_notkick2@test.com", "成员2")
        post("/api/v1/families/join", """{"inviteCode":"$inviteCode"}""", member2Cookie)

        val members = objectMapper.readTree(get("/api/v1/families/$familyId/members", adminCookie).body)
        val target1Id = members.first { it["email"].asText() == "member_notkick1@test.com" }["userId"].asLong()

        // 成员2 尝试踢出成员1（无权限）
        val response = delete("/api/v1/families/$familyId/members/$target1Id", member2Cookie)

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }
}
