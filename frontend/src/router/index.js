import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/signup',
    name: 'SignUp',
    component: () => import('../views/Register.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/register',
    name: 'RegisterCourses',
    component: () => import('../views/RegisterCourses.vue'),
    meta: { requiresAuth: true, roles: ['STUDENT'] }
  },
  {
    path: '/teach',
    name: 'TeachCourses',
    component: () => import('../views/TeachCourses.vue'),
    meta: { requiresAuth: true, roles: ['PROFESSOR'] }
  },
  {
    path: '/grades',
    name: 'SubmitGrades',
    component: () => import('../views/SubmitGrades.vue'),
    meta: { requiresAuth: true, roles: ['PROFESSOR'] }
  },
  {
    path: '/report-card',
    name: 'ReportCard',
    component: () => import('../views/ReportCard.vue'),
    meta: { requiresAuth: true, roles: ['STUDENT'] }
  },
  {
    path: '/users',
    name: 'UserManagement',
    component: () => import('../views/UserManagement.vue'),
    meta: { requiresAuth: true, roles: ['REGISTRAR'] }
  },
  {
    path: '/registration',
    name: 'RegistrationManagement',
    component: () => import('../views/RegistrationManagement.vue'),
    meta: { requiresAuth: true, roles: ['REGISTRAR'] }
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('../views/Notifications.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/waitlist',
    name: 'Waitlist',
    component: () => import('../views/Waitlist.vue'),
    meta: { requiresAuth: true, roles: ['STUDENT'] }
  },
  {
    path: '/settings',
    name: 'SystemSettings',
    component: () => import('../views/SystemSettings.vue'),
    meta: { requiresAuth: true, roles: ['REGISTRAR'] }
  },
  {
    path: '/grade-trend',
    name: 'GradeTrend',
    component: () => import('../views/GradeTrend.vue'),
    meta: { requiresAuth: true, roles: ['STUDENT'] }
  },
  {
    path: '/reviews/:offeringId',
    name: 'CourseReviews',
    component: () => import('../views/CourseReviews.vue'),
    meta: { requiresAuth: true, roles: ['STUDENT'] }
  },
  {
    path: '/recommendations',
    name: 'CourseRecommendation',
    component: () => import('../views/CourseRecommendation.vue'),
    meta: { requiresAuth: true, roles: ['STUDENT'] }
  },
  {
    path: '/change-password',
    name: 'ChangePassword',
    component: () => import('../views/ChangePassword.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/audit-logs',
    name: 'AuditLogs',
    component: () => import('../views/AuditLogs.vue'),
    meta: { requiresAuth: true, roles: ['REGISTRAR'] }
  },
  {
    path: '/enrollment-dashboard',
    name: 'EnrollmentDashboard',
    component: () => import('../views/EnrollmentDashboard.vue'),
    meta: { requiresAuth: true, roles: ['REGISTRAR'] }
  },
  {
    path: '/my-reviews',
    name: 'MyReviews',
    component: () => import('../views/MyReviews.vue'),
    meta: { requiresAuth: true, roles: ['STUDENT'] }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫: 未登录 → 跳转登录页; 角色不匹配 → 跳转首页
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'Login' }
  }
  if (to.meta.public && auth.isLoggedIn) {
    return { name: 'Dashboard' }
  }
  // 角色限制
  if (to.meta.roles && !to.meta.roles.includes(auth.role)) {
    return { name: 'Dashboard' }
  }
})

export default router
