<script setup>
import { computed, onMounted, reactive, ref } from 'vue'

import AppShell from '../components/layout/AppShell.vue'
import { createPropuestaEvento, getEventos } from '../api/eventos.api'
import { getTipos } from '../api/tipos.api'
import { getUsuarios } from '../api/usuarios.api'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

const tipos = ref([])
const usuarios = ref([])
const eventos = ref([])
const loading = ref(false)
const submitting = ref(false)
const formError = ref('')
const fieldErrors = ref({})

const form = reactive({
  tipoId: '',
  titulo: '',
  descripcion: '',
  fechaInicio: '',
  fechaFin: '',
  lugar: '',
  gruposAfectados: '',
  enlaceDocumento: '',
  numAsistentes: '',
})

function normalizeCollection(response) {
  if (Array.isArray(response)) {
    return response
  }

  return response?.content ?? response?.data ?? []
}

function parseDate(value) {
  if (!value) {
    return null
  }

  const parsed = new Date(String(value).replace(' ', 'T'))
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

function fromBackendDateTime(value) {
  if (!value) {
    return ''
  }

  const sanitizedValue = String(value).replace(' ', 'T')
  return sanitizedValue.slice(0, 16)
}

function toLocalDateTime(value) {
  if (!value) {
    return null
  }

  return value.length === 16 ? `${value}:00` : value
}

function normalizeHexColor(color) {
  if (typeof color !== 'string') {
    return ''
  }

  const trimmed = color.trim()
  return /^#([0-9a-f]{6})$/i.test(trimmed) ? trimmed : ''
}

function resetForm() {
  form.tipoId = tipos.value[0] ? String(tipos.value[0].id) : ''
  form.titulo = ''
  form.descripcion = ''
  form.fechaInicio = ''
  form.fechaFin = ''
  form.lugar = ''
  form.gruposAfectados = ''
  form.enlaceDocumento = ''
  form.numAsistentes = ''
  fieldErrors.value = {}
  formError.value = ''
}

const currentUser = computed(() =>
  usuarios.value.find((usuario) => String(usuario.email).toLowerCase() === String(authStore.user?.email ?? '').toLowerCase()) ?? null,
)

const misPropuestas = computed(() => {
  const currentUserId = currentUser.value?.id

  return eventos.value
    .filter((evento) => currentUserId != null && Number(evento.creadorId) === Number(currentUserId))
    .map((evento) => ({
      ...evento,
      fechaInicioDate: parseDate(evento.fechaInicio),
      tipoColorResolved: normalizeHexColor(evento.tipoColor) || '#61d6a7',
    }))
    .sort((a, b) => (a.fechaInicioDate?.getTime() ?? 0) - (b.fechaInicioDate?.getTime() ?? 0))
})

function statusClass(status) {
  const normalized = String(status || '').toUpperCase()

  if (normalized === 'PENDIENTE') {
    return 'propuesta-item__status--warning'
  }

  if (normalized === 'CONFIRMADO') {
    return 'propuesta-item__status--success'
  }

  if (normalized === 'CANCELADO') {
    return 'propuesta-item__status--danger'
  }

  return 'propuesta-item__status--neutral'
}

function statusLabel(status) {
  const normalized = String(status || '').toUpperCase()

  if (normalized === 'PENDIENTE') return 'Pendiente'
  if (normalized === 'CONFIRMADO') return 'Confirmado'
  if (normalized === 'CANCELADO') return 'Cancelado'
  return normalized || 'Sin estado'
}

function formatTime(value) {
  const parsed = value instanceof Date ? value : parseDate(value)

  if (!parsed) {
    return '-'
  }

  return new Intl.DateTimeFormat('es-ES', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(parsed)
}

function itemStyle(evento) {
  return {
    borderLeftColor: evento.tipoColorResolved,
  }
}

async function loadData() {
  loading.value = true
  formError.value = ''

  try {
    const [tiposResponse, usuariosResponse, eventosResponse] = await Promise.all([
      getTipos(),
      getUsuarios(),
      getEventos(),
    ])

    tipos.value = normalizeCollection(tiposResponse)
    usuarios.value = normalizeCollection(usuariosResponse)
    eventos.value = normalizeCollection(eventosResponse)

    if (!form.tipoId && tipos.value.length > 0) {
      form.tipoId = String(tipos.value[0].id)
    }
  } catch (requestError) {
    formError.value = requestError?.response?.data?.message || 'No se pudieron cargar los datos de propuestas.'
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  fieldErrors.value = {}
  formError.value = ''

  if (!currentUser.value?.id) {
    formError.value = 'No se pudo identificar tu usuario para crear la propuesta.'
    return
  }

  submitting.value = true

  try {
    await createPropuestaEvento({
      tipoId: Number(form.tipoId),
      creadorId: Number(currentUser.value.id),
      titulo: form.titulo.trim(),
      descripcion: form.descripcion.trim() || null,
      fechaInicio: toLocalDateTime(form.fechaInicio),
      fechaFin: toLocalDateTime(form.fechaFin),
      lugar: form.lugar.trim(),
      gruposAfectados: form.gruposAfectados.trim(),
      enlaceDocumento: form.enlaceDocumento.trim() || null,
      numAsistentes: form.numAsistentes === '' ? null : Number(form.numAsistentes),
      estado: 'PENDIENTE',
    })

    await loadData()
    resetForm()
  } catch (requestError) {
    const responseData = requestError?.response?.data
    fieldErrors.value = responseData?.fieldErrors ?? {}
    formError.value = responseData?.message || 'No se pudo enviar la propuesta.'
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadData()
})
</script>

<template>
  <AppShell
    title="Mis propuestas"
    eyebrow="Profesorado"
    subtitle="Crea propuestas de evento que quedarán en estado pendiente hasta que administración las revise."
  >
    <section class="panel propuestas-layout">
      <div class="propuestas-card">
        <div class="propuestas-card__header">
          <div>
            <p class="eyebrow">Nueva propuesta</p>
            <h2>Proponer evento</h2>
          </div>
        </div>

        <p v-if="formError" class="propuestas-card__error" role="alert">{{ formError }}</p>
        <p v-if="loading" class="muted">Cargando datos...</p>

        <form v-else class="propuestas-form" @submit.prevent="handleSubmit">
          <label class="propuestas-form__field">
            <span>Tipo *</span>
            <select v-model="form.tipoId" class="select" required>
              <option v-for="tipo in tipos" :key="tipo.id" :value="String(tipo.id)">{{ tipo.nombre }}</option>
            </select>
          </label>

          <label class="propuestas-form__field propuestas-form__field--full">
            <span>Título *</span>
            <input v-model="form.titulo" class="input" type="text" maxlength="100" required />
            <small v-if="fieldErrors.titulo" class="propuestas-form__field-error">{{ fieldErrors.titulo }}</small>
          </label>

          <label class="propuestas-form__field propuestas-form__field--full">
            <span>Descripción</span>
            <textarea v-model="form.descripcion" class="textarea" rows="3" maxlength="500"></textarea>
          </label>

          <label class="propuestas-form__field">
            <span>Fecha inicio *</span>
            <input v-model="form.fechaInicio" class="input" type="datetime-local" step="1" required />
          </label>

          <label class="propuestas-form__field">
            <span>Fecha fin</span>
            <input v-model="form.fechaFin" class="input" type="datetime-local" step="1" />
          </label>

          <label class="propuestas-form__field">
            <span>Lugar *</span>
            <input v-model="form.lugar" class="input" type="text" required />
          </label>

          <label class="propuestas-form__field">
            <span>Número asistentes</span>
            <input v-model="form.numAsistentes" class="input" type="number" min="1" step="1" />
          </label>

          <label class="propuestas-form__field propuestas-form__field--full">
            <span>Grupos afectados *</span>
            <input v-model="form.gruposAfectados" class="input" type="text" required />
          </label>

          <label class="propuestas-form__field propuestas-form__field--full">
            <span>Enlace documento</span>
            <input v-model="form.enlaceDocumento" class="input" type="url" placeholder="https://..." />
          </label>

          <div class="propuestas-form__actions propuestas-form__field--full">
            <button class="btn btn--primary" type="submit" :disabled="submitting">
              {{ submitting ? 'Enviando...' : 'Enviar propuesta' }}
            </button>
          </div>
        </form>
      </div>

      <div class="propuestas-card">
        <div class="propuestas-card__header">
          <div>
            <p class="eyebrow">Mis registros</p>
            <h2>Propuestas enviadas</h2>
          </div>
        </div>

        <div v-if="misPropuestas.length === 0" class="propuestas-empty">
          <p class="muted">Aún no has enviado ninguna propuesta.</p>
        </div>

        <div v-else class="propuestas-list">
          <article v-for="evento in misPropuestas" :key="evento.id" class="propuesta-item" :style="itemStyle(evento)">
            <div class="propuesta-item__main">
              <div class="propuesta-item__top">
                <h3 class="propuesta-item__title">{{ evento.titulo }}</h3>
                <span class="propuesta-item__time">{{ formatTime(evento.fechaInicioDate) }}</span>
              </div>

              <p class="propuesta-item__meta">{{ evento.tipoNombre }}</p>
              <p class="propuesta-item__meta">{{ evento.lugar }}</p>
            </div>

            <span class="propuesta-item__status" :class="statusClass(evento.estado)">
              {{ statusLabel(evento.estado) }}
            </span>
          </article>
        </div>
      </div>
    </section>
  </AppShell>
</template>

<style scoped>
.propuestas-layout {
  padding: 28px;
  display: grid;
  gap: 20px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: 12px;
}

.propuestas-card {
  display: grid;
  gap: 16px;
  padding: 20px;
  background: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 12px;
}

.propuestas-card__header h2 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
}

.propuestas-card__header p {
  margin: 0 0 6px;
}

.propuestas-card__error {
  margin: 0;
  padding: 12px 14px;
  border-radius: 8px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
}

.propuestas-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.propuestas-form__field {
  display: grid;
  gap: 8px;
}

.propuestas-form__field span {
  color: var(--text);
  font-size: 0.9rem;
  font-weight: 500;
}

.propuestas-form__field--full {
  grid-column: 1 / -1;
}

.propuestas-form__field-error {
  color: #dc2626;
  font-size: 0.85rem;
}

.propuestas-form__actions {
  display: flex;
  justify-content: flex-end;
}

.propuestas-empty {
  padding: 18px;
  border-radius: 12px;
  border: 1px dashed var(--border);
  display: grid;
  place-items: center;
}

.propuestas-empty p {
  margin: 0;
}

.propuestas-list {
  display: grid;
  gap: 12px;
}

.propuesta-item {
  padding: 14px 16px;
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 14px;
  border-radius: 12px;
  border: 1px solid var(--border);
  border-left: 4px solid var(--primary);
  background: var(--bg-elevated);
}

.propuesta-item__main {
  flex: 1;
  display: grid;
  gap: 4px;
}

.propuesta-item__top {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.propuesta-item__title {
  margin: 0;
  font-size: 0.98rem;
  font-weight: 600;
}

.propuesta-item__time {
  color: var(--primary);
  font-size: 0.85rem;
  font-weight: 600;
  white-space: nowrap;
}

.propuesta-item__meta {
  margin: 0;
  color: var(--muted);
  font-size: 0.86rem;
}

.propuesta-item__status {
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
}

.propuesta-item__status--warning {
  color: #92400e;
  border-color: #fcd34d;
  background: #fef3c7;
}

.propuesta-item__status--success {
  color: #166534;
  border-color: #bbf7d0;
  background: #dcfce7;
}

.propuesta-item__status--danger {
  color: #991b1b;
  border-color: #fecaca;
  background: #fee2e2;
}

.propuesta-item__status--neutral {
  color: #4b5563;
  border-color: #e6f0eb;
  background: var(--bg-soft);
}

@media (max-width: 980px) {
  .propuestas-layout,
  .propuestas-form {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 820px) {
  .propuestas-layout {
    padding: 20px;
  }

  .propuesta-item,
  .propuesta-item__top {
    flex-direction: column;
  }

  .propuesta-item__status {
    align-self: flex-start;
  }
}
</style>