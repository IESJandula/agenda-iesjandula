import api from './axios'

export function getEventos() {
  return api.get('/api/eventos').then((response) => response.data)
}

export function getEventosPendientes() {
  return api.get('/api/eventos/estado/PENDIENTE').then((response) => response.data)
}

export function getEventoById(id) {
  return api.get(`/api/eventos/${id}`).then((response) => response.data)
}

export function createEvento(payload) {
  return api.post('/api/eventos', payload).then((response) => response.data)
}

export function createPropuestaEvento(payload) {
  return api.post('/api/eventos/propuestas', payload).then((response) => response.data)
}

export function updateEvento(id, payload) {
  return api.put(`/api/eventos/${id}`, payload).then((response) => response.data)
}

export function aprobarEvento(id) {
  return api.put(`/api/eventos/${id}/aprobar`).then((response) => response.data)
}

export function rechazarEvento(id) {
  return api.put(`/api/eventos/${id}/rechazar`).then((response) => response.data)
}

export function deleteEvento(id) {
  return api.delete(`/api/eventos/${id}`).then((response) => response.data)
}