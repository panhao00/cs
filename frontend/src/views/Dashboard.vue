<template>
  <div class="dashboard">
    <AppLayout title="课程注册系统 CRS">
      <template #actions>
        <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notification-badge">
          <el-button :icon="Bell" circle @click="$router.push('/notifications')" />
        </el-badge>
        <el-dropdown @command="handleCommand" style="margin-left: 16px">
          <span class="user-info">
            {{ auth.user?.name }} ({{ auth.role }})
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-card>
              <template #header>
                <span>系统概览</span>
              </template>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="用户ID">
                  {{ auth.user?.userId }}
                </el-descriptions-item>
                <el-descriptions-item label="用户名">
                  {{ auth.user?.username }}
                </el-descriptions-item>
                <el-descriptions-item label="姓名">
                  {{ auth.user?.name }}
                </el-descriptions-item>
                <el-descriptions-item label="角色">
                  <el-tag :type="roleTagType">{{ roleLabel }}</el-tag>
                </el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" style="margin-top: 20px">
          <el-col :span="12">
            <el-card>
              <template #header>
                <span>快捷操作</span>
              </template>
              <div v-if="auth.role === 'STUDENT'">
                <el-button type="primary" size="large" @click="$router.push('/register')">
                  选课 (Register for Courses)
                </el-button>
                <el-button size="large" @click="$router.push('/report-card')" style="margin-left: 12px">
                  查看成绩单
                </el-button>
              </div>
              <div v-else-if="auth.role === 'PROFESSOR'">
                <el-button type="primary" size="large" @click="$router.push('/teach')">
                  选课授课 (Select to Teach)
                </el-button>
                <el-button size="large" @click="$router.push('/grades')" style="margin-left: 12px">
                  提交成绩
                </el-button>
              </div>
              <div v-else-if="auth.role === 'REGISTRAR'">
                <el-button type="primary" size="large" @click="$router.push('/users')">
                  维护学生/教授 (Maintain Users)
                </el-button>
                <el-button size="large" @click="$router.push('/registration')" style="margin-left: 12px">
                  关闭注册 (Close Registration)
                </el-button>
              </div>
              <div v-else>
                <el-tag type="info">该角色专属功能开发中</el-tag>
              </div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card>
              <template #header>
                <span>功能进度</span>
              </template>
              <el-timeline>
                <el-timeline-item
                  v-for="(item, idx) in features"
                  :key="idx"
                  :type="item.done ? 'success' : 'info'"
                  :timestamp="item.status"
                >
                  {{ item.name }}
                </el-timeline-item>
              </el-timeline>
            </el-card>
          </el-col>
        </el-row>
    </AppLayout>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown, Bell } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import { useAuthStore } from '../stores/auth'
import { notificationApi } from '../api/notifications'

const router = useRouter()
const auth = useAuthStore()
const unreadCount = ref(0)
let refreshTimer = null

const roleLabel = computed(() => {
  const map = {
    STUDENT: '学生',
    PROFESSOR: '教授',
    REGISTRAR: '注册主任'
  }
  return map[auth.role] || auth.role
})

const roleTagType = computed(() => {
  const map = {
    STUDENT: 'warning',
    PROFESSOR: 'success',
    REGISTRAR: 'primary'
  }
  return map[auth.role] || 'info'
})

const features = [
  { name: 'Login 登录', status: '已完成', done: true },
  { name: 'Register for Courses 学生选课', status: '已完成', done: true },
  { name: 'Select Courses to Teach 教授选课授课', status: '已完成', done: true },
  { name: 'Submit Grades 提交成绩', status: '已完成', done: true },
  { name: 'View Report Card 查看成绩单', status: '已完成', done: true },
  { name: 'Maintain Student/Professor 信息维护', status: '已完成', done: true },
  { name: 'Close Registration 关闭注册', status: '已完成', done: true }
]

async function refreshUnreadCount() {
  try {
    const res = await notificationApi.getUnreadCount()
    unreadCount.value = res.data.count
  } catch {
    // 静默失败,不影响用户体验
  }
}

onMounted(() => {
  refreshUnreadCount()
  refreshTimer = setInterval(refreshUnreadCount, 30000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})

function handleCommand(command) {
  if (command === 'logout') {
    auth.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.user-info {
  cursor: pointer;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
