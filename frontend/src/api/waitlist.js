import { api } from '../api'

/** 候补名单 API */
export const waitlistApi = {
  /** 加入候补 */
  joinWaitlist(offeringId) {
    return api.post('/waitlist/join', { offeringId })
  },
  /** 取消候补 */
  cancelWaitlist(offeringId) {
    return api.delete(`/waitlist/cancel/${offeringId}`)
  },
  /** 查看我的候补列表 */
  getMyWaitlists() {
    return api.get('/waitlist/me')
  },
  /** 查看某课程候补队列 */
  getOfferingWaitlist(offeringId) {
    return api.get(`/waitlist/offering/${offeringId}`)
  }
}
