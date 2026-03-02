package com.domu.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(private val appProperties: AppProperties) : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val uploadDir = appProperties.uploadDir
        val normalizedPath = if (uploadDir.endsWith("/")) uploadDir else "$uploadDir/"

        registry.addResourceHandler("/files/**")
            .addResourceLocations("file:$normalizedPath")

        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
    }
}
