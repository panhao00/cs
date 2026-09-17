<template>
  <div class="auth-container">
    <!-- Animated background shapes -->
    <div class="bg-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
      <div class="shape shape-4"></div>
      <div class="shape shape-5"></div>
      <div class="shape shape-6"></div>
    </div>

    <div class="auth-card">
      <!-- University branding -->
      <div class="brand-header">
        <div class="brand-icon">
          <span class="cap-icon">&#9960;</span>
        </div>
        <h1 class="brand-name">WYLIE COLLEGE</h1>
        <p class="brand-subtitle">Course Registration System</p>
      </div>

      <h2 class="form-title">Welcome Back</h2>
      <p class="form-desc">Sign in to your account</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            size="large"
            placeholder="Username"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            size="large"
            type="password"
            placeholder="Password"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            class="submit-btn"
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
          >
            Sign In
          </el-button>
        </el-form-item>
      </el-form>

      <el-alert
        v-if="errorMsg"
        :title="errorMsg"
        type="error"
        :closable="false"
        show-icon
      />

      <div class="divider">
        <span>Quick Access</span>
      </div>

      <div class="test-accounts">
        <el-tag
          @click="fillAccount('registrar', 'registrar')"
          class="account-tag"
          effect="plain"
        >
          <el-icon><Setting /></el-icon>
          Registrar
        </el-tag>
        <el-tag
          type="success"
          @click="fillAccount('professor', 'professor')"
          class="account-tag"
          effect="plain"
        >
          <el-icon><UserFilled /></el-icon>
          Professor
        </el-tag>
        <el-tag
          type="warning"
          @click="fillAccount('student', 'student')"
          class="account-tag"
          effect="plain"
        >
          <el-icon><Reading /></el-icon>
          Student
        </el-tag>
      </div>

      <div class="auth-link">
        Don't have an account?
        <router-link to="/signup">Create one</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Setting, UserFilled, Reading } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref()
const loading = ref(false)
const errorMsg = ref('')

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function fillAccount(username, password) {
  form.username = username
  form.password = password
  errorMsg.value = ''
}

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    errorMsg.value = ''
    try {
      await authStore.login(form.username, form.password)
      router.push('/')
    } catch (err) {
      errorMsg.value = err.response?.data?.message || '登录失败,请检查用户名和密码'
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #0f172a 0%, #1e3a5f 40%, #1e40af 70%, #3730a3 100%);
  position: relative;
  overflow: hidden;
}

/* Animated floating shapes */
.bg-shapes {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.06;
  background: white;
}

.shape-1 {
  width: 400px;
  height: 400px;
  top: -100px;
  right: -100px;
  animation: float-1 20s ease-in-out infinite;
}

.shape-2 {
  width: 250px;
  height: 250px;
  bottom: -50px;
  left: -80px;
  animation: float-2 25s ease-in-out infinite;
}

.shape-3 {
  width: 180px;
  height: 180px;
  top: 40%;
  left: 10%;
  animation: float-3 18s ease-in-out infinite;
}

.shape-4 {
  width: 120px;
  height: 120px;
  top: 20%;
  right: 20%;
  animation: float-4 22s ease-in-out infinite;
}

.shape-5 {
  width: 300px;
  height: 300px;
  bottom: 10%;
  right: 30%;
  animation: float-5 30s ease-in-out infinite;
}

.shape-6 {
  width: 80px;
  height: 80px;
  top: 60%;
  left: 40%;
  animation: float-6 15s ease-in-out infinite;
}

@keyframes float-1 {
  0%, 100% { transform: translate(0, 0) rotate(0deg) scale(1); }
  33% { transform: translate(-30px, 30px) rotate(120deg) scale(1.05); }
  66% { transform: translate(20px, -20px) rotate(240deg) scale(0.95); }
}

@keyframes float-2 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(40px, -40px) scale(1.1); }
}

@keyframes float-3 {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  50% { transform: translate(25px, 25px) rotate(180deg); }
}

@keyframes float-4 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(-20px, -15px) scale(1.15); }
  66% { transform: translate(15px, 20px) scale(0.9); }
}

@keyframes float-5 {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  50% { transform: translate(-30px, 20px) rotate(-180deg); }
}

@keyframes float-6 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(20px, -30px) scale(1.2); }
}

/* Frosted glass card */
.auth-card {
  width: 440px;
  max-width: 92vw;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  padding: 40px 36px 32px;
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.25), 0 0 0 1px rgba(255, 255, 255, 0.1) inset;
  position: relative;
  z-index: 1;
  animation: card-enter 0.6s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}

@keyframes card-enter {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* University branding */
.brand-header {
  text-align: center;
  margin-bottom: 28px;
}

.brand-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, #1e3a5f, #1e40af);
  margin-bottom: 14px;
  box-shadow: 0 4px 12px rgba(30, 58, 95, 0.3);
}

.cap-icon {
  font-size: 28px;
  line-height: 1;
  filter: grayscale(0.2);
}

.brand-name {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.2em;
  color: var(--color-primary);
}

.brand-subtitle {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--color-text-muted);
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.form-title {
  margin: 0 0 4px;
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text);
  text-align: center;
}

.form-desc {
  margin: 0 0 24px;
  font-size: 14px;
  color: var(--color-text-secondary);
  text-align: center;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
  letter-spacing: 0.03em;
}

/* Divider */
.divider {
  display: flex;
  align-items: center;
  margin: 20px 0 16px;
  gap: 12px;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--color-border);
}

.divider span {
  font-size: 12px;
  color: var(--color-text-muted);
  white-space: nowrap;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* Test accounts */
.test-accounts {
  display: flex;
  gap: 8px;
  justify-content: center;
  flex-wrap: wrap;
}

.account-tag {
  cursor: pointer;
  border-radius: 8px !important;
  padding: 6px 14px !important;
  transition: all var(--transition-fast);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.account-tag:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.auth-link {
  text-align: center;
  margin-top: 20px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.auth-link a {
  color: var(--color-primary-lighter);
  font-weight: 600;
  text-decoration: none;
  transition: color var(--transition-fast);
}

.auth-link a:hover {
  color: var(--color-primary);
}
</style>
