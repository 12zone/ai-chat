import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    proxy: {
      '/api': {
        // 使用 127.0.0.1 避免 Windows 下 localhost 走 IPv6 而 Tomcat 只监听 IPv4 导致偶发 ECONNRESET
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        // 0：开发代理不限制超时，避免全量重建向量索引等长请求被断开
        timeout: 0,
        proxyTimeout: 0
      }
    }
  }
})
