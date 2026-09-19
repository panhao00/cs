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
              <div class="card-title">
                <el-icon><DocumentAdd /></el-icon>
                <span>可选课程开设</span>
              </div>
              <el-button text @click="loadOfferings">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </template>

          <el-skeleton v-if="loadingOfferings" :rows="6" animated />

          <el-empty v-else-if="offerings.length === 0" description="暂无可选课程">
            <template #image>
              <div class="empty-icon">
                <el-icon :size="48"><Document /></el-icon>
              </div>
            </template>
          </el-empty>

          <el-table
            v-else
            :data="offerings"
            stripe
            @selection-change="handleSelectionChange"
            class="refined-table"
          >
            <el-table-column type="selection" width="45" />
            <el-table-column prop="courseId" label="课程号" width="90" />
            <el-table-column prop="courseTitle" label="名称" min-width="120" show-overflow-tooltip />
            <el-table-column label="时间" width="160">
              <template #default="{ row }">
                <span class="time-cell">{{ row.days }} {{ row.times }}</span>
              </template>
            </el-table-column>
            <el-table-column label="容量" width="100">
              <template #default="{ row }">
                <el-tag :type="capacityTag(row)" size="small" effect="dark">
                  {{ row.enrolledCount }}/{{ row.maxCapacity }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="professorName" label="教授" width="100" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" text @click="viewDetail(row)">
                  详情
                </el-button>
                <el-button
                  v-if="row.enrolledCount >= row.maxCapacity"
                  size="small"
                  type="warning"
                  text
                  @click="handleJoinWaitlist(row)"
                >
                  候补
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="table-actions">
            <el-radio-group v-model="selectKind" size="default">
              <el-radio-button label="PRIMARY">主选</el-radio-button>
              <el-radio-button label="ALTERNATE">备选</el-radio-button>
            </el-radio-group>
            <el-button
              type="primary"
              plain
              :disabled="selectedToAdd.length === 0"
              @click="handleSelectOfferings"
            >
              <el-icon><Plus /></el-icon>
              加入课表
            </el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧: 我的课表 -->
      <el-col :span="10">
        <el-card>
          <template #header>
            <div class="card-header">
              <div class="card-title">
                <el-icon><List /></el-icon>
                <span>我的课表</span>
                <div class="schedule-counts">
                  <el-tag type="primary" size="small">主选 {{ schedule.primaryCount }}/4</el-tag>
                  <el-tag type="info" size="small">备选 {{ schedule.alternateCount }}/2</el-tag>
                </div>
              </div>
              <el-button-group v-if="schedule.items && schedule.items.length > 0">
                <el-button :type="viewMode === 'list' ? 'primary' : ''" @click="viewMode = 'list'" size="small">
                  列表
                </el-button>
                <el-button :type="viewMode === 'timetable' ? 'primary' : ''" @click="viewMode = 'timetable'" size="small">
                  课表
                </el-button>
              </el-button-group>
            </div>
          </template>

          <el-empty
            v-if="!schedule.items || schedule.items.length === 0"
            description="课表为空,请选择课程"
          >
            <template #image>
              <div class="empty-icon schedule-empty">
                <el-icon :size="48"><Calendar /></el-icon>
              </div>
            </template>
          </el-empty>

          <ScheduleTimetable
            v-else-if="viewMode === 'timetable'"
            :items="schedule.items"
            @conflicts-detected="detectedConflicts = $event"
          />

          <el-table v-else :data="schedule.items" size="small" class="refined-table">
            <el-table-column prop="courseId" label="课程" width="80" />
            <el-table-column prop="courseTitle" label="名称" min-width="100" show-overflow-tooltip />
            <el-table-column label="类型" width="60">
              <template #default="{ row }">
                <el-tag
                  size="small"
                  :type="row.kind === 'PRIMARY' ? 'primary' : 'info'"
                  effect="dark"
                >
                  {{ row.kind === 'PRIMARY' ? '主' : '备' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag size="small" :type="statusTag(row.status)" effect="dark">
                  {{ statusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70">
              <template #default="{ row }">
                <el-button
                  size="small"
                  type="danger"
                  text
                  @click="handleRemoveOffering(row.offeringId)"
                >
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
            class="submit-alert"
          >
            <div v-if="submitResult.errors && submitResult.errors.length">
              <div v-for="(e, i) in submitResult.errors" :key="i" class="error-detail">
                <strong>{{ e.courseId }}</strong> [{{ e.errorType }}]: {{ e.message }}
              </div>
            </div>
          </el-alert>
        </el-card>
      </el-col>
    </el-row>

    <!-- Offering Detail Dialog -->
    <el-dialog v-model="showDetail" title="课程开设详情" width="520px">
      <el-descriptions v-if="detailOffering" :column="2" border>
        <el-descriptions-item label="课程号">{{ detailOffering.courseId }}</el-descriptions-item>
        <el-descriptions-item label="课程名称">{{ detailOffering.courseTitle }}</el-descriptions-item>
        <el-descriptions-item label="授课教授">{{ detailOffering.professorName }}</el-descriptions-item>
        <el-descriptions-item label="上课时间">{{ detailOffering.days }} {{ detailOffering.times }}</el-descriptions-item>
        <el-descriptions-item label="教室">{{ detailOffering.room || '-' }}</el-descriptions-item>
        <el-descriptions-item label="选课容量">
          {{ detailOffering.enrolledCount }}/{{ detailOffering.maxCapacity }}
        </el-descriptions-item>
        <el-descriptions-item label="状态" :span="2">
          <el-tag :type="detailOffering.status === 'ACTIVE' ? 'success' : 'info'" size="small">
            {{ detailOffering.status || '-' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" plain @click="goToReviews(detailOffering)">
          <el-icon><ChatDotRound /></el-icon>
          查看评价
        </el-button>
        <el-button @click="showDetail = false">关闭</el-button>
      </template>
    </el-dialog>
  </AppLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  DocumentAdd, Refresh, Plus, List, Calendar, Document, ChatDotRound
} from '@element-plus/icons-vue'
import { catalogApi, scheduleApi } from '../api/courses'
import { waitlistApi } from '../api/waitlist'
import AppLayout from '../components/AppLayout.vue'
import ScheduleTimetable from '../components/ScheduleTimetable.vue'
import { useAuthStore } from '../stores/auth'
import { useWebSocket } from '../composables/useWebSocket'

const auth = useAuthStore()
const router = useRouter()
const semester = import.meta.env.VITE_CURRENT_SEMESTER || '2026-FALL'

// WebSocket for real-time capacity notifications
const userId = computed(() => auth.user?.userId)
const wsToken = computed(() => auth.token)
const { connect: connectWs, disconnect: disconnectWs } = useWebSocket({
  userId,
  token: wsToken
})

// Connect on login, disconnect on logout
watch(
  () => auth.isLoggedIn,
  (loggedIn) => {
    if (loggedIn && userId.value) {
      connectWs()
    } else {
      disconnectWs()
    }
  },
  { immediate: true }
)
const offerings = ref([])
const loadingOfferings = ref(false)
const selectedToAdd = ref([])
const selectKind = ref('PRIMARY')
const submitting = ref(false)
const submitResult = ref(null)
const viewMode = ref('list')
const detectedConflicts = ref([])

const schedule = reactive({
  scheduleId: null,
  state: 'DRAFT',
  items: [],
  primaryCount: 0,
  alternateCount: 0
})

const showDetail = ref(false)
const detailOffering = ref(null)

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
    const res = await scheduleApi.createSchedule(semester)
    Object.assign(schedule, res.data)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

function capacityTag(row) {
  const ratio = row.maxCapacity > 0 ? row.enrolledCount / row.maxCapacity : 1
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

async function viewDetail(offering) {
  try {
    const res = await catalogApi.getOffering(offering.offeringId)
    detailOffering.value = res.data
    showDetail.value = true
  } catch (e) {
    ElMessage.error('加载课程详情失败')
  }
}

async function handleJoinWaitlist(row) {
  try {
    await ElMessageBox.confirm(
      `确定加入 "${row.courseTitle}" 的候补名单？`,
      '加入候补',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
    )
    await waitlistApi.joinWaitlist(row.offeringId)
    ElMessage.success('已加入候补名单')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '加入候补失败')
  }
}

function goToReviews(offering) {
  showDetail.value = false
  router.push(`/reviews/${offering.offeringId}`)
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

.schedule-counts {
  display: flex;
  gap: 6px;
  margin-left: 8px;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border-light);
}

.time-cell {
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--color-text-secondary);
}

.submit-alert {
  margin-top: 16px;
}

.error-detail {
  font-size: 12px;
  padding: 2px 0;
  line-height: 1.6;
}

.empty-icon {
  color: var(--color-text-muted);
  opacity: 0.5;
}

.schedule-empty {
  color: var(--color-primary-lighter);
}

/* --- Mobile Responsive --- */
@media (max-width: 768px) {
  /* Stack columns vertically */
  .el-row .el-col {
    display: block;
    width: 100% !important;
    max-width: 100% !important;
    flex: 0 0 100% !important;
    padding-left: 0 !important;
    padding-right: 0 !important;
    margin-bottom: var(--space-4);
  }

  /* Scrollable tables */
  .refined-table {
    display: block;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  /* Card header */
  .card-header {
    flex-wrap: wrap;
    gap: 8px;
  }

  /* Table actions - stack */
  .table-actions {
    flex-wrap: wrap;
    gap: 8px;
  }

  /* Schedule counts */
  .schedule-counts {
    margin-left: 0;
    margin-top: 4px;
  }

  .card-title {
    flex-wrap: wrap;
  }
}
</style>
