import { api } from '../api'

/** 通知 API(对应用例 7.8 Close Registration 通知) */
export const notificationApi = {
  /** 获取当前用户的所有通知 */
  getAll() {
    return api.get('/notifications')
  },
  /** 获取未读通知数量 */
  getUnreadCount() {
    return api.get('/notifications/unread')
  },
  /** 标记单条通知为已读 */
  markAsRead(id) {
    return api.post(`/notifications/${id}/read`)
  },
  /** 标记所有通知为已读 */
  markAllAsRead() {
    return api.post('/notifications/read-all')
  }
}
