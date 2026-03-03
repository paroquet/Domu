import { useState, useMemo } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Search, ChefHat, Check } from 'lucide-react'
import { getRecipes } from '@/api/recipe'
import { createOrder } from '@/api/order'
import { useFamilyStore } from '@/stores/familyStore'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { toast } from '@/components/ui/use-toast'

export default function RecipeSelectPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const queryClient = useQueryClient()
  const { currentFamilyId } = useFamilyStore()

  const plannedDate = searchParams.get('date') || new Date().toISOString().split('T')[0]

  const [searchQuery, setSearchQuery] = useState('')
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set())

  const { data: recipes = [], isLoading } = useQuery({
    queryKey: ['recipes', currentFamilyId],
    queryFn: () => getRecipes(currentFamilyId!),
    enabled: !!currentFamilyId,
  })

  const filteredRecipes = useMemo(() => {
    if (!searchQuery.trim()) return recipes
    const query = searchQuery.toLowerCase()
    return recipes.filter((r) => r.title.toLowerCase().includes(query))
  }, [recipes, searchQuery])

  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleToggle = (recipeId: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (next.has(recipeId)) {
        next.delete(recipeId)
      } else {
        next.add(recipeId)
      }
      return next
    })
  }

  const handleConfirm = async () => {
    if (!currentFamilyId || selectedIds.size === 0) return
    setIsSubmitting(true)
    try {
      // 串行执行，避免 SQLite 并发写入锁定
      for (const recipeId of selectedIds) {
        await createOrder({
          familyId: currentFamilyId,
          recipeId,
          plannedDate,
        })
      }
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      toast({ title: `成功点了 ${selectedIds.size} 道菜` })
      navigate(-1)
    } catch {
      toast({ title: '点菜失败', variant: 'destructive' })
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3">
        <button
          onClick={() => navigate(-1)}
          className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-900 transition-colors"
        >
          <ArrowLeft className="h-4 w-4" />
          返回
        </button>
        <h1 className="text-xl font-bold text-gray-900">选择菜谱</h1>
      </div>

      {/* Search input */}
      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
        <Input
          placeholder="搜索菜谱..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="pl-9"
        />
      </div>

      {/* Recipe list */}
      {isLoading ? (
        <div className="space-y-3">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="h-16 bg-gray-100 rounded-xl animate-pulse" />
          ))}
        </div>
      ) : filteredRecipes.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <ChefHat className="h-12 w-12 text-gray-300 mb-3" />
          <p className="text-gray-500">
            {searchQuery ? '没有找到匹配的菜谱' : '还没有菜谱'}
          </p>
        </div>
      ) : (
        <div className={`space-y-2 ${selectedIds.size > 0 ? 'pb-24' : ''}`}>
          {filteredRecipes.map((recipe) => {
            const isSelected = selectedIds.has(recipe.id)
            return (
              <Card
                key={recipe.id}
                className={`cursor-pointer transition-all ${
                  isSelected
                    ? 'border-blue-500 bg-blue-50/50 shadow-md'
                    : 'hover:shadow-md hover:border-gray-300'
                }`}
                onClick={() => handleToggle(recipe.id)}
              >
                <CardContent className="p-3">
                  <div className="flex items-center gap-3">
                    {recipe.coverImagePath ? (
                      <img
                        src={recipe.coverImagePath}
                        alt={recipe.title}
                        className="w-12 h-12 rounded-lg object-cover shrink-0"
                      />
                    ) : (
                      <div className="w-12 h-12 rounded-lg bg-gradient-to-br from-orange-100 to-amber-50 flex items-center justify-center shrink-0">
                        <ChefHat className="h-6 w-6 text-orange-300" />
                      </div>
                    )}
                    <div className="flex-1 min-w-0">
                      <h3 className="font-medium text-gray-900 truncate">{recipe.title}</h3>
                      {recipe.description && (
                        <p className="text-xs text-gray-500 truncate mt-0.5">{recipe.description}</p>
                      )}
                    </div>
                    <div
                      className={`shrink-0 w-6 h-6 rounded-full border-2 flex items-center justify-center transition-colors ${
                        isSelected
                          ? 'bg-blue-600 border-blue-600'
                          : 'border-gray-300 bg-white'
                      }`}
                    >
                      {isSelected && <Check className="h-4 w-4 text-white" />}
                    </div>
                  </div>
                </CardContent>
              </Card>
            )
          })}
        </div>
      )}

      {/* Bottom confirm bar */}
      {selectedIds.size > 0 && (
        <div className="fixed bottom-14 md:bottom-0 left-0 right-0 bg-white border-t border-gray-200 p-4 shadow-lg z-50">
          <div className="max-w-5xl mx-auto flex items-center gap-4">
            <div className="flex-1 text-sm text-gray-600">
              已选择 <span className="font-semibold text-blue-600">{selectedIds.size}</span> 道菜
            </div>
            <Button onClick={handleConfirm} disabled={isSubmitting}>
              {isSubmitting ? '提交中...' : '确认点菜'}
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
