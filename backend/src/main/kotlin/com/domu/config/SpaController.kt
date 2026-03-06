package com.domu.config

import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import jakarta.servlet.http.HttpServletRequest

@Controller
class SpaErrorController : ErrorController {

    private val indexHtml = ClassPathResource("static/index.html")

    @GetMapping("/error")
    @ResponseBody
    fun handleError(request: HttpServletRequest): ResponseEntity<*> {
        val status = request.getAttribute("jakarta.servlet.error.status_code") as? Int
            ?: HttpStatus.INTERNAL_SERVER_ERROR.value()

        // 404 且请求 HTML 时返回 SPA
        val acceptHtml = request.getHeader("Accept")?.contains(MediaType.TEXT_HTML_VALUE) == true

        return if (status == HttpStatus.NOT_FOUND.value() && acceptHtml) {
            ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(indexHtml)
        } else {
            ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf(
                    "status" to status,
                    "error" to HttpStatus.valueOf(status).reasonPhrase,
                    "path" to request.getAttribute("jakarta.servlet.error.request_uri")
                ))
        }
    }
}
