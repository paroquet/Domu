import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Plus, Search, ChefHat, Clock, User } from 'lucide-react'
import { format } from 'date-fns'
import { zhCN } from 'date-fns/locale'
import { getRecipes } from '@/api/recipe'
import { useFamilyStore } from '@/stores/familyStore'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent } from '@/components/ui/card'

export default function RecipeListPage() {
  const { currentFamilyId } = useFamilyStore()
  const navigate = useNavigate()
  const [search, setSearch] = useState('')

  const { data: recipes = [], isLoading } = useQuery({
    queryKey: ['recipes', currentFamilyId],
    queryFn: () => getRecipes(currentFamilyId!),
    enabled: !!currentFamilyId,
  })

  const filtered = recipes.filter((r) =>
    r.title.toLowerCase().includes(search.toLowerCase()) ||
    (r.description ?? '').toLowerCase().includes(search.toLowerCase())
  )

  if (!currentFamilyId) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-center">
        <ChefHat className="h-16 w-16 text-gray-300 mb-4" />
        <h2 className="text-xl font-semibold text-gray-700 mb-2">还没有加入家庭</h2>
        <p className="text-gray-500 mb-6">请先创建或加入一个家庭，才能查看和创建菜谱</p>
        <Button onClick={() => navigate('/family')}>去管理家庭</Button>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">菜谱</h1>
        <Button asChild size="sm">
          <Link to="/recipes/new">
            <Plus className="h-4 w-4" />
            新建菜谱
          </Link>
        </Button>
      </div>

      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
        <Input
          placeholder="搜索菜谱..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="pl-9"
        />
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="rounded-xl bg-gray-100 h-56 animate-pulse" />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <ChefHat className="h-12 w-12 text-gray-300 mb-3" />
          <p className="text-gray-500">
            {search ? '没有找到匹配的菜谱' : '还没有菜谱，快来添加第一个吧！'}
          </p>
          {!search && (
            <Button asChild className="mt-4">
              <Link to="/recipes/new">添加菜谱</Link>
            </Button>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map((recipe) => (
            <Link key={recipe.id} to={`/recipes/${recipe.id}`}>
              <Card className="overflow-hidden hover:shadow-md transition-shadow group">
                <div className="relative h-40 bg-gradient-to-br from-orange-100 to-amber-50">
                  {recipe.coverImagePath ? (
                    <img
                      src={recipe.coverImagePath}
                      alt={recipe.title}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center">
                      <ChefHat className="h-12 w-12 text-orange-300" />
                    </div>
                  )}
                </div>
                <CardContent className="p-4">
                  <h3 className="font-semibold text-gray-900 group-hover:text-blue-600 transition-colors line-clamp-1">
                    {recipe.title}
                  </h3>
                  {recipe.description && (
                    <p className="text-sm text-gray-500 mt-1 line-clamp-2">{recipe.description}</p>
                  )}
                  <div className="flex items-center gap-3 mt-3 text-xs text-gray-400">
                    <span className="flex items-center gap-1">
                      <User className="h-3 w-3" />
                      {recipe.authorName}
                    </span>
                    <span className="flex items-center gap-1">
                      <Clock className="h-3 w-3" />
                      {format(new Date(recipe.createdAt), 'MM月dd日', { locale: zhCN })}
                    </span>
                  </div>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
