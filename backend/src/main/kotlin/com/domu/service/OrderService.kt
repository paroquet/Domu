package com.domu.service

import com.domu.dto.CreateOrderRequest
import com.domu.dto.IngredientDto
import com.domu.dto.OrderResponse
import com.domu.dto.ShoppingItem
import com.domu.model.Order
import com.domu.repository.FamilyRepository
import com.domu.repository.OrderRepository
import com.domu.repository.RecipeRepository
import com.domu.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.LocalDate

@Service
@Transactional
class OrderService(
    private val orderRepository: OrderRepository,
    private val familyRepository: FamilyRepository,
    private val userRepository: UserRepository,
    private val recipeRepository: RecipeRepository,
    private val familyAuthService: FamilyAuthService,
    private val objectMapper: ObjectMapper
) {

    fun list(familyId: Long, date: LocalDate?, userId: Long): List<OrderResponse> {
        familyAuthService.requireMember(familyId, userId)
        val orders = if (date != null) {
            orderRepository.findByFamily_IdAndPlannedDate(familyId, date)
        } else {
            orderRepository.findByFamily_IdOrderByCreatedAtDesc(familyId)
        }
        return orders.map { it.toResponse() }
    }

    fun create(request: CreateOrderRequest, userId: Long): OrderResponse {
        familyAuthService.requireMember(request.familyId, userId)
        val orderedBy = userRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }
        val orderedFor = userRepository.findById(request.orderedForId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Ordered for user not found")
        }
        val recipe = recipeRepository.findById(request.recipeId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found")
        }
        val family = familyRepository.findById(request.familyId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Family not found")
        }
        val plannedDate = LocalDate.parse(request.plannedDate)
        val order = Order(
            family = family,
            orderedBy = orderedBy,
            orderedFor = orderedFor,
            recipe = recipe,
            plannedDate = plannedDate,
            status = "PENDING",
            createdAt = Instant.now()
        )
        return orderRepository.save(order).toResponse()
    }

    fun updateStatus(id: Long, status: String, userId: Long): OrderResponse {
        val order = findById(id)
        familyAuthService.requireMember(order.family.id, userId)
        if (status !in listOf("PENDING", "DONE", "CANCELLED")) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status")
        }
        order.status = status
        return orderRepository.save(order).toResponse()
    }

    fun delete(id: Long, userId: Long) {
        val order = findById(id)
        familyAuthService.requireMember(order.family.id, userId)
        orderRepository.delete(order)
    }

    fun getShoppingPlan(familyId: Long, date: LocalDate, userId: Long): List<ShoppingItem> {
        familyAuthService.requireMember(familyId, userId)
        val orders = orderRepository.findByFamily_IdAndPlannedDateAndStatusNot(familyId, date, "CANCELLED")

        data class ItemKey(val name: String, val unit: String)

        val aggregated = mutableMapOf<ItemKey, Double>()

        for (order in orders) {
            val ingredientsList: List<IngredientDto> = try {
                objectMapper.readValue(order.recipe.ingredients)
            } catch (e: Exception) {
                emptyList()
            }
            for (ingredient in ingredientsList) {
                val key = ItemKey(name = ingredient.name, unit = ingredient.unit)
                val amount = ingredient.amount.toDoubleOrNull() ?: 0.0
                aggregated[key] = (aggregated[key] ?: 0.0) + amount
            }
        }

        return aggregated.map { (key, amount) ->
            ShoppingItem(name = key.name, amount = amount, unit = key.unit)
        }.sortedBy { it.name }
    }

    private fun findById(id: Long): Order {
        return orderRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found")
        }
    }

    private fun Order.toResponse() = OrderResponse(
        id = id,
        familyId = family.id,
        orderedById = orderedBy.id,
        orderedByName = orderedBy.name,
        orderedForId = orderedFor.id,
        orderedForName = orderedFor.name,
        recipeId = recipe.id,
        recipeTitle = recipe.title,
        plannedDate = plannedDate.toString(),
        status = status,
        createdAt = createdAt.toString()
    )
}
