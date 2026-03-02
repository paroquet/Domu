import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface FamilyState {
  currentFamilyId: number | null
  setCurrentFamilyId: (id: number | null) => void
}

export const useFamilyStore = create<FamilyState>()(
  persist(
    (set) => ({
      currentFamilyId: null,
      setCurrentFamilyId: (id) => set({ currentFamilyId: id }),
    }),
    { name: 'domu-family' }
  )
)
