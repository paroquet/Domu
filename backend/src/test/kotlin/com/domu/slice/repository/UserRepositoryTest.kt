package com.domu.slice.repository

import com.domu.model.User
import com.domu.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
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
class UserRepositoryTest {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var entityManager: TestEntityManager

    private fun saveUser(email: String, name: String): User {
        val user = User(email = email, passwordHash = "hashed_pw", name = name)
        return entityManager.persistAndFlush(user)
    }

    @Test
    fun `findByEmail - 邮箱存在时返回对应用户`() {
        saveUser("alice@test.com", "Alice")

        val found = userRepository.findByEmail("alice@test.com")

        assertThat(found).isNotNull
        assertThat(found!!.email).isEqualTo("alice@test.com")
        assertThat(found.name).isEqualTo("Alice")
    }

    @Test
    fun `findByEmail - 邮箱不存在时返回 null`() {
        val found = userRepository.findByEmail("nobody@test.com")
        assertThat(found).isNull()
    }

    @Test
    fun `findByEmail - 只返回精确匹配的用户`() {
        saveUser("user@test.com", "用户A")
        saveUser("other@test.com", "用户B")

        val found = userRepository.findByEmail("user@test.com")

        assertThat(found).isNotNull
        assertThat(found!!.name).isEqualTo("用户A")
    }

    @Test
    fun `save - 邮箱唯一约束生效`() {
        saveUser("dup@test.com", "用户X")

        org.junit.jupiter.api.assertThrows<Exception> {
            saveUser("dup@test.com", "用户Y")
        }
    }
}
