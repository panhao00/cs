import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '../api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('crs_token') || '')
  let initialUser = null
  try {
    const stored = localStorage.getItem('crs_user')
    if (stored) initialUser = JSON.parse(stored)
  } catch {
    localStorage.removeItem('crs_user')
  }
  const user = ref(initialUser)

  // Check token expiry on init
  if (token.value) {
    try {
      const payload = JSON.parse(atob(token.value.split('.')[1]))
      if (payload.exp && payload.exp * 1000 < Date.now()) {
        // Token expired, clear it
        token.value = ''
        localStorage.removeItem('crs_token')
        localStorage.removeItem('crs_user')
        user.value = null
      }
    } catch {
      // Invalid token format, clear it
      token.value = ''
      localStorage.removeItem('crs_token')
      localStorage.removeItem('crs_user')
      user.value = null
    }
  }

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => user.value?.role || '')

  async function login(username, password) {
    const res = await api.post('/auth/login', { username, password })
    token.value = res.data.token
    user.value = {
      userId: res.data.userId,
      username: res.data.username,
      name: res.data.name,
      role: res.data.role
    }
    localStorage.setItem('crs_token', token.value)
    localStorage.setItem('crs_user', JSON.stringify(user.value))
    return res.data
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('crs_token')
    localStorage.removeItem('crs_user')
  }

  return { token, user, isLoggedIn, role, login, logout }
})
