import client from './client'
import type { Family, FamilyMember } from '@/types'

export async function createFamily(name: string): Promise<Family> {
  const res = await client.post<Family>('/families', { name })
  return res.data
}

export async function getFamily(id: number): Promise<Family & { members?: FamilyMember[] }> {
  const res = await client.get<Family & { members?: FamilyMember[] }>(`/families/${id}`)
  return res.data
}

export async function getFamilyMembers(id: number): Promise<FamilyMember[]> {
  const res = await client.get<FamilyMember[]>(`/families/${id}/members`)
  return res.data
}

export async function joinFamily(inviteCode: string): Promise<Family> {
  const res = await client.post<Family>('/families/join', { inviteCode })
  return res.data
}

export async function regenerateInviteCode(familyId: number): Promise<{ inviteCode: string }> {
  const res = await client.post<{ inviteCode: string }>(`/families/${familyId}/invite-code/regenerate`)
  return res.data
}

export async function updateMemberRole(familyId: number, userId: number, role: 'ADMIN' | 'MEMBER'): Promise<void> {
  await client.put(`/families/${familyId}/members/${userId}/role`, { role })
}

export async function removeMember(familyId: number, userId: number): Promise<void> {
  await client.delete(`/families/${familyId}/members/${userId}`)
}
