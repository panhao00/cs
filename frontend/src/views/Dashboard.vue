<template>
  <div class="dashboard">
    <AppLayout title="主页">
      <!-- Welcome Section -->
      <div class="welcome-section">
        <div class="welcome-content">
          <h1 class="welcome-greeting">{{ greeting }}, {{ auth.user?.name }}</h1>
          <p class="welcome-subtitle">
            <el-tag :type="roleTagType" size="small" effect="dark" class="role-badge">{{ roleLabel }}</el-tag>
            <span class="welcome-desc">{{ roleDescription }}</span>
          </p>
        </div>
        <div class="welcome-decoration">
          <div class="decoration-circle c1"></div>
          <div class="decoration-circle c2"></div>
        </div>
      </div>

      <!-- Quick Actions -->
      <h2 class="section-title">快捷操作</h2>
      <el-row :gutter="16">
        <template v-if="auth.role === 'STUDENT'">
          <el-col :xs="24" :sm="12" :md="8">
            <div class="action-card" @click="$router.push('/register')">
              <div class="action-icon student">
                <el-icon :size="24"><DocumentAdd /></el-icon>
              </div>
              <div class="action-info">
                <h3>选课</h3>
                <p>选课</p>
              </div>
              <el-icon class="action-arrow"><ArrowRight /></el-icon>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <div class="action-card" @click="$router.push('/report-card')">
              <div class="action-icon report">
                <el-icon :size="24"><DataAnalysis /></el-icon>
              </div>
              <div class="action-info">
                <h3>查看成绩单</h3>
                <p>查看成绩单</p>
              </div>
              <el-icon class="action-arrow"><ArrowRight /></el-icon>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <div class="action-card" @click="$router.push('/grade-trend')">
              <div class="action-icon trend">
                <el-icon :size="24"><TrendCharts /></el-icon>
              </div>
              <div class="action-info">
                <h3>成绩趋势</h3>
                <p>成绩趋势</p>
              </div>
              <el-icon class="action-arrow"><ArrowRight /></el-icon>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <div class="action-card" @click="$router.push('/recommendations')">
              <div class="action-icon recommend">
                <el-icon :size="24"><MagicStick /></el-icon>
              </div>
              <div class="action-info">
                <h3>智能推荐</h3>
                <p>智能推荐</p>
              </div>
              <el-icon class="action-arrow"><ArrowRight /></el-icon>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <div class="action-card" @click="$router.push('/waitlist')">
              <div class="action-icon waitlist">
                <el-icon :size="24"><List /></el-icon>
              </div>
              <div class="action-info">
                <h3>候补名单</h3>
                <p>候补名单</p>
              </div>
              <el-icon class="action-arrow"><ArrowRight /></el-icon>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <div class="action-card" @click="$router.push('/my-reviews')">
              <div class="action-icon recommend">
                <el-icon :size="24"><ChatDotRound /></el-icon>
              </div>
              <div class="action-info">
                <h3>我的评价</h3>
                <p>我的评价</p>
              </div>
              <el-icon class="action-arrow"><ArrowRight /></el-icon>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <div class="action-card" @click="$router.push('/change-password')">
              <div class="action-icon password">
                <el-icon :size="24"><Key /></el-icon>
              </div>
              <div class="action-info">
                <h3>修改密码</h3>
                <p>修改密码</p>
              </div>
              <el-icon class="action-arrow"><ArrowRight /></el-icon>
            </div>
          </el-col>
        </template>
        <template v-else-if="auth.role === 'PROFESSOR'">
          <el-col :xs="24" :sm="12" :md="8">
            <div class="action-card" @click="$router.push('/teach')">
              <div class="action-icon teach">
                <el-icon :size="24"><Reading /></el-icon>
              </div>
              <div class="action-info">
                <h3>选课授课</h3>
                <p>选课授课</p>
              </div>
              <el-icon class="action-arrow"><ArrowRight /></el-icon>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <div class="action-card" @click="$router.push('/grades')">
              <div class="action-icon grades">
                <el-icon :size="24"><EditPen /></el-icon>
              </div>
              <div class="action-info">
                <h3>提交成绩</h3>
                <p>提交成绩</p>
              </div>
              <el-icon class="action-arrow"><ArrowRight /></el-icon>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <div class="action-card" @click="$router.push('/change-password')">
              <div class="action-icon password">
                <el-icon :size="24"><Key /></el-icon>
              </div>
              <div class="action-info">
                <h3>修改密码</h3>
                <p>修改密码</p>
              </div>
              <el-icon class="action-arrow"><ArrowRight /></el-icon>
            </div>
          </el-col>
        </template>
        <template v-else-if="auth.role === 'REGISTRAR'">
          <el-col :xs="24" :sm="12" :md="8">
            <div class="action-card" @click="$router.push('/users')">
              <div class="action-icon users">
                <el-icon :size="24"><User /></el-icon>
              </div>
              <div class="action-info">
                <h3>用户维护</h3>
                <p>用户维护</p>
              </div>
              <el-icon class="action-arrow"><ArrowRight /></el-icon>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <div class="action-card" @click="$router.push('/registration')">
              <div class="action-icon registration">
                <el-icon :size="24"><Lock /></el-icon>
              </div>
              <div class="action-info">
                <h3>关闭注册</h3>
                <p>关闭注册</p>
              </div>
              <el-icon class="action-arrow"><ArrowRight /></el-icon>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <div class="action-card" @click="$router.push('/settings')">
              <div class="action-icon settings">
                <el-icon :size="24"><Setting /></el-icon>
              </div>
              <div class="action-info">
                <h3>系统设置</h3>
                <p>系统设置</p>
              </div>
              <el-icon class="action-arrow"><ArrowRight /></el-icon>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <div class="action-card" @click="$router.push('/enrollment-dashboard')">
              <div class="action-icon report">
                <el-icon :size="24"><DataAnalysis /></el-icon>
              </div>
              <div class="action-info">
                <h3>选课统计</h3>
                <p>选课统计</p>
              </div>
              <el-icon class="action-arrow"><ArrowRight /></el-icon>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <div class="action-card" @click="$router.push('/audit-logs')">
              <div class="action-icon audit">
                <el-icon :size="24"><Document /></el-icon>
              </div>
              <div class="action-info">
                <h3>审计日志</h3>
                <p>审计日志</p>
              </div>
              <el-icon class="action-arrow"><ArrowRight /></el-icon>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <div class="action-card" @click="$router.push('/change-password')">
              <div class="action-icon password">
                <el-icon :size="24"><Key /></el-icon>
              </div>
              <div class="action-info">
                <h3>修改密码</h3>
                <p>修改密码</p>
              </div>
              <el-icon class="action-arrow"><ArrowRight /></el-icon>
            </div>
          </el-col>
        </template>
        <!-- Notification action always visible -->
        <el-col :xs="24" :sm="12" :md="8">
          <div class="action-card" @click="$router.push('/notifications')">
            <div class="action-icon notifications">
              <el-icon :size="24"><Bell /></el-icon>
              <span v-if="unreadCount > 0" class="notification-dot"></span>
            </div>
            <div class="action-info">
              <h3>通知中心</h3>
              <p>通知中心</p>
            </div>
            <el-icon class="action-arrow"><ArrowRight /></el-icon>
          </div>
        </el-col>
      </el-row>

      <!-- Academic Summary (Student only) -->
      <template v-if="auth.role === 'STUDENT'">
        <h2 class="section-title">学业概览</h2>
        <el-row :gutter="16">
          <el-col :xs="24" :md="10">
            <el-card class="info-card">
              <template #header>
                <div class="card-title-row">
                  <el-icon><DataAnalysis /></el-icon>
                  <span>学分与 GPA</span>
                </div>
              </template>
              <div class="gpa-section">
                <div class="gpa-circle">
                  <span class="gpa-value">{{ academic.gpa.toFixed(2) }}</span>
                  <span class="gpa-label">GPA</span>
                </div>
                <div class="gpa-details">
                  <div class="detail-row">
                    <span class="detail-label">已获学分</span>
                    <span class="detail-value">{{ academic.creditsEarned }} / 120</span>
                  </div>
                  <el-progress
                    :percentage="Math.min(100, Math.round(academic.creditsEarned / 120 * 100))"
                    :stroke-width="8"
                    :color="progressColor"
                    :show-text="false"
                  />
                  <div class="detail-row">
                    <span class="detail-label">本学期学分</span>
                    <span class="detail-value">{{ academic.currentCredits }}</span>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :xs="24" :md="14">
            <el-card class="info-card">
              <template #header>
                <div class="card-title-row">
                  <el-icon><Calendar /></el-icon>
                  <span>历史学期</span>
                </div>
              </template>
              <div v-if="academic.semesterHistory.length === 0" class="empty-history">
                暂无历史成绩
              </div>
              <div v-else class="semester-list">
                <div v-for="s in academic.semesterHistory" :key="s.semester" class="semester-item">
                  <div class="semester-header">
                    <span class="semester-name">{{ s.semester }}</span>
                    <el-tag :type="gpaTagType(s.semesterGpa)" size="small" effect="dark">
                      GPA {{ s.semesterGpa.toFixed(2) }}
                    </el-tag>
                    <span class="semester-credits">{{ s.credits }} 学分</span>
                  </div>
                  <div class="semester-courses">
                    <span v-for="c in s.courses" :key="c.courseId" class="course-chip">
                      {{ c.courseId }}
                      <span class="course-grade" :class="'grade-' + (c.grade || 'none')">{{ c.grade || '-' }}</span>
                    </span>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </template>

      <!-- Account Info & Features -->
      <el-row :gutter="16" style="margin-top: 24px">
        <el-col :xs="24" :md="10">
          <el-card class="info-card">
            <template #header>
              <div class="card-title-row">
                <el-icon><InfoFilled /></el-icon>
                <span>账户信息</span>
              </div>
            </template>
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">用户编号</span>
                <span class="info-value">{{ auth.user?.userId }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">用户名</span>
                <span class="info-value">{{ auth.user?.username }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">姓名</span>
                <span class="info-value">{{ auth.user?.name }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">角色</span>
                <span class="info-value">
                  <el-tag :type="roleTagType" size="small">{{ roleLabel }}</el-tag>
                </span>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :md="14">
          <el-card class="features-card">
            <template #header>
              <div class="card-title-row">
                <el-icon><CircleCheck /></el-icon>
                <span>系统功能</span>
              </div>
            </template>
            <div class="features-grid">
              <div
                v-for="(item, idx) in features"
                :key="idx"
                class="feature-item"
                :class="{ done: item.done }"
              >
                <div class="feature-check">
                  <el-icon v-if="item.done"><CircleCheckFilled /></el-icon>
                  <el-icon v-else><CircleClose /></el-icon>
                </div>
                <div class="feature-text">
                  <span class="feature-name">{{ item.name }}</span>
                  <span class="feature-status">{{ item.status }}</span>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </AppLayout>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import {
  DocumentAdd, DataAnalysis, Reading, EditPen, User, Lock, Bell,
  ArrowRight, InfoFilled, CircleCheck, CircleCheckFilled, CircleClose,
  TrendCharts, MagicStick, List, Key, Setting, Document, ChatDotRound,
  Calendar
} from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import { useAuthStore } from '../stores/auth'
import { notificationApi } from '../api/notifications'
import { academicApi } from '../api/stats'

const auth = useAuthStore()
const unreadCount = ref(0)
let refreshTimer = null

const academic = reactive({
  gpa: 0,
  creditsEarned: 0,
  creditsAttempted: 0,
  currentCredits: 0,
  semesterHistory: []
})

const progressColor = computed(() => {
  const pct = Math.min(100, Math.round(academic.creditsEarned / 120 * 100))
  if (pct >= 75) return '#67C23A'
  if (pct >= 50) return '#409EFF'
  return '#E6A23C'
})

function gpaTagType(gpa) {
  if (gpa >= 3.5) return 'success'
  if (gpa >= 3.0) return 'primary'
  if (gpa >= 2.0) return 'warning'
  return 'danger'
}

const roleLabel = computed(() => {
  const map = { STUDENT: '学生', PROFESSOR: '教授', REGISTRAR: '注册主任' }
  return map[auth.role] || auth.role
})

const roleTagType = computed(() => {
  const map = { STUDENT: 'warning', PROFESSOR: 'success', REGISTRAR: 'primary' }
  return map[auth.role] || 'info'
})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const roleDescription = computed(() => {
  const map = {
    STUDENT: '管理课程注册，查看学业进度',
    PROFESSOR: '选择教授课程，管理学生成绩',
    REGISTRAR: '管理用户，控制注册流程'
  }
  return map[auth.role] || '欢迎使用课程注册系统'
})

const features = [
  { name: '登录', status: '已上线', done: true },
  { name: '学生选课', status: '已上线', done: true },
  { name: '教授选课', status: '已上线', done: true },
  { name: '成绩录入', status: '已上线', done: true },
  { name: '成绩单', status: '已上线', done: true },
  { name: '用户管理', status: '已上线', done: true },
  { name: '注册管理', status: '已上线', done: true },
  { name: '课程评价', status: '已上线', done: true },
  { name: '课程推荐', status: '已上线', done: true },
  { name: '候补名单', status: '已上线', done: true },
  { name: '选课统计', status: '已上线', done: true },
  { name: '成绩趋势', status: '已上线', done: true },
  { name: '系统设置', status: '已上线', done: true },
  { name: '审计日志', status: '已上线', done: true },
  { name: '通知中心', status: '已上线', done: true },
  { name: '修改密码', status: '已上线', done: true },
  { name: '课表周视图', status: '已上线', done: true },
  { name: '学业概览', status: '已上线', done: true }
]

async function refreshUnreadCount() {
  if (auth.role !== 'STUDENT') return
  try {
    const res = await notificationApi.getUnreadCount()
    unreadCount.value = res.data.count
  } catch {
    // 静默失败
  }
}

onMounted(() => {
  refreshUnreadCount()
  if (auth.role === 'STUDENT') {
    refreshTimer = setInterval(refreshUnreadCount, 30000)
    loadAcademic()
  }
})

async function loadAcademic() {
  try {
    const res = await academicApi.getSummary()
    academic.gpa = res.data.cumulativeGpa
    academic.creditsEarned = res.data.totalCreditsEarned
    academic.creditsAttempted = res.data.totalCreditsAttempted
    academic.currentCredits = res.data.currentEnrolledCredits
    academic.semesterHistory = res.data.semesterHistory || []
  } catch {
    // 静默失败
  }
}

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped>
/* Welcome Section */
.welcome-section {
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  border-radius: var(--radius-xl);
  padding: 32px 36px;
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;
  color: white;
}

.welcome-content {
  position: relative;
  z-index: 1;
}

.welcome-greeting {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.welcome-subtitle {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  opacity: 0.9;
}

.role-badge {
  border: none !important;
}

.welcome-desc {
  font-size: 14px;
  opacity: 0.8;
}

.welcome-decoration {
  position: absolute;
  top: -30px;
  right: -20px;
}

.decoration-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

.decoration-circle.c1 {
  width: 140px;
  height: 140px;
  right: 0;
  top: 0;
}

.decoration-circle.c2 {
  width: 80px;
  height: 80px;
  right: 100px;
  top: 40px;
}

/* Section titles */
.section-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 14px;
}

/* Action cards */
.action-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-fast);
  margin-bottom: 12px;
}

.action-card:hover {
  border-color: var(--color-primary-lighter);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.action-card:hover .action-arrow {
  opacity: 1;
  transform: translateX(4px);
}

.action-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
  position: relative;
}

.action-icon.student {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
}

.action-icon.report {
  background: linear-gradient(135deg, #8b5cf6, #7c3aed);
}

.action-icon.teach {
  background: linear-gradient(135deg, #059669, #047857);
}

.action-icon.grades {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.action-icon.users {
  background: linear-gradient(135deg, #06b6d4, #0891b2);
}

.action-icon.registration {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}

.action-icon.notifications {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
}

.action-icon.trend {
  background: linear-gradient(135deg, #8b5cf6, #7c3aed);
}

.action-icon.recommend {
  background: linear-gradient(135deg, #ec4899, #db2777);
}

.action-icon.waitlist {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.action-icon.password {
  background: linear-gradient(135deg, #06b6d4, #0891b2);
}

.action-icon.settings {
  background: linear-gradient(135deg, #64748b, #475569);
}

.action-icon.audit {
  background: linear-gradient(135deg, #78716c, #57534e);
}

.notification-dot {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 8px;
  height: 8px;
  background: #ef4444;
  border-radius: 50%;
  border: 2px solid white;
}

.action-info {
  flex: 1;
  min-width: 0;
}

.action-info h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
}

.action-info p {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--color-text-muted);
}

.action-arrow {
  color: var(--color-text-muted);
  opacity: 0;
  transition: all var(--transition-fast);
  flex-shrink: 0;
}

/* Info card */
.info-card,
.features-card {
  height: 100%;
}

.card-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 14px;
}

.info-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--color-border-light);
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 13px;
  color: var(--color-text-muted);
  font-weight: 500;
}

.info-value {
  font-size: 14px;
  color: var(--color-text);
  font-weight: 500;
}

/* Features grid */
.features-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
}

.feature-check {
  flex-shrink: 0;
  font-size: 16px;
}

.feature-item.done .feature-check {
  color: var(--color-success);
}

.feature-item:not(.done) .feature-check {
  color: var(--color-text-muted);
}

.feature-text {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex: 1;
}

.feature-name {
  font-size: 13px;
  color: var(--color-text);
}

.feature-status {
  font-size: 11px;
  color: var(--color-text-muted);
  font-weight: 500;
}

.feature-item.done .feature-name {
  color: var(--color-text);
}

.feature-item:not(.done) .feature-name {
  color: var(--color-text-secondary);
}

/* GPA Section */
.gpa-section {
  display: flex;
  align-items: center;
  gap: 24px;
}

.gpa-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.gpa-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1;
}

.gpa-label {
  font-size: 11px;
  opacity: 0.85;
  margin-top: 2px;
}

.gpa-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-label {
  font-size: 13px;
  color: var(--color-text-muted);
}

.detail-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

/* Semester list */
.empty-history {
  text-align: center;
  color: var(--color-text-muted);
  padding: 24px 0;
}

.semester-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 240px;
  overflow-y: auto;
}

.semester-item {
  padding: 8px 0;
  border-bottom: 1px solid var(--color-border-light);
}

.semester-item:last-child {
  border-bottom: none;
}

.semester-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.semester-name {
  font-weight: 600;
  font-size: 13px;
  color: var(--color-text);
}

.semester-credits {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-left: auto;
}

.semester-courses {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.course-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  background: var(--color-bg-page);
  font-size: 12px;
  font-family: var(--font-mono);
  color: var(--color-text-secondary);
}

.course-grade {
  font-weight: 600;
}

.grade-A { color: #67C23A; }
.grade-B { color: #409EFF; }
.grade-C { color: #E6A23C; }
.grade-D { color: #F56C6C; }
.grade-F, .grade-I { color: #909399; }
.grade-none { color: #C0C4CC; }

/* --- Mobile Responsive --- */
@media (max-width: 768px) {
  .welcome-section {
    padding: 24px 20px;
    border-radius: var(--radius-lg);
  }

  .welcome-greeting {
    font-size: 20px;
  }

  .welcome-subtitle {
    flex-wrap: wrap;
    gap: 6px;
  }

  .welcome-desc {
    font-size: 13px;
  }

  .section-title {
    font-size: 15px;
    margin-bottom: 12px;
  }

  .action-card {
    padding: 14px 16px;
    gap: 12px;
  }

  .action-icon {
    width: 40px;
    height: 40px;
  }

  .action-info h3 {
    font-size: 14px;
  }

  .action-info p {
    font-size: 11px;
  }

  /* Stack info/features cards vertically */
  .info-card,
  .features-card {
    margin-bottom: 16px;
  }
}

@media (max-width: 480px) {
  .welcome-section {
    padding: 20px 16px;
  }

  .welcome-greeting {
    font-size: 18px;
  }

  .action-card {
    padding: 12px 14px;
  }
}
</style>
