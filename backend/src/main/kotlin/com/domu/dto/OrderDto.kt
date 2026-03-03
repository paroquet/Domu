package com.domu.dto

data class CreateOrderRequest(
    val familyId: Long,
    val orderedForId: Long? = null,
    val recipeId: Long,
    val plannedDate: String
)

data class UpdateOrderStatusRequest(val status: String)

data class OrderResponse(
    val id: Long,
    val familyId: Long,
    val orderedById: Long,
    val orderedByName: String,
    val orderedForId: Long,
    val orderedForName: String,
    val recipeId: Long,
    val recipeTitle: String,
    val plannedDate: String,
    val status: String,
    val createdAt: String
)

data class ShoppingItem(
    val name: String,
    val amount: Double,
    val unit: String
)
