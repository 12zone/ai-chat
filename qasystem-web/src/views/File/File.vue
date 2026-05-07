<template>
  <div class="file-container">
    <div class="file-header">
      <div class="file-actions">
        <a-space :size="12" wrap>
          <a-select
            v-model:value="queryType"
            style="width: 180px"
            placeholder="请选择查询方式"
            :options="typeOptions"
            @change="handleTypeChange"
          />
          <a-select
            v-model:value="queryValue"
            style="width: 280px"
            placeholder="请选择或搜索"
            :disabled="Number(queryType) === 0 || queryType === null || queryType === undefined"
            :show-search="true"
            :allow-clear="true"
            option-filter-prop="label"
            :loading="queryOptionsLoading"
          >
            <a-select-option
              v-for="item in queryOptions"
              :key="String(item.value)"
              :value="item.value"
              :label="item.label"
            >
              {{ item.label }}
            </a-select-option>
          </a-select>
          <a-button type="primary" @click="handleSearch">搜索</a-button>
          <a-button v-if="canWrite" type="primary" @click="showUploadModal = true">上传文件</a-button>
          <a-button v-if="canWrite" :loading="reindexing" @click="handleReindexVectors">重建向量索引</a-button>
          <a-button v-if="canWrite" @click="handleVectorStats">索引统计</a-button>
        </a-space>
      </div>
    </div>

    <div class="table-container">
      <a-table
        :columns="columns"
        :data-source="files"
        :loading="loading"
        :pagination="{
          current: pagination.current,
          pageSize: pagination.pageSize,
          total: pagination.total,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `Total ${total} items`,
          onChange: handlePaginationChange
        }"
        row-key="id"
        :scroll="tableScroll"
      >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-space size="small">
            <a-button type="text" @click="viewFile(record)">
              查看
            </a-button>
            <a-popconfirm
              v-if="canWrite"
              title="确认删除该文件吗？"
              ok-text="确认"
              cancel-text="取消"
              @confirm="deleteFile(record.id)"
            >
              <a-button type="text" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
      </a-table>
    </div>

    <a-modal v-model:open="showUploadModal" title="上传文件" @ok="handleUploadOk" @cancel="showUploadModal = false">
      <a-input
        v-model:value="uploadCategory"
        placeholder="请输入文件分类（可选，默认 default）"
        style="margin-bottom: 12px"
        allow-clear
      />
      <a-upload-dragger
        v-model:file-list="uploadFileList"
        :before-upload="beforeUpload"
        @change="handleFileChange"
        multiple
        :disabled="uploading"
      >
        <p class="ant-upload-drag-icon">
          <upload-outlined />
        </p>
        <p class="ant-upload-text">点击或拖拽文件到此区域上传</p>
        <template #tip>
          <div class="upload-tip">
            支持 PDF、DOCX、XLSX、TXT 等格式，可批量上传
          </div>
        </template>
      </a-upload-dragger>
    </a-modal>
    <a-modal
      v-model:open="showPreviewModal"
      :title="canWrite ? '文件预览与编辑' : '文件预览'"
      width="880px"
      :ok-text="canWrite ? '保存' : '关闭'"
      @ok="saveEdit"
      @cancel="showPreviewModal = false"
    >
      <a-form layout="vertical">
        <a-form-item label="标题"><a-input v-model:value="editingFile.name" :disabled="!canWrite" /></a-form-item>
        <a-form-item label="分类"><a-input v-model:value="editingFile.category" :disabled="!canWrite" /></a-form-item>
        <a-form-item label="内容"><a-textarea v-model:value="editingFile.content" :rows="12" :disabled="!canWrite" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { ref, onMounted, computed, inject, unref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { UploadOutlined } from '@ant-design/icons-vue'
import fileApi from '@/api/file'

const REINDEX_MESSAGE_KEY = 'vector-reindex'

export default {
  name: 'File',
  props: {
    canWrite: {
      type: Boolean,
      default: false
    }
  },
  components: {
    UploadOutlined
  },
  setup(props) {
    const layoutCanWrite = inject('canWrite', null)
    const canWrite = computed(() =>
      layoutCanWrite != null ? unref(layoutCanWrite) : props.canWrite
    )

    const files = ref([])
    const typeOptions = ref([
      { value: 0, label: '无条件查询' },
      { value: 1, label: '按ID查询' },
      { value: 2, label: '按分类查询' },
      { value: 3, label: '按标题查询' }
    ])
    const queryOptions = ref([])
    const queryOptionsLoading = ref(false)
    const loading = ref(false)
    const uploading = ref(false)
    const reindexing = ref(false)
    const showUploadModal = ref(false)
    const uploadFileList = ref([])
    const uploadCategory = ref('')
    const showPreviewModal = ref(false)
    const editingFile = ref({ id: null, name: '', category: '', content: '' })
    const queryType = ref(0)
    const queryValue = ref(undefined)

    const pagination = ref({
      current: 1,
      pageSize: 10,
      total: 0
    })

    /** a-table scroll 须为对象；勿写 scroll={{ x }} 会被当成字符串 */
    const tableScroll = { x: 920 }

    const columns = [
      {
        title: 'FileID',
        dataIndex: 'fileId',
        key: 'fileId',
        width: 150
      },
      {
        title: '文件名',
        dataIndex: 'name',
        key: 'name',
        width: 300
      },
      {
        title: '类别',
        dataIndex: 'category',
        key: 'category',
        width: 150
      },
      {
        title: '上传时间',
        dataIndex: 'uploadTime',
        key: 'uploadTime',
        width: 200
      },
      {
        title: '操作',
        key: 'action',
        width: 100
      }
    ]

    const mapFileFields = (file) => {
      return {
        id: file.fileId ?? file.id,
        fileId: file.fileId ?? file.id,
        name: file.title ?? file.name,
        category: file.category ?? '-',
        uploadTime: file.date ?? file.uploadTime ?? '-',
        content: file.content,
        deleted: file.deleted
      }
    }

    const unwrapApiData = (payload) => {
      if (payload && typeof payload === 'object' && Object.prototype.hasOwnProperty.call(payload, 'retCode')) {
        return payload.data
      }
      return payload
    }

    const fetchFiles = async () => {
      loading.value = true
      try {
        const response = await fileApi.getFilesByPage(
          pagination.value.current - 1,
          pagination.value.pageSize,
          {
            type: queryType.value ?? 0,
            value: queryValue.value ?? ''
          }
        )
        const pageData = unwrapApiData(response.data) || {}
        const pageList = Array.isArray(pageData.content) ? pageData.content : []
        const mappedList = pageList.map(mapFileFields)
        files.value = mappedList
        pagination.value.total = Number(pageData.totalElements || 0)
      } catch (error) {
        const errorMessage = error.response?.data?.message || error.message || '获取文件列表失败'
        message.error(errorMessage)
        files.value = []
        pagination.value.total = 0
      } finally {
        loading.value = false
      }
    }

    const normalizeOption = (item) => {
      if (item === null || item === undefined) return null
      if (typeof item === 'string' || typeof item === 'number') {
        return { label: String(item), value: item }
      }
      if (typeof item === 'object') {
        const value = item.value ?? item.fileId ?? item.id ?? item.title ?? item.category
        const label = item.label ?? (value !== undefined ? String(value) : '')
        if (value === undefined || value === null || !label) return null
        return { label, value }
      }
      return null
    }

    const normalizeOptions = (list) => {
      if (!Array.isArray(list)) return []
      return list.map(normalizeOption).filter(Boolean)
    }

    const fetchCategories = async () => {
      try {
        const response = await fileApi.getTypeOptions()
        const options = normalizeOptions(unwrapApiData(response.data))
        typeOptions.value = options.length ? options : typeOptions.value
      } catch (error) {
        console.error('获取类型选项失败:', error)
      }
    }

    const fetchFileOptions = async (type) => {
      queryOptionsLoading.value = true
      try {
        const response = await fileApi.getFileOptions(type)
        queryOptions.value = normalizeOptions(unwrapApiData(response.data))
      } catch (error) {
        console.error('获取文件选项失败:', error)
        queryOptions.value = []
      } finally {
        queryOptionsLoading.value = false
      }
    }

    const beforeUpload = () => false

    const handleFileUpload = async () => {
      if (!canWrite.value) {
        message.error('无权限上传文件')
        return
      }
      if (!uploadFileList.value.length) {
        message.warning('请至少选择一个文件')
        return
      }
      uploading.value = true
      try {
        const selectedFiles = uploadFileList.value
          .map((item) => item.originFileObj || item)
          .filter(Boolean)
        await fileApi.uploadBatchAndImport(selectedFiles, uploadCategory.value || 'default')
        message.success(`成功上传 ${selectedFiles.length} 个文件`)
        fetchFiles()
        uploadFileList.value = []
        uploadCategory.value = ''
        showUploadModal.value = false
      } catch (error) {
        message.error(error.response?.status === 403 ? '无权限上传文件' : '文件上传失败')
        console.error('文件上传失败:', error)
      } finally {
        uploading.value = false
      }
    }

    const handleFileChange = (info) => {
      uploadFileList.value = info.fileList
    }

    const handleUploadOk = () => {
      handleFileUpload()
    }

    const viewFile = async (record) => {
      try {
        const response = await fileApi.getFileById(record.id)
        const file = mapFileFields(unwrapApiData(response.data))
        editingFile.value = { ...file }
        showPreviewModal.value = true
      } catch (error) {
        message.error('获取文件详情失败')
      }
    }

    const saveEdit = async () => {
      if (!canWrite.value) {
        showPreviewModal.value = false
        return
      }
      if (!editingFile.value.id) return
      try {
        await fileApi.updateFile(editingFile.value.id, {
          title: editingFile.value.name,
          category: editingFile.value.category,
          content: editingFile.value.content
        })
        message.success('文件更新成功')
        showPreviewModal.value = false
        fetchFiles()
      } catch (error) {
        message.error(error.response?.status === 403 ? '无权限编辑文件' : '文件更新失败')
      }
    }

    const deleteFile = async (id) => {
      try {
        await fileApi.deleteFile(id)
        message.success('文件删除成功')
        fetchFiles()
      } catch (error) {
        message.error(error.response?.status === 403 ? '无权限删除文件' : '文件删除失败')
        console.error('文件删除失败:', error)
      }
    }

    const handleSearch = () => {
      if ((queryType.value === 1 || queryType.value === 2 || queryType.value === 3) && !queryValue.value) {
        message.warning('请先选择二级查询条件')
        return
      }
      pagination.value.current = 1
      fetchFiles()
    }

    const handleTypeChange = async (type) => {
      const normalizedType = Number(type)
      queryType.value = Number.isNaN(normalizedType) ? type : normalizedType
      queryValue.value = undefined
      pagination.value.current = 1
      if (queryType.value === 1 || queryType.value === 2 || queryType.value === 3) {
        await fetchFileOptions(queryType.value)
      } else {
        queryOptions.value = []
        fetchFiles()
      }
    }

    const handlePaginationChange = (current, pageSize) => {
      pagination.value.current = current
      pagination.value.pageSize = pageSize
      fetchFiles()
    }

    const unwrapBody = (response) => {
      const raw = response?.data
      if (raw && typeof raw === 'object' && Object.prototype.hasOwnProperty.call(raw, 'retCode')) {
        return raw.data
      }
      return raw
    }

    const handleReindexVectors = async () => {
      if (!canWrite.value) {
        message.error('无权限')
        return
      }
      reindexing.value = true
      message.loading({
        content: '正在重建向量索引（可能要几分钟），请稍候…',
        key: REINDEX_MESSAGE_KEY,
        duration: 0
      })
      try {
        const res = await fileApi.reindexVectorFromDb()
        const d = unwrapBody(res) || {}
        const dbN = Number(d.dbFileCount)
        const chunkN = Number(d.indexedChunkCount)
        const skipEmpty = d.filesSkippedEmptyContent
        const skipEx = d.filesSkippedException
        const visited = d.filesProcessed
        message.success({
          content: `向量索引已处理：库中未删文件 ${Number.isFinite(dbN) ? dbN : '-'} 个，向量块 ${Number.isFinite(chunkN) ? chunkN : '-'} 个（遍历 ${visited ?? '-'}，正文为空 ${skipEmpty ?? '-'}，嵌入异常 ${skipEx ?? '-'}）`,
          key: REINDEX_MESSAGE_KEY,
          duration: 6
        })
        if (Number.isFinite(dbN) && dbN > 0 && Number.isFinite(chunkN) && chunkN === 0) {
          Modal.warning({
            title: '有文件但未产生向量块',
            content:
              '接口已成功返回，但 indexedChunkCount 为 0。请检查：① 数据库 files.content 是否为空；② spring.ai.openai.api-key、base-url 与 app.embedding.openai.model 是否配置正确；③ 后端日志中的 reindex skip 与 OpenAI 报错。详见文档「嵌入自检」curl 示例。',
            okText: '知道了'
          })
        }
      } catch (error) {
        const msg =
          error.response?.data?.message ||
          error.response?.data?.retMsg ||
          error.message ||
          '重建向量索引失败'
        message.error({ content: msg, key: REINDEX_MESSAGE_KEY, duration: 8 })
      } finally {
        reindexing.value = false
      }
    }

    const handleVectorStats = async () => {
      if (!canWrite.value) {
        message.error('无权限')
        return
      }
      try {
        const res = await fileApi.vectorIndexStats()
        const d = unwrapBody(res) || {}
        message.info(`库中文件 ${d.dbFileCount ?? '-'} 个，内存向量块 ${d.indexedChunkCount ?? '-'} 个`)
      } catch (error) {
        const msg =
          error.response?.data?.message ||
          error.response?.data?.retMsg ||
          error.message ||
          '获取统计失败'
        message.error(msg)
      }
    }

    onMounted(() => {
      fetchCategories()
      fetchFiles()
    })

    return {
      files,
      typeOptions,
      loading,
      uploading,
      reindexing,
      showUploadModal,
      uploadFileList,
      uploadCategory,
      queryType,
      queryValue,
      queryOptions,
      queryOptionsLoading,
      showPreviewModal,
      canWrite,
      editingFile,
      pagination,
      tableScroll,
      columns,
      handleFileUpload,
      beforeUpload,
      handleFileChange,
      handleUploadOk,
      viewFile,
      deleteFile,
      handleSearch,
      handleTypeChange,
      handlePaginationChange,
      handleReindexVectors,
      handleVectorStats,
      saveEdit
    }
  }
}
</script>

<style scoped>
.file-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.file-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.file-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.file-actions {
  display: flex;
  align-items: center;
  width: 100%;
}

.upload-tip {
  margin-top: 16px;
  color: #666;
}

.table-container {
  flex: 1;
  overflow: auto;
  margin-bottom: 20px;
}

:deep(.ant-table) {
  width: 100%;
}
</style>