import axios from 'axios'

const api = axios.create({
    baseURL: '/api'
})

export const strategyApi = {
    saveDef: (data) => api.post('/strategy/def', data),
    listDef: () => api.get('/strategy/def'),
    createInstance: (data) => api.post('/strategy/instance', data),
    listInstances: () => api.get('/strategy/instance'),
    updateStatus: (id, status) => api.put(`/strategy/instance/${id}/status`, null, { params: { status } })
}

export const monitorApi = {
    getQueueDepth: () => api.get('/monitor/queueDepth'),
    getOrders: () => api.get('/monitor/orders')
}