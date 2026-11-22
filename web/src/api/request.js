import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 统一 request 模块（5.5.3~5.5.5 节）：
 * 请求拦截器统一注入 token；响应拦截器统一处理出参与错误码。
 */
const request = axios.create({ baseURL: '/api', timeout: 15000 })

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = token
  return config
})

request.interceptors.response.use(
  resp => {
    const body = resp.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      // 401/登录失效：清除本地态并引导登录（5.5.3 节）
      if (body.code === 2001 || body.code === 2002) {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        ElMessage.warning(body.message || '请先登录')
      } else {
        ElMessage.error(body.message || '操作失败')
      }
      return Promise.reject(body)
    }
    return body
  },
  error => {
    ElMessage.error(error.message?.includes('timeout') ? '请求超时，请稍后重试' : '网络异常，请稍后重试')
    return Promise.reject(error)
  }
)

export default request
