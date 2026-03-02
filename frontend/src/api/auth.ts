import client from './client'
import type { User } from '@/types'

export async function register(data: { email: string; password: string; name: string }): Promise<User> {
  const res = await client.post<User>('/auth/register', data)
  return res.data
}

export async function login(data: { email: string; password: string }): Promise<User> {
  const res = await client.post<User>('/auth/login', data)
  return res.data
}

export async function logout(): Promise<void> {
  await client.post('/auth/logout')
}

export async function getMe(): Promise<User> {
  const res = await client.get<User>('/auth/me')
  return res.data
}

export async function updateMe(data: { name: string; avatarPath?: string }): Promise<User> {
  const res = await client.put<User>('/auth/me', data)
  return res.data
}
