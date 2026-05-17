<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import { createTipoMap, formatHour, formatLongDate, loadScreenData, normalizeEvento, normalizeHexColor, sortByDateAndPriority, statusClass, statusLabel, getTvTokenFromRoute, validateTvToken } from './pantallaUtils'

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
    error.value = requestError?.response?.data?.message || 'No se pudieron cargar los eventos.'
    eventos.value = []
  } finally {
    loading.value = false
    refresh()
  }
}

const nextEvent = computed(() => eventos.value.filter((evento) => evento.fechaInicioDate && evento.fechaInicioDate >= now.value).sort(sortByDateAndPriority)[0] ?? null)

const nextEvents = computed(() => eventos.value.filter((evento) => evento.fechaInicioDate && evento.fechaInicioDate >= now.value).sort(sortByDateAndPriority).slice(1, 6))

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
  <main class="tv-screen tv-screen--next">
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

    <section v-else>
      <article v-if="nextEvent" class="tv-feature">
        <div class="tv-feature__main" :style="itemStyle(nextEvent)">
          <p class="tv-feature__label">Siguiente evento relevante</p>
          <h2>{{ nextEvent.titulo }}</h2>
          <p class="tv-feature__meta">{{ nextEvent.tipoNombre }}</p>
          <p class="tv-feature__meta">{{ formatLongDate(nextEvent.fechaInicioDate) }}</p>
          <p class="tv-feature__meta">{{ formatHour(nextEvent.fechaInicioDate) }} · {{ nextEvent.lugar }}</p>
        </div>
        <aside class="tv-feature__status-wrap">
          <span class="tv-status tv-status--big" :class="statusClass(nextEvent.estado, 'tv-status')">{{ statusLabel(nextEvent.estado) }}</span>
        </aside>
      </article>

      <div v-if="nextEvents.length > 0" class="tv-next-list">
        <article v-for="evento in nextEvents" :key="evento.id" class="tv-next-item" :style="itemStyle(evento)">
          <strong>{{ evento.titulo }}</strong>
          <span>{{ formatLongDate(evento.fechaInicioDate) }} · {{ formatHour(evento.fechaInicioDate) }} · {{ evento.lugar }}</span>
        </article>
      </div>

      <div v-else-if="!nextEvent" class="tv-empty tv-empty--big">
        <p>No hay eventos próximos.</p>
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
  font-size: 1.5rem;
}

.tv-feature {
  height: calc(100vh - 280px);
  display: grid;
  grid-template-columns: 1.5fr 0.5fr;
  gap: 24px;
}

.tv-feature__main {
  background: rgba(255, 255, 255, 0.92);
  border-radius: 32px;
  border: 4px solid #61d6a7;
  padding: 36px;
  display: grid;
  align-content: center;
  gap: 18px;
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
  font-size: clamp(3.4rem, 4vw, 6.5rem);
  line-height: 1;
}

.tv-feature__meta {
  margin: 0;
  font-size: 1.6rem;
  color: #334155;
}

.tv-feature__status-wrap {
  display: grid;
  place-items: center;
}

.tv-status--big {
  font-size: 1.5rem;
  padding: 1rem 1.4rem;
}

.tv-next-list {
  margin-top: 24px;
  display: grid;
  gap: 14px;
}

.tv-next-item {
  padding: 16px 20px;
  border-radius: 22px;
  border: 2px solid transparent;
  background: rgba(255, 255, 255, 0.88);
  display: grid;
  gap: 6px;
}

.tv-next-item strong {
  font-size: 1.5rem;
}

.tv-next-item span {
  font-size: 1.05rem;
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

  .tv-feature {
    grid-template-columns: 1fr;
    height: auto;
  }
}
</style>