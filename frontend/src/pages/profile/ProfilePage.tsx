import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { LogOut, User, Camera } from 'lucide-react'
import { updateMe, logout } from '@/api/auth'
import { uploadFile } from '@/api/file'
import { useAuthStore } from '@/stores/authStore'
import { useFamilyStore } from '@/stores/familyStore'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Separator } from '@/components/ui/separator'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog'
import { toast } from '@/components/ui/use-toast'

export default function ProfilePage() {
  const { user, setUser } = useAuthStore()
  const { setCurrentFamilyId } = useFamilyStore()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const [name, setName] = useState(user?.name ?? '')
  const [avatarUploading, setAvatarUploading] = useState(false)

  const updateMutation = useMutation({
    mutationFn: updateMe,
    onSuccess: (updated) => {
      setUser(updated)
      toast({ title: '个人信息已更新' })
    },
    onError: () => toast({ title: '更新失败', variant: 'destructive' }),
  })

  const handleSaveName = () => {
    if (!name.trim()) {
      toast({ title: '姓名不能为空', variant: 'destructive' })
      return
    }
    updateMutation.mutate({ name: name.trim(), avatarPath: user?.avatarPath })
  }

  const handleAvatarChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setAvatarUploading(true)
    try {
      const res = await uploadFile(file)
      await updateMutation.mutateAsync({ name: user?.name ?? name, avatarPath: res.path })
    } catch {
      toast({ title: '头像上传失败', variant: 'destructive' })
    } finally {
      setAvatarUploading(false)
    }
  }

  const handleLogout = async () => {
    try {
      await logout()
    } finally {
      setUser(null)
      setCurrentFamilyId(null)
      queryClient.clear()
      navigate('/login')
    }
  }

  const userInitials = user?.name
    ? user.name.slice(0, 2)
    : user?.email?.slice(0, 2).toUpperCase() ?? '?'

  return (
    <div className="max-w-lg mx-auto space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">个人信息</h1>

      {/* Avatar */}
      <Card>
        <CardContent className="pt-6 flex flex-col items-center gap-4">
          <div className="relative">
            <Avatar className="h-24 w-24">
              {user?.avatarPath && <AvatarImage src={user.avatarPath} alt={user.name} />}
              <AvatarFallback className="text-2xl">{userInitials}</AvatarFallback>
            </Avatar>
            <label className="absolute bottom-0 right-0 bg-blue-600 text-white rounded-full p-1.5 cursor-pointer shadow-md hover:bg-blue-700 transition-colors">
              {avatarUploading ? (
                <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
              ) : (
                <Camera className="h-4 w-4" />
              )}
              <input
                type="file"
                accept="image/*"
                className="hidden"
                onChange={handleAvatarChange}
                disabled={avatarUploading}
              />
            </label>
          </div>
          <div className="text-center">
            <p className="font-semibold text-gray-900 text-lg">{user?.name}</p>
            <p className="text-sm text-gray-500">{user?.email}</p>
          </div>
        </CardContent>
      </Card>

      {/* Edit name */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <User className="h-4 w-4 text-blue-600" />
            基本信息
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="name">显示姓名</Label>
            <div className="flex gap-2">
              <Input
                id="name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="你的名字"
                className="flex-1"
              />
              <Button
                onClick={handleSaveName}
                disabled={updateMutation.isPending || name.trim() === user?.name}
              >
                {updateMutation.isPending ? '保存中...' : '保存'}
              </Button>
            </div>
          </div>
          <Separator />
          <div className="space-y-1.5">
            <Label className="text-gray-500">邮箱</Label>
            <p className="text-sm text-gray-800 bg-gray-50 rounded-md px-3 py-2 border border-gray-100">
              {user?.email}
            </p>
            <p className="text-xs text-gray-400">邮箱不可修改</p>
          </div>
        </CardContent>
      </Card>

      {/* Logout */}
      <Card>
        <CardContent className="pt-6">
          <AlertDialog>
            <AlertDialogTrigger asChild>
              <Button variant="outline" className="w-full text-red-600 border-red-200 hover:bg-red-50 hover:border-red-300">
                <LogOut className="h-4 w-4" />
                退出登录
              </Button>
            </AlertDialogTrigger>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>退出登录</AlertDialogTitle>
                <AlertDialogDescription>
                  确定要退出登录吗？
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel>取消</AlertDialogCancel>
                <AlertDialogAction
                  onClick={handleLogout}
                  className="bg-red-500 hover:bg-red-600"
                >
                  退出
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        </CardContent>
      </Card>
    </div>
  )
}
