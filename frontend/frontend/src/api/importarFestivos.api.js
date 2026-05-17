import api from './axios'

export function previewImportarFestivos(formData) {
  return api
    .post('/api/eventos/importar-festivos/preview', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
    .then((response) => response.data)
}

export function confirmarImportarFestivos(payload) {
  return api.post('/api/eventos/importar-festivos/confirmar', payload).then((response) => response.data)
}