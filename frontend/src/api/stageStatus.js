import request from '@/utils/request.js'

export function getStudentStageStatus(studentNo) {
    return request.get(`/students/${studentNo}/stage-status`)
}

export function startStage(data) {
    return request.put('/stage-status/start', data)
}

export function completeStage(data) {
    return request.put('/stage-status/complete', data)
}

export function startStageBatch(data) {
    return request.put('/stage-status/start-batch', data)
}

export function getStageOverview(stage, groupId) {
    return request.get('/stage-status/overview', {
        params: { stage, groupId }
    })
}
