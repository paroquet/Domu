import axios from 'axios'

const client = axios.create({
  baseURL: '/api/v1',
  withCredentials: true,
})

// 用于清除认证并跳转的全局函数
const handleAuthError = () => {
  // 清除 localStorage 中的数据
  localStorage.removeItem('domu-auth')
  // 跳转到登录页
  window.location.href = '/login'
}

client.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response) {
      // 处理 401 (未认证) 和 403 (权限不足)，都视为需要重新登录
      if (error.response.status === 401 || error.response.status === 403) {
        handleAuthError()
      }
    }
    return Promise.reject(error)
  }
)

export default client
