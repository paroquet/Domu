package com.domu.slice.controller

import com.domu.config.AppConfig
import com.domu.config.SecurityConfig
import com.domu.controller.OrderController
import com.domu.dto.OrderResponse
import com.domu.dto.ShoppingItem
import com.domu.model.User
import com.domu.repository.UserRepository
import com.domu.security.JwtAuthenticationFilter
import com.domu.security.JwtTokenProvider
import com.domu.security.UserDetailsServiceImpl
import com.domu.service.OrderService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.util.Optional

@WebMvcTest(controllers = [OrderController::class])
@Import(SecurityConfig::class, JwtTokenProvider::class, JwtAuthenticationFilter::class,
        UserDetailsServiceImpl::class, AppConfig::class)
class OrderControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockkBean
    private lateinit var orderService: OrderService

    @MockkBean
    private lateinit var userRepository: UserRepository

    private val userId = 1L
    private lateinit var authCookie: Cookie

    private val authUser = User(id = 1L, email = "user@test.com", passwordHash = "h", name = "测试用户")

    @BeforeEach
    fun setUp() {
        val token = jwtTokenProvider.createAccessToken(userId)
        authCookie = Cookie("access_token", token)
        every { userRepository.findById(userId) } returns Optional.of(authUser)
    }

    private val orderResponse = OrderResponse(
        id = 1L,
        familyId = 10L,
        orderedById = 1L,
        orderedByName = "用户1",
        orderedForId = 2L,
        orderedForName = "用户2",
        recipeId = 1L,
        recipeTitle = "红烧肉",
        plannedDate = "2026-03-03",
        status = "PENDING",
        createdAt = "2026-01-01T00:00:00Z"
    )

    // ---------- GET /api/v1/orders ----------

    @Test
    fun `GET orders - 列出家庭订单`() {
        every { orderService.list(10L, null, userId) } returns listOf(orderResponse)

        mockMvc.get("/api/v1/orders?familyId=10") {
            cookie(authCookie)
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].recipeTitle") { value("红烧肉") }
            jsonPath("$[0].status") { value("PENDING") }
        }
    }

    @Test
    fun `GET orders - 按日期筛选订单`() {
        val date = LocalDate.of(2026, 3, 3)
        every { orderService.list(10L, date, userId) } returns listOf(orderResponse)

        mockMvc.get("/api/v1/orders?familyId=10&date=2026-03-03") {
            cookie(authCookie)
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].plannedDate") { value("2026-03-03") }
        }
    }

    @Test
    fun `GET orders - 未认证返回 4xx`() {
        mockMvc.get("/api/v1/orders?familyId=10") {
        }.andExpect {
            status { is4xxClientError() }
        }
    }

    @Test
    fun `GET orders - 非成员返回 403`() {
        every { orderService.list(10L, null, userId) } throws
            ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member")

        mockMvc.get("/api/v1/orders?familyId=10") {
            cookie(authCookie)
        }.andExpect {
            status { isForbidden() }
        }
    }

    // ---------- POST /api/v1/orders ----------

    @Test
    fun `POST orders - 创建订单成功返回 201`() {
        every { orderService.create(any(), userId) } returns orderResponse

        mockMvc.post("/api/v1/orders") {
            cookie(authCookie)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "familyId": 10,
                    "orderedForId": 2,
                    "recipeId": 1,
                    "plannedDate": "2026-03-03"
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { value(1) }
            jsonPath("$.status") { value("PENDING") }
            jsonPath("$.recipeTitle") { value("红烧肉") }
        }
    }

    @Test
    fun `POST orders - 菜谱不存在返回 404`() {
        every { orderService.create(any(), userId) } throws
            ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found")

        mockMvc.post("/api/v1/orders") {
            cookie(authCookie)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "familyId": 10,
                    "orderedForId": 2,
                    "recipeId": 999,
                    "plannedDate": "2026-03-03"
                }
            """.trimIndent()
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `POST orders - 未认证返回 4xx`() {
        mockMvc.post("/api/v1/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"familyId": 10, "orderedForId": 2, "recipeId": 1, "plannedDate": "2026-03-03"}"""
        }.andExpect {
            status { is4xxClientError() }
        }
    }

    // ---------- PUT /api/v1/orders/{id}/status ----------

    @Test
    fun `PUT orders id status - 更新状态为 DONE`() {
        val doneOrder = orderResponse.copy(status = "DONE")
        every { orderService.updateStatus(1L, "DONE", userId) } returns doneOrder

        mockMvc.put("/api/v1/orders/1/status") {
            cookie(authCookie)
            contentType = MediaType.APPLICATION_JSON
            content = """{"status": "DONE"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("DONE") }
        }
    }

    @Test
    fun `PUT orders id status - 更新状态为 CANCELLED`() {
        val cancelledOrder = orderResponse.copy(status = "CANCELLED")
        every { orderService.updateStatus(1L, "CANCELLED", userId) } returns cancelledOrder

        mockMvc.put("/api/v1/orders/1/status") {
            cookie(authCookie)
            contentType = MediaType.APPLICATION_JSON
            content = """{"status": "CANCELLED"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("CANCELLED") }
        }
    }

    @Test
    fun `PUT orders id status - 无效状态返回 400`() {
        every { orderService.updateStatus(1L, "INVALID", userId) } throws
            ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status")

        mockMvc.put("/api/v1/orders/1/status") {
            cookie(authCookie)
            contentType = MediaType.APPLICATION_JSON
            content = """{"status": "INVALID"}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `PUT orders id status - 订单不存在返回 404`() {
        every { orderService.updateStatus(999L, "DONE", userId) } throws
            ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found")

        mockMvc.put("/api/v1/orders/999/status") {
            cookie(authCookie)
            contentType = MediaType.APPLICATION_JSON
            content = """{"status": "DONE"}"""
        }.andExpect {
            status { isNotFound() }
        }
    }

    // ---------- DELETE /api/v1/orders/{id} ----------

    @Test
    fun `DELETE orders id - 删除订单成功返回 204`() {
        justRun { orderService.delete(1L, userId) }

        mockMvc.delete("/api/v1/orders/1") {
            cookie(authCookie)
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `DELETE orders id - 订单不存在返回 404`() {
        every { orderService.delete(999L, userId) } throws
            ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found")

        mockMvc.delete("/api/v1/orders/999") {
            cookie(authCookie)
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `DELETE orders id - 非成员返回 403`() {
        every { orderService.delete(1L, userId) } throws
            ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member")

        mockMvc.delete("/api/v1/orders/1") {
            cookie(authCookie)
        }.andExpect {
            status { isForbidden() }
        }
    }

    // ---------- GET /api/v1/orders/shopping-plan ----------

    @Test
    fun `GET orders shopping-plan - 获取买菜计划`() {
        val shoppingItems = listOf(
            ShoppingItem("猪肉", 800.0, "克"),
            ShoppingItem("酱油", 2.0, "勺"),
            ShoppingItem("白糖", 1.0, "勺")
        )
        val date = LocalDate.of(2026, 3, 3)
        every { orderService.getShoppingPlan(10L, date, userId) } returns shoppingItems

        mockMvc.get("/api/v1/orders/shopping-plan?familyId=10&date=2026-03-03") {
            cookie(authCookie)
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(3) }
            jsonPath("$[0].name") { value("猪肉") }
            jsonPath("$[0].amount") { value(800.0) }
            jsonPath("$[0].unit") { value("克") }
        }
    }

    @Test
    fun `GET orders shopping-plan - 空结果返回空列表`() {
        val date = LocalDate.of(2026, 3, 3)
        every { orderService.getShoppingPlan(10L, date, userId) } returns emptyList()

        mockMvc.get("/api/v1/orders/shopping-plan?familyId=10&date=2026-03-03") {
            cookie(authCookie)
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(0) }
        }
    }

    @Test
    fun `GET orders shopping-plan - 非成员返回 403`() {
        val date = LocalDate.of(2026, 3, 3)
        every { orderService.getShoppingPlan(10L, date, userId) } throws
            ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member")

        mockMvc.get("/api/v1/orders/shopping-plan?familyId=10&date=2026-03-03") {
            cookie(authCookie)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `GET orders shopping-plan - 未认证返回 4xx`() {
        mockMvc.get("/api/v1/orders/shopping-plan?familyId=10&date=2026-03-03") {
        }.andExpect {
            status { is4xxClientError() }
        }
    }
}
