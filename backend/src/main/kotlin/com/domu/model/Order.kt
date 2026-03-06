package com.domu.model

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "orders")
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    var family: Family = Family(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordered_by_id", nullable = false)
    var orderedBy: User = User(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordered_for_id", nullable = false)
    var orderedFor: User = User(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    var recipe: Recipe = Recipe(),

    @Column(nullable = false, columnDefinition = "TEXT")
    var plannedDate: LocalDate = LocalDate.now(),

    @Column(nullable = false)
    var status: String = "PENDING",

    @Column(nullable = false, columnDefinition = "TEXT")
    val createdAt: Instant = Instant.now()
)
