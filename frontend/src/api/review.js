import { api } from '../api'

/** 课程评价 API */
export const reviewApi = {
  /** 提交评价 */
  submitReview(offeringId, rating, comment) {
    return api.post('/reviews', { rating, comment }, { params: { offeringId } })
  },

  /** 修改评价 */
  updateReview(reviewId, rating, comment) {
    return api.put(`/reviews/${reviewId}`, { rating, comment })
  },

  /** 删除评价 */
  deleteReview(reviewId) {
    return api.delete(`/reviews/${reviewId}`)
  },

  /** 获取某开设的所有评价 */
  getOfferingReviews(offeringId) {
    return api.get(`/reviews/offering/${offeringId}`)
  },

  /** 获取某开设的平均评分 */
  getOfferingAverage(offeringId) {
    return api.get(`/reviews/offering/${offeringId}/average`)
  },

  /** 获取课程综合平均评分 */
  getCourseAverage(courseId) {
    return api.get(`/reviews/course/${courseId}/average`)
  },

  /** 获取我的评价列表 */
  getMyReviews() {
    return api.get('/reviews/me')
  }
}
