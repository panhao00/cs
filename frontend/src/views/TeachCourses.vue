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
          <span>本学期可授课开设</span>
          <el-button text @click="loadOfferings">刷新</el-button>
        </div>
      </template>
      <el-table
        ref="tableRef"
        :data="offerings"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        stripe
      >
        <el-table-column type="selection" width="45" />
        <el-table-column prop="courseId" label="课程号" width="100" />
        <el-table-column prop="courseTitle" label="名称" />
        <el-table-column prop="department" label="院系" width="150" />
        <el-table-column label="时间" width="160">
          <template #default="{ row }">
            {{ row.days }} {{ row.times }}
          </template>
        </el-table-column>
        <el-table-column prop="room" label="教室" width="90" />
        <el-table-column label="已选" width="80">
          <template #default="{ row }">
            <el-tag :type="row.selectedByMe ? 'success' : 'info'" size="small">
              {{ row.selectedByMe ? '我教' : '未选' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="选课人数" width="100">
          <template #default="{ row }">
            {{ row.enrolledCount }}/{{ row.maxCapacity }}
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
      style="margin-top: 12px"
    >
      <div v-for="(c, i) in conflicts" :key="i" style="font-size: 12px">
        {{ c.courseId1 }} ↔ {{ c.courseId2 }}: {{ c.message }}
      </div>
    </el-alert>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
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
    // 默认选中已选授课(让 el-table 显示勾选状态)
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
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
