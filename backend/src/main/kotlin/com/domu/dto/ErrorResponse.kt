package com.domu.dto

data class ErrorResponse(
    val error: String,
    val message: String,
    val status: Int
)
