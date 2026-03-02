import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Users, Copy, Check, RefreshCw, Crown, UserMinus, Shield } from 'lucide-react'
import {
  createFamily,
  getFamily,
  getFamilyMembers,
  joinFamily,
  regenerateInviteCode,
  updateMemberRole,
  removeMember,
} from '@/api/family'
import { useFamilyStore } from '@/stores/familyStore'
import { useAuthStore } from '@/stores/authStore'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
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
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { toast } from '@/components/ui/use-toast'

export default function FamilyPage() {
  const { currentFamilyId, setCurrentFamilyId } = useFamilyStore()
  const { user } = useAuthStore()
  const queryClient = useQueryClient()

  const [createName, setCreateName] = useState('')
  const [inviteCodeInput, setInviteCodeInput] = useState('')
  const [copiedCode, setCopiedCode] = useState(false)
  const [localInviteCode, setLocalInviteCode] = useState<string | null>(null)

  const { data: family, isLoading: familyLoading } = useQuery({
    queryKey: ['family', currentFamilyId],
    queryFn: () => getFamily(currentFamilyId!),
    enabled: !!currentFamilyId,
  })

  const { data: members = [] } = useQuery({
    queryKey: ['family-members', currentFamilyId],
    queryFn: () => getFamilyMembers(currentFamilyId!),
    enabled: !!currentFamilyId,
  })

  const createMutation = useMutation({
    mutationFn: () => createFamily(createName.trim()),
    onSuccess: (data) => {
      setCurrentFamilyId(data.id)
      queryClient.invalidateQueries({ queryKey: ['family'] })
      toast({ title: `家庭「${data.name}」创建成功` })
    },
    onError: () => toast({ title: '创建失败', variant: 'destructive' }),
  })

  const joinMutation = useMutation({
    mutationFn: () => joinFamily(inviteCodeInput.trim()),
    onSuccess: (data) => {
      setCurrentFamilyId(data.id)
      queryClient.invalidateQueries({ queryKey: ['family'] })
      toast({ title: `成功加入家庭「${data.name}」` })
    },
    onError: () => toast({ title: '加入失败，邀请码无效', variant: 'destructive' }),
  })

  const regenCodeMutation = useMutation({
    mutationFn: () => regenerateInviteCode(currentFamilyId!),
    onSuccess: (data) => {
      setLocalInviteCode(data.inviteCode)
      queryClient.invalidateQueries({ queryKey: ['family', currentFamilyId] })
      toast({ title: '邀请码已重新生成' })
    },
    onError: () => toast({ title: '操作失败', variant: 'destructive' }),
  })

  const updateRoleMutation = useMutation({
    mutationFn: ({ userId, role }: { userId: number; role: 'ADMIN' | 'MEMBER' }) =>
      updateMemberRole(currentFamilyId!, userId, role),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['family-members', currentFamilyId] })
      toast({ title: '角色已更新' })
    },
    onError: () => toast({ title: '操作失败', variant: 'destructive' }),
  })

  const removeMemberMutation = useMutation({
    mutationFn: (userId: number) => removeMember(currentFamilyId!, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['family-members', currentFamilyId] })
      toast({ title: '已移除成员' })
    },
    onError: () => toast({ title: '操作失败', variant: 'destructive' }),
  })

  const currentUserMember = members.find((m) => m.userId === user?.id)
  const isAdmin = currentUserMember?.role === 'ADMIN'
  const inviteCode = localInviteCode ?? family?.inviteCode ?? ''

  const handleCopyCode = async () => {
    try {
      await navigator.clipboard.writeText(inviteCode)
      setCopiedCode(true)
      setTimeout(() => setCopiedCode(false), 2000)
    } catch {
      toast({ title: '复制失败', variant: 'destructive' })
    }
  }

  if (!currentFamilyId || (!familyLoading && !family)) {
    return (
      <div className="max-w-md mx-auto space-y-6">
        <div className="text-center">
          <Users className="h-16 w-16 text-gray-300 mx-auto mb-3" />
          <h1 className="text-2xl font-bold text-gray-900">家庭管理</h1>
          <p className="text-gray-500 mt-1">创建或加入一个家庭开始使用</p>
        </div>

        <Tabs defaultValue="create">
          <TabsList className="w-full">
            <TabsTrigger value="create" className="flex-1">创建家庭</TabsTrigger>
            <TabsTrigger value="join" className="flex-1">加入家庭</TabsTrigger>
          </TabsList>

          <TabsContent value="create">
            <Card>
              <CardHeader>
                <CardTitle className="text-base">创建新家庭</CardTitle>
                <CardDescription>创建后你将成为管理员</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-1.5">
                  <Label htmlFor="familyName">家庭名称</Label>
                  <Input
                    id="familyName"
                    placeholder="例如：我的家庭"
                    value={createName}
                    onChange={(e) => setCreateName(e.target.value)}
                  />
                </div>
                <Button
                  className="w-full"
                  onClick={() => createMutation.mutate()}
                  disabled={!createName.trim() || createMutation.isPending}
                >
                  {createMutation.isPending ? '创建中...' : '创建家庭'}
                </Button>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="join">
            <Card>
              <CardHeader>
                <CardTitle className="text-base">加入家庭</CardTitle>
                <CardDescription>输入家庭管理员提供的邀请码</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-1.5">
                  <Label htmlFor="inviteCode">邀请码</Label>
                  <Input
                    id="inviteCode"
                    placeholder="请输入邀请码"
                    value={inviteCodeInput}
                    onChange={(e) => setInviteCodeInput(e.target.value)}
                  />
                </div>
                <Button
                  className="w-full"
                  onClick={() => joinMutation.mutate()}
                  disabled={!inviteCodeInput.trim() || joinMutation.isPending}
                >
                  {joinMutation.isPending ? '加入中...' : '加入家庭'}
                </Button>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    )
  }

  if (familyLoading) {
    return (
      <div className="space-y-4">
        <div className="h-32 bg-gray-100 rounded-xl animate-pulse" />
        <div className="h-48 bg-gray-100 rounded-xl animate-pulse" />
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">家庭管理</h1>

      {/* Family info */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Users className="h-4 w-4 text-blue-600" />
            {family?.name}
          </CardTitle>
          <CardDescription>{members.length} 位成员</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <Separator />
          <div>
            <Label className="text-xs text-gray-500 mb-2 block">邀请码</Label>
            <div className="flex items-center gap-2">
              <div className="flex-1 bg-gray-50 border border-gray-200 rounded-md px-3 py-2 font-mono text-sm tracking-widest text-gray-800">
                {inviteCode}
              </div>
              <Button variant="outline" size="icon" onClick={handleCopyCode}>
                {copiedCode ? <Check className="h-4 w-4 text-green-600" /> : <Copy className="h-4 w-4" />}
              </Button>
              {isAdmin && (
                <Button
                  variant="outline"
                  size="icon"
                  onClick={() => regenCodeMutation.mutate()}
                  disabled={regenCodeMutation.isPending}
                  title="重新生成邀请码"
                >
                  <RefreshCw className={`h-4 w-4 ${regenCodeMutation.isPending ? 'animate-spin' : ''}`} />
                </Button>
              )}
            </div>
            <p className="text-xs text-gray-400 mt-1">将邀请码分享给家庭成员，让他们加入</p>
          </div>
        </CardContent>
      </Card>

      {/* Members */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">成员列表</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {members.map((member, i) => (
            <div key={member.userId}>
              {i > 0 && <Separator className="mb-3" />}
              <div className="flex items-center gap-3">
                <Avatar className="h-10 w-10">
                  {member.avatarPath && <AvatarImage src={member.avatarPath} alt={member.name} />}
                  <AvatarFallback>{member.name.slice(0, 2)}</AvatarFallback>
                </Avatar>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="font-medium text-gray-900">{member.name}</span>
                    {member.role === 'ADMIN' ? (
                      <Badge variant="default" className="text-xs px-1.5 py-0">
                        <Crown className="h-2.5 w-2.5 mr-0.5" />
                        管理员
                      </Badge>
                    ) : (
                      <Badge variant="secondary" className="text-xs px-1.5 py-0">成员</Badge>
                    )}
                    {member.userId === user?.id && (
                      <Badge variant="outline" className="text-xs px-1.5 py-0">我</Badge>
                    )}
                  </div>
                  <p className="text-sm text-gray-500 truncate">{member.email}</p>
                </div>

                {/* Admin actions */}
                {isAdmin && member.userId !== user?.id && (
                  <div className="flex items-center gap-1 shrink-0">
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8 text-gray-400 hover:text-blue-600"
                      title={member.role === 'ADMIN' ? '降为成员' : '设为管理员'}
                      onClick={() =>
                        updateRoleMutation.mutate({
                          userId: member.userId,
                          role: member.role === 'ADMIN' ? 'MEMBER' : 'ADMIN',
                        })
                      }
                      disabled={updateRoleMutation.isPending}
                    >
                      <Shield className="h-4 w-4" />
                    </Button>
                    <AlertDialog>
                      <AlertDialogTrigger asChild>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8 text-gray-400 hover:text-red-600"
                          title="移出家庭"
                        >
                          <UserMinus className="h-4 w-4" />
                        </Button>
                      </AlertDialogTrigger>
                      <AlertDialogContent>
                        <AlertDialogHeader>
                          <AlertDialogTitle>移出成员</AlertDialogTitle>
                          <AlertDialogDescription>
                            确定要将「{member.name}」从家庭中移出吗？
                          </AlertDialogDescription>
                        </AlertDialogHeader>
                        <AlertDialogFooter>
                          <AlertDialogCancel>取消</AlertDialogCancel>
                          <AlertDialogAction
                            onClick={() => removeMemberMutation.mutate(member.userId)}
                            className="bg-red-500 hover:bg-red-600"
                          >
                            移出
                          </AlertDialogAction>
                        </AlertDialogFooter>
                      </AlertDialogContent>
                    </AlertDialog>
                  </div>
                )}
              </div>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  )
}
