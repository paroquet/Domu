import client from './client'
import type { Order, ShoppingItem } from '@/types'

interface OrderFormData {
  familyId: number
  orderedForId: number
  recipeId: number
  plannedDate: string
}

export async function getOrders(familyId: number, date?: string): Promise<Order[]> {
  const res = await client.get<Order[]>('/orders', {
    params: { familyId, ...(date ? { date } : {}) },
  })
  return res.data
}

export async function createOrder(data: OrderFormData): Promise<Order> {
  const res = await client.post<Order>('/orders', data)
  return res.data
}

export async function updateOrderStatus(id: number, status: 'PENDING' | 'DONE' | 'CANCELLED'): Promise<Order> {
  const res = await client.put<Order>(`/orders/${id}/status`, { status })
  return res.data
}

export async function deleteOrder(id: number): Promise<void> {
  await client.delete(`/orders/${id}`)
}

export async function getShoppingPlan(familyId: number, date: string): Promise<ShoppingItem[]> {
  const res = await client.get<ShoppingItem[]>('/orders/shopping-plan', {
    params: { familyId, date },
  })
  return res.data
}
