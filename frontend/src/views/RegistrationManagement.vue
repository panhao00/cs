<template>
  <div class="reg-mgmt">
    <AppLayout title="注册管理">
      <template #actions>
        <el-tag :type="phaseTagType" effect="dark" size="small">
          <span class="phase-dot" :class="phase === 'closed' ? 'dot-closed' : 'dot-open'"></span>
          {{ phaseLabel }}
        </el-tag>
      </template>

      <!-- Status Card -->
      <el-card class="status-card">
        <template #header>
          <div class="card-title">
            <el-icon><InfoFilled /></el-icon>
            <span>当前注册阶段</span>
          </div>
        </template>
        <div class="status-content">
          <div class="status-item">
            <span class="status-label">学期</span>
            <span class="status-value">{{ status.semester }}</span>
          </div>
          <div class="status-item">
            <span class="status-label">阶段</span>
            <el-tag :type="phaseTagType" effect="dark">
              <span class="phase-dot-inline" :class="phase === 'closed' ? 'dot-closed' : 'dot-open'"></span>
              {{ phaseLabel }}
            </el-tag>
          </div>
        </div>
        <div class="status-actions">
          <el-button
            type="primary"
            :disabled="status.phase === 'closed'"
            :loading="closing"
            @click="closeRegistration"
          >
            <el-icon><Lock /></el-icon>
            关闭注册
          </el-button>
          <el-button
            type="warning"
            :disabled="status.canAdvance !== 'true' || status.phase === 'closed'"
            :loading="advancing"
            @click="advancePhase"
          >
            <el-icon><Right /></el-icon>
            推进阶段
          </el-button>
          <el-button :loading="loading" @click="loadPreview">
            <el-icon><Refresh /></el-icon>
            刷新预览
          </el-button>
        </div>
      </el-card>

      <!-- Preview / Results -->
      <el-card class="preview-card">
        <template #header>
          <div class="card-title">
            <el-icon><DataAnalysis /></el-icon>
            <span>课程开设容量统计</span>
          </div>
        </template>

        <el-skeleton v-if="loading" :rows="4" animated />

        <template v-else-if="preview">
          <div class="stats-row">
            <div class="stat-item">
              <div class="stat-number">{{ preview.totalOfferings }}</div>
              <div class="stat-label">总开设数</div>
            </div>
            <div class="stat-item stat-success">
              <div class="stat-number">{{ preview.fullOfferings }}</div>
              <div class="stat-label">满足最小人数</div>
            </div>
            <div class="stat-item stat-warning">
              <div class="stat-number">{{ preview.underFilledOfferings }}</div>
              <div class="stat-label">未满 (将取消)</div>
            </div>
          </div>

          <el-table
            v-if="preview.offerings && preview.offerings.length"
            :data="preview.offerings"
            stripe
            class="refined-table"
          >
            <el-table-column prop="courseId" label="课程" width="120" />
            <el-table-column prop="enrolledCount" label="已选人数" width="120" />
            <el-table-column prop="minRequired" label="最小人数" width="120" />
            <el-table-column label="是否满足" width="120">
              <template #default="{ row }">
                <el-tag :type="row.meetsMinimum ? 'success' : 'warning'" effect="dark">
                  {{ row.meetsMinimum ? '满足' : '未满' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="说明" min-width="150" show-overflow-tooltip />
          </el-table>

          <el-alert
            v-if="preview.message"
            :title="preview.message"
            :type="preview.billingNotificationSent ? 'success' : 'warning'"
            :closable="false"
            show-icon
            class="preview-alert"
          />
        </template>
      </el-card>
    </AppLayout>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { InfoFilled, Lock, Refresh, DataAnalysis, Right } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import { registrationApi } from '../api/users'

const loading = ref(false)
const closing = ref(false)
const advancing = ref(false)
const status = ref({ phase: 'open', semester: '' })
const preview = ref(null)

const phase = computed(() => status.value.phase)

const phaseLabel = computed(() =>
  status.value.phase === 'closed' ? '已关闭' : '开放中'
)

const phaseTagType = computed(() =>
  status.value.phase === 'closed' ? 'info' : 'success'
)

async function loadStatus() {
  try {
    const { data } = await registrationApi.getStatus()
    status.value = data
  } catch (e) {
    ElMessage.error('加载状态失败: ' + (e.response?.data?.message || e.message))
  }
}

async function loadPreview() {
  loading.value = true
  try {
    const { data } = await registrationApi.preview()
    preview.value = data
  } catch (e) {
    ElMessage.error('加载预览失败: ' + (e.response?.data?.message || e.message))
  } finally {
    loading.value = false
  }
}

async function closeRegistration() {
  try {
    await ElMessageBox.confirm(
      '确认关闭注册?此操作将: 1) COMMITTED 满员开设 2) CANCELLED 未满开设并通知学生 3) 通知 Billing 系统',
      '关闭注册确认',
      { type: 'warning' }
    )
  } catch {
    return
  }

  closing.value = true
  try {
    const { data } = await registrationApi.close(status.value.semester)
    preview.value = data
    ElMessage.success(data.message || '关闭成功')
    await loadStatus()
  } catch (e) {
    ElMessage.error('关闭失败: ' + (e.response?.data?.message || e.message))
  } finally {
    closing.value = false
  }
}

async function advancePhase() {
  try {
    await ElMessageBox.confirm(
      `确认推进注册阶段? 当前阶段: ${status.value.phase}`,
      '推进阶段确认',
      { type: 'warning' }
    )
  } catch {
    return
  }

  advancing.value = true
  try {
    const { data } = await registrationApi.advancePhase()
    ElMessage.success(data.message || '阶段推进成功')
    await loadStatus()
    await loadPreview()
  } catch (e) {
    ElMessage.error('推进失败: ' + (e.response?.data?.message || e.message))
  } finally {
    advancing.value = false
  }
}

onMounted(async () => {
  await loadStatus()
  await loadPreview()
})
</script>

<style scoped>
.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 14px;
}

.status-card {
  margin-bottom: 16px;
}

.status-content {
  display: flex;
  gap: 40px;
  margin-bottom: 20px;
}

.status-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.status-label {
  font-size: 12px;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-weight: 500;
}

.status-value {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
}

.status-actions {
  display: flex;
  gap: 10px;
}

.phase-dot,
.phase-dot-inline {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-right: 4px;
}

.dot-open {
  background: #4ade80;
  box-shadow: 0 0 6px rgba(74, 222, 128, 0.4);
}

.dot-closed {
  background: var(--color-text-muted);
}

.preview-card {
  margin-bottom: 16px;
}

.stats-row {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.stat-item {
  flex: 1;
  background: var(--color-surface-hover);
  border-radius: var(--radius-lg);
  padding: 20px;
  text-align: center;
  border: 1px solid var(--color-border-light);
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 4px;
  font-weight: 500;
}

.stat-success .stat-number {
  color: var(--color-success);
}

.stat-warning .stat-number {
  color: var(--color-warning);
}

.preview-alert {
  margin-top: 16px;
}

/* --- Mobile Responsive --- */
@media (max-width: 768px) {
  /* Status content stacks */
  .status-content {
    flex-direction: column;
    gap: 16px;
  }

  .status-actions {
    flex-wrap: wrap;
    gap: 8px;
  }

  /* Stats row wraps */
  .stats-row {
    flex-wrap: wrap;
    gap: 12px;
  }

  .stat-item {
    flex: 1 1 calc(50% - 6px);
    min-width: 120px;
    padding: 16px;
  }

  .stat-number {
    font-size: 22px;
  }

  /* Table scrollable */
  .refined-table {
    display: block;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  .card-title {
    flex-wrap: wrap;
  }
}

@media (max-width: 480px) {
  .stat-item {
    flex: 1 1 100%;
  }

  .status-actions {
    flex-direction: column;
  }

  .status-actions .el-button {
    width: 100%;
  }
}
</style>
