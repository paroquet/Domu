package com.domu.model

import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant

@Embeddable
class FamilyMemberId(
    @Column(name = "family_id")
    val familyId: Long = 0,

    @Column(name = "user_id")
    val userId: Long = 0
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FamilyMemberId) return false
        return familyId == other.familyId && userId == other.userId
    }

    override fun hashCode(): Int {
        var result = familyId.hashCode()
        result = 31 * result + userId.hashCode()
        return result
    }
}

@Entity
@Table(name = "family_members")
class FamilyMember(
    @EmbeddedId
    val id: FamilyMemberId = FamilyMemberId(),

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("familyId")
    @JoinColumn(name = "family_id")
    val family: Family = Family(),

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    val user: User = User(),

    @Column(nullable = false)
    var role: String = "MEMBER",

    @Column(nullable = false, columnDefinition = "TEXT")
    val joinedAt: Instant = Instant.now()
)
