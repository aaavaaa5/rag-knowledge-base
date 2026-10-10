import request from './request'

export async function fetchDocuments(kbId) {
  const res = await request.get(`/api/knowledge-bases/${kbId}/documents`)
  return { data: res.data.data }
}

export async function uploadDocument(kbId, file, chunkSize, overlap) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('chunkSize', chunkSize)
  formData.append('overlap', overlap)
  const res = await request.post(`/api/knowledge-bases/${kbId}/documents`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return { data: res.data.data }
}

export async function deleteDocument(kbId, docId) {
  await request.delete(`/api/knowledge-bases/${kbId}/documents/${docId}`)
  return { data: null }
}
