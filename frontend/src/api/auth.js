import { api } from './index'

export const authApi = {
  changePassword(oldPassword, newPassword) {
    return api.put('/auth/change-password', { oldPassword, newPassword })
  }
}
