import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '@/components/AppLayout.vue'
import ChatView from '@/views/ChatView.vue'
import KnowledgeBaseView from '@/views/KnowledgeBaseView.vue'
import DocumentView from '@/views/DocumentView.vue'

const routes = [
  {
    path: '/',
    redirect: '/chat',
  },
  {
    path: '/chat',
    name: 'Chat',
    components: { default: AppLayout },
    children: [
      {
        path: '',
        name: 'ChatDefault',
        component: ChatView,
      },
    ],
  },
  {
    path: '/knowledge-base',
    name: 'KnowledgeBase',
    components: { default: AppLayout },
    children: [
      {
        path: '',
        name: 'KnowledgeBaseList',
        component: KnowledgeBaseView,
      },
      {
        path: ':id/documents',
        name: 'Documents',
        component: DocumentView,
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
