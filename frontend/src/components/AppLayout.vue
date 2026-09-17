<template>
  <el-container style="height: 100vh">
    <el-header class="app-header">
      <div class="header-left">
        <el-button text @click="$router.push('/')">← 返回</el-button>
        <h3>{{ title }}</h3>
      </div>
      <div class="header-right">
        <slot name="actions" />
      </div>
    </el-header>
    <el-main class="app-main">
      <slot />
    </el-main>
  </el-container>
</template>

<script setup>
defineProps({
  title: { type: String, required: true }
})
</script>

<style scoped>
.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-left h3 {
  margin: 0;
}
.header-right {
  display: flex;
  gap: 8px;
}
.app-main {
  background: #f5f7fa;
  padding: 20px;
}
</style>
