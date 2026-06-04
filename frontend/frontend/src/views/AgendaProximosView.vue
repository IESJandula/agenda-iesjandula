<script setup>
import { computed, onMounted, ref } from 'vue'

import AppShell from '../components/layout/AppShell.vue'
import { getEventos } from '../api/eventos.api'
import { getTipos } from '../api/tipos.api'

const loading = ref(true)
const error = ref('')
const eventos = ref([])
const tipos = ref([])
const today = startOfDay(new Date())
const rangeEnd = endOfDay(addDays(today, 7))

function normalizeCollection(response) {
  if (Array.isArray(response)) {
    return response
  }

  return response?.content ?? response?.data ?? []
}

function pad(value) {
  return String(value).padStart(2, '0')
}

function startOfDay(date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

function endOfDay(date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate(), 23, 59, 59, 999)
}

function addDays(date, amount) {
  const nextDate = new Date(date)
  nextDate.setDate(nextDate.getDate() + amount)
  return nextDate
}

function parseDate(value) {
  if (!value) {
    return null
  }

  const parsed = new Date(String(value).replace(' ', 'T'))
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

function formatDayKey(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function normalizeHexColor(color) {
  if (typeof color !== 'string') {
    return ''
  }

  const trimmedColor = color.trim()
  return /^#([0-9a-f]{6})$/i.test(trimmedColor) ? trimmedColor : ''
}

function normalizeTipo(tipo) {
  return {
    id: tipo.id,
    nombre: tipo.nombre ?? '-',
    color: normalizeHexColor(tipo.color) || '#61d6a7',
    prioridad: Number.isFinite(Number(tipo.prioridad)) ? Number(tipo.prioridad) : null,
  }
}

function normalizeEvento(evento) {
  const fechaInicioDate = parseDate(evento.fechaInicio ?? evento.fecha_inicio ?? evento.startDate ?? '')

  return {
    id: evento.id ?? evento.eventoId,
    titulo: evento.titulo ?? evento.nombre ?? evento.title ?? 'Sin título',
    tipoId: evento.tipoId ?? evento.tipo?.id ?? null,
    tipoNombre: evento.tipoNombre ?? evento.tipo?.nombre ?? evento.tipo?.descripcion ?? evento.tipo ?? '-',
    tipoColor: normalizeHexColor(evento.tipoColor) || '',
    tipoPrioridad: Number.isFinite(Number(evento.tipoPrioridad)) ? Number(evento.tipoPrioridad) : null,
    fechaInicio: evento.fechaInicio ?? evento.fecha_inicio ?? evento.startDate ?? '',
    fechaInicioDate,
    lugar: evento.lugar ?? evento.ubicacion ?? evento.location ?? '-',
    estado: evento.estado ?? evento.estadoEvento ?? '-',
  }
}

const tiposById = computed(() => {
  const map = new Map()

  for (const tipo of tipos.value) {
    const normalizedTipo = normalizeTipo(tipo)
    map.set(Number(normalizedTipo.id), normalizedTipo)
  }

  return map
})

const eventosProximos = computed(() =>
  eventos.value
    .map((evento) => {
      const tipoMetadata = tiposById.value.get(Number(evento.tipoId))
      const fechaInicioDate = evento.fechaInicioDate

      return {
        ...evento,
        tipoNombre: evento.tipoNombre || tipoMetadata?.nombre || '-',
        tipoColorResolved: evento.tipoColor || tipoMetadata?.color || '#61d6a7',
        tipoPrioridad: evento.tipoPrioridad ?? tipoMetadata?.prioridad ?? null,
        fechaInicioDate,
        dayKey: fechaInicioDate ? formatDayKey(fechaInicioDate) : '',
      }
    })
    .filter((evento) => evento.fechaInicioDate && evento.fechaInicioDate >= today && evento.fechaInicioDate <= rangeEnd)
    .sort((a, b) => {
      const timeA = a.fechaInicioDate.getTime()
      const timeB = b.fechaInicioDate.getTime()

      if (timeA !== timeB) {
        return timeA - timeB
      }

      const priorityA = a.tipoPrioridad ?? Number.MAX_SAFE_INTEGER
      const priorityB = b.tipoPrioridad ?? Number.MAX_SAFE_INTEGER

      if (priorityA !== priorityB) {
        return priorityA - priorityB
      }

      return a.titulo.localeCompare(b.titulo, 'es', { sensitivity: 'base' })
    }),
)

const eventosPorDia = computed(() => {
  const grouped = new Map()

  for (const evento of eventosProximos.value) {
    if (!grouped.has(evento.dayKey)) {
      grouped.set(evento.dayKey, [])
    }

    grouped.get(evento.dayKey).push(evento)
  }

  return [...grouped.entries()].map(([dayKey, items]) => {
    const referenceDate = items[0]?.fechaInicioDate ?? today

    return {
      dayKey,
      date: referenceDate,
      items,
    }
  })
})

const hasEventos = computed(() => eventosPorDia.value.length > 0)

function formatDate(value) {
  const parsed = value instanceof Date ? value : parseDate(value)

  if (!parsed) {
    return '-'
  }

  return new Intl.DateTimeFormat('es-ES', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  }).format(parsed)
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

function statusLabel(status) {
  const normalized = String(status || '').toUpperCase()

  if (normalized === 'PENDIENTE') {
    return 'Pendiente'
  }

  if (normalized === 'CONFIRMADO') {
    return 'Confirmado'
  }

  if (normalized === 'RECHAZADO') {
    return 'Rechazado'
  }

  if (normalized === 'CANCELADO') {
    return 'Cancelado'
  }

  return normalized || 'Sin estado'
}

function statusClass(status) {
  const normalized = String(status || '').toUpperCase()

  if (normalized === 'PENDIENTE') {
    return 'agenda-item__status--warning'
  }

  if (normalized === 'CONFIRMADO') {
    return 'agenda-item__status--success'
  }

  if (normalized === 'RECHAZADO') {
    return 'agenda-item__status--danger'
  }

  if (normalized === 'CANCELADO') {
    return 'agenda-item__status--danger'
  }

  return 'agenda-item__status--neutral'
}

function itemStyle(evento) {
  return {
    borderLeftColor: evento.tipoColorResolved,
  }
}

async function loadAgenda() {
  loading.value = true
  error.value = ''

  try {
    const [eventosResult, tiposResult] = await Promise.allSettled([getEventos(), getTipos()])

    if (eventosResult.status === 'fulfilled') {
      eventos.value = normalizeCollection(eventosResult.value).map(normalizeEvento)
    } else {
      eventos.value = []
      error.value = eventosResult.reason?.response?.data?.message || 'No se pudieron cargar los eventos próximos.'
    }

    if (tiposResult.status === 'fulfilled') {
      tipos.value = normalizeCollection(tiposResult.value)
    } else {
      tipos.value = []
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadAgenda)
</script>

<template>
  <AppShell
    title="Agenda de próximos eventos"
    eyebrow="Vista semanal"
    subtitle="Eventos de hoy y los próximos 7 días, agrupados por día para una lectura rápida."
  >
    <section class="panel agenda-proximos">
      <header class="agenda-proximos__header">
        <div>
          <p class="eyebrow">Próximos 8 días</p>
          <h2>Agenda</h2>
          <p class="muted">Ordenada por fecha y prioridad del tipo.</p>
        </div>
      </header>

      <p v-if="error" class="agenda-proximos__error" role="alert">{{ error }}</p>
      <p v-if="loading" class="muted">Cargando agenda...</p>

      <div v-else-if="!hasEventos" class="agenda-empty">
        <CalendarDays :size="20" />
        <p>No hay eventos próximos en los siguientes 7 días.</p>
      </div>

      <div v-else class="agenda-list">
        <article v-for="day in eventosPorDia" :key="day.dayKey" class="agenda-day">
          <header class="agenda-day__header">
            <div>
              <p class="agenda-day__weekday">{{ formatDate(day.date).split(',')[0] }}</p>
              <h3 class="agenda-day__date">{{ formatDate(day.date) }}</h3>
            </div>
            <span class="agenda-day__count">{{ day.items.length }} evento{{ day.items.length === 1 ? '' : 's' }}</span>
          </header>

          <div class="agenda-day__items">
            <article
              v-for="evento in day.items"
              :key="evento.id"
              class="agenda-item"
              :style="itemStyle(evento)"
            >
              <div class="agenda-item__main">
                <div class="agenda-item__top">
                  <h4 class="agenda-item__title">{{ evento.titulo }}</h4>
                  <span class="agenda-item__time">{{ formatTime(evento.fechaInicioDate) }}</span>
                </div>

                <p class="agenda-item__meta">{{ evento.tipoNombre }}</p>
                <p class="agenda-item__meta">{{ evento.lugar }}</p>
              </div>

              <span class="agenda-item__status" :class="statusClass(evento.estado)">
                {{ statusLabel(evento.estado) }}
              </span>
            </article>
          </div>
        </article>
      </div>
    </section>
  </AppShell>
</template>

<style scoped>
.agenda-proximos {
  padding: 28px;
  display: grid;
  gap: 20px;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: 12px;
}

.agenda-proximos__header h2 {
  margin: 0 0 6px;
  font-size: 1.3rem;
  font-weight: 600;
}

.agenda-proximos__header p {
  margin: 0;
}

.agenda-proximos__error {
  margin: 0;
  padding: 12px 14px;
  border-radius: 8px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
}

.agenda-empty {
  min-height: 220px;
  display: grid;
  place-items: center;
  gap: 12px;
  padding: 24px;
  border-radius: 12px;
  border: 1px dashed var(--border);
  color: var(--muted);
  text-align: center;
}

.agenda-empty p {
  margin: 0;
}

.agenda-list {
  display: grid;
  gap: 18px;
}

.agenda-day {
  padding: 20px;
  display: grid;
  gap: 14px;
  background: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 12px;
}

.agenda-day__header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.agenda-day__weekday {
  margin: 0 0 4px;
  color: var(--muted);
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.agenda-day__date {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 600;
}

.agenda-day__count {
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  color: var(--primary);
  font-size: 0.8rem;
  font-weight: 600;
  white-space: nowrap;
}

.agenda-day__items {
  display: grid;
  gap: 12px;
}

.agenda-item {
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

.agenda-item__main {
  flex: 1;
  display: grid;
  gap: 4px;
  min-width: 0;
}

.agenda-item__top {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.agenda-item__title {
  margin: 0;
  font-size: 0.98rem;
  font-weight: 600;
}

.agenda-item__time {
  color: var(--primary);
  font-size: 0.85rem;
  font-weight: 600;
  white-space: nowrap;
}

.agenda-item__meta {
  margin: 0;
  color: var(--muted);
  font-size: 0.86rem;
}

.agenda-item__status {
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
}

.agenda-item__status--warning {
  color: #92400e;
  border-color: #fcd34d;
  background: #fef3c7;
}

.agenda-item__status--success {
  color: #166534;
  border-color: #bbf7d0;
  background: #dcfce7;
}

.agenda-item__status--danger {
  color: #991b1b;
  border-color: #fecaca;
  background: #fee2e2;
}

.agenda-item__status--neutral {
  color: #4b5563;
  border-color: #e6f0eb;
  background: var(--bg-soft);
}

@media (max-width: 820px) {
  .agenda-proximos {
    padding: 20px;
  }

  .agenda-item,
  .agenda-day__header,
  .agenda-item__top {
    flex-direction: column;
  }

  .agenda-item__status {
    align-self: flex-start;
  }
}
</style>