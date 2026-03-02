import { Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Plus, ClipboardList, User, Clock, ChefHat } from 'lucide-react'
import { format } from 'date-fns'
import { zhCN } from 'date-fns/locale'
import { getCookingRecords } from '@/api/cookingRecord'
import { useFamilyStore } from '@/stores/familyStore'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'

export default function CookingRecordListPage() {
  const { currentFamilyId } = useFamilyStore()
  const navigate = useNavigate()

  const { data: records = [], isLoading } = useQuery({
    queryKey: ['cooking-records', currentFamilyId],
    queryFn: () => getCookingRecords(currentFamilyId!),
    enabled: !!currentFamilyId,
  })

  if (!currentFamilyId) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-center">
        <ClipboardList className="h-16 w-16 text-gray-300 mb-4" />
        <h2 className="text-xl font-semibold text-gray-700 mb-2">还没有加入家庭</h2>
        <p className="text-gray-500 mb-6">请先创建或加入一个家庭</p>
        <Button onClick={() => navigate('/family')}>去管理家庭</Button>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">做菜记录</h1>
        <Button asChild size="sm">
          <Link to="/cooking-records/new">
            <Plus className="h-4 w-4" />
            新建记录
          </Link>
        </Button>
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="h-24 bg-gray-100 rounded-xl animate-pulse" />
          ))}
        </div>
      ) : records.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <ClipboardList className="h-12 w-12 text-gray-300 mb-3" />
          <p className="text-gray-500">还没有做菜记录</p>
          <Button asChild className="mt-4">
            <Link to="/cooking-records/new">添加第一条记录</Link>
          </Button>
        </div>
      ) : (
        <div className="space-y-3">
          {records.map((record) => (
            <Card key={record.id} className="hover:shadow-md transition-shadow">
              <CardContent className="p-4">
                <div className="flex gap-4">
                  {record.images.length > 0 ? (
                    <img
                      src={record.images[0]}
                      alt={record.recipeTitle}
                      className="w-20 h-20 rounded-lg object-cover shrink-0"
                    />
                  ) : (
                    <div className="w-20 h-20 rounded-lg bg-gradient-to-br from-orange-100 to-amber-50 flex items-center justify-center shrink-0">
                      <ChefHat className="h-8 w-8 text-orange-300" />
                    </div>
                  )}
                  <div className="flex-1 min-w-0">
                    <h3 className="font-semibold text-gray-900 truncate">{record.recipeTitle}</h3>
                    {record.notes && (
                      <p className="text-sm text-gray-500 mt-1 line-clamp-2">{record.notes}</p>
                    )}
                    <div className="flex items-center gap-3 mt-2 text-xs text-gray-400">
                      <span className="flex items-center gap-1">
                        <User className="h-3 w-3" />
                        {record.userName}
                      </span>
                      <span className="flex items-center gap-1">
                        <Clock className="h-3 w-3" />
                        {format(new Date(record.cookedAt), 'MM月dd日 HH:mm', { locale: zhCN })}
                      </span>
                      {record.images.length > 1 && (
                        <span className="text-gray-300">{record.images.length} 张图片</span>
                      )}
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}
