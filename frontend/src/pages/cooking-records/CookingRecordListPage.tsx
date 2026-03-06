import { Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Plus, ClipboardList } from 'lucide-react'
import { getCookingRecords } from '@/api/cookingRecord'
import { useFamilyStore } from '@/stores/familyStore'
import { Button } from '@/components/ui/button'
import CookingRecordCard from '@/components/CookingRecordCard'

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
            <CookingRecordCard key={record.id} record={record} />
          ))}
        </div>
      )}
    </div>
  )
}
