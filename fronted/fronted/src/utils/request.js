import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 创建 axios 实例
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    // 如果是 blob 类型的响应（下载文件），直接返回
    if (response.config.responseType === 'blob') {
      // 检查是否是错误响应（后端返回的 JSON 错误）
      if (response.data.type && response.data.type.includes('application/json')) {
        return response.data.text().then(text => {
          const errorData = JSON.parse(text)
          if (errorData.code !== 200) {
            ElMessage.error(errorData.message || '请求失败')
            if (errorData.code === 401) {
              localStorage.removeItem('token')
              localStorage.removeItem('user')
              router.push('/login')
            }
            return Promise.reject(new Error(errorData.message || '请求失败'))
          }
          return response
        })
      }
      return response
    }
    
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    // 显示具体的错误信息
    let message = '网络错误'
    if (error.response) {
      // 后端返回了错误响应
      const data = error.response.data
      // 处理 blob 类型的错误响应（如下载接口返回的错误）
      if (data instanceof Blob) {
        return data.text().then(text => {
          try {
            const errorData = JSON.parse(text)
            message = errorData.message || `请求失败：${error.response.status}`
          } catch (e) {
            message = `请求失败：${error.response.status}`
          }
          ElMessage.error(message)
          return Promise.reject(error)
        })
      } else if (data && data.message) {
        message = data.message
      } else {
        message = `请求失败：${error.response.status}`
      }
    } else if (error.message) {
      message = error.message
    }
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
