package com.domu

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:sqlite::memory:",
    "app.jwt.secret=test-secret-key-that-is-at-least-32-chars-long",
    "app.upload.dir=/tmp/domu-test-uploads",
    "app.base-url=http://localhost:8080"
])
class DomuApplicationTests {

    @Test
    fun contextLoads() {
    }
}
