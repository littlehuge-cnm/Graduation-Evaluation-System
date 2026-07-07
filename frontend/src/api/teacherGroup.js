import request from '@/utils/request.js'

export function getTeacherGroupList() {
  return request.get('/teacher-groups')
}

export function getTeacherGroupById(groupId) {
  return request.get(`/teacher-groups/${groupId}`)
}

export function addTeacherGroup(data) {
  return request.post('/teacher-groups', data)
}

export function updateTeacherGroup(groupId, data) {
  return request.put(`/teacher-groups/${groupId}`, data)
}

export function deleteTeacherGroup(groupId) {
  return request.delete(`/teacher-groups/${groupId}`)
}

export function updateTeacherGroupStatus(groupId, status) {
  return request.put(`/teacher-groups/${groupId}/status`, { groupStatus: status })
}
