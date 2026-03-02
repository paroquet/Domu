package com.domu.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "families")
class Family(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var name: String = "",

    @Column(unique = true, nullable = false)
    var inviteCode: String = "",

    @Column(nullable = false, columnDefinition = "TEXT")
    val createdAt: Instant = Instant.now()
)
