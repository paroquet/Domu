import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Trash2, GripVertical, ArrowLeft } from 'lucide-react'
import { getRecipe, createRecipe, updateRecipe } from '@/api/recipe'
import { useFamilyStore } from '@/stores/familyStore'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import ImageUpload from '@/components/ImageUpload'
import { toast } from '@/components/ui/use-toast'
import type { Ingredient, Step } from '@/types'

function emptyIngredient(): Ingredient {
  return { name: '', amount: '', unit: '' }
}

function emptyStep(order: number): Step {
  return { order, description: '' }
}

export default function RecipeFormPage() {
  const { id } = useParams<{ id: string }>()
  const isEdit = !!id
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { currentFamilyId } = useFamilyStore()

  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [coverImagePath, setCoverImagePath] = useState('')
  const [ingredients, setIngredients] = useState<Ingredient[]>([emptyIngredient()])
  const [steps, setSteps] = useState<Step[]>([emptyStep(1)])

  const { data: recipe } = useQuery({
    queryKey: ['recipe', id],
    queryFn: () => getRecipe(Number(id)),
    enabled: isEdit,
  })

  useEffect(() => {
    if (recipe) {
      setTitle(recipe.title)
      setDescription(recipe.description ?? '')
      setCoverImagePath(recipe.coverImagePath ?? '')
      setIngredients(recipe.ingredients.length ? recipe.ingredients : [emptyIngredient()])
      setSteps(recipe.steps.length ? recipe.steps : [emptyStep(1)])
    }
  }, [recipe])

  const createMutation = useMutation({
    mutationFn: createRecipe,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['recipes'] })
      navigate(`/recipes/${data.id}`)
      toast({ title: '菜谱创建成功' })
    },
    onError: () => toast({ title: '创建失败', variant: 'destructive' }),
  })

  const updateMutation = useMutation({
    mutationFn: (data: Parameters<typeof updateRecipe>[1]) => updateRecipe(Number(id), data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recipe', id] })
      queryClient.invalidateQueries({ queryKey: ['recipes'] })
      navigate(`/recipes/${id}`)
      toast({ title: '菜谱已更新' })
    },
    onError: () => toast({ title: '更新失败', variant: 'destructive' }),
  })

  const isPending = createMutation.isPending || updateMutation.isPending

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!title.trim()) {
      toast({ title: '请输入菜谱标题', variant: 'destructive' })
      return
    }
    if (!currentFamilyId) {
      toast({ title: '请先加入家庭', variant: 'destructive' })
      return
    }

    const validIngredients = ingredients.filter((ing) => ing.name.trim())
    const validSteps = steps.filter((s) => s.description.trim())

    const data = {
      title: title.trim(),
      description: description.trim() || undefined,
      coverImagePath: coverImagePath || undefined,
      ingredients: validIngredients,
      steps: validSteps.map((s, i) => ({ ...s, order: i + 1 })),
      familyId: currentFamilyId,
    }

    if (isEdit) {
      updateMutation.mutate(data)
    } else {
      createMutation.mutate(data)
    }
  }

  // Ingredient helpers
  const updateIngredient = (i: number, field: keyof Ingredient, value: string) => {
    setIngredients((prev) => prev.map((ing, idx) => idx === i ? { ...ing, [field]: value } : ing))
  }
  const addIngredient = () => setIngredients((prev) => [...prev, emptyIngredient()])
  const removeIngredient = (i: number) => {
    if (ingredients.length === 1) return
    setIngredients((prev) => prev.filter((_, idx) => idx !== i))
  }

  // Step helpers
  const updateStep = (i: number, field: keyof Step, value: string) => {
    setSteps((prev) => prev.map((s, idx) => idx === i ? { ...s, [field]: value } : s))
  }
  const addStep = () => setSteps((prev) => [...prev, emptyStep(prev.length + 1)])
  const removeStep = (i: number) => {
    if (steps.length === 1) return
    setSteps((prev) => prev.filter((_, idx) => idx !== i).map((s, idx) => ({ ...s, order: idx + 1 })))
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
        <h1 className="text-2xl font-bold text-gray-900">{isEdit ? '编辑菜谱' : '新建菜谱'}</h1>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Basic info */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">基本信息</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="title">菜谱名称 *</Label>
              <Input
                id="title"
                placeholder="例如：红烧肉"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                required
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="description">描述（可选）</Label>
              <Textarea
                id="description"
                placeholder="简单介绍这道菜..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={3}
              />
            </div>
            <div className="space-y-1.5">
              <Label>封面图片（可选）</Label>
              <ImageUpload
                value={coverImagePath}
                onChange={setCoverImagePath}
              />
            </div>
          </CardContent>
        </Card>

        {/* Ingredients */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">食材清单</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {ingredients.map((ing, i) => (
              <div key={i} className="flex items-center gap-2">
                <GripVertical className="h-4 w-4 text-gray-300 shrink-0" />
                <Input
                  placeholder="食材名称"
                  value={ing.name}
                  onChange={(e) => updateIngredient(i, 'name', e.target.value)}
                  className="flex-1"
                />
                <Input
                  placeholder="用量"
                  value={ing.amount}
                  onChange={(e) => updateIngredient(i, 'amount', e.target.value)}
                  className="w-20"
                />
                <Input
                  placeholder="单位"
                  value={ing.unit}
                  onChange={(e) => updateIngredient(i, 'unit', e.target.value)}
                  className="w-16"
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  onClick={() => removeIngredient(i)}
                  disabled={ingredients.length === 1}
                  className="shrink-0 text-gray-400 hover:text-red-500"
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            ))}
            <Button type="button" variant="outline" size="sm" onClick={addIngredient} className="w-full">
              <Plus className="h-4 w-4" />
              添加食材
            </Button>
          </CardContent>
        </Card>

        {/* Steps */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">制作步骤</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {steps.map((step, i) => (
              <div key={i} className="space-y-2">
                <div className="flex items-center gap-2">
                  <div className="w-7 h-7 bg-blue-600 text-white rounded-full flex items-center justify-center text-sm font-bold shrink-0">
                    {i + 1}
                  </div>
                  <span className="text-sm font-medium text-gray-700">步骤 {i + 1}</span>
                  <div className="flex-1" />
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    onClick={() => removeStep(i)}
                    disabled={steps.length === 1}
                    className="h-7 w-7 text-gray-400 hover:text-red-500"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </Button>
                </div>
                <Textarea
                  placeholder={`描述第 ${i + 1} 步骤...`}
                  value={step.description}
                  onChange={(e) => updateStep(i, 'description', e.target.value)}
                  rows={2}
                />
                <div className="space-y-1">
                  <Label className="text-xs text-gray-500">步骤图片（可选）</Label>
                  <ImageUpload
                    value={step.imagePath}
                    onChange={(path) => updateStep(i, 'imagePath', path)}
                    className="h-auto"
                    placeholder="为此步骤添加图片"
                  />
                </div>
              </div>
            ))}
            <Button type="button" variant="outline" size="sm" onClick={addStep} className="w-full">
              <Plus className="h-4 w-4" />
              添加步骤
            </Button>
          </CardContent>
        </Card>

        <div className="flex gap-3">
          <Button type="button" variant="outline" className="flex-1" onClick={() => navigate(-1)}>
            取消
          </Button>
          <Button type="submit" className="flex-1" disabled={isPending}>
            {isPending ? '保存中...' : isEdit ? '保存更改' : '创建菜谱'}
          </Button>
        </div>
      </form>
    </div>
  )
}
