/**
 * 统一的错误处理工具
 * 提供统一的错误处理和提示功能
 */
import { message } from 'ant-design-vue'

export const ErrorHandler = {
  /**
   * 处理HTTP请求错误
   * @param error 错误对象
   * @param operation 操作名称
   * @param showMessage 是否显示错误消息，默认为true
   * @returns 错误消息字符串
   */
  handleHttpError(error, operation = '操作', showMessage = true) {
    let errorMessage = `${operation}失败，请稍后重试`
    
    if (error.response) {
      const { status, data } = error.response
      if (status === 400) {
        errorMessage = data?.message || '请求参数错误'
      } else if (status === 401) {
        errorMessage = '未授权，请先登录'
      } else if (status === 403) {
        errorMessage = '无权限访问'
      } else if (status === 404) {
        errorMessage = '请求的资源不存在'
      } else if (status === 500) {
        errorMessage = '服务器内部错误'
      } else {
        errorMessage = data?.message || `服务器错误: ${status}`
      }
    } else if (error.request) {
      errorMessage = '网络错误，请检查网络连接'
    } else if (error.message) {
      errorMessage = error.message
    }
    
    if (showMessage) {
      message.error(errorMessage)
    }
    
    console.error(`${operation}失败:`, error)
    
    return errorMessage
  },

  /**
   * 处理业务逻辑错误
   * @param errorMessage 错误消息
   * @param showMessage 是否显示错误消息，默认为true
   */
  handleBusinessError(errorMessage, showMessage = true) {
    if (showMessage) {
      message.error(errorMessage)
    }
    console.error('业务错误:', errorMessage)
  },

  /**
   * 显示成功消息
   * @param successMessage 成功消息
   */
  showSuccess(successMessage) {
    message.success(successMessage)
  },

  /**
   * 显示警告消息
   * @param warningMessage 警告消息
   */
  showWarning(warningMessage) {
    message.warning(warningMessage)
  },

  /**
   * 显示信息消息
   * @param infoMessage 信息消息
   */
  showInfo(infoMessage) {
    message.info(infoMessage)
  }
}
