<template>
  <AppLayout title="我的评价">
    <el-card v-if="loading" class="table-card">
      <el-skeleton :rows="6" animated />
    </el-card>

    <el-card v-else-if="reviews.length === 0" class="empty-card">
      <el-empty description="暂无评价记录">
        <template #image>
          <div class="empty-icon">
            <el-icon :size="48"><ChatDotRound /></el-icon>
          </div>
        </template>
        <el-button type="primary" @click="$router.push('/register')">去选课</el-button>
      </el-empty>
    </el-card>

    <div v-else class="reviews-list">
      <el-card v-for="review in reviews" :key="review.id" class="review-card">
        <div class="review-header">
          <div class="review-course">
            <span class="course-id">{{ review.courseId }}</span>
            <span class="course-title">{{ review.courseTitle || '' }}</span>
          </div>
          <div class="review-rating">
            <el-rate v-model="review.rating" disabled text-color="#ff9900" />
          </div>
        </div>
        <div class="review-meta">
          <span>{{ review.professorName }}</span>
          <span>{{ review.semester }}</span>
          <span>{{ formatTime(review.createdAt) }}</span>
        </div>
        <div class="review-comment" v-if="review.comment">
          {{ review.comment }}
        </div>
        <div class="review-actions">
          <el-button size="small" type="primary" text @click="goToDetail(review.offeringId)">
            查看课程
          </el-button>
        </div>
      </el-card>
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ChatDotRound } from '@element-plus/icons-vue'
import { reviewApi } from '../api/review'
import AppLayout from '../components/AppLayout.vue'

const router = useRouter()
const reviews = ref([])
const loading = ref(false)

onMounted(loadReviews)

async function loadReviews() {
  loading.value = true
  try {
    const res = await reviewApi.getMyReviews()
    reviews.value = res.data
  } catch {
    // silent
  } finally {
    loading.value = false
  }
}

function goToDetail(offeringId) {
  router.push(`/reviews/${offeringId}`)
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  return new Date(timeStr).toLocaleDateString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit'
  })
}
</script>

<style scoped>
.empty-card { text-align: center; }
.empty-icon { color: var(--color-text-muted); opacity: 0.5; }
.reviews-list { display: flex; flex-direction: column; gap: 12px; }
.review-card { transition: box-shadow 0.2s; }
.review-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.review-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.review-course { display: flex; align-items: center; gap: 8px; }
.course-id { font-weight: 600; font-family: var(--font-mono); color: var(--color-primary); }
.course-title { color: var(--color-text); }
.review-meta { display: flex; gap: 12px; font-size: 12px; color: var(--color-text-muted); margin-bottom: 8px; }
.review-comment { font-size: 14px; color: var(--color-text-secondary); line-height: 1.6; margin-bottom: 8px; }
.review-actions { display: flex; justify-content: flex-end; }
</style>
