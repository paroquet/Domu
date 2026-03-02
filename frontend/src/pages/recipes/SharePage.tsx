import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ChefHat, ExternalLink } from 'lucide-react'
import { getSharedRecipe } from '@/api/recipe'
import { Button } from '@/components/ui/button'

export default function SharePage() {
  const { token } = useParams<{ token: string }>()

  const { data: recipe, isLoading, isError } = useQuery({
    queryKey: ['shared-recipe', token],
    queryFn: () => getSharedRecipe(token!),
    enabled: !!token,
  })

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent" />
      </div>
    )
  }

  if (isError || !recipe) {
    return (
      <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center p-4 text-center">
        <ChefHat className="h-16 w-16 text-gray-300 mb-4" />
        <h1 className="text-xl font-semibold text-gray-700 mb-2">菜谱不存在或链接已失效</h1>
        <Button asChild className="mt-4">
          <Link to="/">前往 Domu</Link>
        </Button>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white border-b border-gray-200 sticky top-0 z-40">
        <div className="max-w-2xl mx-auto px-4 h-14 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <ChefHat className="h-6 w-6 text-blue-600" />
            <span className="font-bold text-lg text-gray-900">Domu 家庭餐饮</span>
          </div>
          <Button asChild size="sm">
            <Link to="/login">
              <ExternalLink className="h-4 w-4" />
              登录 Domu
            </Link>
          </Button>
        </div>
      </header>

      <main className="max-w-2xl mx-auto px-4 py-8 space-y-6">
        {/* Cover image */}
        <div className="relative h-64 sm:h-80 rounded-2xl overflow-hidden bg-gradient-to-br from-orange-100 to-amber-50">
          {recipe.coverImagePath ? (
            <img src={recipe.coverImagePath} alt={recipe.title} className="w-full h-full object-cover" />
          ) : (
            <div className="w-full h-full flex items-center justify-center">
              <ChefHat className="h-20 w-20 text-orange-300" />
            </div>
          )}
        </div>

        {/* Title */}
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{recipe.title}</h1>
          <p className="text-sm text-gray-500 mt-1">由 {recipe.authorName} 分享</p>
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
                        className="mt-3 rounded-lg w-full max-w-sm object-cover"
                      />
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* CTA */}
        <div className="bg-blue-50 border border-blue-100 rounded-xl p-6 text-center space-y-3">
          <ChefHat className="h-10 w-10 text-blue-400 mx-auto" />
          <h3 className="font-semibold text-gray-900">在 Domu 中管理你的家庭餐饮</h3>
          <p className="text-sm text-gray-500">记录菜谱、做菜记录、家庭点菜，一个 app 搞定</p>
          <Button asChild>
            <Link to="/register">免费注册</Link>
          </Button>
        </div>
      </main>
    </div>
  )
}
