<template>
  <AppLayout title="选课">
    <template #actions>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        提交课表
      </el-button>
      <el-button @click="handleSave">仅保存</el-button>
      <el-button type="danger" plain @click="handleDelete">删除课表</el-button>
    </template>

    <el-row :gutter="20">
      <!-- 左侧: 可选课程 -->
      <el-col :span="14">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>可选课程开设</span>
              <el-button text @click="loadOfferings">刷新</el-button>
            </div>
          </template>
          <el-table
            :data="offerings"
            v-loading="loadingOfferings"
            stripe
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="45" />
            <el-table-column prop="courseId" label="课程号" width="90" />
            <el-table-column prop="courseTitle" label="名称" />
            <el-table-column label="时间" width="160">
              <template #default="{ row }">
                {{ row.days }} {{ row.times }}
              </template>
            </el-table-column>
            <el-table-column label="容量" width="100">
              <template #default="{ row }">
                <el-tag :type="capacityTag(row)">
                  {{ row.enrolledCount }}/{{ row.maxCapacity }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="professorName" label="教授" width="100" />
          </el-table>

          <div style="margin-top: 12px">
            <el-radio-group v-model="selectKind">
              <el-radio-button label="PRIMARY">主选</el-radio-button>
              <el-radio-button label="ALTERNATE">备选</el-radio-button>
            </el-radio-group>
            <el-button
              style="margin-left: 12px"
              type="primary"
              plain
              :disabled="selectedToAdd.length === 0"
              @click="handleSelectOfferings"
            >
              加入课表
            </el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧: 我的课表 -->
      <el-col :span="10">
        <el-card>
          <template #header>
            <span>我的课表 (主选 {{ schedule.primaryCount }}/4, 备选 {{ schedule.alternateCount }}/2)</span>
          </template>
          <el-empty v-if="!schedule.items || schedule.items.length === 0"
                    description="课表为空,请选择课程" />
          <el-table v-else :data="schedule.items" size="small">
            <el-table-column prop="courseId" label="课程" width="80" />
            <el-table-column prop="courseTitle" label="名称" />
            <el-table-column label="类型" width="60">
              <template #default="{ row }">
                <el-tag size="small" :type="row.kind === 'PRIMARY' ? 'primary' : 'info'">
                  {{ row.kind === 'PRIMARY' ? '主' : '备' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag size="small" :type="statusTag(row.status)">
                  {{ statusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70">
              <template #default="{ row }">
                <el-button size="small" type="danger" text
                           @click="handleRemoveOffering(row.offeringId)">
                  移除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-alert
            v-if="submitResult"
            :title="submitResult.message"
            :type="submitResult.success ? 'success' : 'warning'"
            show-icon
            :closable="true"
            @close="submitResult = null"
            style="margin-top: 12px"
          >
            <div v-if="submitResult.errors && submitResult.errors.length">
              <div v-for="(e, i) in submitResult.errors" :key="i" style="font-size: 12px">
                <strong>{{ e.courseId }}</strong> [{{ e.errorType }}]: {{ e.message }}
              </div>
            </div>
          </el-alert>
        </el-card>
      </el-col>
    </el-row>
  </AppLayout>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { catalogApi, scheduleApi } from '../api/courses'
import AppLayout from '../components/AppLayout.vue'

const semester = import.meta.env.VITE_CURRENT_SEMESTER || '2026-FALL'
const offerings = ref([])
const loadingOfferings = ref(false)
const selectedToAdd = ref([])
const selectKind = ref('PRIMARY')
const submitting = ref(false)
const submitResult = ref(null)

const schedule = reactive({
  scheduleId: null,
  state: 'DRAFT',
  items: [],
  primaryCount: 0,
  alternateCount: 0
})

onMounted(async () => {
  await Promise.all([loadOfferings(), loadMySchedule()])
})

async function loadOfferings() {
  loadingOfferings.value = true
  try {
    const res = await catalogApi.getOfferings(semester)
    offerings.value = res.data
  } catch (e) {
    ElMessage.error('加载课程列表失败')
  } finally {
    loadingOfferings.value = false
  }
}

async function loadMySchedule() {
  try {
    const res = await scheduleApi.getMySchedule(semester)
    Object.assign(schedule, res.data)
  } catch (e) {
    if (e.response?.status === 404) {
      // 课表不存在,自动创建
      try {
        const res = await scheduleApi.createSchedule(semester)
        Object.assign(schedule, res.data)
      } catch (e2) {
        if (e2.response?.status === 409) {
          await loadMySchedule()
        } else {
          ElMessage.error('创建课表失败')
        }
      }
    }
  }
}

function handleSelectionChange(val) {
  selectedToAdd.value = val
}

async function handleSelectOfferings() {
  const selections = selectedToAdd.value.map(o => ({
    offeringId: o.offeringId,
    kind: selectKind.value
  }))
  try {
    const res = await scheduleApi.selectOfferings(semester, selections)
    Object.assign(schedule, res.data)
    selectedToAdd.value = []
    ElMessage.success('已加入课表')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '加入失败')
  }
}

async function handleRemoveOffering(offeringId) {
  try {
    const res = await scheduleApi.removeOffering(semester, offeringId)
    Object.assign(schedule, res.data)
    ElMessage.success('已移除')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '移除失败')
  }
}

async function handleSubmit() {
  submitting.value = true
  submitResult.value = null
  try {
    const res = await scheduleApi.submitSchedule(semester)
    submitResult.value = res.data
    await loadMySchedule()
    if (res.data.success) {
      ElMessage.success('课表提交成功')
    } else {
      ElMessage.warning('部分课程校验失败,请查看详情')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

async function handleSave() {
  try {
    const res = await scheduleApi.saveSchedule(semester)
    Object.assign(schedule, res.data)
    ElMessage.success('已保存')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  }
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确认删除本学期课表?已注册课程将从名单中移除', '提示', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await scheduleApi.deleteSchedule(semester)
    ElMessage.success('课表已删除')
    // 重新创建空课表
    const res = await scheduleApi.createSchedule(semester)
    Object.assign(schedule, res.data)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

function capacityTag(row) {
  const ratio = row.enrolledCount / row.maxCapacity
  if (ratio >= 1) return 'danger'
  if (ratio >= 0.8) return 'warning'
  return 'success'
}

function statusTag(status) {
  return { SELECTED: 'info', ENROLLED_IN: 'success', CANCELLED: 'danger' }[status] || 'info'
}

function statusLabel(status) {
  return { SELECTED: '已选', ENROLLED_IN: '已注册', CANCELLED: '已取消' }[status] || status
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
