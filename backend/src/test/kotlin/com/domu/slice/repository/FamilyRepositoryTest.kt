package com.domu.slice.repository

import com.domu.model.Family
import com.domu.repository.FamilyRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
class FamilyRepositoryTest {

    @Autowired
    private lateinit var familyRepository: FamilyRepository

    @Autowired
    private lateinit var entityManager: TestEntityManager

    private fun saveFamily(name: String, inviteCode: String): Family {
        val family = Family(name = name, inviteCode = inviteCode)
        return entityManager.persistAndFlush(family)
    }

    // ---------- findByInviteCode ----------

    @Test
    fun `findByInviteCode - 邀请码存在时返回对应家庭`() {
        saveFamily("测试家庭", "ABCD1234")

        val found = familyRepository.findByInviteCode("ABCD1234")

        assertThat(found).isNotNull
        assertThat(found!!.name).isEqualTo("测试家庭")
        assertThat(found.inviteCode).isEqualTo("ABCD1234")
    }

    @Test
    fun `findByInviteCode - 邀请码不存在时返回 null`() {
        val found = familyRepository.findByInviteCode("NOTEXIST")
        assertThat(found).isNull()
    }

    @Test
    fun `findByInviteCode - 只返回精确匹配的家庭`() {
        saveFamily("家庭A", "CODE1111")
        saveFamily("家庭B", "CODE2222")

        val found = familyRepository.findByInviteCode("CODE1111")

        assertThat(found).isNotNull
        assertThat(found!!.name).isEqualTo("家庭A")
    }

    @Test
    fun `findByInviteCode - 大小写敏感匹配`() {
        saveFamily("测试家庭", "ABCD1234")

        val foundUpper = familyRepository.findByInviteCode("ABCD1234")
        val foundLower = familyRepository.findByInviteCode("abcd1234")

        assertThat(foundUpper).isNotNull
        assertThat(foundLower).isNull()
    }

    // ---------- save ----------

    @Test
    fun `save - 邀请码唯一约束生效`() {
        saveFamily("家庭A", "UNIQUE01")

        assertThrows<Exception> {
            saveFamily("家庭B", "UNIQUE01")
        }
    }

    @Test
    fun `save - 成功保存家庭并生成 ID`() {
        val family = Family(name = "新家庭", inviteCode = "NEWCODE1")

        val saved = familyRepository.save(family)
        entityManager.flush()

        assertThat(saved.id).isGreaterThan(0)
        assertThat(saved.name).isEqualTo("新家庭")
        assertThat(saved.createdAt).isNotNull
    }

    // ---------- findById ----------

    @Test
    fun `findById - ID 存在时返回家庭`() {
        val family = saveFamily("查询测试", "FINDME01")

        val found = familyRepository.findById(family.id)

        assertThat(found).isPresent
        assertThat(found.get().name).isEqualTo("查询测试")
    }

    @Test
    fun `findById - ID 不存在时返回空`() {
        val found = familyRepository.findById(999L)
        assertThat(found).isEmpty
    }

    // ---------- findAll ----------

    @Test
    fun `findAll - 返回所有家庭`() {
        saveFamily("家庭1", "CODE0001")
        saveFamily("家庭2", "CODE0002")
        saveFamily("家庭3", "CODE0003")

        val all = familyRepository.findAll()

        assertThat(all).hasSize(3)
    }

    // ---------- delete ----------

    @Test
    fun `delete - 成功删除家庭`() {
        val family = saveFamily("待删除", "DELETE01")
        val familyId = family.id

        familyRepository.delete(family)
        entityManager.flush()

        val found = familyRepository.findById(familyId)
        assertThat(found).isEmpty
    }
}
