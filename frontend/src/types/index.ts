export interface User {
  id: number
  email: string
  name: string
  avatarPath?: string
}

export interface Family {
  id: number
  name: string
  inviteCode: string
  createdAt: string
}

export interface FamilyMember {
  userId: number
  name: string
  email: string
  avatarPath?: string
  role: 'ADMIN' | 'MEMBER'
  joinedAt: string
}

export interface Ingredient {
  name: string
  amount: string
  unit: string
}

export interface Step {
  order: number
  description: string
  imagePath?: string
}

export interface Recipe {
  id: number
  title: string
  description?: string
  ingredients: Ingredient[]
  steps: Step[]
  coverImagePath?: string
  authorId: number
  authorName: string
  familyId: number
  shareToken?: string
  shareUrl?: string
  createdAt: string
  updatedAt: string
}

export interface CookingRecord {
  id: number
  recipeId: number
  recipeTitle: string
  userId: number
  userName: string
  familyId: number
  cookedAt: string
  notes?: string
  images: string[]
  createdAt: string
}

export interface Order {
  id: number
  familyId: number
  orderedById: number
  orderedByName: string
  orderedForId: number
  orderedForName: string
  recipeId: number
  recipeTitle: string
  plannedDate: string
  status: 'PENDING' | 'DONE' | 'CANCELLED'
  createdAt: string
}

export interface ShoppingItem {
  name: string
  amount: number
  unit: string
}
