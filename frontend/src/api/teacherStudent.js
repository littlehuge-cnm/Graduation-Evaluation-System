import request from '@/utils/request.js'

export function getTeacherStudentList(params) {
  return request.get('/teacher-students', { params })
}

export function addTeacherStudent(data) {
  return request.post('/teacher-students', data)
}

export function updateTeacherStudent(id, data) {
  return request.put(`/teacher-students/${id}`, data)
}

export function deleteTeacherStudent(id) {
  return request.delete(`/teacher-students/${id}`)
}

export function updateRelationStatus(id, status) {
  return request.put(`/teacher-students/${id}/relation-status`, { relationStatus: status })
}

export function batchAssignTeacherStudent(list) {
  return request.post('/teacher-students/batch-assign', list)
}
