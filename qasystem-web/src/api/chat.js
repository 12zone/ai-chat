import request from './request'

// 聊天接口
const chatApi = {
  /**
   * 与AI模型对话
   * @param {string} modelName - 模型名称
   * @param {string} message - 用户消息
   * @returns {Promise} - 返回AI回复
   */
  chatWithModel: (modelName, message) => {
    const segment = encodeURIComponent(String(modelName || '').trim())
    return request.post(`/api/chat/${segment}`, {
      message
    })
  },
  getModels: () => {
    return request.get('/api/chat/models')
  }
}

export default chatApi