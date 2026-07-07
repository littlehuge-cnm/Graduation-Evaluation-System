import request from '@/utils/request.js'

export function getAdminList(params) {
  return request.get('/admins', { params })
}

export function getAdminById(adminId) {
  return request.get(`/admins/${adminId}`)
}

export function addAdmin(data) {
  return request.post('/admins', data)
}

export function updateAdmin(adminId, data) {
  return request.put(`/admins/${adminId}`, data)
}

export function deleteAdmin(adminId) {
  return request.delete(`/admins/${adminId}`)
}
