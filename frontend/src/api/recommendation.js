import { api } from '../api'

/** 推荐 API */
export const recommendationApi = {
  /** 获取个性化课程推荐 */
  getRecommendations(semester) {
    return api.get('/recommendations', { params: { semester } })
  }
}

/** 选课统计 API */
export const enrollmentStatsApi = {
  /** 学期总览统计 */
  getOverview(semester) {
    return api.get('/stats/overview', { params: { semester } })
  },

  /** 课程热度排名 */
  getPopularity(semester) {
    return api.get('/stats/popularity', { params: { semester } })
  },

  /** 课程选课趋势 */
  getTrend(courseId) {
    return api.get(`/stats/trend/${courseId}`)
  },

  /** 院系选课统计 */
  getDepartmentStats(semester) {
    return api.get('/stats/department', { params: { semester } })
  }
}
