package com.domu.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    var email: String = "",

    @Column(nullable = false)
    var passwordHash: String = "",

    @Column(nullable = false)
    var name: String = "",

    @Column(nullable = true)
    var avatarPath: String? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    val createdAt: Instant = Instant.now()
)
