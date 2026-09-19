<template>
  <AppLayout :title="pageTitle">
    <template #actions>
      <el-button @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
    </template>

    <!-- 课程信息卡 -->
    <div class="course-info-card">
      <div class="course-info-main">
        <h2 class="course-title">{{ courseTitle }}</h2>
        <div class="course-meta">
          <el-tag v-if="courseId" type="primary" effect="plain">{{ courseId }}</el-tag>
          <el-tag v-if="semester" type="info" effect="plain">{{ semester }}</el-tag>
          <el-tag v-if="professorName" effect="plain">教授: {{ professorName }}</el-tag>
        </div>
      </div>
      <div class="course-rating-summary">
        <div class="rating-big">
          <el-rate
            :model-value="averageRating"
            disabled
            show-score
            text-color="#ff9900"
            :size="'large'"
          />
          <div class="rating-count">{{ reviews.length }} 条评价</div>
        </div>
      </div>
    </div>

    <!-- 提交评价表单 -->
    <el-card class="submit-card" v-if="isStudent">
      <template #header>
        <div class="card-title">
          <el-icon><Edit /></el-icon>
          <span>{{ myReview ? '修改我的评价' : '提交评价' }}</span>
        </div>
      </template>

      <el-form :model="reviewForm" label-width="80px" class="review-form">
        <el-form-item label="评分">
          <el-rate
            v-model="reviewForm.rating"
            show-score
            text-color="#ff9900"
            :colors="['#99A9BF', '#F7BA2A', '#FF9900']"
          />
        </el-form-item>
        <el-form-item label="评论">
          <el-input
            v-model="reviewForm.comment"
            type="textarea"
            :rows="3"
            :maxlength="500"
            show-word-limit
            placeholder="分享你对这门课程的看法(可选)"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="submitting"
            :disabled="!reviewForm.rating"
            @click="handleSubmitReview"
          >
            {{ myReview ? '修改评价' : '提交评价' }}
          </el-button>
          <el-button
            v-if="myReview"
            type="danger"
            plain
            @click="handleDeleteReview"
          >
            删除评价
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 评价列表 -->
    <el-card>
      <template #header>
        <div class="card-title">
          <el-icon><ChatLineSquare /></el-icon>
          <span>全部评价</span>
        </div>
      </template>

      <el-skeleton v-if="loading" :rows="4" animated />

      <el-empty v-else-if="reviews.length === 0" description="暂无评价">
        <template #image>
          <div class="empty-icon">
            <el-icon :size="48"><ChatLineSquare /></el-icon>
          </div>
        </template>
      </el-empty>

      <div v-else class="review-list">
        <div v-for="review in reviews" :key="review.id" class="review-item">
          <div class="review-header">
            <div class="review-user">
              <el-avatar :size="32" class="user-avatar">
                {{ (review.studentName || '?')[0] }}
              </el-avatar>
              <div class="user-info">
                <span class="user-name">{{ review.studentName }}</span>
                <span class="review-time">{{ formatTime(review.createdAt) }}</span>
              </div>
            </div>
            <el-rate :model-value="review.rating" disabled :size="'small'" />
          </div>
          <div class="review-content" v-if="review.comment">
            {{ review.comment }}
          </div>
          <div class="review-actions" v-if="isOwner(review)">
            <el-button text size="small" @click="startEdit(review)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button text size="small" type="danger" @click="handleDeleteReviewById(review.id)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </div>
        </div>
      </div>
    </el-card>
  </AppLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Edit, ChatLineSquare, Delete
} from '@element-plus/icons-vue'
import { reviewApi } from '../api/review'
import { catalogApi } from '../api/courses'
import { useAuthStore } from '../stores/auth'
import AppLayout from '../components/AppLayout.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const offeringId = computed(() => Number(route.params.offeringId))
const courseId = ref('')
const courseTitle = ref('')
const semester = ref('')
const professorName = ref('')
const reviews = ref([])
const loading = ref(false)
const submitting = ref(false)
const averageRating = ref(0)
const myReview = ref(null)

const reviewForm = reactive({
  rating: 0,
  comment: ''
})

const isStudent = computed(() => auth.role === 'STUDENT')

const pageTitle = computed(() => {
  return courseId.value ? `${courseId.value} 课程评价` : '课程评价'
})

onMounted(async () => {
  await loadOfferingInfo()
  await loadReviews()
})

async function loadOfferingInfo() {
  try {
    const semesterParam = import.meta.env.VITE_CURRENT_SEMESTER || '2026-FALL'
    const res = await catalogApi.getOfferings(semesterParam)
    const offering = res.data.find(o => o.offeringId === offeringId.value)
    if (offering) {
      courseId.value = offering.courseId
      courseTitle.value = offering.courseTitle || offering.courseId
      semester.value = offering.semester
      professorName.value = offering.professorName
    }
  } catch {
    // 降级: 不影响评价功能
  }
}

async function loadReviews() {
  loading.value = true
  try {
    const res = await reviewApi.getOfferingReviews(offeringId.value)
    reviews.value = res.data
    // 找出我的评价
    myReview.value = reviews.value.find(r => r.studentId === auth.user?.userId) || null
    if (myReview.value) {
      reviewForm.rating = myReview.value.rating
      reviewForm.comment = myReview.value.comment || ''
    }
    // 计算平均评分
    if (reviews.value.length > 0) {
      const sum = reviews.value.reduce((acc, r) => acc + r.rating, 0)
      averageRating.value = Math.round((sum / reviews.value.length) * 10) / 10
    }
  } catch {
    ElMessage.error('加载评价失败')
  } finally {
    loading.value = false
  }
}

function startEdit(review) {
  myReview.value = review
  reviewForm.rating = review.rating
  reviewForm.comment = review.comment || ''
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function handleSubmitReview() {
  if (!reviewForm.rating) {
    ElMessage.warning('请选择评分')
    return
  }
  submitting.value = true
  try {
    if (myReview.value) {
      await reviewApi.updateReview(myReview.value.id, reviewForm.rating, reviewForm.comment)
      ElMessage.success('评价已修改')
    } else {
      await reviewApi.submitReview(offeringId.value, reviewForm.rating, reviewForm.comment)
      ElMessage.success('评价已提交')
    }
    await loadReviews()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleDeleteReview() {
  if (!myReview.value) return
  try {
    await ElMessageBox.confirm('确认删除你的评价?', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await reviewApi.deleteReview(myReview.value.id)
    ElMessage.success('评价已删除')
    myReview.value = null
    reviewForm.rating = 0
    reviewForm.comment = ''
    await loadReviews()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

async function handleDeleteReviewById(reviewId) {
  try {
    await ElMessageBox.confirm('确认删除该评价?', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await reviewApi.deleteReview(reviewId)
    ElMessage.success('评价已删除')
    await loadReviews()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

function isOwner(review) {
  return review.studentId === auth.user?.userId
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function goBack() {
  router.back()
}
</script>

<style scoped>
.course-info-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  border-radius: var(--radius-xl);
  padding: 24px 32px;
  margin-bottom: 20px;
  color: white;
}

.course-info-main {
  flex: 1;
}

.course-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 8px 0;
}

.course-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.course-meta .el-tag {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
  color: white;
}

.course-rating-summary {
  padding-left: 40px;
}

.rating-big {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.rating-count {
  font-size: 13px;
  opacity: 0.8;
}

.submit-card {
  margin-bottom: 20px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 14px;
}

.review-form {
  max-width: 500px;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-item {
  padding: 16px;
  border-radius: var(--radius-lg);
  background: var(--color-bg-lighter, #f5f7fa);
  border: 1px solid var(--color-border-lighter, #ebeef5);
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.review-user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  background: var(--color-primary);
  color: white;
  font-weight: 600;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-weight: 600;
  font-size: 14px;
}

.review-time {
  font-size: 12px;
  color: var(--color-text-muted);
}

.review-content {
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text-regular, #606266);
  margin-bottom: 8px;
}

.review-actions {
  display: flex;
  gap: 4px;
  justify-content: flex-end;
}

.empty-icon {
  color: var(--color-text-muted);
  opacity: 0.5;
}

/* --- Mobile Responsive --- */
@media (max-width: 768px) {
  .course-info-card {
    flex-direction: column;
    gap: 16px;
    padding: 20px;
    text-align: center;
  }

  .course-rating-summary {
    padding-left: 0;
  }

  .course-meta {
    justify-content: center;
  }

  .course-title {
    font-size: 18px;
  }

  .review-form {
    max-width: 100%;
  }

  .review-header {
    flex-wrap: wrap;
    gap: 8px;
  }

  .review-user {
    flex-wrap: wrap;
  }
}

@media (max-width: 480px) {
  .course-info-card {
    padding: 16px;
    border-radius: var(--radius-lg);
  }

  .course-title {
    font-size: 16px;
  }

  .review-item {
    padding: 12px;
  }
}
</style>
