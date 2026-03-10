import { useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Edit, Trash2, Share2, ChefHat, ArrowLeft, ClipboardList, Copy, Check } from 'lucide-react'
import { format } from 'date-fns'
import { zhCN } from 'date-fns/locale'
import ImageLightbox from '@/components/ImageLightbox'
import { getRecipe, deleteRecipe, shareRecipe } from '@/api/recipe'
import { getCookingRecords } from '@/api/cookingRecord'
import { useFamilyStore } from '@/stores/familyStore'
import CookingRecordCard from '@/components/CookingRecordCard'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
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
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { toast } from '@/components/ui/use-toast'

export default function RecipeDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { currentFamilyId } = useFamilyStore()
  const [shareDialogOpen, setShareDialogOpen] = useState(false)
  const [shareUrl, setShareUrl] = useState('')
  const [copied, setCopied] = useState(false)
  const [lightboxOpen, setLightboxOpen] = useState(false)
  const [lightboxIndex, setLightboxIndex] = useState(0)

  const { data: recipe, isLoading } = useQuery({
    queryKey: ['recipe', id],
    queryFn: () => getRecipe(Number(id)),
    enabled: !!id,
  })

  const { data: cookingRecords = [] } = useQuery({
    queryKey: ['cooking-records', currentFamilyId, id],
    queryFn: () => getCookingRecords(currentFamilyId!, Number(id)),
    enabled: !!currentFamilyId && !!id,
  })

  const deleteMutation = useMutation({
    mutationFn: () => deleteRecipe(Number(id)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recipes'] })
      navigate('/recipes')
      toast({ title: '菜谱已删除' })
    },
    onError: () => {
      toast({ title: '删除失败', variant: 'destructive' })
    },
  })

  const shareMutation = useMutation({
    mutationFn: () => shareRecipe(Number(id)),
    onSuccess: (data) => {
      setShareUrl(`${window.location.origin}/share/${data.shareToken}`)
      setShareDialogOpen(true)
    },
    onError: () => {
      toast({ title: '获取分享链接失败', variant: 'destructive' })
    },
  })

  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(shareUrl)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      toast({ title: '复制失败', variant: 'destructive' })
    }
  }

  if (isLoading) {
    return (
      <div className="space-y-4">
        <div className="h-64 bg-gray-100 rounded-xl animate-pulse" />
        <div className="h-8 bg-gray-100 rounded animate-pulse w-1/2" />
        <div className="h-4 bg-gray-100 rounded animate-pulse" />
      </div>
    )
  }

  if (!recipe) {
    return (
      <div className="text-center py-16">
        <p className="text-gray-500">菜谱不存在</p>
        <Button asChild className="mt-4">
          <Link to="/recipes">返回菜谱列表</Link>
        </Button>
      </div>
    )
  }

  const slides = [
    ...(recipe.coverImagePath ? [{ src: recipe.coverImagePath }] : []),
    ...recipe.steps.filter(s => s.imagePath).map(s => ({ src: s.imagePath! })),
  ]

  const coverSlideIndex = recipe.coverImagePath ? 0 : -1
  const stepSlideIndex = (stepOrder: number) => {
    const hasCover = recipe.coverImagePath ? 1 : 0
    const stepsWithImg = recipe.steps.filter(s => s.imagePath)
    const idx = stepsWithImg.findIndex(s => s.order === stepOrder)
    return idx === -1 ? -1 : hasCover + idx
  }

  const openLightbox = (index: number) => {
    if (index < 0 || slides.length === 0) return
    setLightboxIndex(index)
    setLightboxOpen(true)
  }

  return (
    <div className="space-y-6 max-w-2xl mx-auto">
      {/* Back button */}
      <button
        onClick={() => navigate(-1)}
        className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-900 transition-colors"
      >
        <ArrowLeft className="h-4 w-4" />
        返回
      </button>

      {/* Cover image */}
      <div className="relative h-64 sm:h-80 rounded-2xl overflow-hidden bg-gradient-to-br from-orange-100 to-amber-50">
        {recipe.coverImagePath ? (
          <img
            src={recipe.coverImagePath}
            alt={recipe.title}
            className="w-full h-full object-cover cursor-pointer"
            onClick={() => openLightbox(coverSlideIndex)}
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center">
            <ChefHat className="h-20 w-20 text-orange-300" />
          </div>
        )}
      </div>

      {/* Title and meta */}
      <div>
        <div className="flex items-start justify-between gap-4">
          <h1 className="text-2xl font-bold text-gray-900">{recipe.title}</h1>
          <div className="flex items-center gap-2 shrink-0">
            <Button
              variant="outline"
              size="sm"
              onClick={() => shareMutation.mutate()}
              disabled={shareMutation.isPending}
            >
              <Share2 className="h-4 w-4" />
              分享
            </Button>
            <Button variant="outline" size="sm" asChild>
              <Link to={`/recipes/${recipe.id}/edit`}>
                <Edit className="h-4 w-4" />
              </Link>
            </Button>
            <AlertDialog>
              <AlertDialogTrigger asChild>
                <Button variant="outline" size="sm" className="text-red-600 hover:text-red-700">
                  <Trash2 className="h-4 w-4" />
                </Button>
              </AlertDialogTrigger>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>删除菜谱</AlertDialogTitle>
                  <AlertDialogDescription>
                    确定要删除「{recipe.title}」吗？此操作无法撤销。
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>取消</AlertDialogCancel>
                  <AlertDialogAction
                    onClick={() => deleteMutation.mutate()}
                    className="bg-red-500 hover:bg-red-600"
                  >
                    删除
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          </div>
        </div>
        <div className="flex items-center gap-3 mt-2 text-sm text-gray-500">
          <span>作者：{recipe.authorName}</span>
          <span>·</span>
          <span>{format(new Date(recipe.updatedAt), 'yyyy年MM月dd日', { locale: zhCN })}</span>
        </div>
        {recipe.description && (
          <p className="mt-3 text-gray-600 leading-relaxed">{recipe.description}</p>
        )}
      </div>

      {/* Ingredients */}
      {recipe.ingredients.length > 0 && (
        <div>
          <h2 className="text-lg font-semibold text-gray-900 mb-3">食材</h2>
          <div className="bg-orange-50 rounded-xl overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-orange-100">
                  <th className="text-left px-4 py-2.5 font-medium text-gray-600">食材</th>
                  <th className="text-right px-4 py-2.5 font-medium text-gray-600">用量</th>
                  <th className="text-right px-4 py-2.5 font-medium text-gray-600">单位</th>
                </tr>
              </thead>
              <tbody>
                {recipe.ingredients.map((ing, i) => (
                  <tr key={i} className="border-b border-orange-100 last:border-0">
                    <td className="px-4 py-2.5 text-gray-800">{ing.name}</td>
                    <td className="px-4 py-2.5 text-right text-gray-700">{ing.amount}</td>
                    <td className="px-4 py-2.5 text-right text-gray-500">{ing.unit}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Steps */}
      {recipe.steps.length > 0 && (
        <div>
          <h2 className="text-lg font-semibold text-gray-900 mb-3">步骤</h2>
          <div className="space-y-4">
            {recipe.steps.map((step) => (
              <div key={step.order} className="flex gap-4">
                <div className="shrink-0 w-8 h-8 bg-blue-600 text-white rounded-full flex items-center justify-center text-sm font-bold">
                  {step.order}
                </div>
                <div className="flex-1 pt-1">
                  <p className="text-gray-700 leading-relaxed">{step.description}</p>
                  {step.imagePath && (
                    <img
                      src={step.imagePath}
                      alt={`步骤 ${step.order}`}
                      className="mt-3 rounded-lg w-full max-w-sm object-cover cursor-pointer"
                      onClick={() => openLightbox(stepSlideIndex(step.order))}
                    />
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Cooking Records Section */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-gray-900">
            做菜记录 {cookingRecords.length > 0 && `(${cookingRecords.length})`}
          </h2>
          {cookingRecords.length > 5 && (
            <Link
              to={`/cooking-records?recipeId=${recipe.id}`}
              className="text-sm text-blue-600 hover:text-blue-700"
            >
              查看全部
            </Link>
          )}
        </div>

        {/* Add cooking record button */}
        <Button asChild className="w-full" variant="outline">
          <Link to={`/cooking-records/new?recipeId=${recipe.id}&recipeTitle=${encodeURIComponent(recipe.title)}`}>
            <ClipboardList className="h-4 w-4" />
            记录做菜
          </Link>
        </Button>

        {/* Cooking records list */}
        {cookingRecords.length > 0 ? (
          <div className="space-y-3">
            {cookingRecords.slice(0, 5).map((record) => (
              <CookingRecordCard key={record.id} record={record} hideRecipeTitle />
            ))}
          </div>
        ) : (
          <div className="text-center py-8 text-gray-400 text-sm">
            还没有做菜记录，快来记录第一次吧
          </div>
        )}
      </div>

      {/* Tags */}
      {recipe.shareToken && (
        <div className="flex items-center gap-2">
          <Badge variant="secondary">可分享</Badge>
        </div>
      )}

      {/* Lightbox */}
      <ImageLightbox
        open={lightboxOpen}
        close={() => setLightboxOpen(false)}
        index={lightboxIndex}
        slides={slides}
      />

      {/* Share dialog */}
      <Dialog open={shareDialogOpen} onOpenChange={setShareDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>分享菜谱</DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            <p className="text-sm text-gray-500">任何人都可以通过以下链接查看此菜谱：</p>
            <div className="flex items-center gap-2">
              <div className="flex-1 bg-gray-50 border border-gray-200 rounded-md px-3 py-2 text-sm text-gray-700 break-all">
                {shareUrl}
              </div>
              <Button variant="outline" size="icon" onClick={handleCopyLink}>
                {copied ? <Check className="h-4 w-4 text-green-600" /> : <Copy className="h-4 w-4" />}
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}
