import request, { TOKEN_KEY, clearSession } from './request'

const AUTH_USER_KEY = 'qa_user'

const authApi = {
  register: (payload) => request.post('/api/auth/register', payload),
  login: async (payload) => {
    const response = await request.post('/api/auth/login', payload)
    const body = response.data?.data || response.data
    if (body?.token) {
      localStorage.setItem(TOKEN_KEY, body.token)
      localStorage.setItem(AUTH_USER_KEY, JSON.stringify(body))
      window.dispatchEvent(new CustomEvent('qa-session-changed'))
    }
    return response
  },
  logout: () => {
    clearSession()
  },
  getLocalUser: () => {
    const raw = localStorage.getItem(AUTH_USER_KEY)
    if (!raw) return null
    try {
      return JSON.parse(raw)
    } catch (e) {
      return null
    }
  },
  setLocalUser: (user) => {
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user))
    window.dispatchEvent(new CustomEvent('qa-session-changed'))
  }
}

export { AUTH_USER_KEY }
export default authApi
