import { ensureAuthBootstrapped } from './authBootstrap'

function requiresAuth(to) {
  return to.matched.some((record) => record.meta.requiresAuth === true)
}

function safeRedirectPath(raw) {
  if (typeof raw !== 'string' || !raw.startsWith('/') || raw.startsWith('//')) {
    return null
  }
  return raw
}

export function setupGuards(router) {
  router.beforeEach(async (to, from, next) => {
    const { authenticated } = await ensureAuthBootstrapped()

    if (to.name === 'login' || to.path === '/login') {
      if (authenticated) {
        const target = safeRedirectPath(to.query.redirect) || '/chat'
        next({ path: target, replace: true })
      } else {
        next()
      }
      return
    }

    if (requiresAuth(to) && !authenticated) {
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
      return
    }

    next()
  })
}
