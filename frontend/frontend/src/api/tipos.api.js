import api from './axios'

export function getTipos() {
  return api.get('/api/tipos').then((response) => response.data)
}

export function getTiposAdmin() {
  return api.get('/api/tipos/admin').then((response) => response.data)
}

export function createTipo(payload) {
  return api.post('/api/tipos/admin', payload).then((response) => response.data)
}

export function updateTipo(id, payload) {
  return api.put(`/api/tipos/admin/${id}`, payload).then((response) => response.data)
}

export function deleteTipo(id) {
  return api.delete(`/api/tipos/admin/${id}`).then((response) => response.data)
}