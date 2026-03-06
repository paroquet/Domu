package com.domu.security

import com.domu.config.JwtProperties
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(private val jwtProperties: JwtProperties) {

    private val secretKey: SecretKey by lazy {
        val keyBytes = jwtProperties.secret.toByteArray(Charsets.UTF_8)
        val paddedKey = if (keyBytes.size < 32) keyBytes.copyOf(32) else keyBytes
        Keys.hmacShaKeyFor(paddedKey)
    }

    fun createAccessToken(userId: Long): String {
        val now = Date()
        val expiry = Date(now.time + jwtProperties.accessExpiration)
        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", "access")
            .issuedAt(now)
            .expiration(expiry)
            .signWith(secretKey)
            .compact()
    }

    fun createRefreshToken(userId: Long): String {
        val now = Date()
        val expiry = Date(now.time + jwtProperties.refreshExpiration)
        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", "refresh")
            .issuedAt(now)
            .expiration(expiry)
            .signWith(secretKey)
            .compact()
    }

    fun getUserIdFromToken(token: String): Long {
        val claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
        return claims.subject.toLong()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: JwtException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
