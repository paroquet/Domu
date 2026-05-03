import axios from 'axios'

const client = axios.create({
  baseURL: '/api/v1',
  withCredentials: true,
})

// 是否正在刷新 token
let isRefreshing = false
// 等待刷新的请求队列
let failedQueue: Array<{
  resolve: (value?: unknown) => void
  reject: (error: unknown) => void
}> = []

// 处理队列中的请求
const processQueue = (error: unknown) => {
  failedQueue.forEach((promise) => {
    if (error) {
      promise.reject(error)
    } else {
      promise.resolve()
    }
  })
  failedQueue = []
}

// 用于清除认证并跳转的全局函数
const handleAuthError = () => {
  localStorage.removeItem('domu-auth')
  window.location.href = '/login'
}

client.interceptors.response.use(
  (res) => res,
  async (error) => {
    const originalRequest = error.config

    // 如果是 401 且不是刷新 token 的请求，尝试刷新 token
    if (error.response?.status === 401 && !originalRequest._retry) {
      // 排除登录、注册、刷新 token 的请求
      if (originalRequest.url?.includes('/auth/login') ||
          originalRequest.url?.includes('/auth/register') ||
          originalRequest.url?.includes('/auth/refresh')) {
        handleAuthError()
        return Promise.reject(error)
      }

      if (isRefreshing) {
        // 正在刷新，将请求加入队列等待
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then(() => {
          return client(originalRequest)
        }).catch((err) => {
          return Promise.reject(err)
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        // 尝试刷新 token
        await client.post('/auth/refresh')
        processQueue(null)
        // 重新发起原请求
        return client(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError)
        handleAuthError()
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    // 403 直接视为权限不足，不需要刷新 token
    if (error.response?.status === 403) {
      handleAuthError()
    }

    return Promise.reject(error)
  }
)

export default client
