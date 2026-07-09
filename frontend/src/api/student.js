import request from '@/utils/request.js'

export function getStudentList(params) {
  return request.get('/students', { params })
}

export function getStudentById(studentNo) {
  return request.get(`/students/${studentNo}`)
}

export function addStudent(data) {
  return request.post('/students', data)
}

export function updateStudent(studentNo, data) {
  return request.put(`/students/${studentNo}`, data)
}

export function deleteStudent(studentNo) {
  return request.delete(`/students/${studentNo}`)
}

export function importStudents(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/students/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getStudentTeachers(studentNo) {
  return request.get(`/students/${studentNo}/teachers`)
}

export function getStudentDocuments(studentNo, docType) {
  return request.get(`/students/${studentNo}/documents`, {
    params: { docType }
  })
}
