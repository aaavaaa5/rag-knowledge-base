import request from './request'

export async function fetchKnowledgeBases(params) {
  const res = await request.get('/api/knowledge-bases', { params })
  return { data: res.data.data }
}

export async function createKnowledgeBase(name) {
  const res = await request.post('/api/knowledge-bases', { name })
  return { data: res.data.data }
}

export async function deleteKnowledgeBase(id) {
  await request.delete(`/api/knowledge-bases/${id}`)
  return { data: null }
}

export async function getKnowledgeBase(id) {
  const res = await request.get(`/api/knowledge-bases/${id}`)
  return { data: res.data.data }
}
