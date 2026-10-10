<template>
  <div class="page-container chat-page">
    <!-- KB Selector (when no KB selected) -->
    <template v-if="!selectedKb">
      <div class="page-header">
        <div>
          <h1 class="page-title">对话</h1>
          <p class="page-subtitle">选择一个知识库开始对话</p>
        </div>
      </div>
      <div v-if="kbLoading" v-loading="true" class="loading-area" />
      <div v-else-if="kbList.length === 0" class="empty-state">
        <el-icon><ChatDotSquare /></el-icon>
        <p>还没有知识库，请先在知识库管理中创建</p>
      </div>
      <div v-else class="kb-select-grid">
        <div v-for="kb in kbList" :key="kb.id" class="kb-select-card" @click="selectKB(kb)">
          <div class="select-card-icon"><el-icon :size="24"><Collection /></el-icon></div>
          <h3 class="select-card-title">{{ kb.name }}</h3>
          <p class="select-card-meta">{{ kb.docCount }} 个文档</p>
        </div>
      </div>
    </template>

    <!-- Chat Interface (when KB selected) -->
    <template v-else>
      <div class="chat-layout">
        <!-- Header -->
        <div class="chat-header">
          <div class="chat-header-left">
            <el-button text @click="switchKB" class="back-btn"><el-icon><ArrowLeft /></el-icon></el-button>
            <div>
              <h2 class="chat-kb-name">{{ selectedKb.name }}</h2>
              <span class="chat-session-title" v-if="currentSession">{{ currentSession.title }}</span>
            </div>
          </div>
          <div class="chat-header-right">
            <div class="topk-control">
              <span class="topk-label">Top-K</span>
              <el-tooltip :content="`检索时参考前 ${topK} 个相关片段`">
                <el-slider v-model="topK" :min="1" :max="20" :step="1" show-input :show-input-controls="false" input-size="small" class="topk-slider" />
              </el-tooltip>
            </div>
            <el-button @click="startNewSession"><el-icon><Plus /></el-icon>新对话</el-button>
            <el-button @click="showHistory = true"><el-icon><Clock /></el-icon>历史记录</el-button>
          </div>
        </div>

        <!-- Messages -->
        <div class="chat-messages" ref="messagesRef">
          <div v-if="!currentSession || currentSession.messages.length === 0" class="empty-state">
            <el-icon><ChatLineSquare /></el-icon>
            <p>开始提问吧</p>
          </div>
          <div v-else class="messages-list">
            <div v-for="msg in currentSession.messages" :key="msg.id" :class="['message-row', msg.role]">
              <div class="message-bubble">
                <div class="message-role-tag">
                  <el-tag :type="msg.role === 'user' ? 'primary' : 'success'" size="small" effect="plain">
                    {{ msg.role === 'user' ? '你' : 'AI' }}
                  </el-tag>
                </div>
                <div class="message-text markdown-body" v-html="renderMarkdown(msg.content)"></div>
                <el-tag v-if="msg._failed" type="danger" size="small" style="margin-top:8px">发送失败</el-tag>
                <!-- Citations -->
                <div v-if="msg.citations && msg.citations.length" class="citations">
                  <div class="citations-header" @click="toggleCitations(msg)">
                    <el-icon :size="14"><Link /></el-icon>
                    <span>引用来源 ({{ msg.citations.length }})</span>
                    <el-icon :class="['collapse-icon', { expanded: expandedCitations[msg.id] }]"><ArrowDown /></el-icon>
                  </div>
                  <div v-show="expandedCitations[msg.id]" class="citations-list">
                    <div v-for="(cit, ci) in msg.citations" :key="ci" class="citation-item">
                      <div class="citation-doc">
                        <el-icon :size="12"><Document /></el-icon>
                        <span class="citation-doc-name">{{ cit.docName }}</span>
                        <span class="citation-score">{{ (cit.score * 100).toFixed(0) }}%</span>
                      </div>
                      <div class="citation-snippet">"{{ cit.snippet }}"</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Input -->
        <div class="chat-input-area">
          <el-input v-model="inputText" type="textarea" :rows="2" placeholder="输入您的问题，按 Ctrl+Enter 发送" :disabled="sending" @keydown.ctrl.enter="sendMessage" />
          <div class="chat-input-actions">
            <span class="input-hint">Ctrl+Enter 发送</span>
            <el-button type="primary" :loading="sending" :disabled="!inputText.trim()" @click="sendMessage">
              <el-icon><Promotion /></el-icon>发送
            </el-button>
          </div>
        </div>
      </div>
    </template>

    <!-- History Drawer -->
    <el-drawer v-model="showHistory" title="对话历史" size="340px" :with-header="true">
      <div v-if="!selectedKb" class="empty-state" style="padding: 40px 0"><p>请先选择一个知识库</p></div>
      <div v-else-if="historyLoading" v-loading="true" class="loading-area" />
      <div v-else-if="sessions.filter(s => s.messageCount > 0).length === 0" class="empty-state" style="padding: 40px 0"><p>暂无对话记录</p></div>
      <div v-else class="history-list">
        <div v-for="session in sessions.filter(s => s.messageCount > 0)" :key="session.id" :class="['history-item', { active: currentSession?.id === session.id }]" @click="loadSession(session)">
          <div class="history-item-body">
            <div class="history-item-title">{{ session.title }}</div>
            <div class="history-item-meta">
              <span>{{ session.messageCount || session.messages?.length || 0 }} 条消息</span>
              <span>·</span>
              <span>{{ session.updatedAt }}</span>
            </div>
          </div>
          <el-button text type="danger" size="small" class="history-delete-btn" @click.stop="handleDeleteSession(session)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ArrowDown, Plus, Promotion, ChatDotSquare, ChatLineSquare, Collection, Clock, Link, Document, Delete } from '@element-plus/icons-vue'
import { fetchKnowledgeBases } from '@/api/knowledgeBase'
import { fetchSessions, createSession, deleteSession, sendMessage as apiSendMessage, sendMessageStream, getSession } from '@/api/chat'

const selectedKb = ref(null)
const kbList = ref([])
const kbLoading = ref(true)
const currentSession = ref(null)
const sessions = ref([])
const historyLoading = ref(false)
const showHistory = ref(false)
const inputText = ref('')
const sending = ref(false)
const topK = ref(5)
const messagesRef = ref(null)
const expandedCitations = ref({})
let typewriterBuffer = ''
let typewriterTimer = null
let typewriterTarget = null

const renderMarkdown = (text) => {
  if (!text) return ''
  // Escape HTML special chars
  let html = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
  // bold: **text**
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  // unordered list: - item, convert to <ul><li>
  let lines = html.split('\n')
  let inList = false
  let result = []
  for (const line of lines) {
    const listMatch = line.match(/^- (.+)$/)
    if (listMatch) {
      if (!inList) {
        result.push('<ul>')
        inList = true
      }
      result.push('<li>' + listMatch[1] + '</li>')
    } else {
      if (inList) {
        result.push('</ul>')
        inList = false
      }
      result.push(line)
    }
  }
  if (inList) result.push('</ul>')
  html = result.join('\n')
  // paragraphs: split by double newlines, wrap in <p>
  const paragraphs = html.split(/\n\s*\n/)
  html = paragraphs.map(p => {
    const t = p.trim()
    if (!t) return ''
    return '<p>' + t.replace(/\n/g, '<br>') + '</p>'
  }).join('')
  return html
}

const startTypewriter = (aiMsg) => {
  stopTypewriter()
  typewriterTarget = aiMsg
  typewriterBuffer = ''
  typewriterTimer = setInterval(() => {
    if (typewriterTarget && typewriterBuffer.length > 0) {
      typewriterTarget.content += typewriterBuffer[0]
      typewriterBuffer = typewriterBuffer.slice(1)
      nextTick(scrollToBottom)
    }
  }, 30)
}

const addToTypewriter = (text) => {
  typewriterBuffer += text
}

const stopTypewriter = () => {
  clearInterval(typewriterTimer)
  typewriterTimer = null
  if (typewriterTarget && typewriterBuffer.length > 0) {
    typewriterTarget.content += typewriterBuffer
    typewriterBuffer = ''
  }
  typewriterTarget = null
}

const loadKBs = async () => {
  kbLoading.value = true
  try {
    const res = await fetchKnowledgeBases()
    kbList.value = res.data
  } catch {
    ElMessage.error('加载知识库列表失败')
  } finally {
    kbLoading.value = false
  }
}

const selectKB = async (kb) => {
  selectedKb.value = kb
  currentSession.value = null
  loadSessions()
}

const switchKB = () => {
  selectedKb.value = null
  currentSession.value = null
  sessions.value = []
  inputText.value = ''
}

const loadSessions = async () => {
  if (!selectedKb.value) return
  historyLoading.value = true
  try {
    const res = await fetchSessions(selectedKb.value.id)
    sessions.value = res.data
  } catch {
    // silent
  } finally {
    historyLoading.value = false
  }
}

const startNewSession = () => {
  if (!selectedKb.value) return
  currentSession.value = null
  inputText.value = ''
  showHistory.value = false
}

const loadSession = async (session) => {
  try {
    const res = await getSession(selectedKb.value.id, session.id)
    if (res.data) {
      currentSession.value = res.data
      showHistory.value = false
      nextTick(scrollToBottom)
    }
  } catch {
    ElMessage.error('加载对话失败')
  }
}

const handleDeleteSession = async (session) => {
  try {
    await ElMessageBox.confirm('确定要删除这条对话记录吗？', '确认删除', {
      confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning',
    })
    await deleteSession(selectedKb.value.id, session.id)
    ElMessage.success('对话已删除')
    if (currentSession.value?.id === session.id) {
      await startNewSession()
    }
    loadSessions()
  } catch {
    // cancelled or error
  }
}

const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || sending.value || !selectedKb.value) return

  if (!currentSession.value || !currentSession.value.id) {
    try {
      const sessionRes = await createSession(selectedKb.value.id)
      currentSession.value = sessionRes.data
    } catch {
      ElMessage.error('创建会话失败')
      return
    }
  }

  if (!currentSession.value.messages) currentSession.value.messages = []

  const tempUserMsg = { id: Date.now(), role: 'user', content: text, citations: null, createdAt: new Date().toISOString().replace('T', ' ').slice(0, 19) }
  const tempAiMsg = { id: Date.now() + 1, role: 'assistant', content: '', citations: null, createdAt: new Date().toISOString().replace('T', ' ').slice(0, 19) }
  currentSession.value.messages.push(tempUserMsg, tempAiMsg)

  inputText.value = ''
  sending.value = true
  nextTick(scrollToBottom)
  startTypewriter(currentSession.value.messages[currentSession.value.messages.length - 1])

  try {
    const reader = await sendMessageStream(currentSession.value.id, text, topK.value)
    const decoder = new TextDecoder()
    let sseBuf = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      sseBuf += decoder.decode(value, { stream: true })
      const parts = sseBuf.split('\n\n')
      sseBuf = parts.pop()
      for (const part of parts) {
        const lines = part.split('\n')
        let evt = '', dat = ''
         for (const ln of lines) {
           if (ln.startsWith('event:')) { evt = ln.slice(6); if (evt.startsWith(' ')) evt = evt.slice(1) }
           else if (ln.startsWith('data:')) {
             if (dat) dat += '\n'
             const val = ln.slice(5)
             dat += val.startsWith(' ') ? val.slice(1) : val
           }
         }
        if (evt === 'delta') { addToTypewriter(dat) }
        else if (evt === 'done') {
          stopTypewriter()
          const r = JSON.parse(dat)
          if (currentSession.value) {
            const ui = currentSession.value.messages.findIndex(m => m.id === tempUserMsg.id)
            if (ui !== -1) currentSession.value.messages[ui] = r.userMsg
            const ai = currentSession.value.messages.findIndex(m => m.id === tempAiMsg.id)
            if (ai !== -1) {
              currentSession.value.messages[ai].id = r.aiMsg.id
              currentSession.value.messages[ai].citations = r.aiMsg.citations
            }
          }
          nextTick(scrollToBottom)
        } else if (evt === 'error') {
          stopTypewriter()
          ElMessage.error(dat || '发送失败')
          tempAiMsg._failed = true
        }
      }
    }
  } catch {
    stopTypewriter()
    try {
      const res = await apiSendMessage(currentSession.value.id, text, topK.value)
      if (currentSession.value) {
        currentSession.value.messages = currentSession.value.messages.filter(m => m.id !== tempAiMsg.id)
        const ui = currentSession.value.messages.findIndex(m => m.id === tempUserMsg.id)
        if (ui !== -1) currentSession.value.messages[ui] = res.data.userMsg
        currentSession.value.messages.push(res.data.aiMsg)
      }
      nextTick(scrollToBottom)
    } catch {
      ElMessage.error('发送失败，请重试')
      const fm = currentSession.value.messages.find(m => m.id === tempUserMsg.id)
      if (fm) fm._failed = true
      tempAiMsg._failed = true
    }
  } finally {
    sending.value = false
  }
  loadSessions()
}

const scrollToBottom = () => {
  nextTick(() => {
    const el = messagesRef.value?.$el || messagesRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

onMounted(loadKBs)
const toggleCitations = (msg) => {
  expandedCitations.value = { ...expandedCitations.value, [msg.id]: !expandedCitations.value[msg.id] }
}
</script>

<style scoped>
.chat-page {
  display: flex; flex-direction: column;
  padding: 0 !important; height: 100%;
}
.loading-area { min-height: 200px; display: flex; align-items: center; justify-content: center; }

/* KB Selector */
.kb-select-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px; margin-top: 8px;
}
.kb-select-card {
  background: var(--card-bg); border: 1px solid var(--border);
  border-radius: var(--radius-md); padding: 24px;
  cursor: pointer; transition: all 0.2s ease; text-align: center;
}
.kb-select-card:hover {
  border-color: var(--primary-border); box-shadow: var(--shadow-md); transform: translateY(-1px);
}
.select-card-icon {
  width: 48px; height: 48px; margin: 0 auto 12px;
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius-sm); background: var(--primary-light); color: var(--primary);
}
.select-card-title { font-size: 16px; font-weight: 600; color: var(--text-primary); margin-bottom: 4px; }
.select-card-meta { font-size: 13px; color: var(--text-secondary); }

/* Chat Layout */
.chat-layout { display: flex; flex-direction: column; height: 100%; }

/* Chat Header */
.chat-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 24px; background: var(--card-bg);
  border-bottom: 1px solid var(--border); flex-shrink: 0;
}
.chat-header-left { display: flex; align-items: center; gap: 12px; }
.back-btn .el-icon { font-size: 18px; }
.chat-kb-name { font-size: 16px; font-weight: 600; color: var(--text-primary); }
.chat-session-title { font-size: 12px; color: var(--text-muted); }
.chat-header-right { display: flex; align-items: center; gap: 12px; }
.topk-control { display: flex; align-items: center; gap: 8px; }
.topk-label { font-size: 13px; color: var(--text-secondary); white-space: nowrap; }
.topk-slider { width: 140px; }
.topk-slider :deep(.el-input-number--small) { width: 52px; }
.topk-slider :deep(.el-input-number--small .el-input__wrapper) { padding: 0 4px; }

/* Messages */
.chat-messages { flex: 1; overflow-y: auto; padding: 24px; background: var(--main-bg); }
.messages-list { max-width: 800px; margin: 0 auto; }
.message-row { margin-bottom: 20px; }
.message-row.user { display: flex; justify-content: flex-end; }
.message-bubble { max-width: 75%; }
.message-row.user .message-bubble {
  background: var(--primary-light); border: 1px solid var(--primary-border);
  border-radius: var(--radius-md); padding: 12px 16px;
}
.message-row.assistant .message-bubble {
  background: var(--card-bg); border: 1px solid var(--border);
  border-radius: var(--radius-md); padding: 12px 16px;
}
.message-role-tag { margin-bottom: 8px; }
.message-text { font-size: 14px; line-height: 1.7; color: var(--text-primary); word-break: break-word; }
/* Markdown rendering styles */
.markdown-body p { margin-bottom: 8px; }
.markdown-body p:last-child { margin-bottom: 0; }
.markdown-body strong { font-weight: 600; }
.markdown-body ul, .markdown-body ol { padding-left: 20px; margin-bottom: 8px; }
.markdown-body li { margin-bottom: 4px; }
.markdown-body ul:last-child, .markdown-body ol:last-child { margin-bottom: 0; }

/* Loading Animation */
.loading-text { display: flex; gap: 4px; padding: 4px 0; }
.loading-dot { width: 6px; height: 6px; background: var(--text-muted); border-radius: 50%; animation: dotPulse 1.4s infinite ease-in-out both; }
.loading-dot:nth-child(1) { animation-delay: -0.32s; }
.loading-dot:nth-child(2) { animation-delay: -0.16s; }
@keyframes dotPulse { 0%, 80%, 100% { transform: scale(0); } 40% { transform: scale(1); } }

/* Citations */
.citations { margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--border-light); }
.citations-header { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--primary); cursor: pointer; user-select: none; }
.citations-header:hover { color: var(--primary-hover); }
.collapse-icon { transition: transform 0.2s; font-size: 12px; }
.collapse-icon.expanded { transform: rotate(180deg); }
.citations-list { margin-top: 8px; display: flex; flex-direction: column; gap: 8px; }
.citation-item { background: var(--border-light); border-radius: var(--radius-sm); padding: 8px 12px; }
.citation-doc { display: flex; align-items: center; gap: 4px; font-size: 12px; color: var(--text-secondary); margin-bottom: 4px; }
.citation-doc-name { font-weight: 500; color: var(--text-primary); }
.citation-score { margin-left: auto; font-size: 11px; color: var(--primary); font-weight: 500; }
.citation-snippet { font-size: 12px; color: var(--text-secondary); line-height: 1.5; font-style: italic; }

/* Input */
.chat-input-area { padding: 16px 24px; background: var(--card-bg); border-top: 1px solid var(--border); flex-shrink: 0; }
.chat-input-actions { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; }
.input-hint { font-size: 12px; color: var(--text-muted); }

/* History Drawer */
.history-list { display: flex; flex-direction: column; gap: 4px; }
.history-item { display: flex; align-items: center; gap: 8px; padding: 12px; border-radius: var(--radius-sm); cursor: pointer; transition: background 0.15s; }
.history-item:hover, .history-item.active { background: var(--primary-light); }
.history-item-body { flex: 1; min-width: 0; }
.history-item-title { font-size: 14px; font-weight: 500; color: var(--text-primary); margin-bottom: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.history-item-meta { font-size: 12px; color: var(--text-muted); display: flex; align-items: center; gap: 4px; }
.history-delete-btn { opacity: 0; transition: opacity 0.15s; }
.history-item:hover .history-delete-btn { opacity: 1; }
</style>
