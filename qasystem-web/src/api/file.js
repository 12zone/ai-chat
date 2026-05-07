import request from './request'

// 文件管理接口
const fileApi = {
  /**
   * 上传文件
   * @param {File} file - 要上传的文件
   * @returns {Promise} - 返回上传结果
   */
  uploadFile: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/api/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  /**
   * 获取所有未删除文件
   * @returns {Promise} - 返回文件列表
   */
  getAllFiles: () => {
    return request.get('/api/files')
  },

  /**
   * 根据ID获取文件
   * @param {number} id - 文件ID
   * @returns {Promise} - 返回文件详情
   */
  getFileById: (id) => {
    return request.get(`/api/files/${id}`)
  },

  /**
   * 根据分类获取文件
   * @param {string} category - 文件分类
   * @returns {Promise} - 返回文件列表
   */
  getFilesByCategory: (category) => {
    return request.get('/api/files', {
      params: {
        category: category
      }
    })
  },

  /**
   * 从JSON文件导入数据
   * @param {string} resourcePath - JSON文件路径
   * @returns {Promise} - 返回导入结果
   */
  importFromJson: (resourcePath) => {
    return request.post('/api/ingestion/json', { resourcePath })
  },

  /**
   * 上传文件并导入
   * @param {File} file - 要上传的文件
   * @param {string} category - 文件分类
   * @returns {Promise} - 返回导入结果
   */
  uploadAndImport: (file, category) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('category', category)
    return request.post('/api/ingestion/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  /**
   * 删除文件
   * @param {number} id - 文件ID
   * @returns {Promise} - 返回删除结果
   */
  deleteFile: (id) => {
    return request.delete(`/api/files/${id}`)
  },

  /**
   * 获取所有分类
   * @returns {Promise} - 返回分类列表
   */
  getCategories: () => {
    return request.get('/api/files', {
      params: {
        type: 'categories'
      }
    })
  },

  /**
   * 获取一级查询选项（查询方式）
   * @returns {Promise}
   */
  getTypeOptions: () => {
    return request.get('/api/files/findone')
  },

  /**
   * 获取二级查询选项
   * @param {number} type 查询类型
   * @returns {Promise}
   */
  getFileOptions: (type) => {
    return request.get('/api/files/findtwo', {
      params: { type }
    })
  },

  /**
   * 分页查询文件
   * @param {number} page 当前页（0开始）
   * @param {number} size 每页数量
   * @param {object} extra 额外查询参数
   * @returns {Promise}
   */
  getFilesByPage: (page, size, extra = {}) => {
    return request.get('/api/files/page', {
      params: {
        page,
        size,
        ...extra
      }
    })
  },
  uploadBatchAndImport: (files, category) => {
    const formData = new FormData()
    files.forEach((file) => formData.append('files', file))
    formData.append('category', category)
    return request.post('/api/ingestion/upload/batch', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  /** 管理员：从数据库全量重建内存向量索引 */
  reindexVectorFromDb: () => request.post('/api/ingestion/reindex'),
  /** 管理员：向量索引统计 */
  vectorIndexStats: () => request.get('/api/ingestion/index-stats'),
  updateFile: (id, payload) => {
    return request.put(`/api/files/${id}`, payload)
  }
}

export default fileApi