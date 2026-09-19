import { api } from '../api'

/** 成绩统计 API */
export const statsApi = {
  /** 获取 GPA 趋势(学生) */
  getGpaTrend() {
    return api.get('/stats/gpa-trend')
  },

  /** 获取学分统计(学生) */
  getCreditSummary() {
    return api.get('/stats/credit-summary')
  },

  /** 获取成绩分布(教授) */
  getGradeDistribution(offeringId) {
    return api.get(`/stats/grade-distribution/${offeringId}`)
  }
}

/** 学业概览 API */
export const academicApi = {
  /** 获取学业摘要(GPA/学分/历史学期) */
  getSummary() {
    return api.get('/schedules/academic-summary')
  }
}

/** 选课倒计时提醒 API */
export const deadlineApi = {
  /** 获取时间节点列表 */
  getDeadlines(semester) {
    return api.get('/deadlines', { params: { semester } })
  }
}
