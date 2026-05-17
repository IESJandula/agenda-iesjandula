<script setup>
import { computed, onMounted, ref } from 'vue'

import AppShell from '../components/layout/AppShell.vue'
import { aprobarEvento, getEventosPendientes, rechazarEvento } from '../api/eventos.api'
import { getTipos } from '../api/tipos.api'

const loading = ref(false)
const submittingId = ref(null)
const formError = ref('')
const eventos = ref([])
const tipos = ref([])

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
                :disabled="submittingId === evento.id"
                @click="handleAction('rechazar', evento.id)"
              >
                Rechazar
              </button>
            </div>
          </div>
        </article>
      </div>
    </section>
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
}
</style>