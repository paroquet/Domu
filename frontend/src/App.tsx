import { Routes, Route, Navigate } from 'react-router-dom'
import { Toaster } from '@/components/ui/toaster'
import ProtectedRoute from '@/components/ProtectedRoute'
import Layout from '@/components/Layout'
import UpdatePrompt from '@/components/UpdatePrompt'
import { useRegisterSW } from '@/hooks/useRegisterSW'
import LoginPage from '@/pages/auth/LoginPage'
import RegisterPage from '@/pages/auth/RegisterPage'
import SharePage from '@/pages/recipes/SharePage'
import RecipeListPage from '@/pages/recipes/RecipeListPage'
import RecipeDetailPage from '@/pages/recipes/RecipeDetailPage'
import RecipeFormPage from '@/pages/recipes/RecipeFormPage'
import CookingRecordListPage from '@/pages/cooking-records/CookingRecordListPage'
import CookingRecordFormPage from '@/pages/cooking-records/CookingRecordFormPage'
import FamilyPage from '@/pages/family/FamilyPage'
import OrderPage from '@/pages/orders/OrderPage'
import RecipeSelectPage from '@/pages/orders/RecipeSelectPage'
import ProfilePage from '@/pages/profile/ProfilePage'

export default function App() {
  useRegisterSW()

  return (
    <>
      <Routes>
        {/* Public routes */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/share/:token" element={<SharePage />} />

        {/* Protected routes */}
        <Route element={<ProtectedRoute />}>
          <Route element={<Layout />}>
            <Route path="/" element={<Navigate to="/recipes" replace />} />
            <Route path="/recipes" element={<RecipeListPage />} />
            <Route path="/recipes/new" element={<RecipeFormPage />} />
            <Route path="/recipes/:id" element={<RecipeDetailPage />} />
            <Route path="/recipes/:id/edit" element={<RecipeFormPage />} />
            <Route path="/cooking-records" element={<CookingRecordListPage />} />
            <Route path="/cooking-records/new" element={<CookingRecordFormPage />} />
            <Route path="/family" element={<FamilyPage />} />
            <Route path="/orders" element={<OrderPage />} />
            <Route path="/orders/select" element={<RecipeSelectPage />} />
            <Route path="/profile" element={<ProfilePage />} />
          </Route>
        </Route>

        {/* 404 fallback */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      <Toaster />
      <UpdatePrompt />
    </>
  )
}
