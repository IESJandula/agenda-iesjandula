<script setup>
import { computed, ref } from 'vue'

import AppShell from '../components/layout/AppShell.vue'
import { confirmarImportarFestivos, previewImportarFestivos } from '../api/importarFestivos.api'

const fileInput = ref(null)
const selectedFile = ref(null)
const loadingPreview = ref(false)
const loadingConfirm = ref(false)
const preview = ref(null)
const formError = ref('')
const successMessage = ref('')

const previewEventos = computed(() => (Array.isArray(preview.value?.eventos) ? preview.value.eventos : []))
const previewErrores = computed(() => (Array.isArray(preview.value?.errores) ? preview.value.errores : []))
const previewAdvertencias = computed(() => (Array.isArray(preview.value?.advertencias) ? preview.value.advertencias : []))
const hasPreview = computed(() => Boolean(preview.value))
const hasErrors = computed(() => previewErrores.value.length > 0)
const previewValidos = computed(() => Number(preview.value?.validos ?? previewEventos.value.length ?? 0))
const previewDuplicados = computed(() => Number(preview.value?.duplicados ?? previewAdvertencias.value.length ?? 0))
const previewTotalErrores = computed(() => Number(preview.value?.totalErrores ?? previewErrores.value.length ?? 0))
const canConfirm = computed(
  () => hasPreview.value && !hasErrors.value && previewEventos.value.length > 0 && !loadingConfirm.value,
)

function handleFileChange(event) {
  selectedFile.value = event.target.files?.[0] ?? null
  formError.value = ''
  successMessage.value = ''
  preview.value = null
}

function resetState(clearFeedback = true) {
  selectedFile.value = null
  preview.value = null

  if (clearFeedback) {
    formError.value = ''
    successMessage.value = ''
  }

  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

async function handlePreview() {
  if (!selectedFile.value) {
    formError.value = 'Selecciona un archivo CSV antes de continuar.'
    return
  }

  loadingPreview.value = true
  formError.value = ''
  successMessage.value = ''

  try {
    const formData = new FormData()
    formData.append('archivo', selectedFile.value)
    const response = await previewImportarFestivos(formData)
    preview.value = {
      eventos: Array.isArray(response?.eventos) ? response.eventos : [],
      errores: Array.isArray(response?.errores) ? response.errores : [],
      advertencias: Array.isArray(response?.advertencias) ? response.advertencias : [],
      validos: Number(response?.validos ?? 0),
      duplicados: Number(response?.duplicados ?? 0),
      totalErrores: Number(response?.totalErrores ?? 0),
    }
  } catch (requestError) {
    preview.value = null
    formError.value = requestError?.response?.data?.message || 'No se pudo generar la preview del CSV.'
  } finally {
    loadingPreview.value = false
  }
}

async function handleConfirm() {
  if (!canConfirm.value) {
    return
  }

  loadingConfirm.value = true
  formError.value = ''
  successMessage.value = ''

  try {
    const resultado = await confirmarImportarFestivos({ eventos: previewEventos.value })
    const importados = Number(resultado?.importados ?? resultado?.eventos?.length ?? 0)
    const omitidosDuplicados = Number(resultado?.omitidosPorDuplicado ?? resultado?.omitidosDuplicados ?? 0)
    const erroresConfirmacion = Array.isArray(resultado?.errores) ? resultado.errores : []
    const totalErrores = erroresConfirmacion.length
    successMessage.value = `Importación completada: ${importados} festivos creados correctamente${omitidosDuplicados > 0 ? `, ${omitidosDuplicados} omitidos por duplicado` : ''}${totalErrores > 0 ? `, ${totalErrores} con error` : ''}.`

    if (totalErrores > 0) {
      const detalleErrores = erroresConfirmacion
        .map((error) => `Línea ${error?.linea ?? '-'}: ${error?.mensaje ?? 'Error desconocido'}`)
        .join(' | ')
      formError.value = `Se detectaron errores durante la confirmación: ${detalleErrores}`
    }

    resetState(false)
  } catch (requestError) {
    formError.value = requestError?.response?.data?.message || 'No se pudo confirmar la importación.'
  } finally {
    loadingConfirm.value = false
  }
}

function formatDate(value) {
  if (!value) {
    return '-'
  }

  const date = new Date(`${value}T00:00:00`)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat('es-ES', {
    dateStyle: 'medium',
  }).format(date)
}
</script>

<template>
  <AppShell
    title="Importar festivos"
    eyebrow="Administración"
    subtitle="Sube un CSV, revisa la preview y confirma la importación de festivos en estado confirmado."
  >
    <section class="panel import-festivos">
      <div class="import-festivos__header">
        <div>
          <p class="eyebrow">CSV</p>
          <h2>Importación de festivos</h2>
          <p class="muted">Formato: fecha_inicio,fecha_fin,titulo,descripcion</p>
        </div>
      </div>

      <p v-if="formError" class="import-festivos__error" role="alert">{{ formError }}</p>
      <p v-if="successMessage" class="import-festivos__success" role="status">{{ successMessage }}</p>

      <div class="import-festivos__actions">
        <input ref="fileInput" class="input import-festivos__file" type="file" accept=".csv,text/csv" @change="handleFileChange" />
        <button class="btn btn--ghost" type="button" @click="resetState">Limpiar</button>
        <button class="btn btn--primary" type="button" :disabled="loadingPreview || !selectedFile" @click="handlePreview">
          {{ loadingPreview ? 'Generando preview...' : 'Ver preview' }}
        </button>
      </div>

      <div v-if="selectedFile" class="import-festivos__fileinfo">
        <strong>Archivo:</strong>
        <span>{{ selectedFile.name }}</span>
      </div>

      <div v-if="preview" class="import-festivos__preview">
        <div class="import-festivos__preview-header">
          <h3>Preview de eventos</h3>
          <span class="import-festivos__badge">{{ previewValidos }} válidos</span>
        </div>

        <p class="import-festivos__summary">
          Resumen: {{ previewValidos }} válidos, {{ previewDuplicados }} duplicados, {{ previewTotalErrores }} errores.
        </p>

        <p v-if="hasErrors" class="import-festivos__warning">
          Hay errores en el CSV. Corrígelo antes de confirmar la importación.
        </p>

        <p v-else-if="previewAdvertencias.length" class="import-festivos__warning">
          Hay festivos duplicados que se omitirán al confirmar la importación.
        </p>

        <div v-if="previewEventos.length > 0" class="table-wrap">
          <table class="table import-festivos__table">
            <thead>
              <tr>
                <th>Línea</th>
                <th>Fecha inicio</th>
                <th>Fecha fin</th>
                <th>Título</th>
                <th>Descripción</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="evento in previewEventos" :key="`${evento.linea}-${evento.titulo}`">
                <td>{{ evento.linea }}</td>
                <td>{{ formatDate(evento.fechaInicio) }}</td>
                <td>{{ formatDate(evento.fechaFin) }}</td>
                <td>{{ evento.titulo }}</td>
                <td>{{ evento.descripcion || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="previewErrores.length" class="import-festivos__errors">
          <h4>Errores de validación</h4>
          <ul>
            <li v-for="error in previewErrores" :key="`${error.linea}-${error.mensaje}`">
              Línea {{ error.linea }}: {{ error.mensaje }}
            </li>
          </ul>
        </div>

        <div v-if="previewAdvertencias.length" class="import-festivos__errors">
          <h4>Advertencias</h4>
          <ul>
            <li v-for="warning in previewAdvertencias" :key="`${warning.linea}-${warning.mensaje}`">
              Línea {{ warning.linea }}: {{ warning.mensaje }}
            </li>
          </ul>
        </div>

        <div v-if="hasPreview" class="import-festivos__confirm">
          <button class="btn btn--primary" type="button" :disabled="!canConfirm" @click="handleConfirm">
            {{ loadingConfirm ? 'Confirmando...' : 'Confirmar importación' }}
          </button>
        </div>
      </div>
    </section>
  </AppShell>
</template>

<style scoped>
.import-festivos {
  padding: 28px;
  display: grid;
  gap: 18px;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: 12px;
}

.import-festivos__header h2 {
  margin: 0 0 6px;
  font-size: 1.3rem;
  font-weight: 600;
}

.import-festivos__header p {
  margin: 0;
}

.import-festivos__error,
.import-festivos__success,
.import-festivos__warning {
  margin: 0;
  padding: 12px 14px;
  border-radius: 8px;
}

.import-festivos__summary {
  margin: 0;
  color: var(--muted);
}

.import-festivos__error {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
}

.import-festivos__success {
  background: #dcfce7;
  border: 1px solid #bbf7d0;
  color: #166534;
}

.import-festivos__warning {
  background: #fef3c7;
  border: 1px solid #fcd34d;
  color: #92400e;
}

.import-festivos__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.import-festivos__file {
  max-width: 360px;
}

.import-festivos__fileinfo {
  display: flex;
  gap: 8px;
  align-items: center;
  color: var(--muted);
}

.import-festivos__preview {
  display: grid;
  gap: 16px;
}

.import-festivos__preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.import-festivos__preview-header h3 {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 600;
}

.import-festivos__badge {
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  background: var(--bg-soft);
  border: 1px solid var(--border);
  color: var(--primary);
  font-size: 0.8rem;
  font-weight: 600;
}

.import-festivos__errors {
  display: grid;
  gap: 10px;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: var(--bg-soft);
}

.import-festivos__errors h4 {
  margin: 0;
  font-size: 0.95rem;
}

.import-festivos__errors ul {
  margin: 0;
  padding-left: 18px;
  color: #92400e;
}

.import-festivos__confirm {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 820px) {
  .import-festivos {
    padding: 20px;
  }

  .import-festivos__actions,
  .import-festivos__preview-header {
    flex-direction: column;
    align-items: stretch;
  }

  .import-festivos__file {
    max-width: 100%;
  }

  .import-festivos__confirm .btn,
  .import-festivos__actions .btn {
    width: 100%;
  }
}
</style>