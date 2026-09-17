import { api } from '../api'

/** 用户管理 API(对应用例 7.6 / 7.7 Maintain Student/Professor) */
export const userApi = {
  /** 查询用户列表(可按角色过滤) */
  getAll(role) {
    return api.get('/users', { params: role ? { role } : {} })
  },

  /** 创建用户 */
  create(body) {
    return api.post('/users', body)
  },
  /** 更新用户(password 留空表示不修改) */
  update(id, body) {
    return api.put(`/users/${id}`, body)
  },
  /** 设置用户状态 */
  setStatus(id, status) {
    return api.patch(`/users/${id}/status`, null, { params: { status } })
  },
  /** 删除用户(必须先 INACTIVE) */
  remove(id) {
    return api.delete(`/users/${id}`)
  }
}

/** 注册周期管理 API(对应用例 7.8 Close Registration) */
export const registrationApi = {
  /** 查询当前注册阶段 */
  getStatus() {
    return api.get('/registration/status')
  },
  /** 预览关闭注册后的状态(只读) */
  preview() {
    return api.get('/registration/preview')
  },
  /** 关闭注册(含通知 Billing + 满员/未满通知) */
  close(semester) {
    return api.post('/registration/close', null, {
      params: semester ? { semester } : {}
    })
  }
}
