import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

request.interceptors.request.use(
  (config) => {
    const auth = useAuthStore()
    if (auth.token) {
      config.headers.Authorization = `Bearer ${auth.token}`
    }
    return config
  },
  (err) => Promise.reject(err)
)

request.interceptors.response.use(
  (res) => {
    const { code, message, data } = res.data ?? {}
    if (code !== 0 && code !== undefined) {
      ElMessage.error(message || '请求失败')
      return Promise.reject(new Error(message || '请求失败'))
    }
    return data ?? res.data
  },
  (err) => {
    if (err.response?.status === 401) {
      const auth = useAuthStore()
      if (auth.token) {
        auth.logout()
        window.location.href = '/admin/login'
      } else {
        ElMessage.error(err.response?.data?.detail || '用户名或密码错误')
      }
    } else {
      ElMessage.error(err.response?.data?.detail || err.message || '网络错误')
    }
    return Promise.reject(err)
  }
)

export default request
