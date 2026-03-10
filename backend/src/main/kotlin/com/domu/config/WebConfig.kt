package com.domu.config

import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.concurrent.TimeUnit

@Configuration
class WebConfig(private val appProperties: AppProperties) : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val uploadDir = appProperties.uploadDir
        val normalizedPath = if (uploadDir.endsWith("/")) uploadDir else "$uploadDir/"

        registry.addResourceHandler("/files/**")
            .addResourceLocations("file:$normalizedPath")
            .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())

        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
    }
}
