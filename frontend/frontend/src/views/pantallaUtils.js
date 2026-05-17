import { getEventos } from '../api/eventos.api'
import { getTipos } from '../api/tipos.api'
import api from '../api/axios'

export const screenWeekdays = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']

export function normalizeCollection(response) {
  if (Array.isArray(response)) {
    return response
  }

  return response?.content ?? response?.data ?? []
}

export function pad(value) {
  return String(value).padStart(2, '0')
}

export function normalizeHexColor(color) {
  if (typeof color !== 'string') {
    return ''
  }

  const trimmed = color.trim()
  return /^#([0-9a-f]{6})$/i.test(trimmed) ? trimmed : ''
}

export function normalizeTvToken(value) {
  return typeof value === 'string' ? value.trim() : ''
}

export function getTvTokenFromRoute(route) {
  return normalizeTvToken(route?.query?.tvToken)
}

export async function validateTvToken(tvToken) {
  if (!tvToken) {
    throw new Error('Falta el token TV')
  }

  await api.get('/api/tv/validate', {
    params: {
      tvToken,
    },
  })
}

export function parseDate(value) {
  if (!value) {
    return null
  }

  const parsed = new Date(String(value).replace(' ', 'T'))
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

export function startOfDay(date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

export function addDays(date, amount) {
  const nextDate = new Date(date)
  nextDate.setDate(nextDate.getDate() + amount)
  return nextDate
}

export function startOfMonth(date) {
  return new Date(date.getFullYear(), date.getMonth(), 1)
}

export function addMonths(date, amount) {
  return new Date(date.getFullYear(), date.getMonth() + amount, 1)
}

export function formatDayKey(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function monthLabel(date) {
  return new Intl.DateTimeFormat('es-ES', {
    month: 'long',
    year: 'numeric',
  }).format(date)
}

export function shortMonthLabel(date) {
  return new Intl.DateTimeFormat('es-ES', {
    month: 'short',
  }).format(date)
}

export function formatLongDate(date) {
  return new Intl.DateTimeFormat('es-ES', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(date)
}

export function formatHour(date) {
  return new Intl.DateTimeFormat('es-ES', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

export function statusLabel(status) {
  const normalized = String(status || '').toUpperCase()

  if (normalized === 'PENDIENTE') return 'Pendiente'
  if (normalized === 'CONFIRMADO') return 'Confirmado'
  if (normalized === 'CANCELADO') return 'Cancelado'
  return normalized || 'Sin estado'
}

export function statusClass(status, prefix = 'screen-item__status') {
  const normalized = String(status || '').toUpperCase()

  if (normalized === 'PENDIENTE') return `${prefix}--warning`
  if (normalized === 'CONFIRMADO') return `${prefix}--success`
  if (normalized === 'CANCELADO') return `${prefix}--danger`
  return `${prefix}--neutral`
}

export function sortByDateAndPriority(a, b) {
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

export async function loadScreenData() {
  const [eventosResult, tiposResult] = await Promise.allSettled([getEventos(), getTipos()])

  return {
    eventos: eventosResult.status === 'fulfilled' ? normalizeCollection(eventosResult.value) : [],
    tipos: tiposResult.status === 'fulfilled' ? normalizeCollection(tiposResult.value) : [],
    eventosError:
      eventosResult.status === 'rejected'
        ? eventosResult.reason?.response?.data?.message || 'No se pudieron cargar los eventos.'
        : '',
  }
}

export function createTipoMap(tipos) {
  const map = new Map()

  for (const tipo of tipos) {
    const normalizedColor = normalizeHexColor(tipo.color) || '#61d6a7'
    map.set(Number(tipo.id), {
      id: tipo.id,
      nombre: tipo.nombre ?? '-',
      color: normalizedColor,
      prioridad: Number.isFinite(Number(tipo.prioridad)) ? Number(tipo.prioridad) : null,
    })
  }

  return map
}

export function normalizeEvento(evento, tipoMap) {
  const fechaInicioDate = parseDate(evento.fechaInicio ?? evento.fecha_inicio ?? evento.startDate ?? '')
  const tipoId = evento.tipoId ?? evento.tipo?.id ?? null
  const tipoMetadata = tipoMap?.get(Number(tipoId))

  return {
    id: evento.id ?? evento.eventoId,
    titulo: evento.titulo ?? evento.nombre ?? evento.title ?? 'Sin título',
    tipoId,
    tipoNombre: evento.tipoNombre ?? evento.tipo?.nombre ?? evento.tipo?.descripcion ?? evento.tipo ?? tipoMetadata?.nombre ?? '-',
    tipoColor: normalizeHexColor(evento.tipoColor) || tipoMetadata?.color || '#61d6a7',
    tipoPrioridad: tipoMetadata?.prioridad ?? null,
    fechaInicio: evento.fechaInicio ?? evento.fecha_inicio ?? evento.startDate ?? '',
    fechaInicioDate,
    fechaFin: evento.fechaFin ?? evento.fecha_fin ?? evento.endDate ?? '',
    lugar: evento.lugar ?? evento.ubicacion ?? evento.location ?? '-',
    descripcion: evento.descripcion ?? '',
    estado: evento.estado ?? evento.estadoEvento ?? '-',
    creadorNombre: evento.creadorNombre ?? evento.creador?.nombre ?? '',
    gruposAfectados: evento.gruposAfectados ?? '',
  }
}

export function buildDailyGroups(eventos) {
  const grouped = new Map()

  for (const evento of eventos) {
    const dayKey = formatDayKey(evento.fechaInicioDate)

    if (!grouped.has(dayKey)) {
      grouped.set(dayKey, [])
    }

    grouped.get(dayKey).push(evento)
  }

  for (const items of grouped.values()) {
    items.sort(sortByDateAndPriority)
  }

  return grouped
}

export function buildMonthDays(monthDate, eventsByDay) {
  const firstDay = startOfMonth(monthDate)
  const monthIndex = firstDay.getMonth()
  const monthStartOffset = (firstDay.getDay() + 6) % 7
  const calendarStart = addDays(firstDay, -monthStartOffset)
  const todayKey = formatDayKey(startOfDay(new Date()))
  const days = []

  for (let index = 0; index < 42; index += 1) {
    const date = addDays(calendarStart, index)
    const key = formatDayKey(date)
    const dayEvents = [...(eventsByDay.get(key) ?? [])].sort(sortByDateAndPriority)

    days.push({
      date,
      key,
      isCurrentMonth: date.getMonth() === monthIndex,
      isToday: key === todayKey,
      events: dayEvents,
      visibleEvents: dayEvents.slice(0, 2),
      visibleIndicators: dayEvents.slice(0, 3),
      hiddenCount: Math.max(0, dayEvents.length - 2),
    })
  }

  return days
}

export function buildCourseMonths(referenceDate = new Date()) {
  const courseStartYear = referenceDate.getMonth() >= 8 ? referenceDate.getFullYear() : referenceDate.getFullYear() - 1

  return Array.from({ length: 10 }, (_, index) => new Date(courseStartYear, 8 + index, 1))
}
