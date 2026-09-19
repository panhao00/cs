<template>
  <AppLayout :title="'提交成绩 - ' + prevSemester">
    <el-row :gutter="20">
      <!-- 左侧:上学期授课列表 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="card-title">
              <el-icon><Reading /></el-icon>
              <span>上学期授课</span>
            </div>
          </template>

          <el-skeleton v-if="loadingOfferings" :rows="4" animated />

          <el-empty v-else-if="!taughtOfferings.length" description="上学期未授课">
            <template #image>
              <div class="empty-icon">
                <el-icon :size="48"><Reading /></el-icon>
              </div>
            </template>
          </el-empty>

          <el-table
            v-else
            :data="taughtOfferings"
            highlight-current-row
            @current-change="handleSelectOffering"
            size="small"
            class="refined-table offering-list"
          >
            <el-table-column prop="courseId" label="课程号" width="90" />
            <el-table-column prop="semester" label="学期" width="100" />
            <el-table-column label="时间">
              <template #default="{ row }">
                <span class="time-cell">{{ row.days }} {{ row.times }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右侧:班级名单 + 成绩录入 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-title">
              <el-icon><List /></el-icon>
              <span>班级名单</span>
              <span v-if="selectedOffering" class="selected-offering-tag">
                {{ selectedOffering.courseId }}
              </span>
            </div>
          </template>

          <el-empty v-if="!selectedOffering" description="请选择左侧授课">
            <template #image>
              <div class="empty-icon roster-empty">
                <el-icon :size="48"><User /></el-icon>
              </div>
            </template>
          </el-empty>

          <el-skeleton v-else-if="loadingRoster" :rows="6" animated />

          <el-table
            v-else
            :data="roster"
            stripe
            class="refined-table"
          >
            <el-table-column prop="studentId" label="学号" width="80" />
            <el-table-column prop="studentName" label="姓名" width="140" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" type="info" effect="dark">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="成绩" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.grade" :type="gradeTagType(row.grade)" effect="dark">
                  {{ row.grade }}
                </el-tag>
                <el-tag v-else type="info" size="small" effect="plain">未录入</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="录入/修改成绩" min-width="200">
              <template #default="{ row }">
                <div class="grade-input-row">
                  <el-select
                    v-model="gradeInput[row.studentId]"
                    placeholder="选择成绩"
                    size="small"
                    class="grade-select"
                  >
                    <el-option label="A" value="A" />
                    <el-option label="B" value="B" />
                    <el-option label="C" value="C" />
                    <el-option label="D" value="D" />
                    <el-option label="F" value="F" />
                    <el-option label="I (Incomplete)" value="I" />
                  </el-select>
                  <el-button
                    size="small"
                    type="primary"
                    :disabled="!gradeInput[row.studentId]"
                    @click="handleSubmitGrade(row.studentId)"
                  >
                    保存
                  </el-button>
                  <el-button
                    v-if="row.grade"
                    size="small"
                    type="warning"
                    plain
                    @click="handleClearGrade(row.studentId)"
                  >
                    清除
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </AppLayout>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Reading, List, User } from '@element-plus/icons-vue'
import { gradeApi } from '../api/teaching'
import AppLayout from '../components/AppLayout.vue'

const prevSemester = import.meta.env.VITE_PREVIOUS_SEMESTER || '2026-SPRING'
const taughtOfferings = ref([])
const selectedOffering = ref(null)
const roster = ref([])
const loadingRoster = ref(false)
const loadingOfferings = ref(true)
const gradeInput = reactive({})

onMounted(async () => {
  await loadTaughtOfferings()
})

async function loadTaughtOfferings() {
  loadingOfferings.value = true
  try {
    const res = await gradeApi.getTaughtOfferings()
    taughtOfferings.value = res.data
  } catch (e) {
    ElMessage.error('加载授课列表失败')
  } finally {
    loadingOfferings.value = false
  }
}

async function handleSelectOffering(row) {
  if (!row) return
  selectedOffering.value = row
  loadingRoster.value = true
  try {
    const res = await gradeApi.getRoster(row.offeringId)
    roster.value = res.data
    Object.keys(gradeInput).forEach(k => delete gradeInput[k])
  } catch (e) {
    ElMessage.error('加载班级名单失败')
  } finally {
    loadingRoster.value = false
  }
}

async function handleSubmitGrade(studentId) {
  try {
    const res = await gradeApi.submitGrade(
      selectedOffering.value.offeringId,
      studentId,
      gradeInput[studentId],
      false
    )
    const idx = roster.value.findIndex(r => r.studentId === studentId)
    if (idx >= 0) roster.value[idx].grade = res.data.grade
    ElMessage.success(`已录入成绩 ${res.data.grade}`)
    gradeInput[studentId] = null
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '录入失败')
  }
}

async function handleClearGrade(studentId) {
  try {
    const res = await gradeApi.submitGrade(
      selectedOffering.value.offeringId,
      studentId,
      null,
      true
    )
    const idx = roster.value.findIndex(r => r.studentId === studentId)
    if (idx >= 0) roster.value[idx].grade = res.data.grade
    ElMessage.success('已清除成绩')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '清除失败')
  }
}

function gradeTagType(grade) {
  return { A: 'success', B: 'success', C: 'warning', D: 'warning', F: 'danger', I: 'info' }[grade] || 'info'
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

.selected-offering-tag {
  margin-left: auto;
  font-weight: 400;
  font-size: 12px;
  padding: 2px 10px;
  background: var(--color-primary-bg);
  color: var(--color-primary);
  border-radius: var(--radius-sm);
}

.time-cell {
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--color-text-secondary);
}

.grade-input-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.grade-select {
  width: 110px;
}

.offering-list {
  cursor: pointer;
}

.empty-icon {
  color: var(--color-text-muted);
  opacity: 0.5;
}

.roster-empty {
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

  .card-title {
    flex-wrap: wrap;
  }

  /* Grade input row wraps */
  .grade-input-row {
    flex-wrap: wrap;
    gap: 6px;
  }

  .grade-select {
    width: 100%;
    min-width: 0;
  }

  .refined-table {
    display: block;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
}
</style>
