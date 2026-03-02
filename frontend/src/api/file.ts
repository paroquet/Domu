import client from './client'

export async function uploadFile(file: File): Promise<{ path: string; url: string }> {
  const formData = new FormData()
  formData.append('file', file)
  const res = await client.post<{ path: string; url: string }>('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return res.data
}
