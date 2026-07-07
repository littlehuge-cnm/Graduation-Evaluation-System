import request from '@/utils/request.js'

export function getStudentGroupList() {
  return request.get('/student-groups')
}

export function getStudentGroupById(groupId) {
  return request.get(`/student-groups/${groupId}`)
}

export function addStudentGroup(data) {
  return request.post('/student-groups', data)
}

export function updateStudentGroup(groupId, data) {
  return request.put(`/student-groups/${groupId}`, data)
}

export function deleteStudentGroup(groupId) {
  return request.delete(`/student-groups/${groupId}`)
}
