import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { BookOpen, ClipboardList, ShoppingCart, Users, User, LogOut } from 'lucide-react'
import { useAuthStore } from '@/stores/authStore'
import { useFamilyStore } from '@/stores/familyStore'
import { logout } from '@/api/auth'
import { useQuery } from '@tanstack/react-query'
import { getFamily } from '@/api/family'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { cn } from '@/lib/utils'

const navItems = [
  { to: '/recipes', icon: BookOpen, label: '菜谱' },
  { to: '/cooking-records', icon: ClipboardList, label: '做菜记录' },
  { to: '/orders', icon: ShoppingCart, label: '点菜' },
  { to: '/family', icon: Users, label: '家庭' },
  { to: '/profile', icon: User, label: '我的' },
]

export default function Layout() {
  const { user, setUser } = useAuthStore()
  const { currentFamilyId } = useFamilyStore()
  const navigate = useNavigate()

  const { data: family } = useQuery({
    queryKey: ['family', currentFamilyId],
    queryFn: () => getFamily(currentFamilyId!),
    enabled: !!currentFamilyId,
  })

  const handleLogout = async () => {
    try {
      await logout()
    } finally {
      setUser(null)
      navigate('/login')
    }
  }

  const userInitials = user?.name
    ? user.name.slice(0, 2)
    : user?.email?.slice(0, 2).toUpperCase() ?? '?'

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      {/* Top Navigation */}
      <header className="bg-white border-b border-gray-200 sticky top-0 z-40">
        <div className="max-w-5xl mx-auto px-4 h-14 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <img src="/favicon.png" alt="家肴" className="h-6 w-6" />
            <span className="font-bold text-lg text-gray-900">家肴</span>
            {family && (
              <span className="text-sm text-gray-500 hidden sm:block">· {family.name}</span>
            )}
          </div>

          {/* Desktop nav */}
          <nav className="hidden md:flex items-center gap-1">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-1.5 px-3 py-2 rounded-md text-sm font-medium transition-colors',
                    isActive
                      ? 'bg-blue-50 text-blue-600'
                      : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                  )
                }
              >
                <item.icon className="h-4 w-4" />
                {item.label}
              </NavLink>
            ))}
          </nav>

          {/* User menu */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button className="flex items-center gap-2 rounded-full focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2">
                <Avatar className="h-8 w-8">
                  {user?.avatarPath && <AvatarImage src={user.avatarPath} alt={user.name} />}
                  <AvatarFallback className="text-xs">{userInitials}</AvatarFallback>
                </Avatar>
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-48">
              <DropdownMenuLabel>
                <div className="font-medium">{user?.name}</div>
                <div className="text-xs text-gray-500 font-normal truncate">{user?.email}</div>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={() => navigate('/profile')}>
                <User className="h-4 w-4" />
                个人信息
              </DropdownMenuItem>
              <DropdownMenuItem onClick={handleLogout} className="text-red-600 focus:text-red-600 focus:bg-red-50">
                <LogOut className="h-4 w-4" />
                退出登录
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </header>

      {/* Page content */}
      <main className="flex-1 max-w-5xl w-full mx-auto px-4 py-6 pb-20 md:pb-6">
        <Outlet />
      </main>

      {/* Bottom navigation (mobile) */}
      <nav className="md:hidden fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 z-40">
        <div className="flex">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                cn(
                  'flex-1 flex flex-col items-center gap-0.5 py-2 text-xs font-medium transition-colors',
                  isActive ? 'text-blue-600' : 'text-gray-500 hover:text-gray-900'
                )
              }
            >
              <item.icon className="h-5 w-5" />
              {item.label}
            </NavLink>
          ))}
        </div>
      </nav>
    </div>
  )
}
