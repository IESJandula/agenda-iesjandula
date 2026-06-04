<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { X } from 'lucide-vue-next'

import { createTipoMap, formatDayKey, formatHour, formatLongDate, loadScreenData, monthLabel, normalizeEvento, screenWeekdays, startOfMonth, addDays, normalizeHexColor, buildMonthDays, statusClass, statusLabel, getTvTokenFromRoute, validateTvToken } from './pantallaUtils'

const loading = ref(true)
const error = ref('')
const eventos = ref([])
const tipoMap = ref(new Map())
const timerId = ref(null)
const today = ref(new Date())
const selectedDay = ref(null)
const route = useRoute()

function refresh() {
  today.value = new Date()
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
    console.error('[PantallaMesActualView] loadData failed', {
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

const currentMonth = computed(() => startOfMonth(today.value))

const currentMonthEvents = computed(() => eventos.value.filter((evento) => evento.fechaInicioDate && evento.fechaInicioDate.getMonth() === currentMonth.value.getMonth() && evento.fechaInicioDate.getFullYear() === currentMonth.value.getFullYear()))

const groupedByDay = computed(() => {
  const map = new Map()

  for (const evento of currentMonthEvents.value) {
    const key = formatDayKey(evento.fechaInicioDate)
    if (!map.has(key)) {
      map.set(key, [])
    }
    map.get(key).push(evento)
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
})

const calendarDays = computed(() => buildMonthDays(currentMonth.value, groupedByDay.value))
const monthName = computed(() => monthLabel(currentMonth.value))
const titleLabel = computed(() => formatLongDate(today.value))

function indicatorStyle(evento) {
  return { backgroundColor: normalizeHexColor(evento.tipoColor) || '#61d6a7' }
}

const selectedDayEvents = computed(() => selectedDay.value?.events ?? [])

const selectedDayLabel = computed(() => {
  if (!selectedDay.value?.date) {
    return ''
  }

  return formatLongDate(selectedDay.value.date)
})

function openDayModal(day) {
  if (!day?.events?.length) {
    return
  }

  selectedDay.value = day
}

function closeDayModal() {
  selectedDay.value = null
}

function onKeydown(event) {
  if (event.key === 'Escape' && selectedDay.value) {
    closeDayModal()
  }
}

onMounted(async () => {
  await loadData()
  timerId.value = window.setInterval(() => {
    refresh()
    loadData()
  }, 5 * 60 * 1000)

  window.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  if (timerId.value) {
    window.clearInterval(timerId.value)
  }

  window.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <main class="tv-page tv-screen tv-screen--month">
    <header class="tv-header">
      <div>
        <p class="tv-eyebrow">Agenda escolar</p>
        <h1>{{ monthName }}</h1>
      </div>
      <div class="tv-clock">
        <strong>{{ titleLabel }}</strong>
        <span>Actualización automática cada 5 minutos</span>
      </div>
    </header>

    <section v-if="loading" class="tv-empty tv-empty--big">
      <p>Cargando calendario...</p>
    </section>

    <section v-else class="tv-calendar-card">
      <div class="tv-weekdays" aria-hidden="true">
        <span v-for="day in screenWeekdays" :key="day">{{ day }}</span>
      </div>

      <div class="tv-month-grid">
        <button v-for="day in calendarDays" :key="day.key" type="button" class="tv-month-day" :class="{ 'tv-month-day--muted': !day.isCurrentMonth, 'tv-month-day--today': day.isToday, 'tv-month-day--active': day.events.length > 0, 'tv-month-day--clickable': day.events.length > 0 }" :disabled="day.events.length === 0" @click="openDayModal(day)">
          <div class="tv-month-day__top">
            <strong>{{ day.date.getDate() }}</strong>
            <span v-if="day.events.length > 0">{{ day.events.length }}</span>
          </div>

          <div v-if="day.events.length === 0" class="tv-empty-inline">Sin eventos</div>
          <div v-else class="tv-indicators">
            <span v-for="evento in day.visibleIndicators" :key="evento.id" class="tv-indicator" :style="indicatorStyle(evento)"></span>
            <span v-if="day.hiddenCount > 0" class="tv-more">+{{ day.hiddenCount }}</span>
          </div>
        </button>
      </div>
    </section>

    <Teleport to="body">
      <div v-if="selectedDay" class="tv-month-modal-overlay" @click.self="closeDayModal">
        <div class="tv-month-modal" role="dialog" aria-modal="true" :aria-label="selectedDayLabel">
          <header class="tv-month-modal__header">
            <div>
              <p class="tv-month-modal__eyebrow">Eventos del día</p>
              <h2>{{ selectedDayLabel }}</h2>
            </div>

            <button class="tv-month-modal__close" type="button" aria-label="Cerrar" @click="closeDayModal">
              <X :size="18" />
            </button>
          </header>

          <div v-if="selectedDayEvents.length > 0" class="tv-month-modal__list">
            <article v-for="evento in selectedDayEvents" :key="evento.id" class="tv-month-modal-event">
              <div class="tv-month-modal-event__main">
                <div class="tv-month-modal-event__top">
                  <h3 class="tv-month-modal-event__title">{{ evento.titulo }}</h3>
                  <span class="tv-month-modal-event__time">{{ formatHour(evento.fechaInicioDate) }}</span>
                </div>

                <p class="tv-month-modal-event__meta"><strong>Tipo:</strong> {{ evento.tipoNombre }}</p>
                <p class="tv-month-modal-event__meta"><strong>Lugar:</strong> {{ evento.lugar }}</p>
                <p class="tv-month-modal-event__meta"><strong>Estado:</strong> {{ statusLabel(evento.estado) }}</p>
              </div>

              <span class="tv-month-modal-event__status" :class="statusClass(evento.estado, 'tv-month-modal-event__status')">
                {{ statusLabel(evento.estado) }}
              </span>
            </article>
          </div>

          <p v-else class="tv-month-modal__empty">No hay eventos para este día.</p>
        </div>
      </div>
    </Teleport>

    <p v-if="error" class="tv-error">{{ error }}</p>
  </main>
</template>

<style scoped>
.tv-page {
  position: relative;
  min-height: 100vh;
  max-height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
  padding-bottom: 48px;
  scrollbar-width: thin;
  scrollbar-color: rgba(97, 214, 167, 0.7) transparent;
}

.tv-page::-webkit-scrollbar {
  width: 10px;
}

.tv-page::-webkit-scrollbar-track {
  background: transparent;
}

.tv-page::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(97, 214, 167, 0.7);
}

.tv-screen {
  position: relative;
  padding: 32px;
  background: linear-gradient(180deg, #f7fcfa 0%, #eefaf4 100%);
  color: #0f172a;
  overflow: visible;
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
  font-size: 1.6rem;
}

.tv-calendar-card {
  min-height: calc(100vh - 170px);
  background: rgba(255, 255, 255, 0.9);
  border-radius: 28px;
  border: 2px solid rgba(15, 118, 110, 0.12);
  padding: 24px;
  display: grid;
  gap: 18px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.tv-weekdays,
.tv-month-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 12px;
}

.tv-weekdays span {
  text-align: center;
  color: #0f766e;
  font-weight: 700;
  font-size: 1rem;
  text-transform: uppercase;
}

.tv-month-grid {
  flex: 1;
  min-height: 0;
}

.tv-month-day {
  appearance: none;
  width: 100%;
  min-width: 0;
  min-height: 120px;
  padding: 12px;
  border-radius: 18px;
  border: 2px solid #dbe7e2;
  background: #f9fffc;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  text-align: left;
  cursor: default;
}

.tv-month-day--clickable {
  cursor: pointer;
}

.tv-month-day:disabled {
  cursor: default;
}

.tv-month-day--muted {
  opacity: 0.5;
}

.tv-month-day--today {
  border-color: #61d6a7;
  box-shadow: inset 0 0 0 2px rgba(97, 214, 167, 0.18);
}

.tv-month-day--active {
  background: linear-gradient(180deg, rgba(97, 214, 167, 0.08), #f9fffc);
}

.tv-month-day__top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
  font-size: 1.3rem;
}

.tv-month-day__top span {
  font-size: 0.9rem;
  color: #0f766e;
  font-weight: 700;
}

.tv-indicators {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.tv-indicator {
  width: 16px;
  height: 16px;
  border-radius: 999px;
  border: 2px solid rgba(255, 255, 255, 0.7);
}

.tv-more,
.tv-empty-inline {
  font-size: 0.9rem;
  font-weight: 700;
  color: #0f766e;
}

.tv-empty-inline {
  opacity: 0.7;
}

.tv-month-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: rgba(15, 23, 42, 0.58);
}

.tv-month-modal {
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

.tv-month-modal__header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
}

.tv-month-modal__eyebrow {
  margin: 0 0 8px;
  color: #0f766e;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-size: 0.85rem;
  font-weight: 700;
}

.tv-month-modal__header h2 {
  margin: 0;
  font-size: clamp(1.8rem, 2.2vw, 2.6rem);
  line-height: 1.1;
}

.tv-month-modal__close {
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

.tv-month-modal__list {
  display: grid;
  gap: 12px;
}

.tv-month-modal-event {
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid rgba(15, 118, 110, 0.14);
  background: linear-gradient(180deg, rgba(97, 214, 167, 0.1), rgba(255, 255, 255, 0.96));
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 14px;
}

.tv-month-modal-event__main {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.tv-month-modal-event__top {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.tv-month-modal-event__title {
  margin: 0;
  font-size: 1.25rem;
  line-height: 1.15;
}

.tv-month-modal-event__time {
  color: #0f766e;
  font-size: 0.95rem;
  font-weight: 700;
  white-space: nowrap;
}

.tv-month-modal-event__meta {
  margin: 0;
  color: #334155;
  font-size: 1rem;
  line-height: 1.45;
}

.tv-month-modal-event__meta strong {
  color: #0f172a;
}

.tv-month-modal-event__status {
  padding: 0.42rem 0.8rem;
  border-radius: 999px;
  font-size: 0.9rem;
  font-weight: 700;
  white-space: nowrap;
}

.tv-month-modal-event__status--warning {
  color: #92400e;
  background: #fef3c7;
}

.tv-month-modal-event__status--success {
  color: #166534;
  background: #dcfce7;
}

.tv-month-modal-event__status--danger {
  color: #991b1b;
  background: #fee2e2;
}

.tv-month-modal-event__status--neutral {
  color: #334155;
  background: #e2e8f0;
}

.tv-month-modal__empty {
  margin: 0;
  color: #475569;
  font-size: 1.05rem;
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

@media (max-width: 1100px) {
  .tv-screen {
    font-size: 28px;
  }

  .tv-calendar-card {
    min-height: auto;
  }
}

@media (max-width: 820px) {
  .tv-month-modal {
    width: min(100%, calc(100vw - 32px));
    padding: 20px;
    border-radius: 20px;
  }

  .tv-month-modal-event,
  .tv-month-modal__header,
  .tv-month-modal-event__top {
    flex-direction: column;
  }

  .tv-month-modal-event__status {
    align-self: flex-start;
  }
}
</style>