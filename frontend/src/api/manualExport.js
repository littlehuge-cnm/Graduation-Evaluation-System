import request from '@/utils/request.js'

export function exportManual(studentNo) {
  return request.get('/manual-export', {
    params: { studentNo },
    responseType: 'blob'
  })
}
