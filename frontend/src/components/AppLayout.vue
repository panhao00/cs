<template>
  <div class="app-layout">
    <!-- Mobile overlay -->
    <div
      class="mobile-overlay"
      :class="{ visible: sidebarOpen }"
      @click="sidebarOpen = false"
    />

    <!-- Sidebar -->
    <aside class="app-sidebar" :class="{ open: sidebarOpen }">
      <div class="sidebar-brand">
        <div class="brand-mark">
          <span>&#9960;</span>
        </div>
        <div class="brand-text">
          <span class="brand-college">WYLIE COLLEGE</span>
          <span class="brand-system">CRS</span>
        </div>
        <button class="sidebar-close-btn" @click="sidebarOpen = false" aria-label="Close menu">
          <el-icon><Close /></el-icon>
        </button>
      </div>

      <nav class="sidebar-nav">
        <router-link
          to="/"
          class="nav-item"
          :class="{ active: $route.path === '/' }"
          @click="sidebarOpen = false"
        >
          <el-icon><HomeFilled /></el-icon>
          <span>主页</span>
        </router-link>

        <!-- Student nav -->
        <template v-if="role === 'STUDENT'">
          <router-link to="/register" class="nav-item" :class="{ active: $route.path === '/register' }" @click="sidebarOpen = false">
            <el-icon><DocumentAdd /></el-icon>
            <span>选课</span>
          </router-link>
          <router-link to="/report-card" class="nav-item" :class="{ active: $route.path === '/report-card' }" @click="sidebarOpen = false">
            <el-icon><DataAnalysis /></el-icon>
            <span>成绩单</span>
          </router-link>
          <router-link to="/grade-trend" class="nav-item" :class="{ active: $route.path === '/grade-trend' }" @click="sidebarOpen = false">
            <el-icon><TrendCharts /></el-icon>
            <span>成绩趋势</span>
          </router-link>
          <router-link to="/recommendations" class="nav-item" :class="{ active: $route.path === '/recommendations' }" @click="sidebarOpen = false">
            <el-icon><MagicStick /></el-icon>
            <span>智能推荐</span>
          </router-link>
          <router-link to="/waitlist" class="nav-item" :class="{ active: $route.path === '/waitlist' }" @click="sidebarOpen = false">
            <el-icon><List /></el-icon>
            <span>候补名单</span>
          </router-link>
          <router-link to="/my-reviews" class="nav-item" :class="{ active: $route.path === '/my-reviews' }" @click="sidebarOpen = false">
            <el-icon><ChatDotRound /></el-icon>
            <span>我的评价</span>
          </router-link>
        </template>

        <!-- Professor nav -->
        <template v-if="role === 'PROFESSOR'">
          <router-link to="/teach" class="nav-item" :class="{ active: $route.path === '/teach' }" @click="sidebarOpen = false">
            <el-icon><Reading /></el-icon>
            <span>选课授课</span>
          </router-link>
          <router-link to="/grades" class="nav-item" :class="{ active: $route.path === '/grades' }" @click="sidebarOpen = false">
            <el-icon><EditPen /></el-icon>
            <span>提交成绩</span>
          </router-link>
        </template>

        <!-- Registrar nav -->
        <template v-if="role === 'REGISTRAR'">
          <router-link to="/users" class="nav-item" :class="{ active: $route.path === '/users' }" @click="sidebarOpen = false">
            <el-icon><User /></el-icon>
            <span>用户维护</span>
          </router-link>
          <router-link to="/registration" class="nav-item" :class="{ active: $route.path === '/registration' }" @click="sidebarOpen = false">
            <el-icon><Lock /></el-icon>
            <span>关闭注册</span>
          </router-link>
          <router-link to="/settings" class="nav-item" :class="{ active: $route.path === '/settings' }" @click="sidebarOpen = false">
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </router-link>
          <router-link to="/enrollment-dashboard" class="nav-item" :class="{ active: $route.path === '/enrollment-dashboard' }" @click="sidebarOpen = false">
            <el-icon><DataAnalysis /></el-icon>
            <span>选课统计</span>
          </router-link>
          <router-link to="/audit-logs" class="nav-item" :class="{ active: $route.path === '/audit-logs' }" @click="sidebarOpen = false">
            <el-icon><Document /></el-icon>
            <span>审计日志</span>
          </router-link>
        </template>

        <div class="nav-divider"></div>

        <router-link
          to="/notifications"
          class="nav-item"
          :class="{ active: $route.path === '/notifications' }"
          @click="sidebarOpen = false"
        >
          <el-icon><Bell /></el-icon>
          <span>通知中心</span>
          <span v-if="unreadCount > 0" class="nav-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        </router-link>
        <router-link
          to="/change-password"
          class="nav-item"
          :class="{ active: $route.path === '/change-password' }"
          @click="sidebarOpen = false"
        >
          <el-icon><Key /></el-icon>
          <span>修改密码</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="sidebar-version">v0.1.0</div>
      </div>
    </aside>

    <!-- Main content area -->
    <div class="app-main-area">
      <!-- Top header bar -->
      <header class="app-header">
        <div class="header-left">
          <button class="hamburger-btn" @click="sidebarOpen = !sidebarOpen" aria-label="Toggle menu">
            <el-icon :size="20"><Menu /></el-icon>
          </button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="title">{{ title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <slot name="actions" />
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-avatar-btn">
              <div class="avatar-circle">
                {{ avatarInitial }}
              </div>
              <span class="user-name">{{ userName }}</span>
              <el-icon class="avatar-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <div class="dropdown-user-info">
                  <div class="dropdown-user-name">{{ userName }}</div>
                  <div class="dropdown-user-role">{{ roleLabel }}</div>
                </div>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- Page content -->
      <main class="app-content">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  HomeFilled, DocumentAdd, DataAnalysis, Reading, EditPen,
  User, Lock, Bell, ArrowDown, SwitchButton, Close, Menu,
  TrendCharts, MagicStick, List, Key, Setting, Document, ChatDotRound
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { notificationApi } from '../api/notifications'

const props = defineProps({
  title: { type: String, required: true }
})

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const unreadCount = ref(0)
const sidebarOpen = ref(false)
let refreshTimer = null

const role = computed(() => auth.role)
const userName = computed(() => auth.user?.name || '用户')
const avatarInitial = computed(() => {
  const name = auth.user?.name || '用'
  return name.charAt(0).toUpperCase()
})

const roleLabel = computed(() => {
  const map = { STUDENT: '学生', PROFESSOR: '教授', REGISTRAR: '注册主任' }
  return map[auth.role] || auth.role
})

async function refreshUnreadCount() {
  if (auth.role !== 'STUDENT') return
  try {
    const res = await notificationApi.getUnreadCount()
    unreadCount.value = res.data.count
  } catch {
    // 静默失败
  }
}

onMounted(() => {
  refreshUnreadCount()
  if (auth.role === 'STUDENT') {
    refreshTimer = setInterval(refreshUnreadCount, 30000)
  }
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})

function handleCommand(command) {
  if (command === 'logout') {
    auth.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* --- Sidebar --- */
.app-sidebar {
  width: 240px;
  min-width: 240px;
  background: var(--color-primary-dark);
  display: flex;
  flex-direction: column;
  color: var(--color-text-inverse);
  overflow: hidden;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 20px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 10px;
  font-size: 18px;
  flex-shrink: 0;
}

.brand-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.brand-college {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.15em;
  opacity: 0.9;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.brand-system {
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.1em;
  opacity: 0.5;
  text-transform: uppercase;
}

/* Navigation */
.sidebar-nav {
  flex: 1;
  padding: 12px 10px;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 10px;
  color: rgba(255, 255, 255, 0.6);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition-fast);
  margin-bottom: 2px;
  position: relative;
}

.nav-item:hover {
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.08);
}

.nav-item.active {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.14);
  font-weight: 600;
}

.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: var(--color-accent-light);
  border-radius: 0 3px 3px 0;
}

.nav-badge {
  margin-left: auto;
  background: var(--color-danger);
  color: white;
  font-size: 11px;
  font-weight: 600;
  padding: 1px 7px;
  border-radius: 10px;
  min-width: 20px;
  text-align: center;
}

.nav-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.08);
  margin: 8px 14px;
}

/* Sidebar footer */
.sidebar-footer {
  padding: 12px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.sidebar-version {
  font-size: 11px;
  opacity: 0.3;
  text-align: center;
}

/* --- Main area --- */
.app-main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

/* Header */
.app-header {
  height: 56px;
  min-height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: var(--color-bg-elevated);
  border-bottom: 1px solid var(--color-border);
  z-index: var(--z-sticky);
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* User avatar button */
.user-avatar-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-md);
  transition: background var(--transition-fast);
}

.user-avatar-btn:hover {
  background: var(--color-surface-hover);
}

.avatar-circle {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
}

.avatar-arrow {
  color: var(--color-text-muted);
  font-size: 12px;
}

/* Dropdown */
.dropdown-user-info {
  padding: 8px 12px;
  border-bottom: 1px solid var(--color-border-light);
  margin-bottom: 4px;
}

.dropdown-user-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--color-text);
}

.dropdown-user-role {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 2px;
}

/* Content */
.app-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: var(--color-bg);
}

/* --- Mobile Overlay --- */
.mobile-overlay {
  display: none;
}

/* --- Hamburger Button (hidden on desktop) --- */
.hamburger-btn {
  display: none;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  color: var(--color-text);
  transition: background var(--transition-fast);
  margin-right: 8px;
  flex-shrink: 0;
}

.hamburger-btn:hover {
  background: var(--color-surface-hover);
}

/* --- Sidebar Close Button (hidden on desktop) --- */
.sidebar-close-btn {
  display: none;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-sm);
  cursor: pointer;
  color: white;
  margin-left: auto;
  flex-shrink: 0;
  transition: background var(--transition-fast);
}

.sidebar-close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

/* ============================================================
   MOBILE: Tablet (max-width: 768px)
   ============================================================ */
@media (max-width: 768px) {
  /* Show hamburger */
  .hamburger-btn {
    display: flex;
  }

  /* Sidebar: off-screen by default, slides in */
  .app-sidebar {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    z-index: var(--z-modal);
    transform: translateX(-100%);
    transition: transform var(--transition-slow);
    width: 260px;
    min-width: 260px;
  }

  .app-sidebar.open {
    transform: translateX(0);
  }

  /* Show close button in sidebar */
  .sidebar-close-btn {
    display: flex;
  }

  .sidebar-brand {
    padding-right: 12px;
  }

  /* Overlay */
  .mobile-overlay {
    display: block;
    position: fixed;
    inset: 0;
    background: var(--color-bg-overlay);
    z-index: calc(var(--z-modal) - 1);
    opacity: 0;
    pointer-events: none;
    transition: opacity var(--transition-slow);
  }

  .mobile-overlay.visible {
    opacity: 1;
    pointer-events: auto;
  }

  /* Main area fills full width */
  .app-main-area {
    width: 100%;
  }

  /* Header */
  .app-header {
    padding: 0 12px;
  }

  /* Hide username text on tablet */
  .user-name {
    display: none;
  }

  .avatar-arrow {
    display: none;
  }

  /* Content padding */
  .app-content {
    padding: 16px;
  }
}

/* ============================================================
   MOBILE: Phone (max-width: 480px)
   ============================================================ */
@media (max-width: 480px) {
  .app-header {
    height: 48px;
    min-height: 48px;
  }

  .app-content {
    padding: 12px;
  }

  /* Header actions slot wrapping */
  .header-right {
    gap: 6px;
  }
}
</style>
