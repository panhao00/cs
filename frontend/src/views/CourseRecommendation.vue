<template>
  <AppLayout title="智能推荐">
    <template #actions>
      <el-button @click="loadRecommendations" :loading="loading">
        <el-icon><Refresh /></el-icon>
        刷新推荐
      </el-button>
    </template>

    <!-- 推荐理由分类标签 -->
    <el-card class="filter-card" shadow="never">
      <div class="filter-row">
        <span class="filter-label">推荐维度:</span>
        <el-radio-group v-model="activeFilter" size="default">
          <el-radio-button label="all">全部推荐</el-radio-button>
          <el-radio-button label="prerequisite">先修课程链</el-radio-button>
          <el-radio-button label="department">专业匹配</el-radio-button>
          <el-radio-button label="popular">热门推荐</el-radio-button>
          <el-radio-button label="credit">学分平衡</el-radio-button>
        </el-radio-group>
      </div>
    </el-card>

    <!-- 推荐列表 -->
    <el-card shadow="never" class="recommend-card">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon><MagicStick /></el-icon>
            <span>为您推荐的课程</span>
            <el-tag type="primary" size="small" effect="dark">{{ filteredRecommendations.length }} 门</el-tag>
          </div>
        </div>
      </template>

      <el-skeleton v-if="loading" :rows="5" animated />

      <el-empty v-else-if="filteredRecommendations.length === 0" description="暂无推荐课程">
        <template #image>
          <div class="empty-icon">
            <el-icon :size="48"><MagicStick /></el-icon>
          </div>
        </template>
      </el-empty>

      <div v-else class="recommendation-list">
        <div
          v-for="(item, index) in filteredRecommendations"
          :key="item.offering.offeringId"
          class="recommendation-item"
        >
          <div class="rec-rank" :class="rankClass(index)">
            {{ index + 1 }}
          </div>
          <div class="rec-content">
            <div class="rec-header">
              <span class="rec-course-id">{{ item.offering.courseId }}</span>
              <span class="rec-course-title">{{ item.offering.courseTitle }}</span>
              <el-tag size="small" effect="plain" class="rec-dept-tag">
                {{ item.offering.department }}
              </el-tag>
              <el-tag size="small" effect="plain">
                {{ item.offering.credits }} 学分
              </el-tag>
            </div>
            <div class="rec-info">
              <span class="rec-info-item">
                <el-icon><User /></el-icon>
                {{ item.offering.professorName || '待指派' }}
              </span>
              <span class="rec-info-item">
                <el-icon><Clock /></el-icon>
                {{ item.offering.days }} {{ item.offering.times }}
              </span>
              <span class="rec-info-item">
                <el-icon><Location /></el-icon>
                {{ item.offering.room || '待分配' }}
              </span>
              <span class="rec-info-item">
                <el-icon><Star /></el-icon>
                评分 {{ item.averageRating.toFixed(1) }}
              </span>
            </div>
            <div class="rec-reason">
              <el-icon><InfoFilled /></el-icon>
              <span>{{ item.reason }}</span>
            </div>
            <div class="rec-capacity">
              <span class="capacity-text">
                剩余名额:
                <strong :class="remainingClass(item)">
                  {{ item.maxCapacity - item.enrolledCount }}
                </strong>
                / {{ item.maxCapacity }}
              </span>
              <el-progress
                :percentage="capacityPercent(item)"
                :stroke-width="8"
                :color="capacityColor(item)"
                :show-text="false"
                class="capacity-bar"
              />
            </div>
          </div>
          <div class="rec-action">
            <el-button
              type="primary"
              :disabled="item.maxCapacity - item.enrolledCount <= 0"
              @click="handleSelect(item)"
            >
              <el-icon><Plus /></el-icon>
              选择
            </el-button>
            <div class="rec-score">
              推荐分 {{ item.score.toFixed(1) }}
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </AppLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Refresh, MagicStick, User, Clock, Location, Star,
  InfoFilled, Plus
} from '@element-plus/icons-vue'
import { recommendationApi } from '../api/recommendation'
import { scheduleApi } from '../api/courses'
import AppLayout from '../components/AppLayout.vue'

const semester = import.meta.env.VITE_CURRENT_SEMESTER || '2026-FALL'
const loading = ref(false)
const recommendations = ref([])
const activeFilter = ref('all')

const filterMap = {
  all: () => true,
  prerequisite: (item) => item.reason.includes('先修'),
  department: (item) => item.reason.includes('院系'),
  popular: (item) => item.reason.includes('热门'),
  credit: (item) => item.reason.includes('学分')
}

const filteredRecommendations = computed(() => {
  return recommendations.value.filter(filterMap[activeFilter.value] || (() => true))
})

onMounted(loadRecommendations)

async function loadRecommendations() {
  loading.value = true
  try {
    const res = await recommendationApi.getRecommendations(semester)
    recommendations.value = res.data
  } catch (e) {
    ElMessage.error('加载推荐失败: ' + (e.response?.data?.message || e.message))
  } finally {
    loading.value = false
  }
}

async function handleSelect(item) {
  try {
    const selections = [{
      offeringId: item.offering.offeringId,
      kind: 'PRIMARY'
    }]
    await scheduleApi.selectOfferings(semester, selections)
    ElMessage.success(`已将 ${item.offering.courseTitle} 加入课表`)
    await loadRecommendations()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '加入课表失败')
  }
}

function rankClass(index) {
  if (index === 0) return 'rank-gold'
  if (index === 1) return 'rank-silver'
  if (index === 2) return 'rank-bronze'
  return ''
}

function capacityPercent(item) {
  return item.maxCapacity > 0 ? Math.round((item.enrolledCount / item.maxCapacity) * 100) : 0
}

function capacityColor(item) {
  const ratio = item.maxCapacity > 0 ? item.enrolledCount / item.maxCapacity : 1
  if (ratio >= 1) return '#F56C6C'
  if (ratio >= 0.8) return '#E6A23C'
  return '#67C23A'
}

function remainingClass(item) {
  const remaining = item.maxCapacity - item.enrolledCount
  if (remaining <= 0) return 'text-danger'
  if (remaining <= 2) return 'text-warning'
  return 'text-success'
}
</script>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-label {
  font-weight: 500;
  color: var(--color-text-secondary);
  font-size: 14px;
  white-space: nowrap;
}

.recommend-card {
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

.recommendation-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recommendation-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  transition: all var(--transition-normal);
  background: var(--color-bg-elevated);
}

.recommendation-item:hover {
  border-color: var(--color-primary-lighter);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.rec-rank {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  background: var(--color-surface-hover);
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.rank-gold {
  background: linear-gradient(135deg, #FFD700, #FFA500);
  color: white;
}

.rank-silver {
  background: linear-gradient(135deg, #C0C0C0, #A0A0A0);
  color: white;
}

.rank-bronze {
  background: linear-gradient(135deg, #CD7F32, #B8860B);
  color: white;
}

.rec-content {
  flex: 1;
  min-width: 0;
}

.rec-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.rec-course-id {
  font-weight: 700;
  color: var(--color-primary);
  font-family: var(--font-mono);
  font-size: 13px;
}

.rec-course-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--color-text);
}

.rec-dept-tag {
  margin-left: 4px;
}

.rec-info {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.rec-info-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.rec-reason {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-primary);
  background: var(--color-primary-bg);
  padding: 6px 10px;
  border-radius: var(--radius-md);
  margin-bottom: 8px;
}

.rec-capacity {
  display: flex;
  align-items: center;
  gap: 12px;
}

.capacity-text {
  font-size: 12px;
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.capacity-bar {
  flex: 1;
  max-width: 120px;
}

.text-danger { color: #F56C6C; font-weight: 600; }
.text-warning { color: #E6A23C; font-weight: 600; }
.text-success { color: #67C23A; font-weight: 600; }

.rec-action {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.rec-score {
  font-size: 11px;
  color: var(--color-text-muted);
  font-family: var(--font-mono);
}

.empty-icon {
  color: var(--color-text-muted);
  opacity: 0.5;
}

@media (max-width: 767px) {
  .filter-row {
    flex-wrap: wrap;
  }

  .filter-label {
    width: 100%;
  }

  .el-radio-group {
    flex-wrap: wrap;
  }

  .recommendation-item {
    flex-direction: column;
    gap: 12px;
  }

  .rec-action {
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
    width: 100%;
    padding-top: 8px;
    border-top: 1px solid var(--color-border-light);
  }

  .rec-score {
    font-size: 12px;
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
