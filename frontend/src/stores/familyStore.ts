import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { FamilySummary } from '@/types'

interface FamilyState {
  currentFamilyId: number | null
  families: FamilySummary[]
  setCurrentFamilyId: (id: number | null) => void
  setFamilies: (families: FamilySummary[]) => void
  initializeFamily: (families: FamilySummary[]) => void
}

export const useFamilyStore = create<FamilyState>()(
  persist(
    (set, get) => ({
      currentFamilyId: null,
      families: [],
      setCurrentFamilyId: (id) => set({ currentFamilyId: id }),
      setFamilies: (families) => set({ families }),
      initializeFamily: (families) => {
        const currentId = get().currentFamilyId
        // 如果缓存的 currentFamilyId 在新列表中存在，保持不变
        if (currentId && families.some(f => f.id === currentId)) {
          set({ families })
        } else if (families.length > 0) {
          // 否则选择第一个家庭
          set({ families, currentFamilyId: families[0].id })
        } else {
          // 列表为空则清空
          set({ families, currentFamilyId: null })
        }
      },
    }),
    { name: 'domu-family' }
  )
)
