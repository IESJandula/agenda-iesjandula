<script setup>
import { computed, onMounted, ref } from 'vue'
import { CalendarDays, ChevronLeft, ChevronRight, X } from 'lucide-vue-next'

import AppShell from '../components/layout/AppShell.vue'
import { getEventos } from '../api/eventos.api'
import { getTipos } from '../api/tipos.api'

const weekdays = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']
const defaultEventColor = '#61d6a7'

const loading = ref(true)
const error = ref('')
const eventos = ref([])
const tipos = ref([])
const currentMonth = ref(startOfMonth(new Date()))
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

function startOfMonth(date) {
  return new Date(date.getFullYear(), date.getMonth(), 1)
}

function addMonths(date, amount) {
  return new Date(date.getFullYear(), date.getMonth() + amount, 1)
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

function toDayKey(value) {
  const parsed = value instanceof Date ? value : parseDate(value)

  if (!parsed) {
    return ''
  }

  return formatDayKey(parsed)
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
        tipoPrioridad: tipoMetadata?.prioridad ?? null,
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

const monthLabel = computed(() =>
  new Intl.DateTimeFormat('es-ES', {
    month: 'long',
    year: 'numeric',
  }).format(currentMonth.value),
)

const todayKey = formatDayKey(new Date())

const calendarDays = computed(() => {
  const firstDay = startOfMonth(currentMonth.value)
  const monthIndex = firstDay.getMonth()
  const monthStartOffset = (firstDay.getDay() + 6) % 7
  const calendarStart = addDays(firstDay, -monthStartOffset)
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
      visibleEvents: dayEvents.slice(0, 2),
      hiddenCount: Math.max(0, dayEvents.length - 2),
    })
  }

  return days
})

const selectedDay = computed(() => {
  if (!selectedDayKey.value) {
    return null
  }

  return calendarDays.value.find((day) => day.key === selectedDayKey.value) ?? null
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

function eventStyle(evento) {
  const color = normalizeHexColor(evento.tipoColorResolved) || defaultEventColor

  return {
    backgroundColor: hexToRgba(color, 0.16),
    borderColor: hexToRgba(color, 0.34),
    color,
  }
}

function formatEventTime(value) {
  const parsed = parseDate(value)

  if (!parsed) {
    return ''
  }

  return new Intl.DateTimeFormat('es-ES', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(parsed)
}

function openDay(day) {
  selectedDayKey.value = day.key
}

function closeModal() {
  selectedDayKey.value = ''
}

function goToMonth(amount) {
  currentMonth.value = addMonths(currentMonth.value, amount)
}

async function loadCalendarData() {
  loading.value = true
  error.value = ''

  try {
    const [eventosResult, tiposResult] = await Promise.allSettled([getEventos(), getTipos()])

    if (eventosResult.status === 'fulfilled') {
      eventos.value = normalizeCollection(eventosResult.value).map(normalizeEvento)
    } else {
      eventos.value = []
      error.value = eventosResult.reason?.response?.data?.message || 'No se pudieron cargar los eventos.'
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

onMounted(loadCalendarData)
</script>

<template>
  <AppShell
    title="Calendario mensual"
    eyebrow="Agenda escolar"
    subtitle="Vista mensual de eventos con color por tipo, coincidencias visibles y acceso rápido al detalle del día."
  >
    <section class="panel calendario-mensual">
      <header class="calendario-mensual__toolbar">
        <div>
          <p class="eyebrow">Mes activo</p>
          <h2>{{ monthLabel }}</h2>
          <p class="muted">Haz clic en un día para ver todos sus eventos.</p>
        </div>

        <div class="calendario-mensual__controls">
          <button class="btn btn--ghost" type="button" @click="goToMonth(-1)">
            <ChevronLeft :size="16" />
            Anterior
          </button>
          <button class="btn btn--ghost" type="button" @click="goToMonth(1)">
            Siguiente
            <ChevronRight :size="16" />
          </button>
        </div>
      </header>

      <p v-if="error" class="calendario-mensual__error" role="alert">{{ error }}</p>
      <p v-if="loading" class="muted">Cargando calendario...</p>

      <template v-else>
        <div class="calendario-mensual__weekdays" aria-hidden="true">
          <span v-for="day in weekdays" :key="day">{{ day }}</span>
        </div>

        <div class="calendario-mensual__grid">
          <button
            v-for="day in calendarDays"
            :key="day.key"
            type="button"
            class="calendario-day"
            :class="{
              'calendario-day--muted': !day.isCurrentMonth,
              'calendario-day--today': day.isToday,
              'calendario-day--has-events': day.events.length > 0,
            }"
            :aria-label="`${day.date.getDate()} de ${monthLabel}, ${day.events.length} eventos`"
            @click="openDay(day)"
          >
            <div class="calendario-day__header">
              <span class="calendario-day__number">{{ day.date.getDate() }}</span>
              <span v-if="day.events.length > 0" class="calendario-day__count">{{ day.events.length }}</span>
            </div>

            <div class="calendario-day__events">
              <span
                v-for="evento in day.visibleEvents"
                :key="evento.id"
                class="calendario-event"
                :style="eventStyle(evento)"
              >
                {{ evento.titulo }}
              </span>

              <span v-if="day.hiddenCount > 0" class="calendario-day__more">+{{ day.hiddenCount }} más</span>
              <span v-else-if="day.events.length === 0" class="calendario-day__empty">Sin eventos</span>
            </div>
          </button>
        </div>
      </template>
    </section>

    <Teleport to="body">
      <div v-if="selectedDay" class="calendario-modal" @click.self="closeModal">
        <div class="calendario-modal__dialog" role="dialog" aria-modal="true" :aria-label="selectedDayLabel">
          <header class="calendario-modal__header">
            <div>
              <p class="eyebrow">Eventos del día</p>
              <h3>{{ selectedDayLabel }}</h3>
            </div>

            <button class="calendario-modal__close" type="button" @click="closeModal" aria-label="Cerrar">
              <X :size="18" />
            </button>
          </header>

          <div v-if="selectedDayEvents.length > 0" class="calendario-modal__list">
            <article v-for="evento in selectedDayEvents" :key="evento.id" class="calendario-modal__item">
              <span class="calendario-modal__color" :style="{ backgroundColor: evento.tipoColorResolved }"></span>

              <div class="calendario-modal__item-content">
                <div class="calendario-modal__item-top">
                  <strong>{{ evento.titulo }}</strong>
                  <span v-if="formatEventTime(evento.fechaInicio)" class="calendario-modal__time">
                    {{ formatEventTime(evento.fechaInicio) }}
                  </span>
                </div>
                <p>{{ evento.tipoNombre }}</p>
              </div>
            </article>
          </div>

          <p v-else class="calendario-modal__empty muted">No hay eventos para este día.</p>
        </div>
      </div>
    </Teleport>
  </AppShell>
</template>

<style scoped>
.calendario-mensual {
  padding: 28px;
  display: grid;
  gap: 20px;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: 12px;
}

.calendario-mensual__toolbar {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 20px;
}

.calendario-mensual__toolbar h2 {
  margin: 0 0 6px;
  font-size: 1.3rem;
  font-weight: 600;
}

.calendario-mensual__toolbar p {
  margin: 0;
}

.calendario-mensual__controls {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.calendario-mensual__error {
  margin: 0;
  padding: 12px 14px;
  border-radius: 8px;
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #dc2626;
}

.calendario-mensual__weekdays {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 10px;
}

.calendario-mensual__weekdays span {
  padding: 0 8px;
  color: var(--muted);
  font-size: 0.78rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.calendario-mensual__grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 10px;
}

.calendario-day {
  min-height: 150px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: var(--bg-soft);
  color: var(--text);
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}

.calendario-day:hover {
  transform: translateY(-1px);
  border-color: var(--primary);
  box-shadow: 0 10px 20px rgba(97, 214, 167, 0.1);
}

.calendario-day--muted {
  opacity: 0.55;
}

.calendario-day--today {
  border-color: var(--primary);
  box-shadow: inset 0 0 0 1px rgba(97, 214, 167, 0.18);
}

.calendario-day--has-events {
  background: linear-gradient(180deg, rgba(97, 214, 167, 0.08), var(--bg-soft));
}

.calendario-day__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.calendario-day__number {
  width: 2rem;
  height: 2rem;
  border-radius: 999px;
  display: grid;
  place-items: center;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.35);
}

.calendario-day--today .calendario-day__number {
  background: var(--primary);
  color: #ffffff;
}

.calendario-day__count {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--muted);
}

.calendario-day__events {
  display: grid;
  gap: 8px;
}

.calendario-event {
  padding: 0.45rem 0.6rem;
  border-radius: 10px;
  border: 1px solid transparent;
  font-size: 0.78rem;
  font-weight: 600;
  line-height: 1.25;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.calendario-day__more {
  padding: 0.35rem 0.6rem;
  border-radius: 999px;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  color: var(--primary);
  font-size: 0.75rem;
  font-weight: 600;
  width: fit-content;
}

.calendario-day__empty {
  color: var(--muted);
  font-size: 0.78rem;
}

.calendario-modal {
  position: fixed;
  inset: 0;
  background: rgba(17, 24, 39, 0.45);
  display: grid;
  place-items: center;
  padding: 20px;
  z-index: 50;
}

.calendario-modal__dialog {
  width: min(680px, 100%);
  max-height: min(80vh, 720px);
  overflow: auto;
  padding: 24px;
  border-radius: 16px;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  box-shadow: var(--shadow);
  display: grid;
  gap: 18px;
}

.calendario-modal__header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
}

.calendario-modal__header h3 {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 600;
}

.calendario-modal__close {
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

.calendario-modal__list {
  display: grid;
  gap: 12px;
}

.calendario-modal__item {
  display: flex;
  gap: 12px;
  padding: 14px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: var(--bg-soft);
}

.calendario-modal__color {
  width: 12px;
  border-radius: 999px;
  flex: 0 0 auto;
}

.calendario-modal__item-content {
  flex: 1;
  display: grid;
  gap: 4px;
}

.calendario-modal__item-top {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.calendario-modal__item-content p {
  margin: 0;
  color: var(--muted);
  font-size: 0.9rem;
}

.calendario-modal__time {
  color: var(--primary);
  font-size: 0.85rem;
  font-weight: 600;
  white-space: nowrap;
}

.calendario-modal__empty {
  margin: 0;
}

@media (max-width: 1100px) {
  .calendario-day {
    min-height: 130px;
  }
}

@media (max-width: 820px) {
  .calendario-mensual {
    padding: 20px;
  }

  .calendario-mensual__toolbar {
    flex-direction: column;
  }

  .calendario-mensual__controls {
    width: 100%;
  }

  .calendario-mensual__controls .btn {
    flex: 1;
  }

  .calendario-mensual__grid,
  .calendario-mensual__weekdays {
    gap: 8px;
  }

  .calendario-day {
    min-height: 118px;
    padding: 10px;
  }

  .calendario-event {
    font-size: 0.72rem;
  }

  .calendario-modal {
    padding: 12px;
  }

  .calendario-modal__dialog {
    padding: 18px;
  }
}
</style>