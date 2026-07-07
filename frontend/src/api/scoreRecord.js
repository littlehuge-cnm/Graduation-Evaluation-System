import request from '@/utils/request.js'

export function getScoreRecordList(studentNo, itemType) {
  return request.get(`/students/${studentNo}/score-records`, {
    params: { itemType }
  })
}

export function getScoreRecordById(id) {
  return request.get(`/score-records/${id}`)
}

export function addScoreRecord(data, recorderNo) {
  return request.post('/score-records', data, {
    params: { recorderNo }
  })
}

export function updateScoreRecord(id, data) {
  return request.put(`/score-records/${id}`, data)
}

export function confirmScoreRecord(id) {
  return request.put(`/score-records/${id}/confirm`)
}

export function unlockScoreRecord(id) {
  return request.put(`/score-records/${id}/unlock`)
}

export function getScoreRecordTodo(recorderNo, userType) {
  return request.get('/score-records/todo', {
    params: { recorderNo, userType }
  })
}
