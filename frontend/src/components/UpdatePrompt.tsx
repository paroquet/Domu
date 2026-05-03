import { RefreshCw } from 'lucide-react'
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Button } from '@/components/ui/button'
import { useUpdateStore } from '@/stores/updateStore'

export default function UpdatePrompt() {
  const { needRefresh, versionInfo, setNeedRefresh, updateServiceWorker } = useUpdateStore()

  const handleUpdate = async () => {
    setNeedRefresh(false)
    // Tell SW to skip waiting and activate the new version
    if (updateServiceWorker) {
      await updateServiceWorker(true)
    }
    // The page will reload automatically after SW activates
  }

  const handleClose = () => {
    setNeedRefresh(false)
  }

  return (
    <AlertDialog open={needRefresh} onOpenChange={(open) => !open && handleClose()}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle className="flex items-center gap-2">
            <RefreshCw className="h-5 w-5 text-blue-600" />
            发现新版本 {versionInfo?.version && `v${versionInfo.version}`}
          </AlertDialogTitle>
          {versionInfo?.changelog && versionInfo.changelog.length > 0 && (
            <AlertDialogDescription asChild>
              <div className="space-y-2">
                <p className="font-medium text-gray-700">更新内容：</p>
                <ul className="list-disc list-inside space-y-1">
                  {versionInfo.changelog.map((item, index) => (
                    <li key={index}>{item}</li>
                  ))}
                </ul>
              </div>
            </AlertDialogDescription>
          )}
        </AlertDialogHeader>
        <AlertDialogFooter>
          <Button variant="outline" onClick={handleClose}>
            稍后
          </Button>
          <Button onClick={handleUpdate}>
            立即更新
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
