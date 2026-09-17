<template>
  <router-view v-slot="{ Component }">
    <transition name="slide-up" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<script setup>
// 根组件: 仅渲染路由视图
</script>

<style>
/* Root-level styles handled by global.css */
</style>
