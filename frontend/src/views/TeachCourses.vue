<template>
  <AppLayout :title="'选课授课 - ' + semester">
    <template #actions>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        提交授课选择
      </el-button>
    </template>

    <el-card>
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon><Reading /></el-icon>
            <span>本学期可授课开设</span>
          </div>
          <el-button text @click="loadOfferings">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <el-skeleton v-if="loading" :rows="6" animated />

      <el-empty v-else-if="offerings.length === 0" description="暂无可授课开设">
        <template #image>
          <div class="empty-icon">
            <el-icon :size="48"><Reading /></el-icon>
          </div>
        </template>
      </el-empty>

      <el-table
        v-else
        ref="tableRef"
        :data="offerings"
        @selection-change="handleSelectionChange"
        stripe
        class="refined-table"
      >
        <el-table-column type="selection" width="45" />
        <el-table-column prop="courseId" label="课程号" width="100" />
        <el-table-column prop="courseTitle" label="名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="department" label="院系" width="150" />
        <el-table-column label="时间" width="160">
          <template #default="{ row }">
            <span class="time-cell">{{ row.days }} {{ row.times }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="room" label="教室" width="90" />
        <el-table-column label="已选" width="80">
          <template #default="{ row }">
            <el-tag :type="row.selectedByMe ? 'success' : 'info'" size="small" effect="dark">
              {{ row.selectedByMe ? '我教' : '未选' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="选课人数" width="100">
          <template #default="{ row }">
            <div class="capacity-cell">
              <span class="capacity-num">{{ row.enrolledCount }}/{{ row.maxCapacity }}</span>
              <div class="capacity-bar">
                <div
                  class="capacity-fill"
                  :class="capacityBarClass(row)"
                  :style="{ width: Math.min((row.enrolledCount / row.maxCapacity) * 100, 100) + '%' }"
                ></div>
              </div>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-alert
      v-if="conflicts && conflicts.length"
      title="存在时间冲突"
      type="error"
      show-icon
      :closable="true"
      class="conflict-alert"
    >
      <div v-for="(c, i) in conflicts" :key="i" class="conflict-detail">
        {{ c.courseId1 }} &#8596; {{ c.courseId2 }}: {{ c.message }}
      </div>
    </el-alert>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Reading, Refresh } from '@element-plus/icons-vue'
import { teachingApi } from '../api/teaching'
import AppLayout from '../components/AppLayout.vue'

const semester = import.meta.env.VITE_CURRENT_SEMESTER || '2026-FALL'
const offerings = ref([])
const selectedRows = ref([])
const loading = ref(false)
const submitting = ref(false)
const conflicts = ref([])
const tableRef = ref(null)

onMounted(async () => {
  await loadOfferings()
})

async function loadOfferings() {
  loading.value = true
  try {
    const res = await teachingApi.getOfferings(semester)
    offerings.value = res.data
    selectedRows.value = res.data.filter(o => o.selectedByMe)
    nextTick(() => {
      if (tableRef.value && selectedRows.value) {
        selectedRows.value.forEach(row => {
          const found = offerings.value.find(o => o.offeringId === row.offeringId)
          if (found) tableRef.value.toggleRowSelection(found, true)
        })
      }
    })
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function handleSelectionChange(val) {
  selectedRows.value = val
}

function capacityBarClass(row) {
  const ratio = row.enrolledCount / row.maxCapacity
  if (ratio >= 1) return 'bar-full'
  if (ratio >= 0.8) return 'bar-high'
  return 'bar-normal'
}

async function handleSubmit() {
  submitting.value = true
  conflicts.value = []
  try {
    const ids = selectedRows.value.map(o => o.offeringId)
    const res = await teachingApi.selectCourses(semester, ids)
    if (res.data.success) {
      ElMessage.success('授课选择已更新')
      await loadOfferings()
    } else {
      conflicts.value = res.data.conflicts || []
      ElMessage.warning('存在时间冲突,请调整')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 14px;
}

.time-cell {
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--color-text-secondary);
}

.capacity-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.capacity-num {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text);
}

.capacity-bar {
  height: 4px;
  background: var(--color-border-light);
  border-radius: 2px;
  overflow: hidden;
}

.capacity-fill {
  height: 100%;
  border-radius: 2px;
  transition: width var(--transition-slow);
}

.bar-normal {
  background: var(--color-success);
}

.bar-high {
  background: var(--color-warning);
}

.bar-full {
  background: var(--color-danger);
}

.conflict-alert {
  margin-top: 16px;
}

.conflict-detail {
  font-size: 12px;
  padding: 2px 0;
  line-height: 1.6;
}

.empty-icon {
  color: var(--color-text-muted);
  opacity: 0.5;
}
</style>
