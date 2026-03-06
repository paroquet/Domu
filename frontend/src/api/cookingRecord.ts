import client from './client'
import type { CookingRecord } from '@/types'

interface CookingRecordFormData {
  recipeId: number
  familyId: number
  cookedAt: string
  notes?: string
  images?: string[]
}

export async function getCookingRecords(familyId: number, recipeId?: number): Promise<CookingRecord[]> {
  const res = await client.get<CookingRecord[]>('/cooking-records', {
    params: { familyId, ...(recipeId ? { recipeId } : {}) },
  })
  return res.data
}

export async function getCookingRecord(id: number): Promise<CookingRecord> {
  const res = await client.get<CookingRecord>(`/cooking-records/${id}`)
  return res.data
}

export async function createCookingRecord(data: CookingRecordFormData): Promise<CookingRecord> {
  const res = await client.post<CookingRecord>('/cooking-records', data)
  return res.data
}

export async function updateCookingRecord(id: number, data: Partial<CookingRecordFormData>): Promise<CookingRecord> {
  const res = await client.put<CookingRecord>(`/cooking-records/${id}`, data)
  return res.data
}

export async function deleteCookingRecord(id: number): Promise<void> {
  await client.delete(`/cooking-records/${id}`)
}
