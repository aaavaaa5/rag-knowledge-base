import request from './request'

export async function fetchSessions(kbId) {
  const res = await request.get(`/api/knowledge-bases/${kbId}/sessions`)
  return { data: res.data.data }
}

export async function createSession(kbId) {
  const res = await request.post(`/api/knowledge-bases/${kbId}/sessions`)
  return { data: res.data.data }
}

export async function deleteSession(kbId, sessionId) {
  await request.delete(`/api/knowledge-bases/${kbId}/sessions/${sessionId}`)
  return { data: null }
}

export async function sendMessage(sessionId, message, topK = 5) {
  const res = await request.post('/api/chat/send', { sessionId, message, topK })
  return { data: res.data.data }
}

export async function getSession(kbId, sessionId) {
  const res = await request.get(`/api/knowledge-bases/${kbId}/sessions/${sessionId}`)
  return { data: res.data.data }
}

export async function sendMessageStream(sessionId, message, topK = 5) {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  const response = await fetch(`${baseUrl}/api/chat/send/stream`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sessionId, message, topK }),
  })
  if (!response.ok) throw new Error(`HTTP ${response.status}`)
  return response.body.getReader()
}
