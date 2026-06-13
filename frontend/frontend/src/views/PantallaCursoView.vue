<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { X } from 'lucide-vue-next'

import { addDays, buildCourseMonths, createTipoMap, formatLongDate, loadScreenData, monthLabel, normalizeEvento, screenWeekdays, normalizeHexColor, buildMonthDays, getTvTokenFromRoute, validateTvToken } from './pantallaUtils'

const loading = ref(true)
const error = ref('')
const eventos = ref([])
const tipoMap = ref(new Map())
const timerId = ref(null)
const now = ref(new Date())
const selectedDay = ref(null)
const route = useRoute()

function refresh() {
  now.value = new Date()
}

async function loadData() {
  loading.value = true
  error.value = ''

  try {
    const tvToken = getTvTokenFromRoute(route)
    await validateTvToken(tvToken)
    const data = await loadScreenData()
    tipoMap.value = createTipoMap(data.tipos)
    eventos.value = data.eventos.map((evento) => normalizeEvento(evento, tipoMap.value))
    error.value = data.eventosError
  } catch (requestError) {
    console.error('[PantallaCursoView] loadData failed', {
      message: requestError?.message,
      responseStatus: requestError?.response?.status,
      responseData: requestError?.response?.data,
      stack: requestError?.stack,
    })
    error.value = requestError?.response?.data?.message || 'No se pudieron cargar los eventos.'
    eventos.value = []
  } finally {
    loading.value = false
    refresh()
  }
}

const months = computed(() => buildCourseMonths(now.value))

function eventsForMonth(month) {
  return eventos.value.filter((evento) => evento.fechaInicioDate && evento.fechaInicioDate.getMonth() === month.getMonth() && evento.fechaInicioDate.getFullYear() === month.getFullYear())
}

function eventsByDayForMonth(month) {
  const map = new Map()

  for (const evento of eventsForMonth(month)) {
    const dayKey = `${evento.fechaInicioDate.getFullYear()}-${String(evento.fechaInicioDate.getMonth() + 1).padStart(2, '0')}-${String(evento.fechaInicioDate.getDate()).padStart(2, '0')}`
    if (!map.has(dayKey)) {
      map.set(dayKey, [])
    }
    map.get(dayKey).push(evento)
  }

  for (const items of map.values()) {
    items.sort((a, b) => {
      const timeA = a.fechaInicioDate?.getTime() ?? 0
      const timeB = b.fechaInicioDate?.getTime() ?? 0
      if (timeA !== timeB) return timeA - timeB
      const priorityA = a.tipoPrioridad ?? Number.MAX_SAFE_INTEGER
      const priorityB = b.tipoPrioridad ?? Number.MAX_SAFE_INTEGER
      if (priorityA !== priorityB) return priorityA - priorityB
      return a.titulo.localeCompare(b.titulo, 'es', { sensitivity: 'base' })
    })
  }

  return map
}

function indicatorStyle(evento) {
  return { backgroundColor: normalizeHexColor(evento.tipoColor) || '#61d6a7' }
}

function singleEventStyle(evento) {
  return {
    '--event-color': normalizeHexColor(evento.tipoColorResolved ?? evento.tipoColor) || '#61d6a7',
  }
}

function formatHour(value) {
  if (!value) {
    return ''
  }

  return new Intl.DateTimeFormat('es-ES', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(value)
}

function openDayModal(day) {
  console.log('CLICK TV CURSO', day)

  if (!day?.events?.length) {
    return
  }

  selectedDay.value = day
}

function closeDayModal() {
  selectedDay.value = null
}

function selectedDayLabel(day) {
  if (!day?.date) {
    return ''
  }

  return formatLongDate(day.date)
}

function eventStatusLabel(status) {
  const normalized = String(status || '').toUpperCase()

  if (normalized === 'PENDIENTE') return 'Pendiente'
  if (normalized === 'CONFIRMADO') return 'Confirmado'
  if (normalized === 'RECHAZADO') return 'Rechazado'
  if (normalized === 'CANCELADO') return 'Cancelado'
  return normalized || 'Sin estado'
}

function eventStatusClass(status) {
  const normalized = String(status || '').toUpperCase()

  if (normalized === 'PENDIENTE') return 'tv-course-modal-event__status--warning'
  if (normalized === 'CONFIRMADO') return 'tv-course-modal-event__status--success'
  if (normalized === 'RECHAZADO' || normalized === 'CANCELADO') return 'tv-course-modal-event__status--danger'
  return 'tv-course-modal-event__status--neutral'
}

onMounted(async () => {
  await loadData()
  timerId.value = window.setInterval(() => {
    refresh()
    loadData()
  }, 5 * 60 * 1000)
})

onBeforeUnmount(() => {
  if (timerId.value) {
    window.clearInterval(timerId.value)
  }
})
</script>

<template>
  <main class="tv-screen tv-screen--course">
    <header class="tv-header">
      <div>
        <p class="tv-eyebrow">Agenda escolar</p>
        <h1>Curso completo</h1>
      </div>
      <div class="tv-clock">
        <strong>{{ formatLongDate(now) }}</strong>
        <span>Septiembre a junio</span>
      </div>
    </header>

    <section v-if="loading" class="tv-empty tv-empty--big">
      <p>Cargando curso completo...</p>
    </section>

    <section v-else class="tv-course-grid">
      <article v-for="month in months" :key="`${month.getFullYear()}-${month.getMonth()}`" class="tv-course-month">
        <header class="tv-course-month__header">
          <h2>{{ monthLabel(month) }}</h2>
        </header>

        <div class="tv-course-weekdays" aria-hidden="true">
          <span v-for="day in screenWeekdays" :key="day">{{ day }}</span>
        </div>

        <div class="tv-course-grid-days">
          <button
            v-for="day in buildMonthDays(month, eventsByDayForMonth(month))"
            :key="day.key"
            type="button"
            class="tv-course-day"
            :class="{ 'tv-course-day--muted': !day.isCurrentMonth, 'tv-course-day--today': day.isToday, 'tv-course-day--active': day.events.length > 0, 'tv-course-day--clickable': day.events.length > 0 }"
            :disabled="day.events.length === 0"
            @click="openDayModal(day)"
          >
            <div class="tv-course-day__top">
              <strong>{{ day.date.getDate() }}</strong>
            </div>

            <div v-if="day.events.length === 0" class="tv-empty-inline">Sin eventos</div>
            <div v-else-if="day.display.isSingle" class="tv-course-single" :style="singleEventStyle(day.display.primaryEvent)">
              {{ day.display.primaryEvent.titulo || day.display.primaryEvent.tipoNombre || 'Sin título' }}
            </div>
            <div v-else-if="day.display.isUniformGroup" class="tv-course-indicators tv-course-indicators--summary">
              <span class="tv-course-indicator-group">
                <span class="tv-course-indicator" :style="{ backgroundColor: day.display.groups[0].color }"></span>
                <span v-if="day.display.groups[0].count >= 2" class="tv-course-more">{{ day.display.groups[0].count }}</span>
              </span>
            </div>
            <div v-else class="tv-course-mixed">
              <div class="tv-course-single tv-course-single--mixed" :style="singleEventStyle(day.display.primaryEvent)">
                {{ day.display.primaryEvent.titulo || day.display.primaryEvent.tipoNombre || 'Sin título' }}
              </div>

              <div class="tv-course-indicators tv-course-indicators--chips">
                <span v-for="group in day.display.groups.slice(0, 3)" :key="group.key" class="tv-course-indicator-group" :title="group.title">
                  <span class="tv-course-indicator" :style="{ backgroundColor: group.color }"></span>
                  <span v-if="group.count >= 2" class="tv-course-more">{{ group.count }}</span>
                </span>
                <span v-if="day.display.groups.length > 3" class="tv-course-more">+{{ day.display.groups.length - 3 }}</span>
              </div>
            </div>
          </button>
        </div>
      </article>
    </section>

    <Teleport to="body">
      <div v-if="selectedDay" class="tv-course-modal-overlay" @click.self="closeDayModal">
        <div class="tv-course-modal" role="dialog" aria-modal="true" :aria-label="selectedDayLabel(selectedDay)">
          <header class="tv-course-modal__header">
            <div>
              <p class="tv-course-modal__eyebrow">Eventos del día</p>
              <h2>{{ selectedDayLabel(selectedDay) }}</h2>
            </div>

            <button class="tv-course-modal__close" type="button" aria-label="Cerrar" @click="closeDayModal">
              <X :size="18" />
            </button>
          </header>

          <div v-if="selectedDay.events.length > 0" class="tv-course-modal__list">
            <article v-for="evento in selectedDay.events" :key="evento.id" class="tv-course-modal-event">
              <div class="tv-course-modal-event__main">
                <div class="tv-course-modal-event__top">
                  <h3 class="tv-course-modal-event__title">{{ evento.titulo }}</h3>
                  <span class="tv-course-modal-event__time">{{ formatHour(evento.fechaInicioDate) }}</span>
                </div>

                <p class="tv-course-modal-event__meta"><strong>Tipo:</strong> {{ evento.tipoNombre }}</p>
                <p class="tv-course-modal-event__meta"><strong>Lugar:</strong> {{ evento.lugar }}</p>
                <p class="tv-course-modal-event__meta"><strong>Fecha:</strong> {{ formatLongDate(evento.fechaInicioDate) }}</p>
              </div>

              <span class="tv-course-modal-event__status" :class="eventStatusClass(evento.estado)">
                {{ eventStatusLabel(evento.estado) }}
              </span>
            </article>
          </div>

          <p v-else class="tv-course-modal__empty">No hay eventos para este día.</p>
        </div>
      </div>
    </Teleport>

    <p v-if="error" class="tv-error">{{ error }}</p>
  </main>
</template>

<style scoped>
.tv-screen {
  position: fixed;
  inset: 0;
  padding: 32px;
  background: linear-gradient(180deg, #f7fcfa 0%, #eefaf4 100%);
  color: #0f172a;
  overflow: hidden;
  font-size: 32px;
}

.tv-header {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: end;
  margin-bottom: 24px;
}

.tv-eyebrow {
  margin: 0 0 8px;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: #0f766e;
  font-weight: 700;
  font-size: 1rem;
}

.tv-header h1 {
  margin: 0;
  font-size: clamp(3.75rem, 4vw, 5.75rem);
  line-height: 1;
}

.tv-clock {
  text-align: right;
  display: grid;
  gap: 4px;
  font-size: 1.2rem;
}

.tv-clock strong {
  font-size: 1.5rem;
}

.tv-course-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
  height: calc(100vh - 170px);
  overflow: auto;
}

.tv-course-month {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 24px;
  border: 2px solid rgba(15, 118, 110, 0.12);
  padding: 18px;
  display: grid;
  gap: 14px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.tv-course-month__header h2 {
  margin: 0;
  font-size: 2rem;
  text-transform: capitalize;
}

.tv-course-weekdays,
.tv-course-grid-days {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
}

.tv-course-weekdays span {
  text-align: center;
  color: #0f766e;
  font-weight: 700;
  font-size: 0.95rem;
  text-transform: uppercase;
}

.tv-course-day {
  appearance: none;
  width: 100%;
  min-width: 0;
  min-height: 90px;
  padding: 10px;
  border-radius: 16px;
  border: 2px solid #dbe7e2;
  background: #f9fffc;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  cursor: default;
}

.tv-course-day--clickable {
  cursor: pointer;
}

.tv-course-day:disabled {
  cursor: default;
}

.tv-course-day--muted {
  opacity: 0.5;
}

.tv-course-day--today {
  border-color: #61d6a7;
  box-shadow: inset 0 0 0 2px rgba(97, 214, 167, 0.18);
}

.tv-course-day--active {
  background: linear-gradient(180deg, rgba(97, 214, 167, 0.08), #f9fffc);
}

.tv-course-day__top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
  font-size: 1.1rem;
}

.tv-course-day__top span {
  font-size: 0.8rem;
  color: #0f766e;
  font-weight: 700;
}

.tv-course-single,
.tv-course-mixed {
  min-width: 0;
  width: 100%;
}

.tv-course-single {
  padding: 0.24rem 0.38rem;
  border-radius: 10px;
  border-left: 4px solid var(--event-color);
  background: rgba(255, 255, 255, 0.92);
  color: var(--event-color);
  font-size: 0.68rem;
  font-weight: 800;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tv-course-single--mixed {
  margin-bottom: 4px;
}

.tv-course-indicators {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

.tv-course-indicator-group {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.tv-course-indicators--summary {
  justify-content: flex-start;
}

.tv-course-indicators--chips {
  gap: 3px;
}

.tv-course-indicator {
  width: 12px;
  height: 12px;
  border-radius: 999px;
}

.tv-course-more,
.tv-empty-inline {
  font-size: 0.8rem;
  font-weight: 700;
  color: #0f766e;
}

.tv-empty-inline {
  opacity: 0.7;
}

.tv-error {
  position: absolute;
  bottom: 20px;
  left: 32px;
  right: 32px;
  margin: 0;
  padding: 12px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #fecaca;
  color: #dc2626;
  font-size: 1.3rem;
}

@media (max-width: 1400px) {
  .tv-course-grid {
    grid-template-columns: 1fr;
    height: auto;
  }

  .tv-screen {
    overflow: auto;
  }
}

.tv-course-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: rgba(15, 23, 42, 0.58);
}

.tv-course-modal {
  width: min(820px, calc(100vw - 32px));
  max-height: 85vh;
  overflow: auto;
  padding: 24px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
  border: 2px solid rgba(15, 118, 110, 0.14);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.26);
  display: grid;
  gap: 18px;
}

.tv-course-modal__header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
}

.tv-course-modal__eyebrow {
  margin: 0 0 8px;
  color: #0f766e;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-size: 0.85rem;
  font-weight: 700;
}

.tv-course-modal__header h2 {
  margin: 0;
  font-size: clamp(1.8rem, 2.2vw, 2.6rem);
  line-height: 1.1;
}

.tv-course-modal__close {
  width: 40px;
  height: 40px;
  border: 1px solid rgba(15, 118, 110, 0.16);
  border-radius: 12px;
  background: #eefaf4;
  color: #0f172a;
  display: grid;
  place-items: center;
  cursor: pointer;
}

.tv-course-modal__list {
  display: grid;
  gap: 12px;
}

.tv-course-modal-event {
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid rgba(15, 118, 110, 0.14);
  background: linear-gradient(180deg, rgba(97, 214, 167, 0.1), rgba(255, 255, 255, 0.96));
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 14px;
}

.tv-course-modal-event__main {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.tv-course-modal-event__top {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.tv-course-modal-event__title {
  margin: 0;
  font-size: 1.25rem;
  line-height: 1.15;
}

.tv-course-modal-event__time {
  color: #0f766e;
  font-size: 0.95rem;
  font-weight: 700;
  white-space: nowrap;
}

.tv-course-modal-event__meta {
  margin: 0;
  color: #334155;
  font-size: 1rem;
  line-height: 1.45;
}

.tv-course-modal-event__meta strong {
  color: #0f172a;
}

.tv-course-modal-event__status {
  padding: 0.42rem 0.8rem;
  border-radius: 999px;
  font-size: 0.9rem;
  font-weight: 700;
  white-space: nowrap;
}

.tv-course-modal-event__status--warning {
  color: #92400e;
  background: #fef3c7;
}

.tv-course-modal-event__status--success {
  color: #166534;
  background: #dcfce7;
}

.tv-course-modal-event__status--danger {
  color: #991b1b;
  background: #fee2e2;
}

.tv-course-modal-event__status--neutral {
  color: #334155;
  background: #e2e8f0;
}

.tv-course-modal__empty {
  margin: 0;
  color: #475569;
  font-size: 1.05rem;
}

@media (max-width: 820px) {
  .tv-course-modal {
    width: min(100%, calc(100vw - 32px));
    padding: 20px;
    border-radius: 20px;
  }

  .tv-course-modal-event,
  .tv-course-modal__header,
  .tv-course-modal-event__top {
    flex-direction: column;
  }

  .tv-course-modal-event__status {
    align-self: flex-start;
  }
}
</style>