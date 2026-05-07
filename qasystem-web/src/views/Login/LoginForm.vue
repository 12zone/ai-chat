<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import authApi from '@/api/auth'

const loading = ref(false)
const route = useRoute()
const router = useRouter()
const form = reactive({
  username: '',
  password: ''
})

function safeRedirect(raw) {
  if (typeof raw !== 'string' || !raw.startsWith('/') || raw.startsWith('//')) {
    return '/chat'
  }
  return raw
}

const submit = async () => {
  if (!form.username.trim() || !form.password.trim()) {
    message.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await authApi.login({
      username: form.username.trim(),
      password: form.password
    })
    message.success('欢迎回来')
    const target = safeRedirect(route.query.redirect)
    await router.replace(target)
  } catch (error) {
    const msg = error.response?.data?.retMsg || error.response?.data?.message || '登录失败'
    message.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <a-form layout="vertical">
    <a-form-item label="用户名">
      <a-input v-model:value="form.username" placeholder="请输入用户名，例如 admin 或 user" size="large" />
    </a-form-item>
    <a-form-item label="密码">
      <a-input-password v-model:value="form.password" placeholder="请输入密码" size="large" @pressEnter="submit" />
    </a-form-item>
    <a-button type="primary" block size="large" :loading="loading" @click="submit">
      登录系统
    </a-button>
  </a-form>
</template>
