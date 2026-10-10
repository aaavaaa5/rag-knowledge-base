<template>
  <div class="page-container">
    <div class="breadcrumb">
      <el-button text @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回知识库
      </el-button>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-current">{{ kb?.name || '文档管理' }}</span>
    </div>

    <div class="page-header">
      <div>
        <h1 class="page-title">{{ kb?.name || '文档管理' }}</h1>
        <p class="page-subtitle">管理知识库中的文档，上传后系统会自动处理</p>
      </div>
      <el-button type="primary" @click="showUploadDialog = true">
        <el-icon><Upload /></el-icon>
        上传文档
      </el-button>
    </div>

    <!-- Loading -->
    <div v-if="loading" v-loading="true" class="loading-area" />

    <!-- Empty State -->
    <div v-else-if="docs.length === 0" class="empty-state">
      <el-icon><Tickets /></el-icon>
      <p>
        还没有文档<br />
        <el-button text type="primary" @click="showUploadDialog = true">上传第一个文档</el-button>
      </p>
    </div>

    <!-- Document Table -->
    <el-table v-else :data="docs" stripe class="doc-table">
      <el-table-column prop="fileName" label="文件名" min-width="240">
        <template #default="{ row }">
          <div class="doc-name">
            <el-icon><Document /></el-icon>
            <span>{{ row.fileName }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="size" label="大小" width="100">
        <template #default="{ row }">
          {{ formatSize(row.size) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <span :class="['status-tag', row.status]">
            <span v-if="row.status === 'pending'">待处理</span>
            <span v-else-if="row.status === 'processing'">处理中</span>
            <span v-else-if="row.status === 'completed'">已完成</span>
            <span v-else-if="row.status === 'failed'">失败</span>
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="chunkSize" label="切分长度" width="100" />
      <el-table-column prop="overlap" label="重叠长度" width="100" />
      <el-table-column prop="uploadedAt" label="上传时间" width="180" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-popconfirm
            title="确定要删除这个文档吗？"
            confirm-button-text="确认删除"
            cancel-button-text="取消"
            @confirm="handleDelete(row)"
          >
            <template #reference>
              <el-button text type="danger" size="small">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- Upload Dialog -->
    <el-dialog
      v-model="showUploadDialog"
      title="上传文档"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form label-width="100px">
        <el-form-item label="选择文件" required :error="fileError">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :show-file-list="true"
            multiple
            accept=".txt"
            :on-change="handleFileChange"
            :limit="0"
          >
            <el-button type="default">
              <el-icon><FolderOpened /></el-icon>
              选择文件
            </el-button>
            <template #tip>
              <div class="upload-tip">仅支持 .txt 文件，单个文件不超过 50MB</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="切分长度">
          <el-select v-model="chunkSize" class="w-full">
            <el-option :value="200" label="200 字符" />
            <el-option :value="500" label="500 字符" />
            <el-option :value="1000" label="1000 字符" />
            <el-option :value="1500" label="1500 字符" />
          </el-select>
        </el-form-item>

        <el-form-item label="重叠长度">
          <el-input-number
            v-model="overlap"
            :min="0"
            :max="chunkSize - 1"
            :step="10"
            :disabled="!chunkSize"
          />
          <span class="form-hint">字符，默认 50</span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploading" :disabled="uploadFiles.length === 0" @click="handleUpload">
          上传
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Upload, Tickets, Document, FolderOpened, Delete } from '@element-plus/icons-vue'
import { fetchDocuments, uploadDocument, deleteDocument } from '@/api/document'
import { getKnowledgeBase } from '@/api/knowledgeBase'

const route = useRoute()
const router = useRouter()
const kbId = Number(route.params.id)

const kb = ref(null)
const docs = ref([])
const loading = ref(true)

const showUploadDialog = ref(false)
const uploadFiles = ref([])
const fileError = ref('')
const chunkSize = ref(500)
const overlap = ref(50)
const uploading = ref(false)
let eventSource = null

const loadData = async () => {
  loading.value = true
  try {
    const [kbRes, docRes] = await Promise.all([
      getKnowledgeBase(kbId),
      fetchDocuments(kbId),
    ])
    kb.value = kbRes.data
    docs.value = docRes.data
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const formatSize = (bytes) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const handleFileChange = (uploadFile, uploadFilesList) => {
  fileError.value = ''
  if (!uploadFile.name.endsWith('.txt')) {
    fileError.value = `"${uploadFile.name}" 不是 .txt 文件，已跳过`
    return false
  }
  if (uploadFile.size > 50 * 1024 * 1024) {
    fileError.value = `"${uploadFile.name}" 超过 50MB，无法上传`
    return false
  }
  uploadFiles.value = uploadFilesList || []
  return true
}

const handleUpload = async () => {
  if (uploadFiles.value.length === 0) {
    fileError.value = '请选择文件'
    return
  }
  uploading.value = true
  try {
    for (const file of uploadFiles.value) {
      await uploadDocument(kbId, file.raw || file, chunkSize.value, overlap.value)
    }
    ElMessage.success('上传成功，文档正在处理中')
    showUploadDialog.value = false
    uploadFiles.value = []
    await loadData()
  } catch (e) {
    ElMessage.error(e.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

const handleDelete = async (doc) => {
  try {
    await deleteDocument(kbId, doc.id)
    ElMessage.success('文档已删除')
    await loadData()
  } catch {
    ElMessage.error('删除失败')
  }
}

const goBack = () => {
  router.push({ name: 'KnowledgeBaseList' })
}

const startSSE = () => {
  if (eventSource) eventSource.close()
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  const clientId = localStorage.getItem('sse_client_id') || Date.now().toString()
  localStorage.setItem('sse_client_id', clientId)
  eventSource = new EventSource(`${baseUrl}/api/sse/documents?clientId=${clientId}`)
  eventSource.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      if (data.kbId === kbId) {
        const idx = docs.value.findIndex(d => d.id === data.docId)
        if (idx !== -1) {
          docs.value[idx].status = data.status
          docs.value = [...docs.value]
        }
      }
    } catch (e) {
      console.warn('SSE parse error', e)
    }
  }
  eventSource.onerror = () => {}
}

onMounted(() => {
  loadData()
  startSSE()
})

onUnmounted(() => {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
})
</script>

<style scoped>
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
  font-size: 13px;
}
.breadcrumb-sep { color: var(--text-muted); }
.breadcrumb-current { color: var(--text-secondary); }
.loading-area { min-height: 200px; display: flex; align-items: center; justify-content: center; }
.doc-table { width: 100%; }
.doc-name { display: flex; align-items: center; gap: 6px; }
.doc-name .el-icon { color: var(--primary); font-size: 16px; }
.upload-tip { font-size: 12px; color: var(--text-muted); margin-top: 4px; }
.form-hint { font-size: 12px; color: var(--text-muted); margin-left: 8px; }
.w-full { width: 100%; }
</style>
