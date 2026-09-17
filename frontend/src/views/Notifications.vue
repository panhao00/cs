<template>
  <div class="notifications-page">
    <AppLayout title="通知中心">
      <template #actions>
        <el-button
          v-if="notifications.length > 0"
          type="primary"
          text
          @click="handleMarkAllAsRead"
          :disabled="unreadCount === 0"
        >
          全部标为已读
        </el-button>
      </template>
        <div v-if="loading" class="loading-state">
          <el-skeleton :rows="5" animated />
        </div>

        <el-empty v-else-if="notifications.length === 0" description="暂无通知" />

        <div v-else class="notification-list">
          <el-card
            v-for="item in notifications"
            :key="item.id"
            class="notification-card"
            :class="{ unread: !item.read }"
            shadow="hover"
            @click="handleReadNotification(item)"
          >
            <div class="notification-header">
              <el-tag
                :type="typeTagType(item.type)"
                size="small"
                effect="dark"
              >
                {{ typeLabel(item.type) }}
              </el-tag>
              <span class="notification-time">{{ formatTime(item.createdAt) }}</span>
            </div>
            <h4 class="notification-title">{{ item.title }}</h4>
            <p class="notification-content">{{ item.content }}</p>
            <div v-if="item.courseId" class="notification-meta">
              <el-tag size="small" type="info">{{ item.courseId }}</el-tag>
            </div>
          </el-card>
        </div>
    </AppLayout>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import AppLayout from '../components/AppLayout.vue'
import { notificationApi } from '../api/notifications'

const notifications = ref([])
const loading = ref(false)

const unreadCount = computed(() =>
  notifications.value.filter(n => !n.read).length
)

onMounted(async () => {
  await loadNotifications()
})

async function loadNotifications() {
  loading.value = true
  try {
    const res = await notificationApi.getAll()
    notifications.value = res.data
  } catch (e) {
    ElMessage.error('加载通知失败')
  } finally {
    loading.value = false
  }
}

async function handleReadNotification(item) {
  if (item.read) return
  try {
    await notificationApi.markAsRead(item.id)
    item.read = true
  } catch (e) {
    ElMessage.error('标记已读失败')
  }
}

async function handleMarkAllAsRead() {
  try {
    await notificationApi.markAllAsRead()
    notifications.value.forEach(n => { n.read = true })
    ElMessage.success('已全部标为已读')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

function typeTagType(type) {
  const map = {
    OFFERING_CANCELLED: 'danger',
    ENROLLMENT_FAILED: 'danger',
    ENROLLMENT_SUCCESS: 'success',
    GRADE_POSTED: 'success',
    REGISTRATION_CLOSED: 'primary',
    SYSTEM: 'info'
  }
  return map[type] || 'info'
}

function typeLabel(type) {
  const map = {
    OFFERING_CANCELLED: '课程取消',
    ENROLLMENT_FAILED: '选课失败',
    ENROLLMENT_SUCCESS: '选课成功',
    GRADE_POSTED: '成绩录入',
    REGISTRATION_CLOSED: '注册关闭',
    SYSTEM: '系统通知'
  }
  return map[type] || type
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.loading-state {
  max-width: 600px;
  margin: 0 auto;
}

.notification-list {
  max-width: 600px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notification-card {
  cursor: pointer;
  transition: border-color 0.2s;
}

.notification-card.unread {
  border-left: 3px solid #409eff;
  background-color: #f0f6ff;
}

.notification-card.unread .notification-title {
  font-weight: bold;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.notification-time {
  font-size: 12px;
  color: #909399;
}

.notification-title {
  margin: 0 0 4px 0;
  color: #303133;
}

.notification-content {
  margin: 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
}

.notification-meta {
  margin-top: 8px;
}
</style>
