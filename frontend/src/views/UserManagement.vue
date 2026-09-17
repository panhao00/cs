<template>
  <div class="user-mgmt">
    <AppLayout title="用户维护 (Maintain Student / Professor)">
      <template #actions>
        <el-tag type="primary" effect="dark" size="small">
          <el-icon><User /></el-icon>
          注册主任
        </el-tag>
      </template>

      <!-- Filter & Actions Bar -->
      <el-card class="filter-card">
        <div class="filter-row">
          <el-select v-model="roleFilter" placeholder="按角色筛选" clearable class="filter-role">
            <el-option label="全部" :value="null" />
            <el-option label="学生 STUDENT" value="STUDENT" />
            <el-option label="教授 PROFESSOR" value="PROFESSOR" />
            <el-option label="注册主任 REGISTRAR" value="REGISTRAR" />
          </el-select>
          <el-input
            v-model="search"
            placeholder="搜索用户名/姓名"
            clearable
            class="filter-search"
            :prefix-icon="Search"
          />
          <div class="filter-actions">
            <el-button type="primary" @click="openCreate">
              <el-icon><Plus /></el-icon>
              新建用户
            </el-button>
            <el-button @click="loadUsers">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </el-card>

      <!-- User Table -->
      <el-card class="table-card">
        <el-skeleton v-if="loading" :rows="8" animated />

        <el-empty v-else-if="filteredUsers.length === 0" description="暂无用户数据">
          <template #image>
            <div class="empty-icon">
              <el-icon :size="48"><User /></el-icon>
            </div>
          </template>
        </el-empty>

        <el-table v-else :data="filteredUsers" stripe class="refined-table">
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="username" label="用户名" width="120">
            <template #default="{ row }">
              <span class="username-cell">{{ row.username }}</span>
            </template>
          </el-table-column>
          <el-table-column label="角色" width="120">
            <template #default="{ row }">
              <el-tag :type="roleTagType(row.role)" size="small" effect="dark">
                {{ roleLabel(row.role) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="姓名" width="120" />
          <el-table-column prop="department" label="院系" width="120">
            <template #default="{ row }">
              <span class="muted-text">{{ row.department || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="graduationDate" label="毕业日期" width="120">
            <template #default="{ row }">
              <span class="muted-text">{{ row.graduationDate || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="dob" label="出生日期" width="120">
            <template #default="{ row }">
              <span class="muted-text">{{ row.dob || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag
                :type="row.status === 'ACTIVE' ? 'success' : 'info'"
                size="small"
                effect="dark"
                class="status-tag"
              >
                <span class="status-dot" :class="row.status === 'ACTIVE' ? 'dot-active' : 'dot-inactive'"></span>
                {{ row.status === 'ACTIVE' ? '活跃' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <div class="action-btns">
                <el-button size="small" @click="openEdit(row)">
                  <el-icon><Edit /></el-icon>
                  编辑
                </el-button>
                <el-button
                  size="small"
                  :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
                  @click="toggleStatus(row)"
                >
                  {{ row.status === 'ACTIVE' ? '停用' : '激活' }}
                </el-button>
                <el-button
                  size="small"
                  type="danger"
                  :disabled="row.status === 'ACTIVE'"
                  @click="confirmDelete(row)"
                >
                  删除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </AppLayout>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '新建用户'"
      width="560px"
      class="user-dialog"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="登录用户名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码" :required="!isEdit" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                :placeholder="isEdit ? '留空表示不修改' : '必填'"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="角色" prop="role">
              <el-select v-model="form.role" placeholder="选择角色" style="width: 100%">
                <el-option label="学生 STUDENT" value="STUDENT" />
                <el-option label="教授 PROFESSOR" value="PROFESSOR" />
                <el-option label="注册主任 REGISTRAR" value="REGISTRAR" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="真实姓名" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12" v-if="form.role === 'PROFESSOR'">
            <el-form-item label="院系" prop="department">
              <el-input v-model="form.department" placeholder="如:计算机系" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.role === 'STUDENT'">
            <el-form-item label="毕业日期" prop="graduationDate">
              <el-date-picker
                v-model="form.graduationDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期">
              <el-date-picker
                v-model="form.dob"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Refresh, Edit } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import { userApi } from '../api/users'

const loading = ref(false)
const saving = ref(false)
const users = ref([])
const roleFilter = ref(null)
const search = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({})
const formRef = ref(null)

const rules = computed(() => ({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: isEdit.value ? [] : [{ required: true, message: '请输入密码', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}))

const filteredUsers = computed(() => {
  let list = users.value
  if (roleFilter.value) {
    list = list.filter(u => u.role === roleFilter.value)
  }
  if (search.value) {
    const q = search.value.toLowerCase()
    list = list.filter(u =>
      u.username?.toLowerCase().includes(q) || u.name?.toLowerCase().includes(q)
    )
  }
  return list
})

async function loadUsers() {
  loading.value = true
  try {
    const { data } = await userApi.getAll(roleFilter.value)
    users.value = data
  } catch (e) {
    ElMessage.error('加载用户失败: ' + (e.response?.data?.message || e.message))
  } finally {
    loading.value = false
  }
}

function openCreate() {
  isEdit.value = false
  form.value = {
    username: '', password: '', role: 'STUDENT',
    name: '', department: '', dob: null, graduationDate: null
  }
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  form.value = {
    id: row.id,
    username: row.username,
    password: '',
    role: row.role,
    name: row.name,
    department: row.department || '',
    dob: row.dob,
    graduationDate: row.graduationDate
  }
  dialogVisible.value = true
}

async function save() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await userApi.update(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await userApi.create(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await loadUsers()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.response?.data?.message || e.message))
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const target = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  try {
    await userApi.setStatus(row.id, target)
    ElMessage.success(`已设置状态为 ${target}`)
    await loadUsers()
  } catch (e) {
    ElMessage.error('状态更新失败: ' + (e.response?.data?.message || e.message))
  }
}

async function confirmDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除用户 ${row.username} (${row.name})?`,
      '删除确认',
      { type: 'warning' }
    )
    await userApi.remove(row.id)
    ElMessage.success('删除成功')
    await loadUsers()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败: ' + (e.response?.data?.message || e.message))
    }
  }
}

function roleLabel(role) {
  return { STUDENT: '学生', PROFESSOR: '教授', REGISTRAR: '注册主任' }[role] || role
}

function roleTagType(role) {
  return { STUDENT: 'warning', PROFESSOR: 'success', REGISTRAR: 'primary' }[role] || 'info'
}

onMounted(loadUsers)
</script>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-role {
  width: 180px;
}

.filter-search {
  flex: 1;
  min-width: 200px;
}

.filter-actions {
  display: flex;
  gap: 8px;
}

.table-card {
  overflow: visible;
}

.username-cell {
  font-family: var(--font-mono);
  font-weight: 500;
}

.muted-text {
  color: var(--color-text-muted);
  font-size: 13px;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.dot-active {
  background: white;
}

.dot-inactive {
  background: rgba(255, 255, 255, 0.6);
}

.action-btns {
  display: flex;
  gap: 4px;
}

.empty-icon {
  color: var(--color-text-muted);
  opacity: 0.5;
}

.user-dialog :deep(.el-dialog__body) {
  padding-top: var(--space-4);
}
</style>
