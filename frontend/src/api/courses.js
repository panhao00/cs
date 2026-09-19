import { api } from '../api'

/** 课程目录 API */
export const catalogApi = {
  /** 获取某学期可用开设列表 */
  getOfferings(semester) {
    return api.get('/catalog/offerings', { params: { semester } })
  },

  /** 获取单个课程开设详情 */
  getOffering(id) {
    return api.get(`/catalog/offerings/${id}`)
  },

}

/** 课表 API */
export const scheduleApi = {
  /** 获取当前学生课表 */
  getMySchedule(semester) {
    return api.get('/schedules/me', { params: { semester } })
  },
  /** 创建课表 */
  createSchedule(semester) {
    return api.post('/schedules', null, { params: { semester } })
  },
  /** 选择课程开设 */
  selectOfferings(semester, selections) {
    return api.post('/schedules/offerings', { selections }, { params: { semester } })
  },
  /** 从课表移除开设 */
  removeOffering(semester, offeringId) {
    return api.delete(`/schedules/offerings/${offeringId}`, { params: { semester } })
  },
  /** 保存课表(不提交) */
  saveSchedule(semester) {
    return api.post('/schedules/save', null, { params: { semester } })
  },
  /** 提交课表 */
  submitSchedule(semester) {
    return api.post('/schedules/submit', null, { params: { semester } })
  },
  /** 删除课表 */
  deleteSchedule(semester) {
    return api.delete('/schedules', { params: { semester } })
  }
}
