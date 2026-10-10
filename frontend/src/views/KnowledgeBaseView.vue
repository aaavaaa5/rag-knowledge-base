<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">知识库</h1>
        <p class="page-subtitle">管理和维护您的文档知识库</p>
      </div>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>
        新建知识库
      </el-button>
    </div>

    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索知识库名称..."
        clearable
        :prefix-icon="Search"
        class="search-input"
        @input="handleSearch"
      />
    </div>

    <div v-if="loading" v-loading="true" class="loading-area" />

    <div v-else-if="kbs.length === 0" class="empty-state">
      <el-icon><FolderOpened /></el-icon>
      <p v-if="searchKeyword">未找到与"{{ searchKeyword }}"相匹配的知识库</p>
      <p v-else>
        还没有知识库<br />
        <el-button text type="primary" @click="showCreateDialog = true">创建一个</el-button>
        开始吧
      </p>
    </div>

    <div v-else class="kb-grid">
      <div
        v-for="kb in kbs"
        :key="kb.id"
        class="kb-card"
        @click="goToDocuments(kb)"
      >
        <div class="card-icon">
          <el-icon :size="28"><Collection /></el-icon>
        </div>
        <div class="card-body">
          <h3 class="card-title">{{ kb.name }}</h3>
          <div class="card-meta">
            <span class="meta-item">
              <el-icon :size="14"><Document /></el-icon>
              {{ kb.docCount }} 个文档
            </span>
            <span class="meta-divider">|</span>
            <span class="meta-item">
              <el-icon :size="14"><Clock /></el-icon>
              {{ kb.createdAt }}
            </span>
          </div>
        </div>
        <div class="card-actions" @click.stop>
          <el-button text type="primary" size="small" @click="goToDocuments(kb)">
            管理文档
          </el-button>
          <el-popconfirm
            title="确定要删除这个知识库吗？删除后不可恢复。"
            confirm-button-text="确认删除"
            cancel-button-text="取消"
            @confirm="handleDelete(kb)"
          >
            <template #reference>
              <el-button text type="danger" size="small">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </template>
          </el-popconfirm>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="showCreateDialog"
      title="新建知识库"
      width="420px"
      :close-on-click-modal="false"
      @opened="() => { newKbName = ''; createError = ''; $nextTick(() => { const i = document.querySelector(`.kb-create-input input`); if (i) i.focus() }) }"
    >
      <el-form @submit.prevent="handleCreate">
        <el-form-item label="知识库名称" required :error="createError">
          <el-input
            v-model="newKbName"
            class="kb-create-input"
            placeholder="请输入知识库名称"
            maxlength="50"
            show-word-limit
            @keyup.enter="handleCreate"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">
          创建
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Plus, FolderOpened, Collection, Document, Clock, Delete } from '@element-plus/icons-vue'
import { fetchKnowledgeBases, createKnowledgeBase, deleteKnowledgeBase } from '@/api/knowledgeBase'

const router = useRouter()

const kbs = ref([])
const loading = ref(true)
const searchKeyword = ref('')
const showCreateDialog = ref(false)
const newKbName = ref('')
const creating = ref(false)
const createError = ref('')

let searchTimer = null

const loadKBs = async () => {
  loading.value = true
  try {
    const res = await fetchKnowledgeBases({ keyword: searchKeyword.value })
    kbs.value = res.data
  } catch {
    ElMessage.error('加载知识库失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(loadKBs, 300)
}

const handleCreate = async () => {
  const name = newKbName.value.trim()
  if (!name) {
    createError.value = '请输入知识库名称'
    return
  }
  createError.value = ''
  creating.value = true
  try {
    await createKnowledgeBase(name)
    ElMessage.success('知识库创建成功')
    showCreateDialog.value = false
    newKbName.value = ''
    await loadKBs()
  } catch (e) {
    createError.value = e.message || '创建失败'
  } finally {
    creating.value = false
  }
}

const handleDelete = async (kb) => {
  try {
    await deleteKnowledgeBase(kb.id)
    ElMessage.success(`已删除知识库"${kb.name}"`)
    await loadKBs()
  } catch {
    ElMessage.error('删除失败')
  }
}

const goToDocuments = (kb) => {
  router.push({ name: 'Documents', params: { id: kb.id } })
}

onMounted(loadKBs)
</script>

<style scoped>
.search-bar {
  margin-bottom: 20px;
}
.search-input {
  max-width: 400px;
}
.loading-area {
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.kb-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}
.kb-card {
  background: var(--card-bg);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.kb-card:hover {
  border-color: var(--primary-border);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}
.card-icon {
  width: 44px; height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  background: var(--primary-light);
  color: var(--primary);
}
.card-body { flex: 1; }
.card-title {
  font-size: 18px; font-weight: 600;
  color: var(--text-primary); margin-bottom: 8px;
}
.card-meta {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; color: var(--text-secondary);
}
.meta-item { display: flex; align-items: center; gap: 4px; }
.meta-divider { color: var(--border); }
.card-actions {
  display: flex; align-items: center; gap: 8px;
  padding-top: 12px; border-top: 1px solid var(--border-light);
}
@media (max-width: 768px) { .kb-grid { grid-template-columns: 1fr; } }
@media (min-width: 1400px) { .kb-grid { grid-template-columns: repeat(3, 1fr); } }
</style>
