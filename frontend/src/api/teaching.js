import { api } from '../api'

/** 教授课课 API */
export const teachingApi = {
  getOfferings(semester) {
    return api.get('/teaching/offerings', { params: { semester } })
  },
  selectCourses(semester, offeringIds) {
    return api.post('/teaching/select', { offeringIds }, { params: { semester } })
  }
}

/** 成绩 API */
export const gradeApi = {
  getTaughtOfferings() {
    return api.get('/grades/taught')
  },
  getRoster(offeringId) {
    return api.get(`/grades/offerings/${offeringId}/roster`)
  },
  submitGrade(offeringId, studentId, grade, clear = false) {
    return api.post(`/grades/offerings/${offeringId}`,
      { studentId, grade, clear })
  }
}

/** 成绩单 API */
export const reportCardApi = {
  getMyReportCard() {
    return api.get('/report-card')
  }
}
