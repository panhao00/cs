<template>
  <AppLayout title="成绩趋势分析">
    <el-skeleton v-if="loading" :rows="12" animated />

    <template v-else>
      <!-- GPA 趋势折线图 -->
      <el-card class="chart-card">
        <template #header>
          <div class="card-title">
            <el-icon><TrendCharts /></el-icon>
            <span>GPA 趋势</span>
          </div>
        </template>

        <el-empty v-if="gpaTrend.length === 0" description="暂无 GPA 数据" />

        <div v-else class="line-chart-container">
          <!-- Y 轴标签 -->
          <div class="y-axis">
            <span v-for="label in yLabels" :key="label" class="y-label">{{ label }}</span>
          </div>

          <!-- 图表区域 -->
          <div class="chart-area">
            <!-- 水平网格线 -->
            <div class="grid-lines">
              <div v-for="i in 5" :key="i" class="grid-line" :style="{ bottom: (i - 1) * 25 + '%' }"></div>
            </div>

            <!-- 数据点和连线 -->
            <div class="data-points">
              <div
                v-for="(item, index) in gpaTrend"
                :key="item.semester"
                class="point-group"
              >
                <!-- 连线(除第一个点外) -->
                <div
                  v-if="index > 0"
                  class="line-segment"
                  :style="getLineStyle(index)"
                ></div>

                <!-- 数据点 -->
                <div
                  class="data-point"
                  :class="{
                    'is-max': item.gpa === maxGpa,
                    'is-min': item.gpa === minGpa && item.gpa !== maxGpa
                  }"
                  :style="{ bottom: (item.gpa / 4.0 * 100) + '%' }"
                >
                  <div class="point-dot"></div>
                  <div class="point-label">{{ item.gpa.toFixed(2) }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- X 轴标签 -->
          <div class="x-axis">
            <span
              v-for="item in gpaTrend"
              :key="item.semester"
              class="x-label"
            >{{ formatSemester(item.semester) }}</span>
          </div>
        </div>

        <!-- 图例 -->
        <div class="chart-legend">
          <span class="legend-item legend-max">
            <span class="legend-dot max"></span>最高 GPA: {{ maxGpa.toFixed(2) }}
          </span>
          <span class="legend-item legend-min">
            <span class="legend-dot min"></span>最低 GPA: {{ minGpa.toFixed(2) }}
          </span>
        </div>
      </el-card>

      <!-- 学分统计卡片 -->
      <div class="stats-grid">
        <!-- 总学分 -->
        <el-card class="stat-card">
          <div class="stat-icon total">
            <el-icon :size="24"><Collection /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ creditSummary.totalCredits }}</div>
            <div class="stat-label">总学分</div>
          </div>
        </el-card>

        <!-- 已修学分 -->
        <el-card class="stat-card">
          <div class="stat-icon completed">
            <el-icon :size="24"><Finished /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ creditSummary.completedCredits }}</div>
            <div class="stat-label">已修学分</div>
          </div>
        </el-card>

        <!-- 平均 GPA -->
        <el-card class="stat-card">
          <div class="stat-icon gpa">
            <el-icon :size="24"><TrendCharts /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ creditSummary.averageGpa?.toFixed(2) || '0.00' }}</div>
            <div class="stat-label">平均 GPA</div>
          </div>
        </el-card>

        <!-- 院系分布饼图 -->
        <el-card class="stat-card pie-card">
          <template #header>
            <div class="card-title small">
              <span>院系学分分布</span>
            </div>
          </template>
          <div class="pie-chart">
            <div class="pie-ring">
              <svg viewBox="0 0 36 36" class="pie-svg">
                <circle
                  v-for="(slice, index) in pieSlices"
                  :key="index"
                  cx="18" cy="18" r="15.9155"
                  fill="none"
                  :stroke="slice.color"
                  stroke-width="3"
                  :stroke-dasharray="slice.dashArray"
                  :stroke-dashoffset="slice.dashOffset"
                  class="pie-segment"
                />
              </svg>
            </div>
            <div class="pie-legend">
              <div
                v-for="(dept, index) in departmentList"
                :key="dept.name"
                class="pie-legend-item"
              >
                <span class="pie-legend-dot" :style="{ background: pieColors[index % pieColors.length] }"></span>
                <span class="pie-legend-name">{{ dept.name }}</span>
                <span class="pie-legend-value">{{ dept.credits }}学分</span>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 成绩分布柱状图(教授查看) -->
      <el-card v-if="role === 'PROFESSOR'" class="chart-card">
        <template #header>
          <div class="card-title">
            <el-icon><DataAnalysis /></el-icon>
            <span>成绩分布</span>
          </div>
        </template>

        <!-- 选择班级 -->
        <div class="offering-select">
          <el-select
            v-model="selectedOfferingId"
            placeholder="选择班级"
            @change="loadGradeDistribution"
            style="width: 240px"
          >
            <el-option
              v-for="offering in taughtOfferings"
              :key="offering.offeringId"
              :label="offering.courseId + ' - ' + offering.semester"
              :value="offering.offeringId"
            />
          </el-select>
        </div>

        <div v-if="distribution" class="bar-chart">
          <div
            v-for="item in distribution.distribution"
            :key="item.grade"
            class="bar-group"
          >
            <div class="bar-value">{{ item.count }}人 ({{ item.percentage }}%)</div>
            <div class="bar-wrapper">
              <div
                class="bar-fill"
                :class="'bar-' + item.grade.toLowerCase()"
                :style="{ height: barHeight(item.percentage) }"
              ></div>
            </div>
            <div class="bar-label">{{ item.grade }}</div>
          </div>
          <div class="bar-total">共 {{ distribution.totalStudents }} 人</div>
        </div>

        <el-empty v-else-if="selectedOfferingId" description="暂无成绩数据" />
        <el-empty v-else description="请选择班级查看成绩分布" />
      </el-card>
    </template>
  </AppLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import { statsApi } from '../api/stats'
import { gradeApi } from '../api/teaching'
import AppLayout from '../components/AppLayout.vue'
import {
  TrendCharts, DataAnalysis, Collection, Finished
} from '@element-plus/icons-vue'

const auth = useAuthStore()
const role = computed(() => auth.role)

const loading = ref(true)
const gpaTrend = ref([])
const creditSummary = ref({})
const distribution = ref(null)
const selectedOfferingId = ref(null)
const taughtOfferings = ref([])

// Pie chart colors
const pieColors = ['#1e3a5f', '#c8962e', '#059669', '#dc2626', '#7c3aed', '#2563eb']

// Computed
const maxGpa = computed(() => {
  if (gpaTrend.value.length === 0) return 0
  return Math.max(...gpaTrend.value.map(i => i.gpa))
})

const minGpa = computed(() => {
  if (gpaTrend.value.length === 0) return 0
  return Math.min(...gpaTrend.value.map(i => i.gpa))
})

const yLabels = computed(() => ['4.0', '3.0', '2.0', '1.0', '0.0'])

const departmentList = computed(() => {
  const depts = creditSummary.value.departmentCredits || {}
  return Object.entries(depts).map(([name, credits]) => ({ name, credits }))
})

const pieSlices = computed(() => {
  const total = departmentList.value.reduce((sum, d) => sum + d.credits, 0)
  if (total === 0) return []

  let cumulative = 0
  return departmentList.value.map((dept, index) => {
    const pct = (dept.credits / total) * 100
    const dashArray = `${pct} ${100 - pct}`
    const dashOffset = -cumulative
    cumulative += pct
    return {
      color: pieColors[index % pieColors.length],
      dashArray,
      dashOffset
    }
  })
})

// Methods
function formatSemester(sem) {
  if (!sem) return ''
  const parts = sem.split('-')
  if (parts.length < 2) return sem
  const term = parts[1] === 'FALL' ? '秋' : '春'
  return parts[0] + ' ' + term
}

function getLineStyle(index) {
  const prev = gpaTrend.value[index - 1]
  const curr = gpaTrend.value[index]
  const prevPct = (prev.gpa / 4.0) * 100
  const currPct = (curr.gpa / 4.0) * 100
  const angle = Math.atan2(prevPct - currPct, 100) * (180 / Math.PI)
  return {
    transform: `rotate(${-angle}deg)`,
    width: `${100 / gpaTrend.value.length}%`,
    left: `${((index - 1) / gpaTrend.value.length) * 100 + (50 / gpaTrend.value.length)}%`,
    bottom: `${prevPct}%`
  }
}

function barHeight(pct) {
  return Math.max(pct, 2) + '%'
}

async function loadGradeDistribution() {
  if (!selectedOfferingId.value) return
  try {
    const res = await statsApi.getGradeDistribution(selectedOfferingId.value)
    distribution.value = res.data
  } catch {
    distribution.value = null
  }
}

// Load data
onMounted(async () => {
  try {
    const [gpaRes, creditRes] = await Promise.all([
      statsApi.getGpaTrend(),
      statsApi.getCreditSummary()
    ])
    gpaTrend.value = gpaRes.data
    creditSummary.value = creditRes.data

    // Load教授授课列表
    if (role.value === 'PROFESSOR') {
      try {
        const offRes = await gradeApi.getTaughtOfferings()
        taughtOfferings.value = offRes.data
      } catch {
        // 静默
      }
    }
  } catch (err) {
    console.error('加载成绩数据失败:', err)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.chart-card {
  margin-bottom: var(--space-6);
}

.card-title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-md);
}

.card-title.small {
  font-size: var(--font-size-sm);
}

/* ---- Line Chart ---- */
.line-chart-container {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-4) 0;
}

.y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 200px;
  padding-right: var(--space-2);
  text-align: right;
}

.y-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
}

.chart-area {
  position: relative;
  height: 200px;
  margin-left: 36px;
  border-left: 2px solid var(--color-border);
  border-bottom: 2px solid var(--color-border);
}

.grid-lines {
  position: absolute;
  inset: 0;
}

.grid-line {
  position: absolute;
  left: 0;
  right: 0;
  height: 1px;
  background: var(--color-border-light);
}

.data-points {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: flex-start;
}

.point-group {
  position: relative;
  flex: 1;
  height: 100%;
}

.data-point {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 2;
}

.point-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--color-primary);
  border: 3px solid var(--color-bg-elevated);
  box-shadow: 0 0 0 2px var(--color-primary);
  transition: transform var(--transition-fast);
}

.data-point:hover .point-dot {
  transform: scale(1.3);
}

.data-point.is-max .point-dot {
  background: var(--color-success);
  box-shadow: 0 0 0 2px var(--color-success);
}

.data-point.is-min .point-dot {
  background: var(--color-danger);
  box-shadow: 0 0 0 2px var(--color-danger);
}

.point-label {
  margin-top: 4px;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.data-point.is-max .point-label {
  color: var(--color-success);
}

.data-point.is-min .point-label {
  color: var(--color-danger);
}

.line-segment {
  position: absolute;
  height: 2px;
  background: var(--color-primary-lighter);
  transform-origin: left center;
  z-index: 1;
  opacity: 0.6;
}

.x-axis {
  display: flex;
  margin-left: 36px;
  margin-top: var(--space-2);
}

.x-label {
  flex: 1;
  text-align: center;
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.chart-legend {
  display: flex;
  gap: var(--space-6);
  justify-content: center;
  margin-top: var(--space-4);
  padding-top: var(--space-3);
  border-top: 1px solid var(--color-border-light);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.legend-dot.max {
  background: var(--color-success);
}

.legend-dot.min {
  background: var(--color-danger);
}

/* ---- Stats Grid ---- */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

.stat-card {
  text-align: center;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-5) var(--space-6);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon.total {
  background: var(--color-primary-bg);
  color: var(--color-primary);
}

.stat-icon.completed {
  background: var(--color-success-bg);
  color: var(--color-success);
}

.stat-icon.gpa {
  background: var(--color-warning-bg);
  color: var(--color-warning);
}

.stat-content {
  text-align: left;
}

.stat-value {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text);
  line-height: 1;
}

.stat-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
  margin-top: 4px;
}

/* Pie card spans full width */
.pie-card {
  grid-column: 1 / -1;
}

.pie-card :deep(.el-card__body) {
  padding: var(--space-4) var(--space-6);
}

/* ---- Pie Chart ---- */
.pie-chart {
  display: flex;
  align-items: center;
  gap: var(--space-8);
}

.pie-ring {
  width: 140px;
  height: 140px;
  flex-shrink: 0;
}

.pie-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.pie-segment {
  transition: stroke-dasharray var(--transition-slow);
}

.pie-legend {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.pie-legend-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-size-sm);
}

.pie-legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  flex-shrink: 0;
}

.pie-legend-name {
  color: var(--color-text-secondary);
  min-width: 80px;
}

.pie-legend-value {
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
}

/* ---- Bar Chart ---- */
.offering-select {
  margin-bottom: var(--space-4);
}

.bar-chart {
  display: flex;
  align-items: flex-end;
  gap: var(--space-4);
  height: 240px;
  padding: var(--space-4) var(--space-2) 0;
  border-left: 2px solid var(--color-border);
  border-bottom: 2px solid var(--color-border);
  position: relative;
}

.bar-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  justify-content: flex-end;
}

.bar-value {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  margin-bottom: 4px;
  white-space: nowrap;
}

.bar-wrapper {
  width: 100%;
  max-width: 48px;
  height: 180px;
  position: relative;
}

.bar-fill {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  border-radius: var(--radius-sm) var(--radius-sm) 0 0;
  transition: height var(--transition-slow);
}

.bar-a { background: linear-gradient(180deg, #059669, #34d399); }
.bar-b { background: linear-gradient(180deg, #2563eb, #60a5fa); }
.bar-c { background: linear-gradient(180deg, #c8962e, #dbb45c); }
.bar-d { background: linear-gradient(180deg, #d97706, #fbbf24); }
.bar-f { background: linear-gradient(180deg, #dc2626, #f87171); }
.bar-i { background: linear-gradient(180deg, #6b7280, #9ca3af); }

.bar-label {
  margin-top: var(--space-2);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-secondary);
}

.bar-total {
  position: absolute;
  top: var(--space-2);
  right: var(--space-2);
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

/* ---- Empty states ---- */
.empty-grades {
  color: var(--color-text-muted);
}

.muted-text {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

@media (max-width: 767px) {
  /* Stats grid: 2 columns on tablet */
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .pie-card {
    grid-column: 1 / -1;
  }

  /* Pie chart stacks */
  .pie-chart {
    flex-direction: column;
    align-items: center;
    gap: var(--space-4);
  }

  .pie-ring {
    width: 120px;
    height: 120px;
  }

  /* Chart area shrinks */
  .chart-area {
    margin-left: 28px;
  }

  .y-axis {
    padding-right: var(--space-1);
  }

  .x-label {
    font-size: 10px;
  }

  /* Bar chart adapts */
  .bar-chart {
    gap: var(--space-2);
    height: 200px;
  }

  .bar-wrapper {
    max-width: 36px;
    height: 140px;
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

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .stat-card :deep(.el-card__body) {
    padding: var(--space-4);
  }

  .bar-chart {
    height: 160px;
  }

  .bar-wrapper {
    height: 120px;
  }
}
</style>
