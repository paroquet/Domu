package com.domu.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "app.jwt")
data class JwtProperties(
    val secret: String = "change-this-secret-in-production-must-be-at-least-32-chars",
    val accessExpiration: Long = 86400000L,
    val refreshExpiration: Long = 604800000L
)

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val uploadDir: String = "/app/data/uploads",
    val baseUrl: String = "http://localhost:8080"
)

@Configuration
@EnableConfigurationProperties(JwtProperties::class, AppProperties::class)
class AppConfig
