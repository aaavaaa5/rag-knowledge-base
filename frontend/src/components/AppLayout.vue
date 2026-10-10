<template>
  <el-container class="app-layout">
    <el-aside :width="sidebarWidth" class="app-sidebar">
      <div class="sidebar-brand">
        <div class="brand-icon">
          <el-icon :size="22"><Connection /></el-icon>
        </div>
        <span class="brand-text">RAG 知识库</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        router
        class="sidebar-menu"
        background-color="#1E2A3A"
        text-color="#A0AEC0"
        active-text-color="#FFFFFF"
      >
        <el-menu-item index="/chat">
          <el-icon><ChatDotSquare /></el-icon>
          <span>对话</span>
        </el-menu-item>
        <el-menu-item index="/knowledge-base">
          <el-icon><FolderOpened /></el-icon>
          <span>知识库管理</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <div class="status-indicator" :class="backendConnected ? 'connected' : 'disconnected'">
          <span class="status-dot" />
          <span class="status-text">{{ backendConnected ? '后端已连接' : '后端服务未连接' }}</span>
        </div>
      </div>
    </el-aside>

    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/api/request'

const route = useRoute()
const sidebarWidth = '220px'
const backendConnected = ref(false)

const activeMenu = computed(() => {
  if (route.path.startsWith('/knowledge-base')) return '/knowledge-base'
  return '/chat'
})

const checkHealth = async () => {
  try {
    const res = await request.get('/api/health')
    backendConnected.value = res.data?.data?.status === 'ok'
  } catch {
    backendConnected.value = false
  }
}

onMounted(checkHealth)
watch(() => route.path, checkHealth)
</script>

<style scoped>
.app-layout {
  height: 100vh;
  overflow: hidden;
}

.app-sidebar {
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  user-select: none;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 20px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.brand-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: var(--sidebar-active);
  color: #fff;
}

.brand-text {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  letter-spacing: -0.01em;
}

.sidebar-menu {
  flex: 1;
  border-right: none;
  padding: 8px 0;
  background: transparent;
}

.sidebar-menu .el-menu-item {
  height: 44px;
  line-height: 44px;
  margin: 2px 8px;
  border-radius: 8px;
  font-size: 14px;
}

.sidebar-menu .el-menu-item:hover {
  background: rgba(255, 255, 255, 0.06) !important;
}

.sidebar-menu .el-menu-item.is-active {
  background: var(--sidebar-active) !important;
  color: #fff !important;
}

.sidebar-menu .el-menu-item .el-icon {
  font-size: 18px;
  margin-right: 10px;
}

.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--sidebar-text);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.connected .status-dot {
  background: #10B981;
  box-shadow: 0 0 6px rgba(16, 185, 129, 0.4);
}

.disconnected .status-dot {
  background: #EF4444;
  box-shadow: 0 0 6px rgba(239, 68, 68, 0.4);
}

.app-main {
  padding: 0;
  overflow: hidden;
  background: var(--main-bg);
}
</style>
