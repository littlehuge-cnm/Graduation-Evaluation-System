import request from '@/utils/request.js'

export function getMyStudentDocuments(teacherNo, docType) {
  return request.get('/documents/my-students', {
    params: { teacherNo, docType }
  })
}

export function getDocumentById(id) {
  return request.get(`/documents/${id}`)
}

export function saveDocument(data) {
  return request.post('/documents', data)
}

export function updateDocument(id, data) {
  return request.put(`/documents/${id}`, data)
}

export function submitDocument(id) {
  return request.put(`/documents/${id}/submit`)
}
