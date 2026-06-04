<script setup>
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { useRoute } from 'vue-router'

import { loadScreenData, createTipoMap, normalizeEvento, buildDailyGroups, startOfDay, addDays, formatLongDate, formatHour, statusClass, statusLabel, normalizeHexColor, sortByDateAndPriority, sortByPriorityAndHour, getTvTokenFromRoute, validateTvToken, getUpcomingFestivos } from './pantallaUtils'

const loading = ref(true)
const error = ref('')
const currentDateTime = ref(new Date())
const eventos = ref([])
const tipoMap = ref(new Map())
const timerId = ref(null)
const route = useRoute()

function updateClock() {
  currentDateTime.value = new Date()
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
    console.error('[PantallaHoyProximosView] loadData failed', {
      message: requestError?.message,
      responseStatus: requestError?.response?.status,
      responseData: requestError?.response?.data,
      stack: requestError?.stack,
    })
    error.value = requestError?.response?.data?.message || 'No se pudieron cargar los eventos.'
    eventos.value = []
  } finally {
    loading.value = false
    updateClock()
  }
}

const today = computed(() => startOfDay(currentDateTime.value))
const nextSevenDaysEnd = computed(() => addDays(today.value, 7))

const todaysEvents = computed(() =>
  eventos.value
    .filter((evento) => evento.fechaInicioDate && evento.fechaInicioDate >= today.value && evento.fechaInicioDate < addDays(today.value, 1))
    .sort(sortByDateAndPriority),
)

const upcomingEvents = computed(() =>
  eventos.value
    .filter((evento) => evento.fechaInicioDate && evento.fechaInicioDate >= addDays(today.value, 1) && evento.fechaInicioDate <= nextSevenDaysEnd.value)
    .sort(sortByPriorityAndHour),
)

const groupedUpcoming = computed(() => buildDailyGroups(upcomingEvents.value, sortByPriorityAndHour))
const upcomingFestivos = computed(() => getUpcomingFestivos(eventos.value, addDays(today.value, 1), 3))
const footerMessage = computed(() => {
  if (upcomingFestivos.value.length > 0) {
    return upcomingFestivos.value
      .map((evento) => `${formatLongDate(evento.fechaInicioDate)} · ${evento.titulo}`)
      .join(' · ')
  }

  return 'Sin festivos próximos programados en este periodo.'
})

const currentDateLabel = computed(() => formatLongDate(currentDateTime.value))
const currentTimeLabel = computed(() => new Intl.DateTimeFormat('es-ES', { hour: '2-digit', minute: '2-digit', second: '2-digit' }).format(currentDateTime.value))

function itemStyle(evento) {
  const color = normalizeHexColor(evento.tipoColor) || '#61d6a7'
  return { borderColor: color, backgroundColor: `rgba(97, 214, 167, 0.08)` }
}

onMounted(async () => {
  await loadData()
  timerId.value = window.setInterval(() => {
    updateClock()
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
  <main class="tv-page tv-screen tv-screen--today tv-hoy-proximos">
    <header class="tv-header">
      <div>
        <p class="tv-eyebrow">Agenda escolar</p>
        <h1>Hoy y próximos 7 días</h1>
      </div>
      <div class="tv-clock">
        <strong>{{ currentDateLabel }}</strong>
        <span>{{ currentTimeLabel }}</span>
      </div>
    </header>

    <section v-if="loading" class="tv-empty tv-empty--big">
      <p>Cargando eventos...</p>
    </section>

    <section v-else class="tv-two-col">
      <article class="tv-panel">
        <h2>Eventos de hoy</h2>
        <div v-if="todaysEvents.length === 0" class="tv-empty">
          <p>No hay eventos para hoy.</p>
        </div>
        <div v-else class="tv-list">
          <div v-for="evento in todaysEvents" :key="evento.id" class="tv-item" :style="itemStyle(evento)">
            <div class="tv-item__main">
              <strong>{{ evento.titulo }}</strong>
              <span>{{ formatHour(evento.fechaInicioDate) }} · {{ evento.lugar }}</span>
            </div>
            <span class="tv-status" :class="statusClass(evento.estado, 'tv-status')">{{ statusLabel(evento.estado) }}</span>
          </div>
        </div>
      </article>

      <article class="tv-panel">
        <h2>Próximos 7 días</h2>
        <div v-if="groupedUpcoming.size === 0" class="tv-empty">
          <p>No hay eventos próximos.</p>
        </div>
        <div v-else class="tv-group-list">
          <section v-for="[dayKey, dayEvents] in groupedUpcoming" :key="dayKey" class="tv-day-group">
            <h3>{{ formatLongDate(dayEvents[0].fechaInicioDate) }}</h3>
            <div class="tv-list">
              <div v-for="evento in dayEvents" :key="evento.id" class="tv-item" :style="itemStyle(evento)">
                <div class="tv-item__main">
                  <strong>{{ evento.titulo }}</strong>
                  <span>{{ formatHour(evento.fechaInicioDate) }} · {{ evento.lugar }}</span>
                </div>
                <span class="tv-status" :class="statusClass(evento.estado, 'tv-status')">{{ statusLabel(evento.estado) }}</span>
              </div>
            </div>
          </section>
        </div>
      </article>
    </section>

    <footer class="tv-footer">
      <div class="tv-footer__block">
        <strong>Próximos festivos</strong>
        <span>{{ footerMessage }}</span>
      </div>
    </footer>

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
  background:
    radial-gradient(circle at top left, rgba(97, 214, 167, 0.18), transparent 28%),
    radial-gradient(circle at bottom right, rgba(97, 214, 167, 0.1), transparent 24%),
    #f4fbf7;
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

.tv-header h1,
.tv-panel h2,
.tv-day-group h3 {
  margin: 0;
}

.tv-header h1 {
  font-size: clamp(3.75rem, 4vw, 5.75rem);
  line-height: 1;
}

.tv-clock {
  text-align: right;
  display: grid;
  gap: 4px;
  font-size: 1.4rem;
}

.tv-clock strong {
  font-size: 1.6rem;
}

.tv-two-col {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.15fr);
  gap: 24px;
  align-items: start;
}

.tv-panel {
  background: rgba(255, 255, 255, 0.82);
  border: 2px solid rgba(15, 118, 110, 0.12);
  border-radius: 28px;
  padding: 22px 24px;
  min-width: 0;
  min-height: 0;
  display: grid;
  gap: 14px;
  overflow: hidden;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.tv-panel h2 {
  font-size: clamp(2.2rem, 2.2vw, 3.4rem);
}

.tv-list,
.tv-group-list {
  display: grid;
  gap: 12px;
  overflow: auto;
}

.tv-day-group {
  display: grid;
  gap: 10px;
}

.tv-day-group h3 {
  font-size: 1.35rem;
  color: #0f766e;
}

.tv-item {
  border-radius: 20px;
  border: 2px solid transparent;
  padding: 14px 16px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.tv-item__main {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.tv-item__main strong {
  font-size: 1.35rem;
  line-height: 1.08;
}

.tv-item__main span {
  font-size: 0.98rem;
  color: #334155;
}

.tv-status {
  padding: 0.42rem 0.8rem;
  border-radius: 999px;
  font-size: 0.9rem;
  font-weight: 700;
  white-space: nowrap;
}

.tv-status--warning {
  color: #92400e;
  background: #fef3c7;
}

.tv-status--success {
  color: #166534;
  background: #dcfce7;
}

.tv-status--danger {
  color: #991b1b;
  background: #fee2e2;
}

.tv-status--neutral {
  color: #334155;
  background: #e2e8f0;
}

.tv-empty {
  min-height: 160px;
  display: grid;
  place-items: center;
  text-align: center;
  border-radius: 20px;
  border: 2px dashed rgba(15, 118, 110, 0.2);
  color: #475569;
  padding: 24px;
}

.tv-empty--big {
  min-height: 240px;
}

.tv-empty p,
.tv-error {
  margin: 0;
  font-size: 1.4rem;
}

.tv-footer {
  margin-top: 18px;
  padding: 22px 24px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.88);
  border: 2px solid rgba(15, 118, 110, 0.12);
}

.tv-footer__block {
  display: grid;
  gap: 10px;
  align-items: start;
  font-size: 1.05rem;
  line-height: 1.45;
}

.tv-footer__block strong {
  color: #0f766e;
  font-size: 1rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.tv-footer__block span {
  color: #334155;
}

@media (max-width: 1600px) {
  .tv-header h1 {
    font-size: clamp(3.25rem, 3.6vw, 5rem);
  }
}

.tv-error {
  position: absolute;
  bottom: 20px;
  left: 32px;
  right: 32px;
  padding: 12px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #fecaca;
  color: #dc2626;
}

@media (max-width: 1600px) {
  .tv-screen {
    font-size: 28px;
  }

  .tv-item__main strong {
    font-size: 1.4rem;
  }
}

@media (max-width: 1100px) {
  .tv-two-col {
    grid-template-columns: 1fr;
    min-height: auto;
  }
}
</style>