package com.domu

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationListener

@SpringBootApplication
class DomuApplication : ApplicationListener<ApplicationReadyEvent> {

    @Value("\${server.port}")
    private lateinit var serverPort: String

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        println("Application is running at http://localhost:$serverPort")
    }
}

fun main(args: Array<String>) {
    runApplication<DomuApplication>(*args)
}
