<script setup>
import { ref, onMounted } from 'vue'
import { RouterView } from 'vue-router'
import { ensureAuthBootstrapped } from '@/router/authBootstrap'

const ready = ref(false)

onMounted(async () => {
  await ensureAuthBootstrapped()
  ready.value = true
})
</script>

<template>
  <div v-if="!ready" class="boot-loading">正在校验登录状态...</div>
  <RouterView v-else />
</template>

<style>
.boot-loading {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
}
</style>
