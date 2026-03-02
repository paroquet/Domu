import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { format } from 'date-fns'
import { zhCN } from 'date-fns/locale'
import { Plus, ShoppingCart, Check, X, ShoppingBag, ChevronLeft, ChevronRight } from 'lucide-react'
import { getOrders, createOrder, updateOrderStatus, deleteOrder, getShoppingPlan } from '@/api/order'
import { getRecipes } from '@/api/recipe'
import { getFamilyMembers } from '@/api/family'
import { useFamilyStore } from '@/stores/familyStore'
import { useAuthStore } from '@/stores/authStore'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import { Separator } from '@/components/ui/separator'
import { toast } from '@/components/ui/use-toast'
import type { Order } from '@/types'

function getStatusBadge(status: Order['status']) {
  switch (status) {
    case 'PENDING':
      return <Badge variant="warning">待做</Badge>
    case 'DONE':
      return <Badge variant="success">已完成</Badge>
    case 'CANCELLED':
      return <Badge variant="secondary">已取消</Badge>
  }
}

function addDays(date: Date, days: number) {
  const d = new Date(date)
  d.setDate(d.getDate() + days)
  return d
}

export default function OrderPage() {
  const { currentFamilyId } = useFamilyStore()
  const { user } = useAuthStore()
  const queryClient = useQueryClient()

  const [selectedDate, setSelectedDate] = useState(new Date())
  const [createDialogOpen, setCreateDialogOpen] = useState(false)
  const [shoppingDialogOpen, setShoppingDialogOpen] = useState(false)
  const [newOrderRecipeId, setNewOrderRecipeId] = useState('')
  const [newOrderForId, setNewOrderForId] = useState('')

  const dateStr = format(selectedDate, 'yyyy-MM-dd')

  const { data: orders = [], isLoading } = useQuery({
    queryKey: ['orders', currentFamilyId, dateStr],
    queryFn: () => getOrders(currentFamilyId!, dateStr),
    enabled: !!currentFamilyId,
  })

  const { data: recipes = [] } = useQuery({
    queryKey: ['recipes', currentFamilyId],
    queryFn: () => getRecipes(currentFamilyId!),
    enabled: !!currentFamilyId,
  })

  const { data: members = [] } = useQuery({
    queryKey: ['family-members', currentFamilyId],
    queryFn: () => getFamilyMembers(currentFamilyId!),
    enabled: !!currentFamilyId,
  })

  const { data: shoppingPlan = [], refetch: refetchShopping } = useQuery({
    queryKey: ['shopping-plan', currentFamilyId, dateStr],
    queryFn: () => getShoppingPlan(currentFamilyId!, dateStr),
    enabled: false,
  })

  const createMutation = useMutation({
    mutationFn: createOrder,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders', currentFamilyId, dateStr] })
      setCreateDialogOpen(false)
      setNewOrderRecipeId('')
      setNewOrderForId('')
      toast({ title: '点菜成功' })
    },
    onError: () => toast({ title: '操作失败', variant: 'destructive' }),
  })

  const updateStatusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: Order['status'] }) =>
      updateOrderStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders', currentFamilyId, dateStr] })
    },
    onError: () => toast({ title: '操作失败', variant: 'destructive' }),
  })

  const deleteMutation = useMutation({
    mutationFn: deleteOrder,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders', currentFamilyId, dateStr] })
      toast({ title: '已取消点菜' })
    },
    onError: () => toast({ title: '操作失败', variant: 'destructive' }),
  })

  const handleCreateOrder = () => {
    if (!newOrderRecipeId || !newOrderForId || !currentFamilyId) {
      toast({ title: '请选择菜谱和用餐人', variant: 'destructive' })
      return
    }
    createMutation.mutate({
      familyId: currentFamilyId,
      recipeId: Number(newOrderRecipeId),
      orderedForId: Number(newOrderForId),
      plannedDate: dateStr,
    })
  }

  const handleViewShopping = async () => {
    await refetchShopping()
    setShoppingDialogOpen(true)
  }

  if (!currentFamilyId) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-center">
        <ShoppingCart className="h-16 w-16 text-gray-300 mb-4" />
        <h2 className="text-xl font-semibold text-gray-700 mb-2">还没有加入家庭</h2>
        <p className="text-gray-500">请先创建或加入一个家庭</p>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">点菜</h1>
        <Button size="sm" onClick={() => setCreateDialogOpen(true)}>
          <Plus className="h-4 w-4" />
          点菜
        </Button>
      </div>

      {/* Date picker */}
      <div className="flex items-center gap-2 bg-white border border-gray-200 rounded-lg p-2">
        <Button
          variant="ghost"
          size="icon"
          className="h-8 w-8"
          onClick={() => setSelectedDate((d) => addDays(d, -1))}
        >
          <ChevronLeft className="h-4 w-4" />
        </Button>
        <div className="flex-1 text-center">
          <div className="font-semibold text-gray-900">
            {format(selectedDate, 'MM月dd日', { locale: zhCN })}
          </div>
          <div className="text-xs text-gray-400">
            {format(selectedDate, 'EEEE', { locale: zhCN })}
          </div>
        </div>
        <Button
          variant="ghost"
          size="icon"
          className="h-8 w-8"
          onClick={() => setSelectedDate((d) => addDays(d, 1))}
        >
          <ChevronRight className="h-4 w-4" />
        </Button>
        <Button
          variant="outline"
          size="sm"
          onClick={() => setSelectedDate(new Date())}
          className="text-xs"
        >
          今天
        </Button>
      </div>

      {/* Shopping plan button */}
      {orders.length > 0 && (
        <Button variant="outline" size="sm" className="w-full" onClick={handleViewShopping}>
          <ShoppingBag className="h-4 w-4" />
          查看当日买菜计划
        </Button>
      )}

      {/* Orders list */}
      {isLoading ? (
        <div className="space-y-3">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="h-20 bg-gray-100 rounded-xl animate-pulse" />
          ))}
        </div>
      ) : orders.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <ShoppingCart className="h-12 w-12 text-gray-300 mb-3" />
          <p className="text-gray-500">今天还没有点菜</p>
          <Button className="mt-4" size="sm" onClick={() => setCreateDialogOpen(true)}>
            立即点菜
          </Button>
        </div>
      ) : (
        <div className="space-y-3">
          {orders.map((order) => (
            <Card key={order.id}>
              <CardContent className="p-4">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-semibold text-gray-900">{order.recipeTitle}</span>
                      {getStatusBadge(order.status)}
                    </div>
                    <div className="text-sm text-gray-500 mt-1">
                      <span>{order.orderedByName} 为 </span>
                      <span className="font-medium text-gray-700">{order.orderedForName}</span>
                      <span> 点的</span>
                    </div>
                  </div>
                  <div className="flex items-center gap-1 shrink-0">
                    {order.status === 'PENDING' && (
                      <>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8 text-green-600 hover:bg-green-50"
                          title="标记完成"
                          onClick={() => updateStatusMutation.mutate({ id: order.id, status: 'DONE' })}
                          disabled={updateStatusMutation.isPending}
                        >
                          <Check className="h-4 w-4" />
                        </Button>
                        {(order.orderedById === user?.id) && (
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8 text-gray-400 hover:text-red-600"
                            title="取消"
                            onClick={() => deleteMutation.mutate(order.id)}
                            disabled={deleteMutation.isPending}
                          >
                            <X className="h-4 w-4" />
                          </Button>
                        )}
                      </>
                    )}
                    {order.status === 'DONE' && (
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8 text-gray-400"
                        title="撤销完成"
                        onClick={() => updateStatusMutation.mutate({ id: order.id, status: 'PENDING' })}
                        disabled={updateStatusMutation.isPending}
                      >
                        <X className="h-4 w-4" />
                      </Button>
                    )}
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Create order dialog */}
      <Dialog open={createDialogOpen} onOpenChange={setCreateDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>点菜</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <div className="space-y-1.5">
              <Label>选择菜谱</Label>
              <Select value={newOrderRecipeId} onValueChange={setNewOrderRecipeId}>
                <SelectTrigger>
                  <SelectValue placeholder="请选择菜谱" />
                </SelectTrigger>
                <SelectContent>
                  {recipes.map((r) => (
                    <SelectItem key={r.id} value={String(r.id)}>
                      {r.title}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label>点给谁吃</Label>
              <Select value={newOrderForId} onValueChange={setNewOrderForId}>
                <SelectTrigger>
                  <SelectValue placeholder="请选择家庭成员" />
                </SelectTrigger>
                <SelectContent>
                  {members.map((m) => (
                    <SelectItem key={m.userId} value={String(m.userId)}>
                      {m.name}
                      {m.userId === user?.id ? '（我）' : ''}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="text-sm text-gray-500">
              用餐日期：{format(selectedDate, 'yyyy年MM月dd日', { locale: zhCN })}
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateDialogOpen(false)}>取消</Button>
            <Button onClick={handleCreateOrder} disabled={createMutation.isPending}>
              {createMutation.isPending ? '提交中...' : '确认点菜'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Shopping plan dialog */}
      <Dialog open={shoppingDialogOpen} onOpenChange={setShoppingDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {format(selectedDate, 'MM月dd日', { locale: zhCN })} 买菜计划
            </DialogTitle>
          </DialogHeader>
          <div className="py-2">
            {shoppingPlan.length === 0 ? (
              <p className="text-center text-gray-500 py-4">暂无食材需要购买</p>
            ) : (
              <div className="space-y-1">
                <div className="grid grid-cols-3 gap-2 text-xs font-medium text-gray-500 pb-2">
                  <span>食材</span>
                  <span className="text-right">数量</span>
                  <span className="text-right">单位</span>
                </div>
                <Separator />
                {shoppingPlan.map((item, i) => (
                  <div key={i} className="grid grid-cols-3 gap-2 py-2 text-sm border-b border-gray-50 last:border-0">
                    <span className="text-gray-800">{item.name}</span>
                    <span className="text-right text-gray-700">{item.amount}</span>
                    <span className="text-right text-gray-500">{item.unit}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}
