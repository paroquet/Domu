package com.domu.security

import com.domu.model.User
import com.domu.repository.UserRepository
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(private val userRepository: UserRepository) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val userId = username.toLongOrNull()
            ?: throw UsernameNotFoundException("Invalid user ID: $username")
        val user = userRepository.findById(userId).orElseThrow {
            UsernameNotFoundException("User not found with ID: $userId")
        }
        return DomuUserDetails(user)
    }

    inner class DomuUserDetails(private val user: User) : UserDetails {
        override fun getAuthorities(): Collection<GrantedAuthority> = emptyList()
        override fun getPassword(): String = user.passwordHash
        override fun getUsername(): String = user.id.toString()
        override fun isAccountNonExpired(): Boolean = true
        override fun isAccountNonLocked(): Boolean = true
        override fun isCredentialsNonExpired(): Boolean = true
        override fun isEnabled(): Boolean = true
        fun getUser(): User = user
    }
}
