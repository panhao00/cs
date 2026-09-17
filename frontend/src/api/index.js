import axios from 'axios'
import router from '../router'

// 创建 axios 实例,统一配置 baseURL 和请求拦截器
const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器: 自动携带 JWT
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('crs_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器: 401 → 跳转登录
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('crs_token')
      localStorage.removeItem('crs_user')
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export { api }
