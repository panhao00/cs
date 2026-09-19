import { api } from './index'

export const settingsApi = {
  getAll() {
    return api.get('/settings')
  },
  get(key) {
    return api.get(`/settings/${key}`)
  },
  update(settings) {
    return api.put('/settings', { settings })
  }
}
