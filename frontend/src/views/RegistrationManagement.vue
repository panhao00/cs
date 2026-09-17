<template>
  <div class="reg-mgmt">
    <AppLayout title="注册管理(Close Registration)">
      <template #actions>
        <el-tag :type="phaseTagType">{{ phaseLabel }}</el-tag>
      </template>
        <!-- 当前状态卡片 -->
        <el-card>
          <template #header>当前注册阶段</template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="学期">{{ status.semester }}</el-descriptions-item>
            <el-descriptions-item label="阶段">
              <el-tag :type="phaseTagType">{{ phaseLabel }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
          <div style="margin-top: 16px">
            <el-button
              type="primary"
              :disabled="status.phase === 'closed'"
              :loading="closing"
              @click="closeRegistration"
            >
              关闭注册
            </el-button>
            <el-button :loading="loading" @click="loadPreview">刷新预览</el-button>
          </div>
        </el-card>

        <!-- 预览/结果卡片 -->
        <el-card style="margin-top: 16px" v-loading="loading">
          <template #header>课程开设容量统计</template>
          <el-descriptions v-if="preview" :column="3" border>
            <el-descriptions-item label="总开设数">{{ preview.totalOfferings }}</el-descriptions-item>
            <el-descriptions-item label="满足最小人数">{{ preview.fullOfferings }}</el-descriptions-item>
            <el-descriptions-item label="未满(将取消)">
              <el-tag :type="preview.underFilledOfferings > 0 ? 'warning' : 'success'">
                {{ preview.underFilledOfferings }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <el-table
            v-if="preview && preview.offerings?.length"
            :data="preview.offerings"
            border
            style="margin-top: 12px"
          >
            <el-table-column prop="courseId" label="课程" width="120" />
            <el-table-column prop="enrolledCount" label="已选人数" width="120" />
            <el-table-column prop="minRequired" label="最小人数" width="120" />
            <el-table-column label="是否满足" width="120">
              <template #default="{ row }">
                <el-tag :type="row.meetsMinimum ? 'success' : 'warning'">
                  {{ row.meetsMinimum ? '满足' : '未满' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="说明" />
          </el-table>

          <el-alert
            v-if="preview && preview.message"
            :title="preview.message"
            :type="preview.billingNotificationSent ? 'success' : 'warning'"
            :closable="false"
            style="margin-top: 12px"
          />
        </el-card>
    </AppLayout>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppLayout from '../components/AppLayout.vue'
import { registrationApi } from '../api/users'

const loading = ref(false)
const closing = ref(false)
const status = ref({ phase: 'open', semester: '' })
const preview = ref(null)

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

onMounted(async () => {
  await loadStatus()
  await loadPreview()
})
</script>


