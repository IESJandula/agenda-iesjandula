<script setup>
import { computed, onMounted, ref } from 'vue'
import { CalendarDays, ChevronLeft, ChevronRight, X } from 'lucide-vue-next'

import AppShell from '../components/layout/AppShell.vue'
import { getEventos } from '../api/eventos.api'
import { getTipos } from '../api/tipos.api'

const weekdays = ['L', 'M', 'X', 'J', 'V', 'S', 'D']
const defaultEventColor = '#61d6a7'

const loading = ref(true)
const error = ref('')
const eventos = ref([])
const tipos = ref([])
const currentCourseStartYear = ref(getCourseStartYear(new Date()))
const selectedDayKey = ref('')

function normalizeCollection(response) {
  if (Array.isArray(response)) {
    return response
  }

  return response?.content ?? response?.data ?? []
}

function pad(value) {
  return String(value).padStart(2, '0')
}

function getCourseStartYear(date) {
  return date.getMonth() >= 8 ? date.getFullYear() : date.getFullYear() - 1
}

function getCourseMonths(startYear) {
  return Array.from({ length: 10 }, (_, index) => new Date(startYear, 8 + index, 1))
}

function startOfMonth(date) {
  return new Date(date.getFullYear(), date.getMonth(), 1)
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

function hexToRgba(color, alpha) {
  const normalizedColor = normalizeHexColor(color)

  if (!normalizedColor) {
    return ''
  }

  const hex = normalizedColor.slice(1)
  const red = Number.parseInt(hex.slice(0, 2), 16)
  const green = Number.parseInt(hex.slice(2, 4), 16)
  const blue = Number.parseInt(hex.slice(4, 6), 16)

  return `rgba(${red}, ${green}, ${blue}, ${alpha})`
}

function normalizeTipo(tipo) {
  return {
    id: tipo.id,
    nombre: tipo.nombre ?? '-',
    color: normalizeHexColor(tipo.color) || defaultEventColor,
    prioridad: Number.isFinite(Number(tipo.prioridad)) ? Number(tipo.prioridad) : null,
  }
}

function normalizeEvento(evento) {
  return {
    id: evento.id ?? evento.eventoId,
    titulo: evento.titulo ?? evento.nombre ?? evento.title ?? 'Sin título',
    tipoId: evento.tipoId ?? evento.tipo?.id ?? null,
    tipoNombre: evento.tipoNombre ?? evento.tipo?.nombre ?? evento.tipo?.descripcion ?? evento.tipo ?? '-',
    tipoColor: normalizeHexColor(evento.tipoColor) || '',
    tipoPrioridad: Number.isFinite(Number(evento.tipoPrioridad)) ? Number(evento.tipoPrioridad) : null,
    fechaInicio: evento.fechaInicio ?? evento.fecha_inicio ?? evento.startDate ?? '',
    lugar: evento.lugar ?? evento.ubicacion ?? evento.location ?? '-',
    estado: evento.estado ?? evento.estadoEvento ?? '-',
  }
}

function sortEventosByPriority(a, b) {
  const priorityA = a.tipoPrioridad ?? Number.MAX_SAFE_INTEGER
  const priorityB = b.tipoPrioridad ?? Number.MAX_SAFE_INTEGER

  if (priorityA !== priorityB) {
    return priorityA - priorityB
  }

  const timeA = a.fechaInicioDate?.getTime() ?? 0
  const timeB = b.fechaInicioDate?.getTime() ?? 0

  if (timeA !== timeB) {
    return timeA - timeB
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

const eventosEnriquecidos = computed(() =>
  eventos.value
    .map((evento) => {
      const fechaInicioDate = parseDate(evento.fechaInicio)
      const tipoMetadata = tiposById.value.get(Number(evento.tipoId))

      return {
        ...evento,
        fechaInicioDate,
        dayKey: fechaInicioDate ? formatDayKey(fechaInicioDate) : '',
        tipoNombre: evento.tipoNombre || tipoMetadata?.nombre || '-',
        tipoColorResolved: evento.tipoColor || tipoMetadata?.color || defaultEventColor,
        tipoPrioridad: evento.tipoPrioridad ?? tipoMetadata?.prioridad ?? null,
      }
    })
    .filter((evento) => Boolean(evento.fechaInicioDate)),
)

const eventosPorDia = computed(() => {
  const grouped = new Map()

  for (const evento of eventosEnriquecidos.value) {
    if (!grouped.has(evento.dayKey)) {
      grouped.set(evento.dayKey, [])
    }

    grouped.get(evento.dayKey).push(evento)
  }

  for (const items of grouped.values()) {
    items.sort(sortEventosByPriority)
  }

  return grouped
})

const courseMonths = computed(() => getCourseMonths(currentCourseStartYear.value))

const courseLabel = computed(() => {
  const startYear = currentCourseStartYear.value
  const endYear = startYear + 1
  return `${startYear}/${String(endYear).slice(-2)}`
})

const selectedDay = computed(() => {
  if (!selectedDayKey.value) {
    return null
  }

  for (const month of courseMonths.value) {
    const days = buildMonthDays(month)
    const found = days.find((day) => day.key === selectedDayKey.value)

    if (found) {
      return found
    }
  }

  return null
})

const selectedDayEvents = computed(() => selectedDay.value?.events ?? [])

const selectedDayLabel = computed(() => {
  if (!selectedDay.value) {
    return ''
  }

  return new Intl.DateTimeFormat('es-ES', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(selectedDay.value.date)
})

function buildMonthDays(monthDate) {
  const firstDay = startOfMonth(monthDate)
  const monthIndex = firstDay.getMonth()
  const monthStartOffset = (firstDay.getDay() + 6) % 7
  const calendarStart = addDays(firstDay, -monthStartOffset)
  const todayKey = formatDayKey(new Date())
  const days = []

  for (let index = 0; index < 42; index += 1) {
    const date = addDays(calendarStart, index)
    const key = formatDayKey(date)
    const dayEvents = [...(eventosPorDia.value.get(key) ?? [])].sort(sortEventosByPriority)

    days.push({
      date,
      key,
      isCurrentMonth: date.getMonth() === monthIndex,
      isToday: key === todayKey,
      events: dayEvents,
      visibleIndicators: dayEvents.slice(0, 3),
      hiddenCount: Math.max(0, dayEvents.length - 3),
    })
  }

  return days
}

function monthLabel(monthDate) {
  return new Intl.DateTimeFormat('es-ES', {
    month: 'long',
  }).format(monthDate)
}

function eventStyle(evento) {
  const color = normalizeHexColor(evento.tipoColorResolved) || defaultEventColor

  return {
    backgroundColor: hexToRgba(color, 0.18),
    borderColor: hexToRgba(color, 0.35),
    color,
  }
}

function dayIndicatorStyle(evento) {
  const color = normalizeHexColor(evento.tipoColorResolved) || defaultEventColor

  return {
    backgroundColor: color,
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

function formatShortDate(value) {
  const parsed = value instanceof Date ? value : parseDate(value)

  if (!parsed) {
    return '-'
  }

  return new Intl.DateTimeFormat('es-ES', {
    day: '2-digit',
    month: '2-digit',
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
    return 'curso-item__status--warning'
  }

  if (normalized === 'CONFIRMADO') {
    return 'curso-item__status--success'
  }

  if (normalized === 'RECHAZADO') {
    return 'curso-item__status--danger'
  }

  if (normalized === 'CANCELADO') {
    return 'curso-item__status--danger'
  }

  return 'curso-item__status--neutral'
}

function openDay(dayKey) {
  selectedDayKey.value = dayKey
}

function closeModal() {
  selectedDayKey.value = ''
}

async function loadCourseCalendar() {
  loading.value = true
  error.value = ''

  try {
    const [eventosResult, tiposResult] = await Promise.allSettled([getEventos(), getTipos()])

    if (eventosResult.status === 'fulfilled') {
      eventos.value = normalizeCollection(eventosResult.value).map(normalizeEvento)
    } else {
      eventos.value = []
      error.value = eventosResult.reason?.response?.data?.message || 'No se pudieron cargar los eventos del curso.'
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

onMounted(loadCourseCalendar)
</script>

<template>
  <AppShell
    title="Curso completo"
    eyebrow="Vista anual"
    subtitle="Parrilla compacta de septiembre a junio con mini calendarios y detalle diario de eventos."
  >
    <section class="panel curso-completo">
      <header class="curso-completo__header">
        <div>
          <p class="eyebrow">Curso académico</p>
          <h2>{{ courseLabel }}</h2>
          <p class="muted">Septiembre a junio. Haz clic en un día para ver sus eventos.</p>
        </div>

        <div class="curso-completo__legend">
          <span class="curso-completo__legend-item"><span class="curso-completo__legend-dot"></span>Evento</span>
          <span class="curso-completo__legend-item"><span class="curso-completo__legend-today"></span>Hoy</span>
        </div>
      </header>

      <p v-if="error" class="curso-completo__error" role="alert">{{ error }}</p>
      <p v-if="loading" class="muted">Cargando curso completo...</p>

      <div v-else class="curso-grid">
        <article v-for="month in courseMonths" :key="`${month.getFullYear()}-${month.getMonth()}`" class="curso-month">
          <header class="curso-month__header">
            <h3>{{ monthLabel(month) }}</h3>
          </header>

          <div class="curso-month__weekdays" aria-hidden="true">
            <span v-for="weekday in weekdays" :key="weekday">{{ weekday }}</span>
          </div>

          <div class="curso-month__grid">
            <template v-for="day in buildMonthDays(month)" :key="day.key">
              <button
                type="button"
                class="curso-day"
                :class="{
                  'curso-day--muted': !day.isCurrentMonth,
                  'curso-day--today': day.isToday,
                  'curso-day--has-events': day.events.length > 0,
                }"
                :aria-label="`${day.date.getDate()} ${monthLabel(month)} ${day.events.length} eventos`"
                @click="openDay(day.key)"
              >
                <span class="curso-day__number">{{ day.date.getDate() }}</span>

                <span class="curso-day__indicators">
                  <span
                    v-for="evento in day.visibleIndicators"
                    :key="evento.id"
                    class="curso-day__indicator"
                    :style="dayIndicatorStyle(evento)"
                  ></span>
                </span>

                <span v-if="day.hiddenCount > 0" class="curso-day__more">+{{ day.hiddenCount }}</span>
              </button>
            </template>
          </div>
        </article>
      </div>

      <div v-if="!loading && eventosEnriquecidos.length === 0" class="curso-empty">
        <CalendarDays :size="20" />
        <p>No hay eventos para mostrar en el curso actual.</p>
      </div>
    </section>

    <Teleport to="body">
      <div v-if="selectedDay" class="curso-modal" @click.self="closeModal">
        <div class="curso-modal__dialog" role="dialog" aria-modal="true" :aria-label="selectedDayLabel">
          <header class="curso-modal__header">
            <div>
              <p class="eyebrow">Eventos del día</p>
              <h3>{{ selectedDayLabel }}</h3>
            </div>

            <button class="curso-modal__close" type="button" @click="closeModal" aria-label="Cerrar">
              <X :size="18" />
            </button>
          </header>

          <div v-if="selectedDayEvents.length > 0" class="curso-modal__list">
            <article v-for="evento in selectedDayEvents" :key="evento.id" class="curso-item" :style="eventStyle(evento)">
              <div class="curso-item__main">
                <div class="curso-item__top">
                  <h4 class="curso-item__title">{{ evento.titulo }}</h4>
                  <span class="curso-item__time">{{ formatTime(evento.fechaInicioDate) }}</span>
                </div>

                <p class="curso-item__meta">{{ evento.tipoNombre }}</p>
                <p class="curso-item__meta">{{ evento.lugar }}</p>
                <p class="curso-item__meta">{{ formatShortDate(evento.fechaInicioDate) }}</p>
              </div>

              <span class="curso-item__status" :class="statusClass(evento.estado)">
                {{ statusLabel(evento.estado) }}
              </span>
            </article>
          </div>

          <p v-else class="curso-modal__empty muted">No hay eventos para este día.</p>
        </div>
      </div>
    </Teleport>
  </AppShell>
</template>

<style scoped>
.curso-completo {
  width: min(100%, 1440px);
  margin: 0 auto;
  padding: 28px;
  display: grid;
  gap: 20px;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow-x: hidden;
}

.curso-completo__header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 20px;
}

.curso-completo__header h2,
.curso-month__header h3 {
  margin: 0 0 6px;
  font-weight: 600;
}

.curso-completo__header h2 {
  font-size: 1.3rem;
}

.curso-completo__header p {
  margin: 0;
}

.curso-completo__legend {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.curso-completo__legend-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--muted);
  font-size: 0.8rem;
  font-weight: 600;
}

.curso-completo__legend-dot,
.curso-completo__legend-today {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: #61d6a7;
}

.curso-completo__legend-today {
  background: var(--primary);
}

.curso-completo__error {
  margin: 0;
  padding: 12px 14px;
  border-radius: 8px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
}

.curso-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
  align-items: start;
}

.curso-month {
  min-width: 0;
  width: 100%;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: var(--bg-soft);
  display: grid;
  gap: 12px;
}

.curso-month__header h3 {
  font-size: 1rem;
  text-transform: capitalize;
}

.curso-month__weekdays,
.curso-month__grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 4px;
}

.curso-month__weekdays span {
  color: var(--muted);
  font-size: 0.72rem;
  font-weight: 600;
  text-align: center;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.curso-day {
  width: 100%;
  min-width: 0;
  min-height: 60px;
  padding: 6px 4px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--bg-elevated);
  color: var(--text);
  display: grid;
  gap: 6px;
  justify-items: center;
  align-content: start;
  cursor: pointer;
  transition: all 0.2s ease;
}

.curso-day:hover {
  transform: translateY(-1px);
  border-color: var(--primary);
  box-shadow: 0 8px 18px rgba(97, 214, 167, 0.1);
}

.curso-day:disabled {
  cursor: default;
}

.curso-day--muted {
  opacity: 0.5;
}

.curso-day--today {
  border-color: var(--primary);
  box-shadow: inset 0 0 0 1px rgba(97, 214, 167, 0.2);
}

.curso-day--has-events {
  background: linear-gradient(180deg, rgba(97, 214, 167, 0.08), var(--bg-elevated));
}

.curso-day__number {
  width: 1.65rem;
  height: 1.65rem;
  border-radius: 999px;
  display: grid;
  place-items: center;
  font-size: 0.78rem;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.35);
}

.curso-day--today .curso-day__number {
  background: var(--primary);
  color: #ffffff;
}

.curso-day__indicators {
  min-height: 10px;
  display: flex;
  gap: 3px;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
}

.curso-day__indicator {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.35);
}

.curso-day__more {
  font-size: 0.64rem;
  font-weight: 600;
  color: var(--primary);
}

.curso-empty {
  min-height: 180px;
  border: 1px dashed var(--border);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-direction: column;
  color: var(--muted);
  text-align: center;
}
.curso-empty p {
  margin: 0;
}

.curso-modal {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(17, 24, 39, 0.45);
  padding: 16px;
  z-index: 50;
}

.curso-modal__dialog {
  width: min(760px, calc(100vw - 32px));
  max-width: 760px;
  max-height: min(80vh, calc(100vh - 32px));
  overflow: auto;
  padding: 24px;
  border-radius: 16px;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  box-shadow: var(--shadow);
  display: grid;
  gap: 18px;
}

.curso-modal__header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
}

.curso-modal__header h3 {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 600;
}

.curso-modal__close {
  width: 38px;
  height: 38px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-soft);
  color: var(--text);
  display: grid;
  place-items: center;
  cursor: pointer;
}

.curso-modal__list {
  display: grid;
  gap: 12px;
}

.curso-item {
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

.curso-item__main {
  flex: 1;
  display: grid;
  gap: 4px;
  min-width: 0;
}

.curso-item__top {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.curso-item__title {
  margin: 0;
  font-size: 0.98rem;
  font-weight: 600;
}

.curso-item__time {
  color: var(--primary);
  font-size: 0.85rem;
  font-weight: 600;
  white-space: nowrap;
}

.curso-item__meta {
  margin: 0;
  color: var(--muted);
  font-size: 0.86rem;
}

.curso-item__status {
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
}

.curso-item__status--warning {
  color: #92400e;
  border-color: #fcd34d;
  background: #fef3c7;
}

.curso-item__status--success {
  color: #166534;
  border-color: #bbf7d0;
  background: #dcfce7;
}

.curso-item__status--danger {
  color: #991b1b;
  border-color: #fecaca;
  background: #fee2e2;
}

.curso-item__status--neutral {
  color: #4b5563;
  border-color: #e6f0eb;
  background: var(--bg-soft);
}

@media (max-width: 1100px) {
  .curso-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 820px) {
  .curso-completo {
    padding: 20px;
  }

  .curso-grid {
    gap: 16px;
  }

  .curso-completo__header,
  .curso-item,
  .curso-item__top,
  .curso-modal__header {
    flex-direction: column;
  }

  .curso-item__status {
    align-self: flex-start;
  }

  .curso-modal {
    padding: 16px;
  }

  .curso-modal__dialog {
    width: min(100%, calc(100vw - 32px));
    max-height: calc(100vh - 32px);
    padding: 18px;
  }
}
</style>