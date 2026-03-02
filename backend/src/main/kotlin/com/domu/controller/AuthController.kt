package com.domu.controller

import com.domu.config.JwtProperties
import com.domu.dto.LoginRequest
import com.domu.dto.RegisterRequest
import com.domu.dto.TokenResponse
import com.domu.dto.UserResponse
import com.domu.security.JwtTokenProvider
import com.domu.service.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtProperties: JwtProperties
) {

    @PostMapping("/register")
    fun register(
        @RequestBody request: RegisterRequest,
        httpRequest: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<UserResponse> {
        val userResponse = authService.register(request)
        val accessToken = jwtTokenProvider.createAccessToken(userResponse.id)
        val refreshToken = jwtTokenProvider.createRefreshToken(userResponse.id)
        val isSecure = httpRequest.isSecure
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie("access_token", accessToken, jwtProperties.accessExpiration / 1000, isSecure).toString())
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie("refresh_token", refreshToken, jwtProperties.refreshExpiration / 1000, isSecure).toString())
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse)
    }

    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
        httpRequest: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<UserResponse> {
        val user = authService.login(request.email, request.password)
        val accessToken = jwtTokenProvider.createAccessToken(user.id)
        val refreshToken = jwtTokenProvider.createRefreshToken(user.id)
        val isSecure = httpRequest.isSecure
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie("access_token", accessToken, jwtProperties.accessExpiration / 1000, isSecure).toString())
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie("refresh_token", refreshToken, jwtProperties.refreshExpiration / 1000, isSecure).toString())
        val userResponse = UserResponse(
            id = user.id,
            email = user.email,
            name = user.name,
            avatarPath = user.avatarPath
        )
        return ResponseEntity.ok(userResponse)
    }

    @PostMapping("/logout")
    fun logout(
        httpRequest: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        val isSecure = httpRequest.isSecure
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie("access_token", "", 0L, isSecure).toString())
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie("refresh_token", "", 0L, isSecure).toString())
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/refresh")
    fun refresh(
        httpRequest: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<UserResponse> {
        val refreshToken = httpRequest.cookies
            ?.find { it.name == "refresh_token" }
            ?.value
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "No refresh token")

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token")
        }

        val userId = jwtTokenProvider.getUserIdFromToken(refreshToken)
        val newAccessToken = jwtTokenProvider.createAccessToken(userId)
        val isSecure = httpRequest.isSecure
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie("access_token", newAccessToken, jwtProperties.accessExpiration / 1000, isSecure).toString())

        return ResponseEntity.ok().build()
    }

    private fun createCookie(name: String, value: String, maxAge: Long, isSecure: Boolean): ResponseCookie =
        ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(isSecure)
            .path("/")
            .maxAge(maxAge)
            .sameSite("Lax")
            .build()
}
