<template>
  <AppLayout title="操作日志">
    <!-- Filter bar -->
    <el-card class="filter-card" shadow="never">
      <div class="filter-bar">
        <el-select
          v-model="filters.action"
          placeholder="操作类型"
          clearable
          style="width: 180px"
        >
          <el-option
            v-for="(label, key) in actionLabels"
            :key="key"
            :label="label"
            :value="key"
          />
        </el-select>
        <el-input
          v-model="filters.keyword"
          placeholder="搜索目标类型/操作者"
          clearable
          style="width: 200px"
          @input="handleFilter"
        />
        <el-button type="primary" @click="loadLogs(0)">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
      </div>
    </el-card>

    <!-- Table -->
    <el-card>
      <el-skeleton v-if="loading" :rows="8" animated />

      <el-table v-else :data="logs" stripe class="refined-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="actorName" label="操作者" width="120" />
        <el-table-column prop="action" label="操作类型" width="140">
          <template #default="{ row }">
            <el-tag size="small" :type="actionTagType(row.action)">
              {{ actionLabels[row.action] || row.action }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetType" label="目标类型" width="120" />
        <el-table-column prop="targetId" label="目标ID" width="100" />
        <el-table-column prop="details" label="详情" min-width="180" show-overflow-tooltip />
<el-table-column prop="createdAt" label="时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && logs.length === 0" description="暂无日志记录" />

      <div class="pagination-wrapper" v-if="totalElements > 0">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="totalElements"
          layout="total, prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </AppLayout>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import { auditApi } from '../api/audit'

const logs = ref([])
const loading = ref(true)
const currentPage = ref(1)
const pageSize = 20
const totalElements = ref(0)

const filters = reactive({
  action: '',
  keyword: ''
})

const actionLabels = {
  LOGIN_SUCCESS: '登录成功',
  LOGIN_FAILED: '登录失败',
  GRADE_ENTRY: '成绩录入',
  SCHEDULE_SUBMIT: '课表提交',
  SCHEDULE_CREATE: '课表创建',
  COURSE_SELECT: '选课操作',
  COURSE_DROP: '退课操作',
  REGISTRATION_CLOSE: '关闭注册',
  USER_CREATE: '用户创建',
  USER_UPDATE: '用户更新',
  USER_DELETE: '用户删除',
  USER_STATUS_CHANGE: '用户状态变更',
  PASSWORD_CHANGE: '密码修改',
  SETTINGS_UPDATE: '设置更新'
}

function actionTagType(action) {
  if (action?.includes('SUCCESS') || action === 'GRADE_ENTRY') return 'success'
  if (action?.includes('FAILED') || action === 'USER_DELETE') return 'danger'
  if (action?.includes('SUBMIT') || action?.includes('SELECT')) return 'primary'
  return 'info'
}

function formatTime(timestamp) {
  if (!timestamp) return '-'
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

function handleFilter() {
  loadLogs(0)
}

async function loadLogs(page = 0) {
  loading.value = true
  try {
    const params = {
      page,
      size: pageSize
    }
    if (filters.action) params.action = filters.action
    const res = await auditApi.getLogs(params)
    const data = res.data
    if (Array.isArray(data)) {
      logs.value = data
      totalElements.value = data.length
    } else {
      logs.value = data.content || data.records || []
      totalElements.value = data.totalElements || data.total || 0
    }
    currentPage.value = page + 1
  } catch (e) {
    ElMessage.error('加载操作日志失败')
  } finally {
    loading.value = false
  }
}

function handlePageChange(page) {
  loadLogs(page - 1)
}

onMounted(() => {
  loadLogs(0)
})
</script>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 767px) {
  .filter-bar {
    flex-wrap: wrap;
  }

  .filter-bar .el-select,
  .filter-bar .el-input {
    width: 100% !important;
    min-width: 0;
  }

  .el-table {
    overflow-x: auto;
  }
  .el-card__header {
    padding: 12px 16px;
  }
  .el-card__body {
    padding: 12px 16px;
  }
}
</style>
