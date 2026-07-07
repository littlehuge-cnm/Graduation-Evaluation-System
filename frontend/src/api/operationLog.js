import request from '@/utils/request.js'

export function getOperationLogList(params) {
  return request.get('/operation-logs', { params })
}

export function getOperationLogById(id) {
  return request.get(`/operation-logs/${id}`)
}
