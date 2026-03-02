package com.domu.exception

import com.domu.dto.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<ErrorResponse> {
        val status = ex.statusCode.value()
        val error = HttpStatus.valueOf(status).reasonPhrase
        val response = ErrorResponse(
            error = error,
            message = ex.reason ?: ex.message,
            status = status
        )
        return ResponseEntity.status(status).body(response)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        val response = ErrorResponse(
            error = "Validation Failed",
            message = errors,
            status = HttpStatus.BAD_REQUEST.value()
        )
        return ResponseEntity.badRequest().body(response)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            error = "Internal Server Error",
            message = ex.message ?: "An unexpected error occurred",
            status = HttpStatus.INTERNAL_SERVER_ERROR.value()
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }
}
