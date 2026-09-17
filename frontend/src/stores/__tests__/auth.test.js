import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../auth'

// Mock the api module
vi.mock('../../api', () => ({
  api: {
    post: vi.fn()
  }
}))

import { api } from '../../api'

describe('Auth Store', () => {
  beforeEach(() => {
    // Clear localStorage
    localStorage.clear()
    // Create fresh pinia for each test
    setActivePinia(createPinia())
    // Clear all mocks
    vi.clearAllMocks()
  })

  describe('initial state', () => {
    it('has isLoggedIn=false when no token', () => {
      const store = useAuthStore()
      expect(store.isLoggedIn).toBe(false)
    })

    it('has user=null when no stored user', () => {
      const store = useAuthStore()
      expect(store.user).toBeNull()
    })

    it('has token=empty string when no stored token', () => {
      const store = useAuthStore()
      expect(store.token).toBe('')
    })

    it('has role=empty string when no user', () => {
      const store = useAuthStore()
      expect(store.role).toBe('')
    })

    it('restores user from localStorage', () => {
      const mockUser = { userId: 1, username: 'test', name: 'Test User', role: 'STUDENT' }
      localStorage.setItem('crs_user', JSON.stringify(mockUser))
      // Use a valid JWT-format token with future expiry
      const payload = { exp: Math.floor(Date.now() / 1000) + 3600 }
      const validToken = `header.${btoa(JSON.stringify(payload))}.signature`
      localStorage.setItem('crs_token', validToken)

      const store = useAuthStore()
      expect(store.user).toEqual(mockUser)
    })

    it('clears expired token on init', () => {
      // Create a token with exp in the past
      const payload = { exp: Math.floor(Date.now() / 1000) - 3600 }
      const expiredToken = `header.${btoa(JSON.stringify(payload))}.signature`
      localStorage.setItem('crs_token', expiredToken)
      localStorage.setItem('crs_user', JSON.stringify({ userId: 1 }))

      const store = useAuthStore()
      expect(store.token).toBe('')
      expect(store.user).toBeNull()
      expect(localStorage.getItem('crs_token')).toBeNull()
    })

    it('clears invalid token format on init', () => {
      localStorage.setItem('crs_token', 'not-a-valid-jwt')
      localStorage.setItem('crs_user', JSON.stringify({ userId: 1 }))

      const store = useAuthStore()
      expect(store.token).toBe('')
      expect(store.user).toBeNull()
    })

    it('keeps valid non-expired token on init', () => {
      const payload = { exp: Math.floor(Date.now() / 1000) + 3600 }
      const validToken = `header.${btoa(JSON.stringify(payload))}.signature`
      localStorage.setItem('crs_token', validToken)

      const store = useAuthStore()
      expect(store.token).toBe(validToken)
      expect(store.isLoggedIn).toBe(true)
    })
  })

  describe('login()', () => {
    it('sets user, token, and role from response', async () => {
      const mockResponse = {
        data: {
          token: 'new.jwt.token',
          userId: 3,
          username: 'student',
          name: 'Charlie Brown',
          role: 'STUDENT'
        }
      }
      api.post.mockResolvedValue(mockResponse)

      const store = useAuthStore()
      const result = await store.login('student', 'student')

      expect(api.post).toHaveBeenCalledWith('/auth/login', {
        username: 'student',
        password: 'student'
      })
      expect(store.token).toBe('new.jwt.token')
      expect(store.user).toEqual({
        userId: 3,
        username: 'student',
        name: 'Charlie Brown',
        role: 'STUDENT'
      })
      expect(store.isLoggedIn).toBe(true)
      expect(store.role).toBe('STUDENT')
      expect(result).toEqual(mockResponse.data)
    })

    it('persists token and user to localStorage', async () => {
      const mockResponse = {
        data: {
          token: 'new.jwt.token',
          userId: 3,
          username: 'student',
          name: 'Charlie Brown',
          role: 'STUDENT'
        }
      }
      api.post.mockResolvedValue(mockResponse)

      const store = useAuthStore()
      await store.login('student', 'student')

      expect(localStorage.getItem('crs_token')).toBe('new.jwt.token')
      const storedUser = JSON.parse(localStorage.getItem('crs_user'))
      expect(storedUser.username).toBe('student')
      expect(storedUser.role).toBe('STUDENT')
    })
  })

  describe('logout()', () => {
    it('clears all state', async () => {
      // First login
      api.post.mockResolvedValue({
        data: {
          token: 'new.jwt.token',
          userId: 3,
          username: 'student',
          name: 'Charlie Brown',
          role: 'STUDENT'
        }
      })
      const store = useAuthStore()
      await store.login('student', 'student')
      expect(store.isLoggedIn).toBe(true)

      // Then logout
      store.logout()
      expect(store.token).toBe('')
      expect(store.user).toBeNull()
      expect(store.isLoggedIn).toBe(false)
      expect(store.role).toBe('')
      expect(localStorage.getItem('crs_token')).toBeNull()
      expect(localStorage.getItem('crs_user')).toBeNull()
    })
  })

  describe('isLoggedIn computed', () => {
    it('returns true when token exists', async () => {
      api.post.mockResolvedValue({
        data: {
          token: 'some.token',
          userId: 1,
          username: 'test',
          name: 'Test',
          role: 'STUDENT'
        }
      })
      const store = useAuthStore()
      await store.login('test', 'pass')
      expect(store.isLoggedIn).toBe(true)
    })

    it('returns false when token is empty', () => {
      const store = useAuthStore()
      expect(store.isLoggedIn).toBe(false)
    })
  })

  describe('role computed', () => {
    it('returns correct role from user', async () => {
      api.post.mockResolvedValue({
        data: {
          token: 'some.token',
          userId: 1,
          username: 'prof',
          name: 'Prof',
          role: 'PROFESSOR'
        }
      })
      const store = useAuthStore()
      await store.login('prof', 'pass')
      expect(store.role).toBe('PROFESSOR')
    })

    it('returns empty string when no user', () => {
      const store = useAuthStore()
      expect(store.role).toBe('')
    })
  })
})
