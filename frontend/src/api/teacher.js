import request from '@/utils/request.js'

export function getTeacherList(params) {
  return request.get('/teachers', { params })
}

export function getTeacherById(teacherNo) {
  return request.get(`/teachers/${teacherNo}`)
}

export function addTeacher(data) {
  return request.post('/teachers', data)
}

export function updateTeacher(teacherNo, data) {
  return request.put(`/teachers/${teacherNo}`, data)
}

export function deleteTeacher(teacherNo) {
  return request.delete(`/teachers/${teacherNo}`)
}

export function importTeachers(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/teachers/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getTeacherStudents(teacherNo, relationType) {
  return request.get(`/teachers/${teacherNo}/students`, {
    params: { relationType }
  })
}
