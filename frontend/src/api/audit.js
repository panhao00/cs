import { api } from './index'

export const auditApi = {
  getLogs(params) {
    return api.get('/audit-logs', { params })
  },
  getStats() {
    return api.get('/audit-logs/stats')
  }
}
