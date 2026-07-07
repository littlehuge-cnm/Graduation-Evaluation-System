import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
    baseURL: '/api',
    timeout: 10000
})

request.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

request.interceptors.response.use(
    (response) => {
        if (response.config.responseType === 'blob') {
            return response
        }
        const { code, message, data } = response.data
        if (code !== 200) {
            ElMessage.error(message || '请求失败')
            return Promise.reject(new Error(message || '请求失败'))
        }
        return data
    },
    (error) => {
        const status = error.response?.status
        if (status === 401) {
            localStorage.removeItem('token')
            localStorage.removeItem('userType')
            window.location.href = '/login'
        } else {
            ElMessage.error(error.response?.data?.message || error.message || '网络错误')
        }
        return Promise.reject(error)
    }
)

export default request
