import request from '@/utils/request.js'

export function updateAccountStatus(data) {
  return request.put('/accounts/status', data)
}
