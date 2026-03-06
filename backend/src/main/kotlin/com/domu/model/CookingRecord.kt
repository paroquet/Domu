package com.domu.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "cooking_records")
class CookingRecord(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    var recipe: Recipe = Recipe(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = User(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    var family: Family = Family(),

    @Column(nullable = false, columnDefinition = "TEXT")
    var cookedAt: Instant = Instant.now(),

    @Column(nullable = true, columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    var images: String = "[]",

    @Column(nullable = false, columnDefinition = "TEXT")
    val createdAt: Instant = Instant.now()
)
