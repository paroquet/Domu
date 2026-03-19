import { useEffect } from 'react'
import { registerSW } from 'virtual:pwa-register'
import { useUpdateStore } from '@/stores/updateStore'
import type { VersionInfo } from '@/version'

export function useRegisterSW() {
  const { setNeedRefresh, setVersionInfo, setUpdateServiceWorker } = useUpdateStore()

  useEffect(() => {
    // Fetch the latest version info from server (bypassing cache)
    const fetchLatestVersion = async (): Promise<VersionInfo | null> => {
      try {
        const response = await fetch('/version.json?t=' + Date.now(), {
          cache: 'no-store',
        })
        if (response.ok) {
          return await response.json()
        }
      } catch (error) {
        console.error('[SW] Failed to fetch version info:', error)
      }
      return null
    }

    // Register service worker
    const updateSW = registerSW({
      immediate: false,
      async onNeedRefresh() {
        console.log('[SW] New version available, fetching version info...')
        const newVersion = await fetchLatestVersion()
        if (newVersion) {
          console.log('[SW] New version:', newVersion.version)
          setVersionInfo(newVersion)
        }
        setNeedRefresh(true)
      },
      onOfflineReady() {
        console.log('[SW] App ready for offline use')
      },
      onRegistered(registration) {
        console.log('[SW] Service worker registered, current version:', __APP_VERSION__)
        // Check for updates periodically
        if (registration) {
          setInterval(() => {
            registration.update()
          }, 60 * 60 * 1000)
        }
      },
      onRegisterError(error: Error) {
        console.error('[SW] Registration error:', error)
      },
    })

    // Save the update function for later use
    setUpdateServiceWorker(updateSW)
  }, [setNeedRefresh, setVersionInfo, setUpdateServiceWorker])
}
