import { describe, it, expect, beforeEach, vi } from 'vitest'

// Mock the router module before any imports - use vi.hoisted for variables used in vi.mock
const { mockRouterPush } = vi.hoisted(() => ({
  mockRouterPush: vi.fn()
}))
vi.mock('../../router', () => ({
  default: {
    push: mockRouterPush
  }
}))

// Import api after mocking
import { api } from '../../api'
import router from '../../router'

describe('API Instance', () => {
  beforeEach(() => {
    // Clear localStorage
    localStorage.clear()
    // Clear all mocks
    vi.clearAllMocks()
  })

  describe('axios instance creation', () => {
    it('creates an axios instance', () => {
      expect(api).toBeDefined()
      expect(typeof api.get).toBe('function')
      expect(typeof api.post).toBe('function')
    })

    it('has baseURL set to /api', () => {
      expect(api.defaults.baseURL).toBe('/api')
    })

    it('has timeout set to 10000', () => {
      expect(api.defaults.timeout).toBe(10000)
    })
  })

  describe('request interceptor', () => {
    it('attaches Authorization header when token exists', () => {
      localStorage.setItem('crs_token', 'test-jwt-token')

      const config = { headers: {} }
      const interceptor = api.interceptors.request.handlers[0]
      const result = interceptor.fulfilled(config)

      expect(result.headers.Authorization).toBe('Bearer test-jwt-token')
    })

    it('does NOT attach header when no token', () => {
      const config = { headers: {} }
      const interceptor = api.interceptors.request.handlers[0]
      const result = interceptor.fulfilled(config)

      expect(result.headers.Authorization).toBeUndefined()
    })

    it('does NOT attach header when token is empty string', () => {
      localStorage.setItem('crs_token', '')

      const config = { headers: {} }
      const interceptor = api.interceptors.request.handlers[0]
      const result = interceptor.fulfilled(config)

      expect(result.headers.Authorization).toBeUndefined()
    })
  })

  describe('response interceptor', () => {
    it('handles 401 by clearing auth and redirecting to login', async () => {
      localStorage.setItem('crs_token', 'expired-token')
      localStorage.setItem('crs_user', JSON.stringify({ userId: 1 }))

      const error = {
        response: { status: 401 }
      }

      // Find the response error interceptor (the one with a rejected handler)
      const responseHandlers = api.interceptors.response.handlers
      const rejectedHandler = responseHandlers.find(h => h.rejected)
      expect(rejectedHandler).toBeDefined()

      try {
        await rejectedHandler.rejected(error)
      } catch (e) {
        // Expected to reject
      }

      expect(localStorage.getItem('crs_token')).toBeNull()
      expect(localStorage.getItem('crs_user')).toBeNull()
      expect(router.push).toHaveBeenCalledWith('/login')
    })

    it('does NOT clear auth for non-401 errors', async () => {
      localStorage.setItem('crs_token', 'valid-token')
      localStorage.setItem('crs_user', JSON.stringify({ userId: 1 }))

      const error = {
        response: { status: 500 }
      }

      const responseHandlers = api.interceptors.response.handlers
      const rejectedHandler = responseHandlers.find(h => h.rejected)

      try {
        await rejectedHandler.rejected(error)
      } catch (e) {
        // Expected to reject
      }

      expect(localStorage.getItem('crs_token')).toBe('valid-token')
      expect(localStorage.getItem('crs_user')).toBe(JSON.stringify({ userId: 1 }))
    })

    it('rejects the promise after handling 401', async () => {
      const error = {
        response: { status: 401 }
      }

      const responseHandlers = api.interceptors.response.handlers
      const rejectedHandler = responseHandlers.find(h => h.rejected)

      await expect(rejectedHandler.rejected(error)).rejects.toEqual(error)
    })
  })
})
