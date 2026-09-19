import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import Login from '../Login.vue'
import { useAuthStore } from '../../stores/auth'

// Mock the auth store module
vi.mock('../../stores/auth', () => ({
  useAuthStore: vi.fn()
}))

// Mock the icons module
vi.mock('@element-plus/icons-vue', () => ({
  User: {},
  Lock: {},
  Setting: {},
  UserFilled: {},
  Reading: {}
}))

describe('Login.vue', () => {
  let pinia
  let router
  let mockLogin
  let mockRouterPush

  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()

    pinia = createPinia()
    setActivePinia(pinia)

    mockLogin = vi.fn()
    vi.mocked(useAuthStore).mockReturnValue({
      login: mockLogin,
      isLoggedIn: false,
      user: null,
      token: '',
      role: ''
    })

    mockRouterPush = vi.fn()
    router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', name: 'home', component: { template: '<div />' } },
        { path: '/login', name: 'login', component: Login },
        { path: '/signup', name: 'signup', component: { template: '<div />' } }
      ]
    })
    router.push = mockRouterPush
  })

  function createWrapper(options = {}) {
    return mount(Login, {
      global: {
        plugins: [pinia, router],
        stubs: {
          'el-form': {
            template: '<form @submit.prevent><slot /></form>',
            props: ['model', 'rules', 'labelWidth'],
            methods: {
              validate(callback) {
                callback(true)
              }
            }
          },
          'el-form-item': {
            template: '<div class="el-form-item"><slot /></div>',
            props: ['prop']
          },
          'el-input': {
            template: '<input :type="type" :placeholder="placeholder" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" @keyup.enter="$emit(\'keyup.enter\', $event)" />',
            props: ['modelValue', 'placeholder', 'size', 'type', 'prefixIcon', 'showPassword'],
            emits: ['update:modelValue', 'keyup.enter']
          },
          'el-button': {
            template: '<button class="el-button submit-btn" :disabled="loading" @click="$emit(\'click\', $event)"><slot /></button>',
            props: ['type', 'size', 'loading'],
            emits: ['click']
          },
          'el-alert': {
            template: '<div class="el-alert" v-if="title">{{ title }}</div>',
            props: ['title', 'type', 'closable', 'showIcon']
          },
          'el-tag': {
            template: '<span class="account-tag" @click="$emit(\'click\', $event)"><slot /></span>',
            props: ['type', 'effect'],
            emits: ['click']
          },
          'el-icon': {
            template: '<span class="el-icon"><slot /></span>'
          },
          'router-link': {
            template: '<a :href="to"><slot /></a>',
            props: ['to']
          }
        }
      },
      ...options
    })
  }

  // Helper: set form values and trigger login
  async function fillFormAndLogin(wrapper, username = 'student', password = 'student') {
    const inputs = wrapper.findAll('input')
    await inputs[0].setValue(username)
    await inputs[1].setValue(password)
    await wrapper.find('.submit-btn').trigger('click')
    await flushPromises()
  }

  describe('组件渲染', () => {
    it('渲染登录页面的基本结构', () => {
      const wrapper = createWrapper()
      expect(wrapper.find('.auth-container').exists()).toBe(true)
      expect(wrapper.find('.auth-card').exists()).toBe(true)
      expect(wrapper.find('.brand-name').text()).toBe('WYLIE COLLEGE')
    })

    it('显示表单标题和描述', () => {
      const wrapper = createWrapper()
      expect(wrapper.find('.form-title').text()).toBe('欢迎回来')
      expect(wrapper.find('.form-desc').text()).toBe('登录您的账号')
    })

    it('渲染用户名和密码输入框', () => {
      const wrapper = createWrapper()
      const inputs = wrapper.findAll('input')
      expect(inputs.length).toBe(2)
      expect(inputs[0].attributes('placeholder')).toBe('用户名')
      expect(inputs[1].attributes('placeholder')).toBe('密码')
    })

    it('渲染登录按钮', () => {
      const wrapper = createWrapper()
      const button = wrapper.find('.submit-btn')
      expect(button.exists()).toBe(true)
      expect(button.text()).toBe('登录')
    })

    it('渲染快速登录标签', () => {
      const wrapper = createWrapper()
      const tags = wrapper.findAll('.account-tag')
      expect(tags.length).toBe(3)
    })

    it('渲染注册链接', () => {
      const wrapper = createWrapper()
      expect(wrapper.find('.auth-link').text()).toContain('注册账号')
    })
  })

  describe('表单输入绑定', () => {
    it('用户名输入框双向绑定到 form.username', async () => {
      const wrapper = createWrapper()
      const usernameInput = wrapper.findAll('input')[0]
      await usernameInput.setValue('testuser')
      expect(usernameInput.element.value).toBe('testuser')
    })

    it('密码输入框双向绑定到 form.password', async () => {
      const wrapper = createWrapper()
      const passwordInput = wrapper.findAll('input')[1]
      await passwordInput.setValue('testpass')
      expect(passwordInput.element.value).toBe('testpass')
    })
  })

  describe('快速登录标签点击', () => {
    it('点击注册主任标签填充对应账号密码', async () => {
      const wrapper = createWrapper()
      const tags = wrapper.findAll('.account-tag')
      await tags[0].trigger('click')

      const inputs = wrapper.findAll('input')
      expect(inputs[0].element.value).toBe('registrar')
      expect(inputs[1].element.value).toBe('registrar')
    })

    it('点击教授标签填充对应账号密码', async () => {
      const wrapper = createWrapper()
      const tags = wrapper.findAll('.account-tag')
      await tags[1].trigger('click')

      const inputs = wrapper.findAll('input')
      expect(inputs[0].element.value).toBe('professor')
      expect(inputs[1].element.value).toBe('professor')
    })

    it('点击学生标签填充对应账号密码', async () => {
      const wrapper = createWrapper()
      const tags = wrapper.findAll('.account-tag')
      await tags[2].trigger('click')

      const inputs = wrapper.findAll('input')
      expect(inputs[0].element.value).toBe('student')
      expect(inputs[1].element.value).toBe('student')
    })

    it('点击标签后清除错误信息', async () => {
      const wrapper = createWrapper()

      // First trigger an error
      mockLogin.mockRejectedValue({ response: { data: { message: '登录失败' } } })
      await fillFormAndLogin(wrapper, 'wrong', 'wrong')
      expect(wrapper.find('.el-alert').exists()).toBe(true)

      // Click a tag to clear the error
      const tags = wrapper.findAll('.account-tag')
      await tags[0].trigger('click')
      expect(wrapper.find('.el-alert').exists()).toBe(false)
    })
  })

  describe('登录按钮点击', () => {
    it('点击登录按钮调用 handleLogin', async () => {
      mockLogin.mockResolvedValue({})
      const wrapper = createWrapper()

      await fillFormAndLogin(wrapper, 'student', 'student')

      expect(mockLogin).toHaveBeenCalledTimes(1)
      expect(mockLogin).toHaveBeenCalledWith('student', 'student')
    })

    it('密码框按回车触发登录', async () => {
      mockLogin.mockResolvedValue({})
      const wrapper = createWrapper()

      const inputs = wrapper.findAll('input')
      await inputs[0].setValue('student')
      await inputs[1].setValue('student')
      await inputs[1].trigger('keyup.enter')
      await flushPromises()

      expect(mockLogin).toHaveBeenCalledWith('student', 'student')
    })
  })

  describe('表单验证（空用户名/密码）', () => {
    it('用户名为空时阻止提交', async () => {
      // Override validate to simulate validation failure
      const wrapper = createWrapper({
        global: {
          stubs: {
            'el-form': {
              template: '<form @submit.prevent><slot /></form>',
              props: ['model', 'rules', 'labelWidth'],
              methods: {
                validate(callback) {
                  callback(false)
                }
              }
            }
          }
        }
      })

      await wrapper.find('.submit-btn').trigger('click')
      await flushPromises()

      expect(mockLogin).not.toHaveBeenCalled()
    })
  })

  describe('登录成功跳转', () => {
    it('登录成功后调用 router.push 跳转到首页', async () => {
      mockLogin.mockResolvedValue({})
      const wrapper = createWrapper()

      await fillFormAndLogin(wrapper, 'student', 'student')

      expect(mockRouterPush).toHaveBeenCalledWith('/')
    })

    it('登录成功后清除错误信息', async () => {
      mockLogin.mockResolvedValue({})
      const wrapper = createWrapper()

      // Ensure no error shown initially
      expect(wrapper.find('.el-alert').exists()).toBe(false)

      await fillFormAndLogin(wrapper, 'student', 'student')

      expect(wrapper.find('.el-alert').exists()).toBe(false)
    })
  })

  describe('登录失败显示错误', () => {
    it('登录失败时显示服务端返回的错误信息', async () => {
      mockLogin.mockRejectedValue({
        response: { data: { message: '用户名或密码错误' } }
      })
      const wrapper = createWrapper()

      await fillFormAndLogin(wrapper, 'wrong', 'wrong')

      const alert = wrapper.find('.el-alert')
      expect(alert.exists()).toBe(true)
      expect(alert.text()).toBe('用户名或密码错误')
    })

    it('无 response message 时显示默认错误信息', async () => {
      mockLogin.mockRejectedValue(new Error('Network Error'))
      const wrapper = createWrapper()

      await fillFormAndLogin(wrapper, 'student', 'student')

      const alert = wrapper.find('.el-alert')
      expect(alert.exists()).toBe(true)
      expect(alert.text()).toBe('登录失败,请检查用户名和密码')
    })

    it('登录失败后可再次尝试登录', async () => {
      mockLogin
        .mockRejectedValueOnce({ response: { data: { message: '密码错误' } } })
        .mockResolvedValueOnce({})
      const wrapper = createWrapper()

      // First attempt fails
      await fillFormAndLogin(wrapper, 'student', 'wrong')
      expect(wrapper.find('.el-alert').text()).toBe('密码错误')

      // Second attempt succeeds
      await wrapper.find('.submit-btn').trigger('click')
      await flushPromises()
      expect(mockLogin).toHaveBeenCalledTimes(2)
      expect(mockRouterPush).toHaveBeenCalledWith('/')
    })
  })
})
