<template>
  <AppLayout :title="'提交成绩 - ' + prevSemester">
    <el-row :gutter="20">
          <!-- 左侧:上学期授课列表 -->
          <el-col :span="8">
            <el-card v-loading="loadingOfferings">
              <template #header>
                <span>上学期授课</span>
              </template>
              <el-empty v-if="!taughtOfferings.length"
                        description="上学期未授课" />
              <el-table
                v-else
                :data="taughtOfferings"
                highlight-current-row
                @current-change="handleSelectOffering"
                size="small"
              >
                <el-table-column prop="courseId" label="课程号" width="90" />
                <el-table-column prop="semester" label="学期" width="100" />
                <el-table-column label="时间">
                  <template #default="{ row }">
                    {{ row.days }} {{ row.times }}
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>

          <!-- 右侧:班级名单 + 成绩录入 -->
          <el-col :span="16">
            <el-card>
              <template #header>
                <span>班级名单</span>
              </template>
              <el-empty v-if="!selectedOffering"
                        description="请选择左侧授课" />
              <el-table
                v-else
                :data="roster"
                v-loading="loadingRoster"
                stripe
              >
                <el-table-column prop="studentId" label="学号" width="80" />
                <el-table-column prop="studentName" label="姓名" width="140" />
                <el-table-column prop="status" label="状态" width="100" />
                <el-table-column label="成绩" width="100">
                  <template #default="{ row }">
                    <el-tag v-if="row.grade" :type="gradeTagType(row.grade)">
                      {{ row.grade }}
                    </el-tag>
                    <el-tag v-else type="info" size="small">未录入</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="录入/修改成绩">
                  <template #default="{ row }">
                    <el-select
                      v-model="gradeInput[row.studentId]"
                      placeholder="选择成绩"
                      size="small"
                      style="width: 90px"
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
                      style="margin-left: 8px"
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
    // 清空成绩输入
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
    // 更新本地数据
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



