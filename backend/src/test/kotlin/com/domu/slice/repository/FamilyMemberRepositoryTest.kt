package com.domu.slice.repository

import com.domu.model.Family
import com.domu.model.FamilyMember
import com.domu.model.FamilyMemberId
import com.domu.model.User
import com.domu.repository.FamilyMemberRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.TestPropertySource

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:sqlite::memory:",
    "spring.datasource.driver-class-name=org.sqlite.JDBC",
    "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
])
class FamilyMemberRepositoryTest {

    @Autowired
    private lateinit var familyMemberRepository: FamilyMemberRepository

    @Autowired
    private lateinit var entityManager: TestEntityManager

    private lateinit var familyA: Family
    private lateinit var familyB: Family
    private lateinit var userAdmin: User
    private lateinit var userMember: User

    @BeforeEach
    fun setUp() {
        familyA = entityManager.persistAndFlush(Family(name = "家庭A", inviteCode = "AAAA1111"))
        familyB = entityManager.persistAndFlush(Family(name = "家庭B", inviteCode = "BBBB2222"))
        userAdmin = entityManager.persistAndFlush(
            User(email = "admin@test.com", passwordHash = "h", name = "管理员")
        )
        userMember = entityManager.persistAndFlush(
            User(email = "member@test.com", passwordHash = "h", name = "成员")
        )

        // 管理员加入家庭A
        entityManager.persistAndFlush(FamilyMember(
            id = FamilyMemberId(familyA.id, userAdmin.id),
            family = familyA, user = userAdmin, role = "ADMIN"
        ))
        // 成员加入家庭A
        entityManager.persistAndFlush(FamilyMember(
            id = FamilyMemberId(familyA.id, userMember.id),
            family = familyA, user = userMember, role = "MEMBER"
        ))
        // 管理员加入家庭B
        entityManager.persistAndFlush(FamilyMember(
            id = FamilyMemberId(familyB.id, userAdmin.id),
            family = familyB, user = userAdmin, role = "MEMBER"
        ))

        entityManager.clear()
    }

    // ---------- findByFamily_Id ----------

    @Test
    fun `findByFamily_Id - 返回指定家庭的所有成员`() {
        val members = familyMemberRepository.findByFamily_Id(familyA.id)

        assertThat(members).hasSize(2)
        assertThat(members.map { it.user.email })
            .containsExactlyInAnyOrder("admin@test.com", "member@test.com")
    }

    @Test
    fun `findByFamily_Id - 不跨家庭返回数据`() {
        val members = familyMemberRepository.findByFamily_Id(familyB.id)

        assertThat(members).hasSize(1)
        assertThat(members[0].user.email).isEqualTo("admin@test.com")
    }

    // ---------- findByFamily_IdAndUser_Id ----------

    @Test
    fun `findByFamily_IdAndUser_Id - 存在时返回 FamilyMember`() {
        val member = familyMemberRepository.findByFamily_IdAndUser_Id(familyA.id, userAdmin.id)

        assertThat(member).isNotNull
        assertThat(member!!.role).isEqualTo("ADMIN")
    }

    @Test
    fun `findByFamily_IdAndUser_Id - 用户不在该家庭时返回 null`() {
        val member = familyMemberRepository.findByFamily_IdAndUser_Id(familyB.id, userMember.id)

        assertThat(member).isNull()
    }

    // ---------- existsByFamily_IdAndUser_Id ----------

    @Test
    fun `existsByFamily_IdAndUser_Id - 成员存在时返回 true`() {
        val exists = familyMemberRepository.existsByFamily_IdAndUser_Id(familyA.id, userMember.id)
        assertThat(exists).isTrue()
    }

    @Test
    fun `existsByFamily_IdAndUser_Id - 成员不存在时返回 false`() {
        val exists = familyMemberRepository.existsByFamily_IdAndUser_Id(familyB.id, userMember.id)
        assertThat(exists).isFalse()
    }

    // ---------- findByUser_Id ----------

    @Test
    fun `findByUser_Id - 返回用户加入的所有家庭`() {
        val memberships = familyMemberRepository.findByUser_Id(userAdmin.id)

        assertThat(memberships).hasSize(2)
        assertThat(memberships.map { it.family.name })
            .containsExactlyInAnyOrder("家庭A", "家庭B")
    }
}
