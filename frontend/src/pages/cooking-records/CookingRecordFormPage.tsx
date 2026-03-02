import { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Plus, X } from 'lucide-react'
import { format } from 'date-fns'
import { createCookingRecord } from '@/api/cookingRecord'
import { getRecipes } from '@/api/recipe'
import { uploadFile } from '@/api/file'
import { useFamilyStore } from '@/stores/familyStore'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { toast } from '@/components/ui/use-toast'

export default function CookingRecordFormPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const queryClient = useQueryClient()
  const { currentFamilyId } = useFamilyStore()

  const prefilledRecipeId = searchParams.get('recipeId')

  const [recipeId, setRecipeId] = useState(prefilledRecipeId ?? '')
  const [cookedAt, setCookedAt] = useState(format(new Date(), "yyyy-MM-dd'T'HH:mm"))
  const [notes, setNotes] = useState('')
  const [images, setImages] = useState<string[]>([])
  const [uploading, setUploading] = useState(false)

  const { data: recipes = [] } = useQuery({
    queryKey: ['recipes', currentFamilyId],
    queryFn: () => getRecipes(currentFamilyId!),
    enabled: !!currentFamilyId,
  })

  useEffect(() => {
    if (prefilledRecipeId) setRecipeId(prefilledRecipeId)
  }, [prefilledRecipeId])

  const createMutation = useMutation({
    mutationFn: createCookingRecord,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cooking-records'] })
      navigate('/cooking-records')
      toast({ title: '记录创建成功' })
    },
    onError: () => toast({ title: '创建失败', variant: 'destructive' }),
  })

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files ?? [])
    if (files.length === 0) return
    setUploading(true)
    try {
      const paths = await Promise.all(files.map((f) => uploadFile(f).then((r) => r.path)))
      setImages((prev) => [...prev, ...paths])
    } catch {
      toast({ title: '图片上传失败', variant: 'destructive' })
    } finally {
      setUploading(false)
    }
  }

  const removeImage = (i: number) => {
    setImages((prev) => prev.filter((_, idx) => idx !== i))
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!recipeId) {
      toast({ title: '请选择菜谱', variant: 'destructive' })
      return
    }
    if (!currentFamilyId) {
      toast({ title: '请先加入家庭', variant: 'destructive' })
      return
    }
    createMutation.mutate({
      recipeId: Number(recipeId),
      familyId: currentFamilyId,
      cookedAt: new Date(cookedAt).toISOString(),
      notes: notes.trim() || undefined,
      images,
    })
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div className="flex items-center gap-3">
        <button
          onClick={() => navigate(-1)}
          className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-900 transition-colors"
        >
          <ArrowLeft className="h-4 w-4" />
          返回
        </button>
        <h1 className="text-2xl font-bold text-gray-900">新建做菜记录</h1>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">记录信息</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-1.5">
              <Label>选择菜谱 *</Label>
              <Select value={recipeId} onValueChange={setRecipeId}>
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
              <Label htmlFor="cookedAt">做菜时间 *</Label>
              <input
                id="cookedAt"
                type="datetime-local"
                value={cookedAt}
                onChange={(e) => setCookedAt(e.target.value)}
                required
                className="flex h-9 w-full rounded-md border border-gray-200 bg-white px-3 py-1 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500"
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="notes">心得体会（可选）</Label>
              <Textarea
                id="notes"
                placeholder="这次做菜的感想、改进点..."
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                rows={4}
              />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">做菜照片</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="grid grid-cols-3 gap-2">
              {images.map((img, i) => (
                <div key={i} className="relative aspect-square">
                  <img src={img} alt="" className="w-full h-full object-cover rounded-lg" />
                  <button
                    type="button"
                    onClick={() => removeImage(i)}
                    className="absolute top-1 right-1 bg-black/60 text-white rounded-full p-0.5"
                  >
                    <X className="h-3.5 w-3.5" />
                  </button>
                </div>
              ))}
              <label className="aspect-square border-2 border-dashed border-gray-200 rounded-lg flex flex-col items-center justify-center gap-1 cursor-pointer hover:border-gray-300 transition-colors">
                {uploading ? (
                  <div className="h-5 w-5 animate-spin rounded-full border-2 border-blue-600 border-t-transparent" />
                ) : (
                  <>
                    <Plus className="h-5 w-5 text-gray-400" />
                    <span className="text-xs text-gray-400">添加图片</span>
                  </>
                )}
                <input
                  type="file"
                  accept="image/*"
                  multiple
                  className="hidden"
                  onChange={handleImageUpload}
                  disabled={uploading}
                />
              </label>
            </div>
          </CardContent>
        </Card>

        <div className="flex gap-3">
          <Button type="button" variant="outline" className="flex-1" onClick={() => navigate(-1)}>
            取消
          </Button>
          <Button type="submit" className="flex-1" disabled={createMutation.isPending}>
            {createMutation.isPending ? '保存中...' : '保存记录'}
          </Button>
        </div>
      </form>
    </div>
  )
}
