<template>
  <div class="dashboard">
    <AppLayout title="Dashboard">
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
      <h2 class="section-title">Quick Actions</h2>
      <el-row :gutter="16">
        <template v-if="auth.role === 'STUDENT'">
          <el-col :xs="24" :sm="12" :md="8">
            <div class="action-card" @click="$router.push('/register')">
              <div class="action-icon student">
                <el-icon :size="24"><DocumentAdd /></el-icon>
              </div>
              <div class="action-info">
                <h3>选课</h3>
                <p>Register for Courses</p>
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
                <p>View Report Card</p>
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
                <p>Select Courses to Teach</p>
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
                <p>Submit Grades</p>
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
                <p>Maintain Users</p>
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
                <p>Close Registration</p>
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
              <p>Notifications</p>
            </div>
            <el-icon class="action-arrow"><ArrowRight /></el-icon>
          </div>
        </el-col>
      </el-row>

      <!-- Account Info & Features -->
      <el-row :gutter="16" style="margin-top: 24px">
        <el-col :xs="24" :md="10">
          <el-card class="info-card">
            <template #header>
              <div class="card-title-row">
                <el-icon><InfoFilled /></el-icon>
                <span>Account Information</span>
              </div>
            </template>
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">User ID</span>
                <span class="info-value">{{ auth.user?.userId }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">Username</span>
                <span class="info-value">{{ auth.user?.username }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">Full Name</span>
                <span class="info-value">{{ auth.user?.name }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">Role</span>
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
                <span>System Features</span>
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  DocumentAdd, DataAnalysis, Reading, EditPen, User, Lock, Bell,
  ArrowRight, InfoFilled, CircleCheck, CircleCheckFilled, CircleClose
} from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import { useAuthStore } from '../stores/auth'
import { notificationApi } from '../api/notifications'

const router = useRouter()
const auth = useAuthStore()
const unreadCount = ref(0)
let refreshTimer = null

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
  if (hour < 6) return 'Good Night'
  if (hour < 12) return 'Good Morning'
  if (hour < 18) return 'Good Afternoon'
  return 'Good Evening'
})

const roleDescription = computed(() => {
  const map = {
    STUDENT: 'Manage your course registrations and view academic progress',
    PROFESSOR: 'Select courses to teach and manage student grades',
    REGISTRAR: 'Manage users and control the registration process'
  }
  return map[auth.role] || 'Welcome to the Course Registration System'
})

const features = [
  { name: 'Login', status: 'Active', done: true },
  { name: 'Student Registration', status: 'Active', done: true },
  { name: 'Teaching Selection', status: 'Active', done: true },
  { name: 'Grade Submission', status: 'Active', done: true },
  { name: 'Report Card', status: 'Active', done: true },
  { name: 'User Management', status: 'Active', done: true },
  { name: 'Close Registration', status: 'Active', done: true }
]

async function refreshUnreadCount() {
  try {
    const res = await notificationApi.getUnreadCount()
    unreadCount.value = res.data.count
  } catch {
    // 静默失败
  }
}

onMounted(() => {
  refreshUnreadCount()
  refreshTimer = setInterval(refreshUnreadCount, 30000)
})

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
</style>
