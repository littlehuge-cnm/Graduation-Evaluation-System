import request from '@/utils/request.js'

export function exportManual(studentNo) {
  return request.get('/manual-export', {
    params: { studentNo },
    responseType: 'blob'
  })
}

export function exportManualBatch(studentNos) {
  return request.post('/manual-export/batch', { studentNos }, {
    responseType: 'blob'
  })
}

export function exportManualByGroup(studentGroupId) {
  return request.post('/manual-export/by-group', { studentGroupId }, {
    responseType: 'blob'
  })
}
