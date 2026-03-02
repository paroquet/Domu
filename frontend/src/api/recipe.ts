import client from './client'
import type { Recipe, Ingredient, Step } from '@/types'

interface RecipeFormData {
  title: string
  description?: string
  ingredients: Ingredient[]
  steps: Step[]
  coverImagePath?: string
  familyId: number
}

export async function getRecipes(familyId: number): Promise<Recipe[]> {
  const res = await client.get<Recipe[]>('/recipes', { params: { familyId } })
  return res.data
}

export async function getRecipe(id: number): Promise<Recipe> {
  const res = await client.get<Recipe>(`/recipes/${id}`)
  return res.data
}

export async function getSharedRecipe(token: string): Promise<Recipe> {
  const res = await client.get<Recipe>(`/recipes/share/${token}`)
  return res.data
}

export async function createRecipe(data: RecipeFormData): Promise<Recipe> {
  const res = await client.post<Recipe>('/recipes', data)
  return res.data
}

export async function updateRecipe(id: number, data: Partial<RecipeFormData>): Promise<Recipe> {
  const res = await client.put<Recipe>(`/recipes/${id}`, data)
  return res.data
}

export async function deleteRecipe(id: number): Promise<void> {
  await client.delete(`/recipes/${id}`)
}

export async function shareRecipe(id: number): Promise<{ shareToken: string; shareUrl: string }> {
  const res = await client.post<{ shareToken: string; shareUrl: string }>(`/recipes/${id}/share`)
  return res.data
}
