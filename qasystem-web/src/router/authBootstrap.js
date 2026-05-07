import userApi from '@/api/user'
import authApi from '@/api/auth'
import { TOKEN_KEY, clearSession } from '@/api/request'

export function isTokenExpired(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    if (!payload?.exp) {
      return true
    }
    return payload.exp * 1000 <= Date.now()
  } catch {
    return true
  }
}

/** localStorage token string used for last bootstrap cache key */
let cacheKey = undefined
let inflight = null

export function invalidateAuthBootstrap() {
  cacheKey = undefined
  inflight = null
}

async function runBootstrap() {
  const token = localStorage.getItem(TOKEN_KEY)
  if (!token || isTokenExpired(token)) {
    if (token) {
      clearSession()
    }
    return { authenticated: false }
  }
  try {
    const response = await userApi.getProfile()
    const user = response.data?.data || response.data
    authApi.setLocalUser(user)
    return { authenticated: true, user }
  } catch {
    clearSession()
    return { authenticated: false }
  }
}

/**
 * Resolve current session once per token value (re-runs when token changes or after invalidate).
 */
export async function ensureAuthBootstrapped() {
  const token = localStorage.getItem(TOKEN_KEY) ?? null
  const key = token ?? ''
  if (key !== cacheKey) {
    cacheKey = key
    inflight = runBootstrap()
  }
  return inflight
}
