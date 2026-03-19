import { create } from 'zustand'
import type { VersionInfo } from '@/version'

interface UpdateState {
  needRefresh: boolean
  versionInfo: VersionInfo | null
  updateServiceWorker: ((reloadPage?: boolean) => Promise<void>) | null
  setNeedRefresh: (need: boolean) => void
  setVersionInfo: (info: VersionInfo) => void
  setUpdateServiceWorker: (fn: (reloadPage?: boolean) => Promise<void>) => void
}

export const useUpdateStore = create<UpdateState>((set) => ({
  needRefresh: false,
  versionInfo: null,
  updateServiceWorker: null,
  setNeedRefresh: (need) => set({ needRefresh: need }),
  setVersionInfo: (info) => set({ versionInfo: info }),
  setUpdateServiceWorker: (fn) => set({ updateServiceWorker: fn }),
}))
