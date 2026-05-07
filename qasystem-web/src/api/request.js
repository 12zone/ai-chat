import axios from 'axios'
import { message } from 'ant-design-vue'

const TOKEN_KEY = 'qa_token'
const USER_KEY = 'qa_user'

const clearSession = () => {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  window.dispatchEvent(new CustomEvent('qa-session-changed'))
}

const request = axios.create({
  // 0 = 不限制超时（重建向量索引等长耗时请求依赖此项；短请求由浏览器/代理自行约束）
  timeout: 0
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      clearSession()
      message.error('登录已失效，请重新登录')
      void Promise.all([
        import('@/router/authBootstrap').then((m) => m.invalidateAuthBootstrap()),
        import('@/router').then((m) => {
          const r = m.default
          if (r.currentRoute.value.path !== '/login') {
            return r.replace({
              path: '/login',
              query: { redirect: r.currentRoute.value.fullPath }
            })
          }
        })
      ]).catch(() => {})
    } else if (status === 403) {
      message.error('没有访问权限')
    }
    return Promise.reject(error)
  }
)

export { clearSession }
export { TOKEN_KEY }
export default request
