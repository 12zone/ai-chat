<template>
  <div class="user-profile">
    <a-card title="用户信息">
      <a-form layout="vertical">
        <a-form-item label="用户名">
          <a-input :value="profile.username" disabled />
        </a-form-item>
        <a-form-item label="昵称">
          <a-input v-model:value="profile.nickname" />
        </a-form-item>
        <a-form-item label="邮箱">
          <a-input v-model:value="profile.email" />
        </a-form-item>
        <a-form-item label="角色">
          <a-tag v-for="role in profile.roles" :key="role">{{ role }}</a-tag>
        </a-form-item>
        <a-button type="primary" @click="saveProfile" :loading="saving">保存</a-button>
      </a-form>
    </a-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import userApi from '@/api/user'
import authApi from '@/api/auth'

const saving = ref(false)
const profile = reactive({
  username: '',
  nickname: '',
  email: '',
  roles: []
})

const unwrap = (payload) => {
  if (payload && typeof payload === 'object' && Object.prototype.hasOwnProperty.call(payload, 'retCode')) {
    return payload.data
  }
  return payload
}

const loadProfile = async () => {
  const response = await userApi.getProfile()
  const data = unwrap(response.data)
  profile.username = data.username || ''
  profile.nickname = data.nickname || ''
  profile.email = data.email || ''
  profile.roles = Array.isArray(data.roles) ? data.roles : []
  authApi.setLocalUser({ username: data.username, nickname: data.nickname, roles: data.roles })
}

const saveProfile = async () => {
  saving.value = true
  try {
    await userApi.updateProfile({
      nickname: profile.nickname,
      email: profile.email
    })
    await loadProfile()
    message.success('个人信息已更新')
  } catch (error) {
    message.error(error.response?.data?.message || '更新失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    await loadProfile()
  } catch (error) {
    message.error('加载用户信息失败')
  }
})
</script>

<style scoped>
.user-profile {
  padding: 24px;
}
</style>
