import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ChefHat } from 'lucide-react'
import { register } from '@/api/auth'
import { useAuthStore } from '@/stores/authStore'
import { useFamilyStore } from '@/stores/familyStore'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'

export default function RegisterPage() {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { setUser } = useAuthStore()
  const { initializeFamily } = useFamilyStore()
  const navigate = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    if (password.length < 6) {
      setError('密码至少需要 6 个字符')
      return
    }
    setLoading(true)
    try {
      const user = await register({ email, password, name })
      setUser(user)
      initializeFamily(user.families)
      navigate('/')
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : '注册失败，该邮箱可能已被注册'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-4">
      <div className="w-full max-w-sm">
        <div className="flex flex-col items-center mb-8">
          <div className="bg-blue-600 text-white rounded-2xl p-3 mb-3">
            <ChefHat className="h-8 w-8" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">家肴</h1>
          <p className="text-gray-500 text-sm mt-1">记录家庭的每一餐美味</p>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>创建账号</CardTitle>
            <CardDescription>填写信息完成注册</CardDescription>
          </CardHeader>
          <form onSubmit={handleSubmit}>
            <CardContent className="space-y-4">
              {error && (
                <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-md px-3 py-2">
                  {error}
                </div>
              )}
              <div className="space-y-1.5">
                <Label htmlFor="name">姓名</Label>
                <Input
                  id="name"
                  type="text"
                  placeholder="你的名字"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                  autoComplete="name"
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="email">邮箱</Label>
                <Input
                  id="email"
                  type="email"
                  placeholder="your@email.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  autoComplete="email"
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="password">密码</Label>
                <Input
                  id="password"
                  type="password"
                  placeholder="至少 6 个字符"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  autoComplete="new-password"
                />
              </div>
            </CardContent>
            <CardFooter className="flex flex-col gap-3">
              <Button type="submit" className="w-full" disabled={loading}>
                {loading ? '注册中...' : '创建账号'}
              </Button>
              <p className="text-sm text-gray-500 text-center">
                已有账号？{' '}
                <Link to="/login" className="text-blue-600 hover:underline font-medium">
                  立即登录
                </Link>
              </p>
            </CardFooter>
          </form>
        </Card>
      </div>
    </div>
  )
}
