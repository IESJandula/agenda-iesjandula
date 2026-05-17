<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import { createTipoMap, formatDayKey, formatLongDate, loadScreenData, monthLabel, normalizeEvento, screenWeekdays, startOfMonth, addDays, normalizeHexColor, buildMonthDays, statusClass, statusLabel, getTvTokenFromRoute, validateTvToken } from './pantallaUtils'

const loading = ref(true)
const error = ref('')
const eventos = ref([])
const tipoMap = ref(new Map())
const timerId = ref(null)
const today = ref(new Date())
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
  <main class="tv-screen tv-screen--month">
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
        <article v-for="day in calendarDays" :key="day.key" class="tv-month-day" :class="{ 'tv-month-day--muted': !day.isCurrentMonth, 'tv-month-day--today': day.isToday, 'tv-month-day--active': day.events.length > 0 }">
          <div class="tv-month-day__top">
            <strong>{{ day.date.getDate() }}</strong>
            <span v-if="day.events.length > 0">{{ day.events.length }}</span>
          </div>

          <div v-if="day.events.length === 0" class="tv-empty-inline">Sin eventos</div>
          <div v-else class="tv-indicators">
            <span v-for="evento in day.visibleIndicators" :key="evento.id" class="tv-indicator" :style="indicatorStyle(evento)"></span>
            <span v-if="day.hiddenCount > 0" class="tv-more">+{{ day.hiddenCount }}</span>
          </div>
        </article>
      </div>
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
  font-size: clamp(3rem, 3vw, 5rem);
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
  height: calc(100vh - 170px);
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
  min-height: 120px;
  padding: 12px;
  border-radius: 18px;
  border: 2px solid #dbe7e2;
  background: #f9fffc;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
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
    height: auto;
  }
}
</style>