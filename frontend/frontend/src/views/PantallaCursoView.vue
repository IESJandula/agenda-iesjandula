<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import { addDays, buildCourseMonths, createTipoMap, formatLongDate, loadScreenData, monthLabel, normalizeEvento, screenWeekdays, normalizeHexColor, buildMonthDays, getTvTokenFromRoute, validateTvToken } from './pantallaUtils'

const loading = ref(true)
const error = ref('')
const eventos = ref([])
const tipoMap = ref(new Map())
const timerId = ref(null)
const now = ref(new Date())
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
          <article v-for="day in buildMonthDays(month, eventsByDayForMonth(month))" :key="day.key" class="tv-course-day" :class="{ 'tv-course-day--muted': !day.isCurrentMonth, 'tv-course-day--today': day.isToday, 'tv-course-day--active': day.events.length > 0 }">
            <div class="tv-course-day__top">
              <strong>{{ day.date.getDate() }}</strong>
              <span v-if="day.events.length > 0">{{ day.events.length }}</span>
            </div>

            <div v-if="day.events.length === 0" class="tv-empty-inline">Sin eventos</div>
            <div v-else class="tv-course-indicators">
              <span v-for="evento in day.visibleIndicators" :key="evento.id" class="tv-course-indicator" :style="indicatorStyle(evento)"></span>
              <span v-if="day.hiddenCount > 0" class="tv-course-more">+{{ day.hiddenCount }}</span>
            </div>
          </article>
        </div>
      </article>
    </section>

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
  min-height: 90px;
  padding: 10px;
  border-radius: 16px;
  border: 2px solid #dbe7e2;
  background: #f9fffc;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
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

.tv-course-indicators {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
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
</style>