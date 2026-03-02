package com.domu

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DomuApplication

fun main(args: Array<String>) {
    runApplication<DomuApplication>(*args)
}
