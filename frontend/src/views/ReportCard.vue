<template>
  <AppLayout :title="'成绩单 - ' + report.semester">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>上学期成绩</span>
          <el-tag size="large" type="success">GPA: {{ report.gpa }}</el-tag>
        </div>
      </template>
      <el-empty v-if="!report.entries || report.entries.length === 0"
                description="上学期无成绩记录" />
      <el-table v-else :data="report.entries" stripe>
        <el-table-column prop="courseId" label="课程号" width="100" />
        <el-table-column prop="courseTitle" label="名称" />
        <el-table-column prop="department" label="院系" width="150" />
        <el-table-column prop="credits" label="学分" width="80" />
        <el-table-column label="成绩" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.grade" :type="gradeTagType(row.grade)">
              {{ row.grade }}
            </el-tag>
            <el-tag v-else type="info" size="small">未录入</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="professorName" label="教授" width="120" />
      </el-table>
    </el-card>
  </AppLayout>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { reportCardApi } from '../api/teaching'
import AppLayout from '../components/AppLayout.vue'

const report = reactive({
  semester: '',
  entries: [],
  gpa: 0
})
const loading = ref(true)

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
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
