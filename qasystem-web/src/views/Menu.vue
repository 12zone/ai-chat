<script setup>
import { ref, h } from 'vue'
import { MessageOutlined, FileOutlined } from '@ant-design/icons-vue'

const currentMenu = ref('chat')
const selectedKeys = ref([currentMenu.value])

const handleMenuClick = (key) => {
  currentMenu.value = key
  selectedKeys.value = [key]
}

// 暴露当前选中的菜单给父组件
defineExpose({
  currentMenu
})
</script>

<template>
  <a-menu
    v-model:selectedKeys="selectedKeys"
    mode="inline"
    @click="handleMenuClick"
    style="height: 100%; border-right: 1px solid #f0f0f0"
  >
    <a-menu-item key="chat">
      <template #icon><MessageOutlined /></template>
      Chat
    </a-menu-item>
    <a-menu-item key="file">
      <template #icon><FileOutlined /></template>
      File
    </a-menu-item>
  </a-menu>
</template>

<style scoped>
/* 可以添加自定义样式 */
</style>