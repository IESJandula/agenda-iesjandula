<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'

import AppShell from '../components/layout/AppShell.vue'
import { aprobarEvento, getEventosPendientes, rechazarEvento } from '../api/eventos.api'
import { getTipos } from '../api/tipos.api'

const loading = ref(false)
const submittingId = ref(null)
const rejectingId = ref(null)
const rejectForm = ref({ motivo: '' })
const rejectFieldError = ref('')
const rejectFormError = ref('')
const formError = ref('')
const eventos = ref([])
const tipos = ref([])
const rejectTextareaRef = ref(null)

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

function normalizeHexColor(color) {
  if (typeof color !== 'string') {
    return ''
  }

  const trimmed = color.trim()
  return /^#([0-9a-f]{6})$/i.test(trimmed) ? trimmed : ''
}

function normalizeTipo(tipo) {
  return {
    id: tipo.id,
    nombre: tipo.nombre ?? '-',
    color: normalizeHexColor(tipo.color) || '#61d6a7',
    prioridad: Number.isFinite(Number(tipo.prioridad)) ? Number(tipo.prioridad) : null,
  }
}

function sortEventos(a, b) {
  const timeA = a.fechaInicioDate?.getTime() ?? 0
  const timeB = b.fechaInicioDate?.getTime() ?? 0

  if (timeA !== timeB) {
    return timeA - timeB
  }

  const priorityA = a.tipoPrioridad ?? Number.MAX_SAFE_INTEGER
  const priorityB = b.tipoPrioridad ?? Number.MAX_SAFE_INTEGER

  if (priorityA !== priorityB) {
    return priorityA - priorityB
  }

  return a.titulo.localeCompare(b.titulo, 'es', { sensitivity: 'base' })
}

const tiposById = computed(() => {
  const map = new Map()

  for (const tipo of tipos.value) {
    const normalizedTipo = normalizeTipo(tipo)
    map.set(Number(normalizedTipo.id), normalizedTipo)
  }

  return map
})

const pendingProposals = computed(() =>
  eventos.value
    .map((evento) => {
      const fechaInicioDate = parseDate(evento.fechaInicio)
      const tipoMetadata = tiposById.value.get(Number(evento.tipoId))

      return {
        ...evento,
        fechaInicioDate,
        tipoNombre: evento.tipoNombre || tipoMetadata?.nombre || '-',
        tipoColorResolved: normalizeHexColor(evento.tipoColor) || tipoMetadata?.color || '#61d6a7',
        tipoPrioridad: tipoMetadata?.prioridad ?? null,
      }
    })
    .sort(sortEventos),
)

function statusLabel(status) {
  const normalized = String(status || '').toUpperCase()

  if (normalized === 'PENDIENTE') return 'Pendiente'
  if (normalized === 'CONFIRMADO') return 'Confirmado'
  if (normalized === 'RECHAZADO') return 'Rechazado'
  if (normalized === 'CANCELADO') return 'Cancelado'
  return normalized || 'Sin estado'
}

function statusClass(status) {
  const normalized = String(status || '').toUpperCase()

  if (normalized === 'PENDIENTE') {
    return 'admin-proposals__status--warning'
  }

  if (normalized === 'CONFIRMADO') {
    return 'admin-proposals__status--success'
  }

  if (normalized === 'RECHAZADO') {
    return 'admin-proposals__status--danger'
  }

  if (normalized === 'CANCELADO') {
    return 'admin-proposals__status--danger'
  }

  return 'admin-proposals__status--neutral'
}

function itemStyle(evento) {
  return {
    borderLeftColor: evento.tipoColorResolved,
  }
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

async function loadData() {
  loading.value = true
  formError.value = ''

  try {
    const [eventosResponse, tiposResponse] = await Promise.all([getEventosPendientes(), getTipos()])
    eventos.value = normalizeCollection(eventosResponse)
    tipos.value = normalizeCollection(tiposResponse)
  } catch (requestError) {
    formError.value = requestError?.response?.data?.message || 'No se pudieron cargar las propuestas pendientes.'
    eventos.value = []
  } finally {
    loading.value = false
  }
}

async function handleAction(action, id) {
  submittingId.value = id
  formError.value = ''

  try {
    if (action === 'aprobar') {
      await aprobarEvento(id)
    } else {
      await rechazarEvento(id)
    }

    await loadData()
  } catch (requestError) {
    formError.value = requestError?.response?.data?.message || 'No se pudo procesar la propuesta.'
  } finally {
    submittingId.value = null
  }
}

function openRejectModal(id) {
  rejectingId.value = id
  rejectForm.value = { motivo: '' }
  rejectFieldError.value = ''
  rejectFormError.value = ''

  nextTick(() => {
    rejectTextareaRef.value?.focus?.()
  })
}

function closeRejectModal() {
  rejectingId.value = null
  rejectForm.value = { motivo: '' }
  rejectFieldError.value = ''
  rejectFormError.value = ''
}

async function submitReject() {
  const motivo = rejectForm.value.motivo.trim()

  if (!motivo) {
    rejectFieldError.value = 'El motivo es obligatorio.'
    return
  }

  if (motivo.length < 5) {
    rejectFieldError.value = 'El motivo debe tener al menos 5 caracteres.'
    return
  }

  if (!rejectingId.value) {
    return
  }

  submittingId.value = rejectingId.value
  rejectFieldError.value = ''
  rejectFormError.value = ''

  try {
    await rechazarEvento(rejectingId.value, { motivo })
    closeRejectModal()
    await loadData()
  } catch (requestError) {
    rejectFormError.value = requestError?.response?.data?.message || 'No se pudo rechazar la propuesta.'
  } finally {
    submittingId.value = null
  }
}

function isRejecting(id) {
  return rejectingId.value === id
}

onMounted(loadData)
</script>

<template>
  <AppShell
    title="Propuestas pendientes"
    eyebrow="Administración"
    subtitle="Bandeja de revisión para aprobar o rechazar propuestas de profesorado."
  >
    <section class="panel admin-proposals">
      <header class="admin-proposals__header">
        <div>
          <p class="eyebrow">Pendientes</p>
          <h2>Propuestas para revisar</h2>
        </div>
      </header>

      <p v-if="formError" class="admin-proposals__error" role="alert">{{ formError }}</p>
      <p v-if="loading" class="muted">Cargando propuestas...</p>

      <div v-else-if="pendingProposals.length === 0" class="admin-proposals__empty">
        <p class="muted">No hay propuestas pendientes en este momento.</p>
      </div>

      <div v-else class="admin-proposals__list">
        <article v-for="evento in pendingProposals" :key="evento.id" class="admin-proposal" :style="itemStyle(evento)">
          <div class="admin-proposal__main">
            <div class="admin-proposal__top">
              <h3 class="admin-proposal__title">{{ evento.titulo }}</h3>
              <span class="admin-proposal__time">{{ formatTime(evento.fechaInicioDate) }}</span>
            </div>

            <p class="admin-proposal__meta">{{ evento.tipoNombre }}</p>
            <p class="admin-proposal__meta">{{ evento.lugar }}</p>
            <p class="admin-proposal__meta">{{ evento.creadorNombre || 'Sin creador' }}</p>
            <p v-if="evento.motivoRechazo" class="admin-proposal__reason">
              Motivo rechazo: {{ evento.motivoRechazo }}
            </p>
          </div>

          <div class="admin-proposal__actions">
            <span class="admin-proposal__status" :class="statusClass(evento.estado)">
              {{ statusLabel(evento.estado) }}
            </span>
            <div class="admin-proposal__buttons">
              <button
                class="btn btn--primary"
                type="button"
                :disabled="submittingId === evento.id"
                @click="handleAction('aprobar', evento.id)"
              >
                Aprobar
              </button>
              <button
                class="btn btn--ghost"
                type="button"
                :disabled="submittingId === evento.id || isRejecting(evento.id)"
                @click="openRejectModal(evento.id)"
              >
                Rechazar
              </button>
            </div>
          </div>
        </article>
      </div>
    </section>

    <Teleport to="body">
      <div v-if="rejectingId" class="reject-modal__backdrop" @click.self="closeRejectModal">
        <div class="reject-modal panel" role="dialog" aria-modal="true" aria-labelledby="reject-modal-title">
          <header class="reject-modal__header">
            <div>
              <p class="eyebrow">Rechazo</p>
              <h3 id="reject-modal-title">Indica el motivo</h3>
            </div>
            <button class="reject-modal__close" type="button" @click="closeRejectModal">×</button>
          </header>

          <p v-if="rejectFormError" class="reject-modal__error" role="alert">{{ rejectFormError }}</p>

          <label class="reject-modal__field">
            <span>Motivo *</span>
            <textarea
              ref="rejectTextareaRef"
              v-model="rejectForm.motivo"
              class="textarea"
              rows="5"
              minlength="5"
              maxlength="500"
              placeholder="Explica por qué se rechaza la propuesta"
            />
            <small v-if="rejectFieldError" class="reject-modal__field-error">{{ rejectFieldError }}</small>
          </label>

          <footer class="reject-modal__actions">
            <button class="btn btn--ghost" type="button" @click="closeRejectModal">Cancelar</button>
            <button class="btn btn--primary" type="button" :disabled="submittingId === rejectingId" @click="submitReject">
              {{ submittingId === rejectingId ? 'Rechazando...' : 'Rechazar propuesta' }}
            </button>
          </footer>
        </div>
      </div>
    </Teleport>
  </AppShell>
</template>

<style scoped>
.admin-proposals {
  padding: 28px;
  display: grid;
  gap: 20px;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: 12px;
}

.admin-proposals__header h2 {
  margin: 0;
  font-size: 1.3rem;
  font-weight: 600;
}

.admin-proposals__header p {
  margin: 0 0 6px;
}

.admin-proposals__error {
  margin: 0;
  padding: 12px 14px;
  border-radius: 8px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
}

.admin-proposals__empty {
  min-height: 160px;
  display: grid;
  place-items: center;
  border: 1px dashed var(--border);
  border-radius: 12px;
}

.admin-proposals__empty p {
  margin: 0;
}

.admin-proposals__list {
  display: grid;
  gap: 12px;
}

.admin-proposal {
  padding: 14px 16px;
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 14px;
  border-radius: 12px;
  border: 1px solid var(--border);
  border-left: 4px solid var(--primary);
  background: var(--bg-soft);
}

.admin-proposal__main {
  flex: 1;
  display: grid;
  gap: 4px;
}

.admin-proposal__top {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.admin-proposal__title {
  margin: 0;
  font-size: 0.98rem;
  font-weight: 600;
}

.admin-proposal__time {
  color: var(--primary);
  font-size: 0.85rem;
  font-weight: 600;
  white-space: nowrap;
}

.admin-proposal__meta {
  margin: 0;
  color: var(--muted);
  font-size: 0.86rem;
}

.admin-proposal__reason {
  margin: 4px 0 0;
  color: #7c2d12;
  font-size: 0.85rem;
}

.admin-proposal__actions {
  display: grid;
  gap: 10px;
  justify-items: end;
}

.admin-proposal__buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.admin-proposal__status {
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
}

.admin-proposal__status--warning {
  color: #92400e;
  border-color: #fcd34d;
  background: #fef3c7;
}

.admin-proposal__status--success {
  color: #166534;
  border-color: #bbf7d0;
  background: #dcfce7;
}

.admin-proposal__status--danger {
  color: #991b1b;
  border-color: #fecaca;
  background: #fee2e2;
}

.admin-proposal__status--neutral {
  color: #4b5563;
  border-color: #e6f0eb;
  background: var(--bg-soft);
}

.reject-modal__backdrop {
  position: fixed;
  inset: 0;
  z-index: 50;
  background: rgba(15, 23, 42, 0.55);
  display: grid;
  place-items: center;
  padding: 20px;
}

.reject-modal {
  width: min(720px, 100%);
  padding: 22px;
  display: grid;
  gap: 16px;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: 16px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.28);
}

.reject-modal__header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.reject-modal__header h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
}

.reject-modal__close {
  width: 36px;
  height: 36px;
  border: 1px solid var(--border);
  background: var(--bg-soft);
  color: var(--text);
  border-radius: 10px;
  font-size: 1.2rem;
  line-height: 1;
  cursor: pointer;
}

.reject-modal__error {
  margin: 0;
  padding: 12px 14px;
  border-radius: 10px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
}

.reject-modal__field {
  display: grid;
  gap: 8px;
}

.reject-modal__field span {
  color: var(--text);
  font-weight: 500;
}

.reject-modal__field-error {
  color: #dc2626;
  font-size: 0.85rem;
}

.reject-modal__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 860px) {
  .admin-proposals {
    padding: 20px;
  }

  .admin-proposal,
  .admin-proposal__top {
    flex-direction: column;
  }

  .admin-proposal__actions {
    justify-items: start;
  }

  .reject-modal__actions {
    flex-direction: column;
  }

  .reject-modal__actions .btn {
    width: 100%;
  }
}
</style>