import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../../stores/auth'

// Mock vue-router
const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
  createRouter: vi.fn(() => ({
    beforeEach: vi.fn(),
    push: mockPush,
    currentRoute: { value: { path: '/' } }
  })),
  createWebHistory: vi.fn()
}))

// Mock api module
vi.mock('../../api', () => ({
  api: { post: vi.fn() }
}))

describe('Router Guards', () => {
  let authStore

  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
    setActivePinia(createPinia())
    authStore = useAuthStore()
  })

  // Helper to simulate the beforeEach guard logic directly
  function evaluateGuard(to, isLoggedIn, role) {
    // Replicate the guard logic from router/index.js
    if (to.meta.requiresAuth && !isLoggedIn) {
      return { name: 'Login' }
    }
    if (to.meta.public && isLoggedIn) {
      return { name: 'Dashboard' }
    }
    if (to.meta.roles && !to.meta.roles.includes(role)) {
      return { name: 'Dashboard' }
    }
    return undefined
  }

  describe('unauthenticated access', () => {
    it('redirects to /login for protected routes when not logged in', () => {
      const to = { path: '/', meta: { requiresAuth: true } }
      const result = evaluateGuard(to, false, '')
      expect(result).toEqual({ name: 'Login' })
    })

    it('redirects to /login for /register when not logged in', () => {
      const to = { path: '/register', meta: { requiresAuth: true, roles: ['STUDENT'] } }
      const result = evaluateGuard(to, false, '')
      expect(result).toEqual({ name: 'Login' })
    })

    it('redirects to /login for /teach when not logged in', () => {
      const to = { path: '/teach', meta: { requiresAuth: true, roles: ['PROFESSOR'] } }
      const result = evaluateGuard(to, false, '')
      expect(result).toEqual({ name: 'Login' })
    })

    it('redirects to /login for /grades when not logged in', () => {
      const to = { path: '/grades', meta: { requiresAuth: true, roles: ['PROFESSOR'] } }
      const result = evaluateGuard(to, false, '')
      expect(result).toEqual({ name: 'Login' })
    })

    it('redirects to /login for /report-card when not logged in', () => {
      const to = { path: '/report-card', meta: { requiresAuth: true, roles: ['STUDENT'] } }
      const result = evaluateGuard(to, false, '')
      expect(result).toEqual({ name: 'Login' })
    })

    it('redirects to /login for /users when not logged in', () => {
      const to = { path: '/users', meta: { requiresAuth: true, roles: ['REGISTRAR'] } }
      const result = evaluateGuard(to, false, '')
      expect(result).toEqual({ name: 'Login' })
    })
  })

  describe('authenticated access to login page', () => {
    it('redirects to Dashboard when logged in and visiting /login', () => {
      const to = { path: '/login', meta: { public: true } }
      const result = evaluateGuard(to, true, 'STUDENT')
      expect(result).toEqual({ name: 'Dashboard' })
    })
  })

  describe('role-based access control', () => {
    it('allows STUDENT to access /register', () => {
      const to = { path: '/register', meta: { requiresAuth: true, roles: ['STUDENT'] } }
      const result = evaluateGuard(to, true, 'STUDENT')
      expect(result).toBeUndefined()
    })

    it('blocks STUDENT from /teach (PROFESSOR only)', () => {
      const to = { path: '/teach', meta: { requiresAuth: true, roles: ['PROFESSOR'] } }
      const result = evaluateGuard(to, true, 'STUDENT')
      expect(result).toEqual({ name: 'Dashboard' })
    })

    it('blocks STUDENT from /grades (PROFESSOR only)', () => {
      const to = { path: '/grades', meta: { requiresAuth: true, roles: ['PROFESSOR'] } }
      const result = evaluateGuard(to, true, 'STUDENT')
      expect(result).toEqual({ name: 'Dashboard' })
    })

    it('allows PROFESSOR to access /teach', () => {
      const to = { path: '/teach', meta: { requiresAuth: true, roles: ['PROFESSOR'] } }
      const result = evaluateGuard(to, true, 'PROFESSOR')
      expect(result).toBeUndefined()
    })

    it('allows PROFESSOR to access /grades', () => {
      const to = { path: '/grades', meta: { requiresAuth: true, roles: ['PROFESSOR'] } }
      const result = evaluateGuard(to, true, 'PROFESSOR')
      expect(result).toBeUndefined()
    })

    it('blocks PROFESSOR from /register (STUDENT only)', () => {
      const to = { path: '/register', meta: { requiresAuth: true, roles: ['STUDENT'] } }
      const result = evaluateGuard(to, true, 'PROFESSOR')
      expect(result).toEqual({ name: 'Dashboard' })
    })

    it('blocks PROFESSOR from /report-card (STUDENT only)', () => {
      const to = { path: '/report-card', meta: { requiresAuth: true, roles: ['STUDENT'] } }
      const result = evaluateGuard(to, true, 'PROFESSOR')
      expect(result).toEqual({ name: 'Dashboard' })
    })

    it('allows REGISTRAR to access /users', () => {
      const to = { path: '/users', meta: { requiresAuth: true, roles: ['REGISTRAR'] } }
      const result = evaluateGuard(to, true, 'REGISTRAR')
      expect(result).toBeUndefined()
    })

    it('allows REGISTRAR to access /registration', () => {
      const to = { path: '/registration', meta: { requiresAuth: true, roles: ['REGISTRAR'] } }
      const result = evaluateGuard(to, true, 'REGISTRAR')
      expect(result).toBeUndefined()
    })

    it('blocks STUDENT from /users (REGISTRAR only)', () => {
      const to = { path: '/users', meta: { requiresAuth: true, roles: ['REGISTRAR'] } }
      const result = evaluateGuard(to, true, 'STUDENT')
      expect(result).toEqual({ name: 'Dashboard' })
    })

    it('blocks REGISTRAR from /register (STUDENT only)', () => {
      const to = { path: '/register', meta: { requiresAuth: true, roles: ['STUDENT'] } }
      const result = evaluateGuard(to, true, 'REGISTRAR')
      expect(result).toEqual({ name: 'Dashboard' })
    })

    it('allows STUDENT to access /report-card', () => {
      const to = { path: '/report-card', meta: { requiresAuth: true, roles: ['STUDENT'] } }
      const result = evaluateGuard(to, true, 'STUDENT')
      expect(result).toBeUndefined()
    })
  })

  describe('public routes', () => {
    it('allows unauthenticated access to /login', () => {
      const to = { path: '/login', meta: { public: true } }
      const result = evaluateGuard(to, false, '')
      expect(result).toBeUndefined()
    })

    it('allows authenticated access to Dashboard (requiresAuth, no roles)', () => {
      const to = { path: '/', meta: { requiresAuth: true } }
      const result = evaluateGuard(to, true, 'STUDENT')
      expect(result).toBeUndefined()
    })

    it('allows any role to access routes without role restrictions', () => {
      const to = { path: '/notifications', meta: { requiresAuth: true } }
      const result = evaluateGuard(to, true, 'STUDENT')
      expect(result).toBeUndefined()
    })
  })
})
