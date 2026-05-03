import { useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Trash2, ChefHat, User, Clock } from 'lucide-react'
import { format } from 'date-fns'
import { zhCN } from 'date-fns/locale'
import { getCookingRecord, deleteCookingRecord } from '@/api/cookingRecord'
import { useAuthStore } from '@/stores/authStore'
import { Button } from '@/components/ui/button'
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

export default function CookingRecordDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { user } = useAuthStore()
  const [previewImage, setPreviewImage] = useState<string | null>(null)

  const { data: record, isLoading } = useQuery({
    queryKey: ['cooking-record', id],
    queryFn: () => getCookingRecord(Number(id)),
    enabled: !!id,
  })

  const deleteMutation = useMutation({
    mutationFn: () => deleteCookingRecord(Number(id)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cooking-records'] })
      navigate(-1)
      toast({ title: '记录已删除' })
    },
    onError: () => {
      toast({ title: '删除失败', variant: 'destructive' })
    },
  })

  if (isLoading) {
    return (
      <div className="space-y-4 max-w-2xl mx-auto">
        <div className="h-8 bg-muted rounded animate-pulse w-1/3" />
        <div className="h-32 bg-muted rounded-xl animate-pulse" />
        <div className="h-48 bg-muted rounded-xl animate-pulse" />
      </div>
    )
  }

  if (!record) {
    return (
      <div className="text-center py-16">
        <p className="text-muted-foreground">记录不存在</p>
        <Button asChild className="mt-4">
          <Link to="/cooking-records">返回做菜记录</Link>
        </Button>
      </div>
    )
  }

  const isAuthor = user?.id === record.userId

  return (
    <div className="space-y-6 max-w-2xl mx-auto">
      <button
        onClick={() => navigate(-1)}
        className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors"
      >
        <ArrowLeft className="h-4 w-4" />
        返回
      </button>

      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0">
          <Link
            to={`/recipes/${record.recipeId}`}
            className="inline-flex items-center gap-1.5 text-sm text-primary hover:text-primary/80"
          >
            <ChefHat className="h-4 w-4" />
            {record.recipeTitle}
          </Link>
          <h1 className="text-2xl font-bold text-foreground mt-1">做菜记录</h1>
          <div className="flex items-center gap-3 mt-2 text-sm text-muted-foreground">
            <span className="flex items-center gap-1">
              <User className="h-3.5 w-3.5" />
              {record.userName}
            </span>
            <span>·</span>
            <span className="flex items-center gap-1">
              <Clock className="h-3.5 w-3.5" />
              {format(new Date(record.cookedAt), 'yyyy年MM月dd日 HH:mm', { locale: zhCN })}
            </span>
          </div>
        </div>
        {isAuthor && (
          <AlertDialog>
            <AlertDialogTrigger asChild>
              <Button variant="outline" size="sm" className="text-destructive hover:text-destructive shrink-0">
                <Trash2 className="h-4 w-4" />
              </Button>
            </AlertDialogTrigger>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>删除做菜记录</AlertDialogTitle>
                <AlertDialogDescription>确定要删除这条做菜记录吗？此操作无法撤销。</AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel>取消</AlertDialogCancel>
                <AlertDialogAction
                  onClick={() => deleteMutation.mutate()}
                  className="bg-destructive hover:bg-destructive/90"
                >
                  删除
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        )}
      </div>

      {record.notes && (
        <div>
          <h2 className="text-lg font-semibold text-foreground mb-3">心得</h2>
          <p className="text-foreground/80 leading-relaxed whitespace-pre-line bg-muted rounded-xl p-4">
            {record.notes}
          </p>
        </div>
      )}

      {record.images.length > 0 && (
        <div>
          <h2 className="text-lg font-semibold text-foreground mb-3">照片</h2>
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
            {record.images.map((img, i) => (
              <button
                key={i}
                type="button"
                onClick={() => setPreviewImage(img)}
                className="aspect-square overflow-hidden rounded-lg bg-muted"
              >
                <img
                  src={img}
                  alt={`照片 ${i + 1}`}
                  className="w-full h-full object-cover hover:scale-105 transition-transform"
                />
              </button>
            ))}
          </div>
        </div>
      )}

      {previewImage && (
        <div
          className="fixed inset-0 z-50 bg-black/80 flex items-center justify-center p-4"
          onClick={() => setPreviewImage(null)}
        >
          <img src={previewImage} alt="" className="max-w-full max-h-full object-contain rounded-lg" />
        </div>
      )}
    </div>
  )
}
