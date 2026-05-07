<script setup>
import { computed, h, onMounted, onUnmounted, provide, ref, watch } from 'vue'
import { useRoute, useRouter, RouterView } from 'vue-router'
import { UserOutlined, UploadOutlined } from '@ant-design/icons-vue'
import authApi from '@/api/auth'
import { TOKEN_KEY } from '@/api/request'
import { invalidateAuthBootstrap } from '@/router/authBootstrap'

const collapsed = ref(false)
const route = useRoute()
const router = useRouter()

const authUser = ref(authApi.getLocalUser())
const roles = computed(() => (Array.isArray(authUser.value?.roles) ? authUser.value.roles : []))
const isAdmin = computed(() => roles.value.includes('ADMIN'))

provide('canWrite', isAdmin)

const selectedKeys = ref([route.name || 'chat'])

watch(
  () => route.name,
  (name) => {
    if (name && ['chat', 'file', 'profile'].includes(String(name))) {
      selectedKeys.value = [String(name)]
    }
  },
  { immediate: true }
)

const menuItems = [
  { key: 'chat', icon: h(UserOutlined), label: 'Chat' },
  { key: 'file', icon: h(UploadOutlined), label: 'File' },
  { key: 'profile', icon: h(UserOutlined), label: 'User' }
]

const handleMenuClick = ({ key }) => {
  router.push({ name: key })
}

const onCollapse = (collapsedState) => {
  collapsed.value = collapsedState
}

const onBreakpoint = () => {}

const syncSessionFromStorage = () => {
  const hasToken = !!localStorage.getItem(TOKEN_KEY)
  authUser.value = hasToken ? authApi.getLocalUser() : null
  if (!hasToken) {
    invalidateAuthBootstrap()
    if (route.path !== '/login') {
      router.replace({ path: '/login', query: { redirect: route.fullPath } })
    }
  }
}

const logout = () => {
  authApi.logout()
  invalidateAuthBootstrap()
  router.replace('/login')
}

onMounted(() => {
  window.addEventListener('qa-session-changed', syncSessionFromStorage)
})
onUnmounted(() => {
  window.removeEventListener('qa-session-changed', syncSessionFromStorage)
})
</script>

<template>
  <div class="app-container">
    <a-layout style="min-height: 100vh; width: 100%;">
      <a-layout-sider
        v-model:collapsed="collapsed"
        breakpoint="lg"
        collapsed-width="0"
        @breakpoint="onBreakpoint"
        @collapse="onCollapse"
      >
        <div class="demo-logo-vertical" />
        <a-menu
          v-model:selectedKeys="selectedKeys"
          theme="dark"
          mode="inline"
          :items="menuItems"
          @click="handleMenuClick"
        />
      </a-layout-sider>
      <a-layout style="flex: 1;">
        <a-layout-header
          style="background: #fff; padding: 0 16px; width: 100%; display: flex; justify-content: flex-end; align-items: center;"
        >
          <a-dropdown>
            <a class="user-badge" @click.prevent>
              {{ authUser?.nickname || authUser?.username || '用户' }}（{{ isAdmin ? '管理员' : '普通用户' }}）
            </a>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="handleMenuClick({ key: 'profile' })">个人中心</a-menu-item>
                <a-menu-item @click="logout">退出登录</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-layout-header>
        <a-layout-content style="margin: 0; padding: 0; width: 100%;">
          <div class="main-content-wrapper">
            <RouterView />
          </div>
        </a-layout-content>
      </a-layout>
    </a-layout>
  </div>
</template>

<style scoped>
.app-container {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}

.user-badge {
  padding: 8px 12px;
  border-radius: 999px;
  background: #f0f5ff;
}

.main-content-wrapper {
  width: 100%;
  height: calc(100vh - 64px);
  background: #fff;
}

.demo-logo-vertical {
  height: 32px;
  margin: 16px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 6px;
}
</style>
