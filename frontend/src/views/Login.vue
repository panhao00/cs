<template>
  <div class="login-container">
    <el-card class="login-card" shadow="always">
      <template #header>
        <div class="card-header">
          <h2>课程注册系统</h2>
          <p class="subtitle">Wylie College · Course Registration System</p>
        </div>
      </template>

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
            placeholder="用户名"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            size="large"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            style="width: 100%"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
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

      <el-divider>默认测试账号</el-divider>
      <div class="test-accounts">
        <el-tag @click="fillAccount('registrar', 'registrar')" class="account-tag">
          Registrar
        </el-tag>
        <el-tag type="success" @click="fillAccount('professor', 'professor')" class="account-tag">
          Professor
        </el-tag>
        <el-tag type="warning" @click="fillAccount('student', 'student')" class="account-tag">
          Student
        </el-tag>
      </div>

      <div class="register-link">
        还没有账号? <router-link to="/signup">立即注册</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
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
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 420px;
  max-width: 90vw;
}

.card-header {
  text-align: center;
}

.card-header h2 {
  margin: 0 0 8px 0;
  color: #303133;
}

.subtitle {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.test-accounts {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.account-tag {
  cursor: pointer;
}

.register-link {
  text-align: center;
  margin-top: 12px;
  font-size: 13px;
  color: #909399;
}

.register-link a {
  color: #409eff;
  text-decoration: none;
}
</style>
