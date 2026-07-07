import request from '@/utils/request.js'

export function login(data) {
  return request.post('/auth/login', data)
}

export function getUserInfo() {
  return request.get('/auth/info')
}

export function logout() {
  return request.post('/auth/logout')
}

export function changePassword(data) {
  return request.put('/auth/password', data)
}

export function resetPassword(data) {
  return request.put('/auth/password/reset', data)
}
