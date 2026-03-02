import { useState, useRef } from 'react'
import { Upload, X, ImageIcon } from 'lucide-react'
import { uploadFile } from '@/api/file'
import { cn } from '@/lib/utils'
import { toast } from '@/components/ui/use-toast'

interface ImageUploadProps {
  value?: string
  onChange: (path: string) => void
  className?: string
  placeholder?: string
}

export default function ImageUpload({ value, onChange, className, placeholder = '点击或拖拽上传图片' }: ImageUploadProps) {
  const [uploading, setUploading] = useState(false)
  const [dragging, setDragging] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)

  const handleFile = async (file: File) => {
    if (!file.type.startsWith('image/')) {
      toast({ title: '请上传图片文件', variant: 'destructive' })
      return
    }
    setUploading(true)
    try {
      const res = await uploadFile(file)
      onChange(res.path)
    } catch {
      toast({ title: '上传失败，请重试', variant: 'destructive' })
    } finally {
      setUploading(false)
    }
  }

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) handleFile(file)
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    setDragging(false)
    const file = e.dataTransfer.files[0]
    if (file) handleFile(file)
  }

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault()
    setDragging(true)
  }

  const handleDragLeave = () => setDragging(false)

  const handleClear = (e: React.MouseEvent) => {
    e.stopPropagation()
    onChange('')
    if (inputRef.current) inputRef.current.value = ''
  }

  return (
    <div
      className={cn(
        'relative rounded-lg border-2 border-dashed transition-colors cursor-pointer',
        dragging ? 'border-blue-400 bg-blue-50' : 'border-gray-200 hover:border-gray-300',
        className
      )}
      onClick={() => inputRef.current?.click()}
      onDrop={handleDrop}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
    >
      <input
        ref={inputRef}
        type="file"
        accept="image/*"
        className="hidden"
        onChange={handleChange}
      />
      {value ? (
        <div className="relative">
          <img
            src={value}
            alt="uploaded"
            className="w-full h-48 object-cover rounded-lg"
          />
          <button
            type="button"
            onClick={handleClear}
            className="absolute top-2 right-2 bg-white rounded-full p-1 shadow-md hover:bg-gray-100 transition-colors"
          >
            <X className="h-4 w-4 text-gray-600" />
          </button>
        </div>
      ) : (
        <div className="flex flex-col items-center justify-center gap-2 p-8 text-gray-400">
          {uploading ? (
            <div className="flex flex-col items-center gap-2">
              <div className="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent" />
              <span className="text-sm">上传中...</span>
            </div>
          ) : (
            <>
              {dragging ? (
                <Upload className="h-8 w-8 text-blue-400" />
              ) : (
                <ImageIcon className="h-8 w-8" />
              )}
              <span className="text-sm">{placeholder}</span>
            </>
          )}
        </div>
      )}
    </div>
  )
}
