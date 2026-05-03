package com.domu.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@ConfigurationProperties(prefix = "app.jwt")
data class JwtProperties(
    val secret: String = "change-this-secret-in-production-must-be-at-least-32-chars",
    val accessExpiration: Long = 86400000L,
    val refreshExpiration: Long = 604800000L
)

/**
 * "app.upload-dir" 绑定到 AppProperties.uploadDir
 */
@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val uploadDir: String = "/app/data/uploads",
    val baseUrl: String = "http://localhost:8080"
)

@Configuration
@EnableConfigurationProperties(JwtProperties::class, AppProperties::class)
class AppConfig(
    private val env: Environment,
    private val appProperties: AppProperties
) {
    private val log = LoggerFactory.getLogger(AppConfig::class.java)

    @PostConstruct
    fun logConfig() {
        log.info(">>> Active profiles  : {}", env.activeProfiles.ifEmpty { arrayOf("(default)") }.toList())
        log.info(">>> upload-dir       : {}", appProperties.uploadDir)
        log.info(">>> base-url         : {}", appProperties.baseUrl)
    }
}
