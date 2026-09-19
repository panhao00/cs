<template>
  <AppLayout title="系统设置">
    <el-row justify="center">
      <el-col :xs="24" :sm="20" :md="16" :lg="12">
        <el-card>
          <template #header>
            <div class="card-title">
              <el-icon><Setting /></el-icon>
              <span>系统设置</span>
            </div>
          </template>

          <el-skeleton v-if="loading" :rows="8" animated />

          <el-form
            v-else
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="auto"
            label-position="top"
            @submit.prevent="handleSave"
          >
            <el-form-item label="当前学期" prop="currentSemester">
              <el-input v-model="form.currentSemester" placeholder="例: 2026-FALL" />
            </el-form-item>

            <el-form-item label="上学期" prop="previousSemester">
              <el-input v-model="form.previousSemester" placeholder="例: 2026-SPRING" />
            </el-form-item>

            <el-form-item label="注册阶段" prop="registrationPhase">
              <el-select v-model="form.registrationPhase" placeholder="选择注册阶段" style="width: 100%">
                <el-option label="开放 (open)" value="open" />
                <el-option label="增删课 (add_drop)" value="add_drop" />
                <el-option label="已关闭 (closed)" value="closed" />
              </el-select>
            </el-form-item>

            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="主选课最大数" prop="maxPrimaryCourses">
                  <el-input-number v-model="form.maxPrimaryCourses" :min="1" :max="10" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="备选课最大数" prop="maxAlternateCourses">
                  <el-input-number v-model="form.maxAlternateCourses" :min="0" :max="5" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="开设最大人数" prop="offeringMaxStudents">
                  <el-input-number v-model="form.offeringMaxStudents" :min="1" :max="100" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="开设最少人数" prop="offeringMinStudents">
                  <el-input-number v-model="form.offeringMinStudents" :min="1" :max="50" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item>
              <el-button type="primary" native-type="submit" :loading="saving" style="width: 100%">
                保存设置
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </AppLayout>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Setting } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import { settingsApi } from '../api/settings'

const formRef = ref(null)
const loading = ref(true)
const saving = ref(false)

const form = reactive({
  currentSemester: '',
  previousSemester: '',
  registrationPhase: 'open',
  maxPrimaryCourses: 4,
  maxAlternateCourses: 2,
  offeringMaxStudents: 30,
  offeringMinStudents: 1
})

const rules = {
  currentSemester: [{ required: true, message: '请输入当前学期', trigger: 'blur' }],
  previousSemester: [{ required: true, message: '请输入上学期', trigger: 'blur' }],
  registrationPhase: [{ required: true, message: '请选择注册阶段', trigger: 'change' }]
}

onMounted(async () => {
  try {
    const res = await settingsApi.getAll()
    const data = res.data || {}
    form.currentSemester = data.currentSemester || data.CURRENT_SEMESTER || ''
    form.previousSemester = data.previousSemester || data.PREVIOUS_SEMESTER || ''
    form.registrationPhase = data.registrationPhase || data.REGISTRATION_PHASE || 'open'
    form.maxPrimaryCourses = data.maxPrimaryCourses || data.MAX_PRIMARY_COURSES || 4
    form.maxAlternateCourses = data.maxAlternateCourses || data.MAX_ALTERNATE_COURSES || 2
    form.offeringMaxStudents = data.offeringMaxStudents || data.OFFERING_MAX_STUDENTS || 30
    form.offeringMinStudents = data.offeringMinStudents || data.OFFERING_MIN_STUDENTS || 1
  } catch (e) {
    ElMessage.error('加载设置失败')
  } finally {
    loading.value = false
  }
})

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await settingsApi.update({
      CURRENT_SEMESTER: form.currentSemester,
      PREVIOUS_SEMESTER: form.previousSemester,
      REGISTRATION_PHASE: form.registrationPhase,
      MAX_PRIMARY_COURSES: String(form.maxPrimaryCourses),
      MAX_ALTERNATE_COURSES: String(form.maxAlternateCourses),
      OFFERING_MAX_STUDENTS: String(form.offeringMaxStudents),
      OFFERING_MIN_STUDENTS: String(form.offeringMinStudents)
    })
    ElMessage.success('设置已保存')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
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

/* --- Mobile Responsive --- */
@media (max-width: 480px) {
  .card-title {
    font-size: 13px;
  }
}
</style>
