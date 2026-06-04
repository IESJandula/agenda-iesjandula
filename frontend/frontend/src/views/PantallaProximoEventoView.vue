<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import { createTipoMap, formatHour, formatLongDate, loadScreenData, normalizeEvento, normalizeHexColor, sortByPriorityAndHour, getTvTokenFromRoute, validateTvToken } from './pantallaUtils'

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
    console.error('[PantallaProximoEventoView] loadData failed', {
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

const nextEvent = computed(() => eventos.value.filter((evento) => evento.fechaInicioDate && evento.fechaInicioDate >= now.value).sort(sortByPriorityAndHour)[0] ?? null)
const nextEvents = computed(() => eventos.value.filter((evento) => evento.fechaInicioDate && evento.fechaInicioDate >= now.value).sort(sortByPriorityAndHour).slice(1, 6))

function itemStyle(evento) {
  const color = normalizeHexColor(evento.tipoColor) || '#61d6a7'
  return { borderColor: color }
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
  <main class="tv-page tv-screen tv-screen--next tv-proximo-evento">
    <header class="tv-header">
      <div>
        <p class="tv-eyebrow">Agenda escolar</p>
        <h1>Próximo evento</h1>
      </div>
      <div class="tv-clock">
        <strong>{{ formatLongDate(now) }}</strong>
        <span>Actualización automática cada 5 minutos</span>
      </div>
    </header>

    <section v-if="loading" class="tv-empty tv-empty--big">
      <p>Cargando evento próximo...</p>
    </section>

    <section v-else class="tv-content">
      <article v-if="nextEvent" class="tv-feature">
        <div class="tv-feature__main" :style="itemStyle(nextEvent)">
          <p class="tv-feature__label">Siguiente evento relevante</p>
          <h2>{{ nextEvent.titulo }}</h2>
          <div class="tv-feature__details">
            <p class="tv-feature__meta"><strong>Tipo:</strong> {{ nextEvent.tipoNombre }}</p>
            <p class="tv-feature__meta"><strong>Fecha:</strong> {{ formatLongDate(nextEvent.fechaInicioDate) }}</p>
            <p class="tv-feature__meta"><strong>Hora:</strong> {{ formatHour(nextEvent.fechaInicioDate) }}</p>
            <p class="tv-feature__meta"><strong>Lugar:</strong> {{ nextEvent.lugar }}</p>
          </div>
        </div>
      </article>

      <section v-if="nextEvents.length > 0" class="tv-secondary-block">
        <div class="tv-secondary-block__header">
          <p class="tv-feature__label">Próximos eventos</p>
          <span class="tv-secondary-block__count">{{ nextEvents.length }} disponibles</span>
        </div>

        <div class="tv-next-list">
          <article v-for="evento in nextEvents" :key="evento.id" class="tv-next-item" :style="itemStyle(evento)">
            <strong>{{ evento.titulo }}</strong>
            <span>{{ formatLongDate(evento.fechaInicioDate) }} · {{ formatHour(evento.fechaInicioDate) }} · {{ evento.lugar }}</span>
          </article>
        </div>
      </section>

      <div v-else-if="!nextEvent" class="tv-empty tv-empty--big">
        <p>No hay eventos próximos.</p>
      </div>
    </section>

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
  font-size: 1.5rem;
}

.tv-content {
  display: grid;
  gap: 18px;
  align-content: start;
}

.tv-feature {
  min-height: 0;
}

.tv-feature__main {
  background: rgba(255, 255, 255, 0.92);
  border-radius: 32px;
  border: 4px solid #61d6a7;
  padding: 24px 26px;
  display: grid;
  align-content: start;
  gap: 14px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.tv-feature__label {
  margin: 0;
  color: #0f766e;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-size: 1rem;
}

.tv-feature__main h2 {
  margin: 0;
  font-size: clamp(2.6rem, 3.1vw, 4.4rem);
  line-height: 1;
}

.tv-feature__details {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  align-items: start;
}

.tv-feature__meta {
  margin: 0;
  font-size: 1rem;
  line-height: 1.45;
  color: #334155;
}

.tv-feature__meta strong {
  color: #0f172a;
}

.tv-secondary-block {
  display: grid;
  gap: 16px;
}

.tv-secondary-block__header {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 16px;
}

.tv-secondary-block__count {
  color: #0f766e;
  font-size: 0.9rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.tv-next-list {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-items: start;
}

.tv-next-item {
  min-height: 124px;
  padding: 18px 18px;
  border-radius: 20px;
  border: 2px solid transparent;
  background: rgba(255, 255, 255, 0.88);
  display: grid;
  gap: 8px;
  align-content: start;
}

.tv-next-item strong {
  font-size: 1.28rem;
  line-height: 1.12;
}

.tv-next-item span {
  font-size: 1rem;
  color: #334155;
}

.tv-empty {
  min-height: 220px;
  display: grid;
  place-items: center;
  text-align: center;
  border-radius: 20px;
  border: 2px dashed rgba(15, 118, 110, 0.2);
  color: #475569;
  padding: 24px;
}

.tv-empty--big {
  min-height: calc(100vh - 170px);
}

.tv-empty p,
.tv-error {
  margin: 0;
  font-size: 1.4rem;
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

@media (max-width: 1100px) {
  .tv-screen {
    font-size: 28px;
  }

  .tv-feature__details {
    grid-template-columns: 1fr;
  }

  .tv-next-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .tv-next-list {
    grid-template-columns: 1fr;
  }

  .tv-feature__main {
    padding: 22px;
  }
}
</style>
