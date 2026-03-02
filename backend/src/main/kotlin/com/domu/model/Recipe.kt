package com.domu.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "recipes")
class Recipe(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var title: String = "",

    @Column(nullable = true, columnDefinition = "TEXT")
    var description: String? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    var ingredients: String = "[]",

    @Column(nullable = false, columnDefinition = "TEXT")
    var steps: String = "[]",

    @Column(nullable = true)
    var coverImagePath: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    var author: User = User(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    var family: Family = Family(),

    @Column(nullable = true, unique = true)
    var shareToken: String? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false, columnDefinition = "TEXT")
    var updatedAt: Instant = Instant.now()
)
