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
          <el-icon><Finished /></el-icon>
          全部标为已读
        </el-button>
      </template>

      <!-- Loading state -->
      <div v-if="loading" class="loading-state">
        <el-skeleton :rows="5" animated />
      </div>

      <!-- Empty state -->
      <el-empty v-else-if="notifications.length === 0" description="暂无通知">
        <template #image>
          <div class="empty-notifications">
            <el-icon :size="56"><Bell /></el-icon>
          </div>
        </template>
      </el-empty>

      <!-- Notification list -->
      <div v-else class="notification-list">
        <div class="notification-summary">
          <span class="summary-text">
            共 {{ notifications.length }} 条通知,
            <strong>{{ unreadCount }}</strong> 条未读
          </span>
        </div>

        <div
          v-for="item in notifications"
          :key="item.id"
          class="notification-card"
          :class="{ unread: !item.read }"
          @click="handleReadNotification(item)"
        >
          <div class="notification-indicator" :class="{ 'indicator-unread': !item.read }"></div>
          <div class="notification-body">
            <div class="notification-header">
              <el-tag
                :type="typeTagType(item.type)"
                size="small"
                effect="dark"
                class="type-tag"
              >
                {{ typeLabel(item.type) }}
              </el-tag>
              <span class="notification-time">{{ formatTime(item.createdAt) }}</span>
            </div>
            <h4 class="notification-title">{{ item.title }}</h4>
            <p class="notification-content">{{ item.content }}</p>
            <div v-if="item.courseId" class="notification-meta">
              <el-tag size="small" type="info" effect="plain">
                <el-icon><Document /></el-icon>
                {{ item.courseId }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>
    </AppLayout>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Finished, Bell, Document } from '@element-plus/icons-vue'
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
  max-width: 640px;
  margin: 0 auto;
}

.notification-list {
  max-width: 680px;
  margin: 0 auto;
}

.notification-summary {
  margin-bottom: 16px;
  padding: 0 4px;
}

.summary-text {
  font-size: 13px;
  color: var(--color-text-muted);
}

.summary-text strong {
  color: var(--color-primary);
  font-weight: 600;
}

.notification-card {
  display: flex;
  gap: 0;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  margin-bottom: 8px;
  cursor: pointer;
  transition: all var(--transition-fast);
  overflow: hidden;
}

.notification-card:hover {
  border-color: var(--color-primary-lighter);
  box-shadow: var(--shadow-sm);
  transform: translateY(-1px);
}

.notification-card.unread {
  border-left: none;
}

.notification-indicator {
  width: 4px;
  min-height: 100%;
  background: transparent;
  flex-shrink: 0;
}

.indicator-unread {
  background: var(--color-primary-lighter);
}

.notification-body {
  flex: 1;
  padding: 16px 20px;
  min-width: 0;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.type-tag {
  border: none;
}

.notification-time {
  font-size: 12px;
  color: var(--color-text-muted);
}

.notification-title {
  margin: 0 0 4px;
  font-size: 15px;
  color: var(--color-text);
}

.notification-card.unread .notification-title {
  font-weight: 600;
}

.notification-content {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 14px;
  line-height: 1.6;
}

.notification-meta {
  margin-top: 10px;
}

.empty-notifications {
  color: var(--color-text-muted);
  opacity: 0.4;
}

/* --- Mobile Responsive --- */
@media (max-width: 768px) {
  .notification-list {
    max-width: 100%;
  }

  .notification-body {
    padding: 12px 14px;
  }

  .notification-header {
    flex-wrap: wrap;
    gap: 6px;
  }

  .notification-title {
    font-size: 14px;
  }

  .notification-content {
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .notification-card {
    margin-bottom: 6px;
  }

  .notification-body {
    padding: 10px 12px;
  }

  .notification-title {
    font-size: 13px;
  }

  .type-tag {
    font-size: 11px;
  }
}
</style>
