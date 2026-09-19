<template>
  <AppLayout :title="'成绩单 - ' + report.semester">
    <el-skeleton v-if="loading" :rows="8" animated />

    <template v-else>
      <!-- GPA Summary Card -->
      <div class="gpa-summary">
        <div class="gpa-main">
          <div class="gpa-label">GPA</div>
          <div class="gpa-value">{{ report.gpa }}</div>
        </div>
        <div class="gpa-details">
          <div class="gpa-detail-item">
            <span class="detail-number">{{ report.entries?.length || 0 }}</span>
            <span class="detail-label">课程数</span>
          </div>
          <div class="gpa-detail-item">
            <span class="detail-number">{{ totalCredits }}</span>
            <span class="detail-label">学分</span>
          </div>
          <div class="gpa-detail-item">
            <span class="detail-number">{{ report.semester }}</span>
            <span class="detail-label">学期</span>
          </div>
        </div>
      </div>

      <!-- Grades Table -->
      <el-card>
        <template #header>
          <div class="card-title">
            <el-icon><DataAnalysis /></el-icon>
            <span>上学期成绩明细</span>
          </div>
        </template>

        <el-empty v-if="!report.entries || report.entries.length === 0" description="上学期无成绩记录">
          <template #image>
            <div class="empty-grades">
              <el-icon :size="48"><Document /></el-icon>
            </div>
          </template>
        </el-empty>

        <el-table v-else :data="report.entries" stripe class="refined-table">
          <el-table-column prop="courseId" label="课程号" width="100" />
          <el-table-column prop="courseTitle" label="名称" min-width="140" show-overflow-tooltip />
          <el-table-column prop="department" label="院系" width="150">
            <template #default="{ row }">
              <span class="muted-text">{{ row.department }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="credits" label="学分" width="80" align="center">
            <template #default="{ row }">
              <span class="credit-cell">{{ row.credits }}</span>
            </template>
          </el-table-column>
          <el-table-column label="成绩" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.grade" :type="gradeTagType(row.grade)" effect="dark" class="grade-tag">
                {{ row.grade }}
              </el-tag>
              <el-tag v-else type="info" size="small" effect="plain">未录入</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="professorName" label="教授" width="120">
            <template #default="{ row }">
              <span class="muted-text">{{ row.professorName }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </AppLayout>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { DataAnalysis, Document } from '@element-plus/icons-vue'
import { reportCardApi } from '../api/teaching'
import AppLayout from '../components/AppLayout.vue'

const report = reactive({
  semester: '',
  entries: [],
  gpa: 0
})
const loading = ref(true)

const totalCredits = computed(() => {
  if (!report.entries || report.entries.length === 0) return 0
  return report.entries.reduce((sum, e) => sum + (e.credits || 0), 0)
})

onMounted(async () => {
  loading.value = true
  try {
    const res = await reportCardApi.getMyReportCard()
    Object.assign(report, res.data)
  } catch (e) {
    ElMessage.error('加载成绩单失败')
  } finally {
    loading.value = false
  }
})

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

/* GPA Summary */
.gpa-summary {
  display: flex;
  align-items: center;
  gap: 40px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  border-radius: var(--radius-xl);
  padding: 28px 36px;
  margin-bottom: 20px;
  color: white;
}

.gpa-main {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-right: 40px;
  border-right: 1px solid rgba(255, 255, 255, 0.2);
}

.gpa-label {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.1em;
  opacity: 0.8;
  text-transform: uppercase;
}

.gpa-value {
  font-size: 40px;
  font-weight: 800;
  line-height: 1.2;
}

.gpa-details {
  display: flex;
  gap: 32px;
}

.gpa-detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-number {
  font-size: 20px;
  font-weight: 700;
}

.detail-label {
  font-size: 12px;
  opacity: 0.7;
}

.muted-text {
  color: var(--color-text-muted);
  font-size: 13px;
}

.credit-cell {
  font-weight: 600;
  color: var(--color-primary);
}

.grade-tag {
  font-weight: 700;
  min-width: 36px;
  text-align: center;
}

.empty-grades {
  color: var(--color-text-muted);
  opacity: 0.5;
}

/* --- Mobile Responsive --- */
@media (max-width: 768px) {
  /* GPA summary - stack vertically */
  .gpa-summary {
    flex-direction: column;
    gap: 20px;
    padding: 20px;
  }

  .gpa-main {
    padding-right: 0;
    padding-bottom: 20px;
    border-right: none;
    border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  }

  .gpa-value {
    font-size: 32px;
  }

  .gpa-details {
    gap: 24px;
    justify-content: center;
  }

  .detail-number {
    font-size: 18px;
  }

  /* Scrollable table */
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
  .gpa-summary {
    padding: 16px;
    gap: 16px;
  }

  .gpa-value {
    font-size: 28px;
  }

  .gpa-details {
    gap: 16px;
  }

  .gpa-detail-item {
    align-items: center;
  }
}
</style>
