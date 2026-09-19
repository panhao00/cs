<template>
  <AppLayout title="选课热度总览">
    <template #actions>
      <el-button @click="loadAll" :loading="loadingOverview">
        <el-icon><Refresh /></el-icon>
        刷新数据
      </el-button>
    </template>

    <!-- 顶部: 4个统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6" :xs="24">
        <el-card shadow="never" class="stat-card stat-courses">
          <div class="stat-icon">
            <el-icon :size="28"><Reading /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ overview.totalCourses || 0 }}</div>
            <div class="stat-label">总课程数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6" :xs="24">
        <el-card shadow="never" class="stat-card stat-students">
          <div class="stat-icon">
            <el-icon :size="28"><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ overview.totalStudents || 0 }}</div>
            <div class="stat-label">总学生数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6" :xs="24">
        <el-card shadow="never" class="stat-card stat-enrollments">
          <div class="stat-icon">
            <el-icon :size="28"><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ overview.totalEnrollments || 0 }}</div>
            <div class="stat-label">总选课数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6" :xs="24">
        <el-card shadow="never" class="stat-card stat-rate">
          <div class="stat-icon">
            <el-icon :size="28"><DataAnalysis /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ overview.averageEnrollmentRate || 0 }}%</div>
            <div class="stat-label">平均选课率</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 中部: 课程热度排名表格 -->
    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon><TrendCharts /></el-icon>
            <span>课程热度排名</span>
          </div>
          <el-select v-model="deptFilter" placeholder="按院系筛选" clearable size="default" style="width: 180px">
            <el-option label="全部院系" value="" />
            <el-option
              v-for="dept in departments"
              :key="dept"
              :label="dept"
              :value="dept"
            />
          </el-select>
        </div>
      </template>

      <el-skeleton v-if="loadingPopularity" :rows="4" animated />

      <el-table v-else :data="filteredPopularity" stripe class="refined-table">
        <el-table-column label="排名" width="60" type="index" :index="1" />
        <el-table-column prop="courseId" label="课程号" width="100" />
        <el-table-column prop="courseTitle" label="课程名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="department" label="院系" width="120" />
        <el-table-column label="选课/容量" width="110">
          <template #default="{ row }">
            <span>{{ row.enrolledCount }} / {{ row.maxCapacity }}</span>
          </template>
        </el-table-column>
        <el-table-column label="选课率" width="180" sortable :sort-method="(a, b) => a.popularityRate - b.popularityRate">
          <template #default="{ row }">
            <div class="rate-cell">
              <el-progress
                :percentage="row.popularityRate"
                :stroke-width="10"
                :color="rateColor(row.popularityRate)"
                :text-inside="true"
                style="flex: 1"
              />
              <span class="rate-text">{{ row.popularityRate }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="showTrend(row)">
              查看趋势
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 院系统计 -->
    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="card-title">
          <el-icon><OfficeBuilding /></el-icon>
          <span>院系统计</span>
        </div>
      </template>

      <el-skeleton v-if="loadingDepartment" :rows="3" animated />

      <el-table v-else :data="departmentStats" stripe class="refined-table">
        <el-table-column prop="department" label="院系" min-width="140" />
        <el-table-column prop="courseCount" label="开设课程数" width="110" />
        <el-table-column prop="totalEnrollments" label="总选课数" width="100" />
        <el-table-column prop="totalCapacity" label="总容量" width="100" />
        <el-table-column label="选课率" width="200">
          <template #default="{ row }">
            <div class="rate-cell">
              <el-progress
                :percentage="row.enrollmentRate"
                :stroke-width="10"
                :color="rateColor(row.enrollmentRate)"
                :text-inside="true"
                style="flex: 1"
              />
              <span class="rate-text">{{ row.enrollmentRate }}%</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 底部: 选课趋势折线图 (CSS bar chart) -->
    <el-card shadow="never" class="section-card" v-if="trendData.length > 0">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon><DataLine /></el-icon>
            <span>选课趋势 - {{ trendCourseTitle }}</span>
          </div>
          <el-button text @click="trendData = []">
            <el-icon><Close /></el-icon>
            关闭
          </el-button>
        </div>
      </template>

      <div class="trend-chart">
        <div class="chart-bars">
          <div
            v-for="item in trendData"
            :key="item.semester"
            class="chart-bar-group"
          >
            <div class="bar-value">{{ item.enrolledCount }}</div>
            <div class="bar-track">
              <div
                class="bar-fill"
                :style="{ height: barHeight(item) + '%' }"
                :class="barClass(item)"
              />
            </div>
            <div class="bar-label">{{ item.semester }}</div>
            <div class="bar-rate">{{ item.popularityRate }}%</div>
          </div>
        </div>
        <div class="chart-y-axis">
          <span>{{ maxEnrolled }}</span>
          <span>{{ Math.round(maxEnrolled / 2) }}</span>
          <span>0</span>
        </div>
      </div>
    </el-card>
  </AppLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Refresh, Reading, User, Document, DataAnalysis,
  TrendCharts, OfficeBuilding, DataLine, Close
} from '@element-plus/icons-vue'
import { enrollmentStatsApi } from '../api/recommendation'
import AppLayout from '../components/AppLayout.vue'

const semester = import.meta.env.VITE_CURRENT_SEMESTER || '2026-FALL'

const loadingOverview = ref(false)
const loadingPopularity = ref(false)
const loadingDepartment = ref(false)

const overview = ref({})
const popularity = ref([])
const departmentStats = ref([])
const trendData = ref([])
const trendCourseTitle = ref('')
const deptFilter = ref('')

const departments = computed(() => {
  const set = new Set(popularity.value.map(p => p.department).filter(Boolean))
  return [...set].sort()
})

const filteredPopularity = computed(() => {
  if (!deptFilter.value) return popularity.value
  return popularity.value.filter(p => p.department === deptFilter.value)
})

const maxEnrolled = computed(() => {
  if (trendData.value.length === 0) return 10
  return Math.max(...trendData.value.map(t => t.enrolledCount), 1)
})

onMounted(loadAll)

async function loadAll() {
  await Promise.all([loadOverview(), loadPopularity(), loadDepartmentStats()])
}

async function loadOverview() {
  loadingOverview.value = true
  try {
    const res = await enrollmentStatsApi.getOverview(semester)
    overview.value = res.data
  } catch (e) {
    ElMessage.error('加载总览失败')
  } finally {
    loadingOverview.value = false
  }
}

async function loadPopularity() {
  loadingPopularity.value = true
  try {
    const res = await enrollmentStatsApi.getPopularity(semester)
    popularity.value = res.data
  } catch (e) {
    ElMessage.error('加载热度排名失败')
  } finally {
    loadingPopularity.value = false
  }
}

async function loadDepartmentStats() {
  loadingDepartment.value = true
  try {
    const res = await enrollmentStatsApi.getDepartmentStats(semester)
    departmentStats.value = res.data
  } catch (e) {
    ElMessage.error('加载院系统计失败')
  } finally {
    loadingDepartment.value = false
  }
}

async function showTrend(row) {
  trendCourseTitle.value = row.courseTitle
  try {
    const res = await enrollmentStatsApi.getTrend(row.courseId)
    trendData.value = res.data
  } catch (e) {
    ElMessage.error('加载趋势数据失败')
  }
}

function rateColor(rate) {
  if (rate >= 90) return '#F56C6C'
  if (rate >= 70) return '#E6A23C'
  if (rate >= 40) return '#409EFF'
  return '#67C23A'
}

function barHeight(item) {
  return Math.round((item.enrolledCount / maxEnrolled.value) * 100)
}

function barClass(item) {
  const rate = item.popularityRate
  if (rate >= 90) return 'bar-danger'
  if (rate >= 70) return 'bar-warning'
  if (rate >= 40) return 'bar-primary'
  return 'bar-success'
}
</script>

<style scoped>
.stats-row {
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 4px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-courses .stat-icon {
  background: rgba(64, 158, 255, 0.1);
  color: #409EFF;
}

.stat-students .stat-icon {
  background: rgba(103, 194, 58, 0.1);
  color: #67C23A;
}

.stat-enrollments .stat-icon {
  background: rgba(230, 162, 60, 0.1);
  color: #E6A23C;
}

.stat-rate .stat-icon {
  background: rgba(144, 113, 199, 0.1);
  color: #9071C7;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-top: 2px;
}

.section-card {
  margin-bottom: 16px;
}

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

.rate-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rate-text {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
  white-space: nowrap;
  min-width: 40px;
  text-align: right;
}

/* CSS Bar Chart */
.trend-chart {
  display: flex;
  gap: 8px;
  padding: 20px 0;
}

.chart-y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding-bottom: 24px;
  font-size: 11px;
  color: var(--color-text-muted);
  min-width: 30px;
  text-align: right;
}

.chart-bars {
  display: flex;
  align-items: flex-end;
  gap: 24px;
  flex: 1;
  height: 200px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--color-border-light);
}

.chart-bar-group {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  height: 100%;
}

.bar-value {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 4px;
}

.bar-track {
  flex: 1;
  width: 40px;
  background: var(--color-surface-hover);
  border-radius: var(--radius-md) var(--radius-md) 0 0;
  display: flex;
  align-items: flex-end;
  overflow: hidden;
}

.bar-fill {
  width: 100%;
  border-radius: var(--radius-md) var(--radius-md) 0 0;
  transition: height 0.6s ease;
  min-height: 4px;
}

.bar-success { background: linear-gradient(180deg, #67C23A, #85CE61); }
.bar-primary { background: linear-gradient(180deg, #409EFF, #66B1FF); }
.bar-warning { background: linear-gradient(180deg, #E6A23C, #EBB563); }
.bar-danger { background: linear-gradient(180deg, #F56C6C, #F89898); }

.bar-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 8px;
  white-space: nowrap;
}

.bar-rate {
  font-size: 11px;
  color: var(--color-text-muted);
  font-family: var(--font-mono);
}

@media (max-width: 767px) {
  /* Stats cards 2x2 grid */
  .stats-row .el-col {
    display: flex !important;
  }

  .stat-card {
    width: 100%;
  }

  .stat-icon {
    width: 44px;
    height: 44px;
  }

  .stat-value {
    font-size: 20px;
  }

  .card-header {
    flex-wrap: wrap;
    gap: 8px;
  }

  /* Chart bars responsive */
  .chart-bars {
    gap: 12px;
  }

  .bar-track {
    width: 28px;
  }

  .el-table {
    overflow-x: auto;
  }
  .el-card__header {
    padding: 12px 16px;
  }
  .el-card__body {
    padding: 12px 16px;
  }
}
</style>
