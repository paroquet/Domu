import { useState } from 'react'
import { Link } from 'react-router-dom'
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

  const openLightbox = (e: React.MouseEvent, index: number) => {
    e.preventDefault()
    e.stopPropagation()
    setLightboxIndex(index)
    setLightboxOpen(true)
  }

  return (
    <>
      <Link to={`/cooking-records/${record.id}`} className="block">
        <Card className="hover:shadow-md transition-shadow">
          <CardContent className="p-4">
            <div className="flex gap-4">
              {record.images.length > 0 ? (
                <img
                  src={record.images[0]}
                  alt={record.recipeTitle}
                  className="w-20 h-20 rounded-lg object-cover shrink-0 cursor-pointer"
                  onClick={(e) => openLightbox(e, 0)}
                />
              ) : (
                <div className="w-20 h-20 rounded-lg bg-gradient-to-br from-primary/10 to-primary/5 flex items-center justify-center shrink-0">
                  <ChefHat className="h-8 w-8 text-primary/40" />
                </div>
              )}
              <div className="flex-1 min-w-0">
                {!hideRecipeTitle && (
                  <h3 className="font-semibold text-foreground truncate">{record.recipeTitle}</h3>
                )}
                {record.notes && (
                  <p className={`text-sm text-muted-foreground line-clamp-2 ${hideRecipeTitle ? '' : 'mt-1'}`}>
                    {record.notes}
                  </p>
                )}
                <div className={`flex items-center gap-3 text-xs text-muted-foreground ${record.notes || !hideRecipeTitle ? 'mt-2' : 'mt-1'}`}>
                  <span className="flex items-center gap-1">
                    <User className="h-3 w-3" />
                    {record.userName}
                  </span>
                  <span className="flex items-center gap-1">
                    <Clock className="h-3 w-3" />
                    {format(new Date(record.cookedAt), 'MM月dd日 HH:mm', { locale: zhCN })}
                  </span>
                  {record.images.length > 1 && (
                    <span className="text-muted-foreground/60">{record.images.length} 张图片</span>
                  )}
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </Link>
      <ImageLightbox
        open={lightboxOpen}
        close={() => setLightboxOpen(false)}
        index={lightboxIndex}
        slides={slides}
      />
    </>
  )
}
