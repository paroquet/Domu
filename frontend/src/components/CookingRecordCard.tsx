import { useState } from 'react'
import { format } from 'date-fns'
import { zhCN } from 'date-fns/locale'
import { User, Clock, ChefHat } from 'lucide-react'
import ImageLightbox from '@/components/ImageLightbox'
import { Card, CardContent } from '@/components/ui/card'
import type { CookingRecord } from '@/types'

interface CookingRecordCardProps {
  record: CookingRecord
  /** 是否隐藏菜谱标题（在菜谱详情页使用时隐藏） */
  hideRecipeTitle?: boolean
}

export default function CookingRecordCard({ record, hideRecipeTitle = false }: CookingRecordCardProps) {
  const [lightboxOpen, setLightboxOpen] = useState(false)
  const [lightboxIndex, setLightboxIndex] = useState(0)
  const slides = record.images.map(src => ({ src }))

  return (
    <Card className="hover:shadow-md transition-shadow">
      <CardContent className="p-4">
        <div className="flex gap-4">
          {record.images.length > 0 ? (
            <img
              src={record.images[0]}
              alt={record.recipeTitle}
              className="w-20 h-20 rounded-lg object-cover shrink-0 cursor-pointer"
              onClick={() => { setLightboxIndex(0); setLightboxOpen(true) }}
            />
          ) : (
            <div className="w-20 h-20 rounded-lg bg-gradient-to-br from-orange-100 to-amber-50 flex items-center justify-center shrink-0">
              <ChefHat className="h-8 w-8 text-orange-300" />
            </div>
          )}
          <div className="flex-1 min-w-0">
            {!hideRecipeTitle && (
              <h3 className="font-semibold text-gray-900 truncate">{record.recipeTitle}</h3>
            )}
            {record.notes && (
              <p className={`text-sm text-gray-500 line-clamp-2 ${hideRecipeTitle ? '' : 'mt-1'}`}>
                {record.notes}
              </p>
            )}
            <div className={`flex items-center gap-3 text-xs text-gray-400 ${record.notes || !hideRecipeTitle ? 'mt-2' : 'mt-1'}`}>
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
      <ImageLightbox
        open={lightboxOpen}
        close={() => setLightboxOpen(false)}
        index={lightboxIndex}
        slides={slides}
      />
    </Card>
  )
}
