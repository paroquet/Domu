package com.domu.controller

import com.domu.dto.CreateOrderRequest
import com.domu.dto.OrderResponse
import com.domu.dto.ShoppingItem
import com.domu.dto.UpdateOrderStatusRequest
import com.domu.service.OrderService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(private val orderService: OrderService) {

    @GetMapping
    fun listOrders(
        @RequestParam familyId: Long,
        @RequestParam(required = false) date: String?
    ): ResponseEntity<List<OrderResponse>> {
        val userId = getCurrentUserId()
        val localDate = date?.let { LocalDate.parse(it) }
        return ResponseEntity.ok(orderService.list(familyId, localDate, userId))
    }

    @PostMapping
    fun createOrder(@RequestBody request: CreateOrderRequest): ResponseEntity<OrderResponse> {
        val userId = getCurrentUserId()
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request, userId))
    }

    @PutMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: Long,
        @RequestBody request: UpdateOrderStatusRequest
    ): ResponseEntity<OrderResponse> {
        val userId = getCurrentUserId()
        return ResponseEntity.ok(orderService.updateStatus(id, request.status, userId))
    }

    @DeleteMapping("/{id}")
    fun deleteOrder(@PathVariable id: Long): ResponseEntity<Void> {
        val userId = getCurrentUserId()
        orderService.delete(id, userId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/shopping-plan")
    fun getShoppingPlan(
        @RequestParam familyId: Long,
        @RequestParam date: String
    ): ResponseEntity<List<ShoppingItem>> {
        val userId = getCurrentUserId()
        val localDate = LocalDate.parse(date)
        return ResponseEntity.ok(orderService.getShoppingPlan(familyId, localDate, userId))
    }

    private fun getCurrentUserId(): Long {
        return SecurityContextHolder.getContext().authentication.name.toLong()
    }
}
