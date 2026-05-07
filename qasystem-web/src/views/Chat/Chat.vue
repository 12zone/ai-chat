<template>
  <div class="chat-container">
    <div class="chat-toolbar">
      <a-space :size="12" wrap align="center">
        <a-select v-model:value="modelName" style="width: 180px">
          <a-select-option v-for="item in modelOptions" :key="item" :value="item">{{ item }}</a-select-option>
        </a-select>
        <span class="rag-hint">回答会先做向量检索（使用 OpenAI 嵌入，需在服务端配置 spring.ai.openai.api-key）。文件在「File」页由管理员上传入库；后端启动时会从数据库自动重建索引。</span>
      </a-space>
    </div>
    <div ref="messageListRef" class="chat-messages">
      <div v-for="(item, idx) in messages" :key="idx" :class="['message-item', item.isUser ? 'user-message' : 'bot-message']">
        {{ item.content }}
      </div>
      <div v-if="!messages.length" class="empty-tip">请输入问题，系统将结合知识库进行回答</div>
      <div v-if="loading" class="loading-message"><a-spin size="small" /><span style="margin-left: 8px;">正在处理...</span></div>
    </div>
    <div class="chat-input-area">
      <a-input v-model:value="inputMessage" placeholder="输入问题，Shift + Enter 换行，Enter 发送" :rows="3" type="textarea" @pressEnter="handleEnter" />
      <a-button type="primary" @click="sendMessage" :disabled="!inputMessage.trim() || loading">发送</a-button>
    </div>
  </div>
</template>

<script>
import { nextTick } from 'vue'
import { message, Modal } from 'ant-design-vue'
import chatApi from '@/api/chat'

export default {
  name: 'Chat',
  data() {
    return {
      messages: [],
      inputMessage: '',
      loading: false,
      modelName: 'deepseek',
      modelOptions: ['deepseek'],
      messageListRef: null
    }
  },
  mounted() {
    this.loadModels()
  },
  methods: {
    resolveTransportError(error) {
      if (error?.code === 'ECONNABORTED' || String(error?.message || '').toLowerCase().includes('timeout')) {
        return '请求超时：请稍后重试。'
      }
      if (!error?.response) {
        return '网络连接异常：请检查后端服务是否启动，以及本机网络是否正常。'
      }
      const d = error.response.data
      if (typeof d === 'string') {
        try {
          const parsed = JSON.parse(d)
          return parsed?.retMsg || parsed?.message || parsed?.detail || d
        } catch {
          return d
        }
      }
      return d?.retMsg || d?.message || d?.detail || '聊天接口调用失败，请稍后重试。'
    },
    unwrapApiResponse(payload) {
      if (payload && typeof payload === 'object' && Object.prototype.hasOwnProperty.call(payload, 'retCode')) {
        return payload
      }
      return { retCode: 200, retMsg: 'success', data: payload }
    },
    async loadModels() {
      try {
        const response = await chatApi.getModels()
        const apiResult = this.unwrapApiResponse(response.data)
        if (Array.isArray(apiResult.data) && apiResult.data.length) {
          this.modelOptions = apiResult.data
          if (!this.modelOptions.includes(this.modelName)) {
            this.modelName = this.modelOptions[0]
          }
        }
      } catch (error) {
        console.error('获取模型列表失败', error)
      }
    },
    scrollToBottom() {
      nextTick(() => {
        if (!this.messageListRef) return
        this.messageListRef.scrollTop = this.messageListRef.scrollHeight
      })
    },
    handleEnter(event) {
      if (event.shiftKey) return
      event.preventDefault()
      this.sendMessage()
    },
    async sendMessage() {
      if (!this.inputMessage.trim() || this.loading) return
      const userMessage = this.inputMessage.trim()
      this.messages.push({ content: userMessage, isUser: true })
      this.inputMessage = ''
      this.loading = true
      this.scrollToBottom()
      try {
        const response = await chatApi.chatWithModel(this.modelName, userMessage)
        const apiResult = this.unwrapApiResponse(response.data)
        if (apiResult.retCode !== 200 || typeof apiResult.data !== 'string' || !apiResult.data.trim()) {
          const msg = apiResult.retMsg || '聊天服务返回异常，请稍后再试'
          this.messages.push({ content: `[系统提示] ${msg}`, isUser: false })
          message.error(msg)
          Modal.error({
            title: `聊天请求失败${apiResult.retCode ? `（${apiResult.retCode}）` : ''}`,
            content: msg,
            okText: '我知道了'
          })
          return
        }
        this.messages.push({ content: apiResult.data, isUser: false })
      } catch (error) {
        const errorMessage = this.resolveTransportError(error)
        this.messages.push({ content: `[系统提示] ${errorMessage}`, isUser: false })
        message.error(errorMessage)
        Modal.error({
          title: '聊天请求失败',
          content: errorMessage,
          okText: '我知道了'
        })
      } finally {
        this.loading = false
        this.scrollToBottom()
      }
    }
  }
}
</script>

<style scoped>
.chat-container {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #f7f9fc 0%, #f3f6fb 100%);
  border-radius: 0;
  overflow: hidden;
  border: 0;
}

.chat-toolbar {
  padding: 14px 16px;
  background: #fff;
  border-bottom: 1px solid #edf1f6;
}

.rag-hint {
  font-size: 12px;
  color: #8c8c8c;
  max-width: 520px;
  line-height: 1.5;
}

.chat-messages {
  flex: 1;
  padding: 20px 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-item {
  max-width: 76%;
  padding: 10px 14px;
  border-radius: 14px;
  word-wrap: break-word;
  line-height: 1.65;
}

.user-message {
  align-self: flex-end;
  background: linear-gradient(135deg, #1677ff 0%, #4096ff 100%);
  color: white;
  border-bottom-right-radius: 4px;
  box-shadow: 0 4px 10px rgba(22, 119, 255, 0.25);
}

.bot-message {
  align-self: flex-start;
  background-color: #fff;
  color: #1f1f1f;
  border-bottom-left-radius: 4px;
  box-shadow: 0 4px 10px rgba(31, 35, 41, 0.08);
}

.loading-message {
  align-self: flex-start;
  display: flex;
  align-items: center;
  padding: 10px 15px;
  background-color: white;
  border-radius: 18px;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.chat-input-area {
  padding: 16px;
  background-color: #fff;
  border-top: 1px solid #edf1f6;
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.chat-input-area :deep(.ant-input) {
  flex: 1;
  min-height: 80px;
  resize: none;
}

.chat-input-area :deep(.ant-btn) {
  align-self: flex-end;
  height: 40px;
}

.empty-tip {
  align-self: center;
  margin-top: 36px;
  color: #8c8c8c;
  font-size: 14px;
}
</style>