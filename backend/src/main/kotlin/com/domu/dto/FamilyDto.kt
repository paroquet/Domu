package com.domu.dto

data class CreateFamilyRequest(val name: String)

data class JoinFamilyRequest(val inviteCode: String)

data class UpdateRoleRequest(val role: String)

data class FamilyResponse(
    val id: Long,
    val name: String,
    val inviteCode: String,
    val createdAt: String
)

data class MemberResponse(
    val userId: Long,
    val name: String,
    val email: String,
    val avatarPath: String?,
    val role: String,
    val joinedAt: String
)

data class InviteCodeResponse(val inviteCode: String)
