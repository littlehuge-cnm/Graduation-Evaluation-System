import request from '@/utils/request.js'

export function getGroupMappingList(params) {
  return request.get('/group-mappings', { params })
}

export function getGroupMappingById(id) {
  return request.get(`/group-mappings/${id}`)
}

export function addGroupMapping(data) {
  return request.post('/group-mappings', data)
}

export function updateGroupMapping(id, data) {
  return request.put(`/group-mappings/${id}`, data)
}

export function deleteGroupMapping(id) {
  return request.delete(`/group-mappings/${id}`)
}
