<template>
  <AppLayout title="候补名单">
    <template #actions>
      <el-button @click="loadMyWaitlists" :loading="loading">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </template>

    <el-card>
      <template #header>
        <div class="card-title">
          <el-icon><Clock /></el-icon>
          <span>我的候补列表</span>
          <el-tag size="small" type="info" effect="dark">
            {{ waitlists.length }} 门课程
          </el-tag>
        </div>
      </template>

      <el-skeleton v-if="loading" :rows="4" animated />

      <el-empty v-else-if="waitlists.length === 0" description="暂无候补课程">
        <template #image>
          <div class="empty-icon">
            <el-icon :size="48"><Clock /></el-icon>
          </div>
        </template>
      </el-empty>

      <el-table v-else :data="waitlists" class="refined-table" stripe>
        <el-table-column prop="courseId" label="课程号" width="100" />
        <el-table-column prop="courseTitle" label="课程名称" min-width="150" show-overflow-tooltip />
        <el-table-column label="排队位置" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="warning" effect="dark" size="small">
              第 {{ row.position }} 位
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.status)" effect="dark">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="加入时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-popconfirm
              title="确认取消候补?"
              confirm-button-text="取消候补"
              cancel-button-text="返回"
              @confirm="handleCancel(row)"
            >
              <template #reference>
                <el-button size="small" type="danger" text :disabled="row.status !== 'WAITING'">
                  取消候补
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 候补说明 -->
    <el-card class="info-card" shadow="never">
      <template #header>
        <div class="card-title">
          <el-icon><InfoFilled /></el-icon>
          <span>候补规则说明</span>
        </div>
      </template>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="加入候补">
          当课程满员时,您可以加入候补名单排队等待
        </el-descriptions-item>
        <el-descriptions-item label="递补机制">
          当有学生退选时,系统按排队位置自动递补注册
        </el-descriptions-item>
        <el-descriptions-item label="递补条件">
          课程仍在开放状态、仍有容量、您满足先修课要求
        </el-descriptions-item>
        <el-descriptions-item label="递补通知">
          递补成功后,系统将通过通知中心告知您
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Clock, InfoFilled } from '@element-plus/icons-vue'
import { waitlistApi } from '../api/waitlist'
import AppLayout from '../components/AppLayout.vue'

const loading = ref(false)
const waitlists = ref([])

onMounted(() => {
  loadMyWaitlists()
})

async function loadMyWaitlists() {
  loading.value = true
  try {
    const res = await waitlistApi.getMyWaitlists()
    waitlists.value = res.data
  } catch {
    ElMessage.error('加载候补列表失败')
  } finally {
    loading.value = false
  }
}

async function handleCancel(row) {
  try {
    await waitlistApi.cancelWaitlist(row.offeringId)
    ElMessage.success('已取消候补')
    await loadMyWaitlists()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '取消候补失败')
  }
}

function statusTag(status) {
  return { WAITING: 'warning', ENROLLED: 'success', CANCELLED: 'info' }[status] || 'info'
}

function statusLabel(status) {
  return { WAITING: '等待中', ENROLLED: '已递补', CANCELLED: '已取消' }[status] || status
}

function formatTime(isoString) {
  if (!isoString) return '-'
  const d = new Date(isoString)
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
.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 14px;
}

.empty-icon {
  color: var(--color-text-muted);
  opacity: 0.5;
}

.info-card {
  margin-top: 16px;
}

@media (max-width: 767px) {
  .el-table {
    overflow-x: auto;
  }
  .el-card__header {
    padding: 12px 16px;
  }
  .el-card__body {
    padding: 12px 16px;
  }

  .card-title {
    flex-wrap: wrap;
    gap: 6px;
  }
}
</style>
