<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import AppShell from '../components/layout/AppShell.vue'
import { deleteEvento, getEventos } from '../api/eventos.api'

const router = useRouter()

const eventos = ref([])
const loading = ref(false)
const error = ref('')

const hasEventos = computed(() => eventos.value.length > 0)

function normalizeEvento(evento) {
  return {
    id: evento.id ?? evento.eventoId,
    titulo: evento.titulo ?? evento.nombre ?? evento.title ?? 'Sin título',
    tipo: evento.tipo?.nombre ?? evento.tipo?.descripcion ?? evento.tipo ?? '-',
    fechaInicio: evento.fechaInicio ?? evento.fecha_inicio ?? evento.startDate ?? '',
    fechaFin: evento.fechaFin ?? evento.fecha_fin ?? evento.endDate ?? '',
    lugar: evento.lugar ?? evento.ubicacion ?? evento.location ?? '-',
    estado: evento.estado ?? evento.estadoEvento ?? '-',
    motivoRechazo: evento.motivoRechazo ?? evento.motivo_rechazo ?? '',
    responsables: Array.isArray(evento.responsables) ? evento.responsables : [],
    responsablesIds: Array.isArray(evento.responsablesIds) ? evento.responsablesIds : [],
  }
}

function formatResponsables(evento) {
  const nombres = (evento.responsables ?? [])
    .map((responsable) => responsable?.nombre)
    .filter(Boolean)

  if (nombres.length === 0) {
    if (Array.isArray(evento.responsablesIds) && evento.responsablesIds.length > 0) {
      return `${evento.responsablesIds.length} responsables`
    }

    return '-'
  }

  if (nombres.length <= 2) {
    return nombres.join(', ')
  }

  return `${nombres.length} responsables`
}

function statusLabel(status) {
  const normalized = String(status || '').toUpperCase()

  if (normalized === 'PENDIENTE') return 'Pendiente'
  if (normalized === 'CONFIRMADO') return 'Confirmado'
  if (normalized === 'RECHAZADO') return 'Rechazado'
  if (normalized === 'CANCELADO') return 'Cancelado'
  return normalized || 'Sin estado'
}

function statusClass(status) {
  const normalized = String(status || '').toUpperCase()

  if (normalized === 'PENDIENTE') return 'evento-card__estado--warning'
  if (normalized === 'CONFIRMADO') return 'evento-card__estado--success'
  if (normalized === 'RECHAZADO' || normalized === 'CANCELADO') return 'evento-card__estado--danger'
  return 'evento-card__estado--neutral'
}

function formatDate(value) {
  if (!value) {
    return '-'
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat('es-ES', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

async function loadEventos() {
  loading.value = true
  error.value = ''

  try {
    const response = await getEventos()
    const rawEventos = Array.isArray(response) ? response : response?.content ?? response?.data ?? []
    eventos.value = rawEventos.map(normalizeEvento)
  } catch (requestError) {
    error.value = requestError?.response?.data?.message || 'No se pudieron cargar los eventos.'
    eventos.value = []
  } finally {
    loading.value = false
  }
}

async function handleDelete(id) {
  const confirmed = window.confirm('¿Seguro que quieres eliminar este evento?')

  if (!confirmed) {
    return
  }

  try {
    await deleteEvento(id)
    await loadEventos()
  } catch (requestError) {
    error.value = requestError?.response?.data?.message || 'No se pudo eliminar el evento.'
  }
}

function goToNewEvento() {
  router.push({ name: 'evento-nuevo' })
}

onMounted(loadEventos)
</script>

<template>
  <AppShell
    title="Eventos"
    eyebrow="Gestión de calendario"
    subtitle="Listado de eventos consumido desde tu backend Spring Boot con acciones de edición y borrado."
  >
    <section class="eventos-toolbar panel">
      <div>
        <p class="eyebrow">Listado</p>
        <h2>Eventos registrados</h2>
        <p class="muted">Título, tipo, fecha de inicio, fecha de fin, lugar y estado.</p>
      </div>

      <button class="btn btn--primary" type="button" @click="goToNewEvento">
        Nuevo evento
      </button>
    </section>

    <section class="eventos-state panel" v-if="loading">
      <p class="muted">Cargando eventos...</p>
    </section>

    <section class="eventos-state panel eventos-state--error" v-else-if="error">
      <p role="alert">{{ error }}</p>
      <button class="btn btn--ghost" type="button" @click="loadEventos">Reintentar</button>
    </section>

    <section class="eventos-state panel" v-else-if="!hasEventos">
      <p class="muted">No hay eventos disponibles todavía.</p>
    </section>

    <section v-else class="eventos-grid">
      <article v-for="evento in eventos" :key="evento.id" class="evento-card panel">
        <div class="evento-card__header">
          <div>
            <p class="evento-card__label">{{ evento.tipo }}</p>
            <h3 class="evento-card__title">{{ evento.titulo }}</h3>
          </div>

          <span class="evento-card__estado" :class="statusClass(evento.estado)">{{ statusLabel(evento.estado) }}</span>
        </div>

        <dl class="evento-card__details">
          <div>
            <dt>Fecha inicio</dt>
            <dd>{{ formatDate(evento.fechaInicio) }}</dd>
          </div>
          <div>
            <dt>Fecha fin</dt>
            <dd>{{ formatDate(evento.fechaFin) }}</dd>
          </div>
          <div>
            <dt>Lugar</dt>
            <dd>{{ evento.lugar }}</dd>
          </div>
        </dl>

        <p v-if="evento.motivoRechazo" class="evento-card__reason">
          Motivo de rechazo: {{ evento.motivoRechazo }}
        </p>

        <dl class="evento-card__details evento-card__details--responsables">
          <div>
            <dt>Responsables</dt>
            <dd>{{ formatResponsables(evento) }}</dd>
          </div>
        </dl>

        <div class="evento-card__actions">
          <RouterLink
            class="btn btn--ghost"
            :to="{ name: 'evento-editar', params: { id: evento.id } }"
          >
            Editar
          </RouterLink>
          <button class="btn btn--ghost evento-card__delete" type="button" @click="handleDelete(evento.id)">
            Eliminar
          </button>
        </div>
      </article>
    </section>
  </AppShell>
</template>

<style scoped>
.eventos-toolbar {
  padding: 24px 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: 12px;
}

.eventos-toolbar h2 {
  margin: 0 0 6px;
  font-size: 1.2rem;
  font-weight: 600;
}

.eventos-toolbar > div p {
  margin: 0;
}

.eventos-state {
  padding: 24px;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: 12px;
}

.eventos-state--error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: #fef2f2;
  border-color: #fecaca;
}

.eventos-state--error p {
  color: #dc2626;
}

.eventos-state p {
  margin: 0;
}

.eventos-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.evento-card {
  padding: 24px;
  display: grid;
  gap: 16px;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: 12px;
  transition: all 0.3s ease;
}

.evento-card:hover {
  border-color: var(--primary);
  box-shadow: 0 8px 20px rgba(97, 214, 167, 0.12);
}

.evento-card__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: start;
}

.evento-card__label {
  margin: 0 0 6px;
  color: var(--primary);
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.evento-card__title {
  margin: 0;
  font-size: 1.1rem;
  line-height: 1.3;
  font-weight: 600;
}

.evento-card__estado {
  padding: 0.4rem 0.7rem;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
}

.evento-card__estado--warning {
  background: #fef3c7;
  color: #92400e;
  border: 1px solid #fcd34d;
}

.evento-card__estado--success {
  background: #dcfce7;
  color: #166534;
  border: 1px solid #bbf7d0;
}

.evento-card__estado--danger {
  background: #fee2e2;
  color: #991b1b;
  border: 1px solid #fecaca;
}

.evento-card__estado--neutral {
  background: var(--bg-soft);
  color: #4b5563;
  border: 1px solid #e6f0eb;
}

.evento-card__reason {
  margin: 0;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fff7ed;
  border: 1px solid #fdba74;
  color: #9a3412;
  font-size: 0.9rem;
}

.evento-card__details {
  margin: 0;
  display: grid;
  gap: 12px;
}

.evento-card__details--responsables {
  gap: 8px;
}

.evento-card__details div {
  display: grid;
  gap: 4px;
}

.evento-card__details dt {
  color: var(--muted);
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-weight: 600;
}

.evento-card__details dd {
  margin: 0;
  color: var(--text);
  font-size: 0.95rem;
}

.evento-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 4px;
}

.evento-card__delete {
  border-color: #fecaca;
  color: #dc2626;
  background: #fef2f2;
}

.evento-card__delete:hover {
  background: #fee2e2;
}

@media (max-width: 720px) {
  .eventos-toolbar,
  .eventos-state--error {
    flex-direction: column;
    align-items: stretch;
  }

  .eventos-toolbar button,
  .eventos-state--error button,
  .evento-card__actions a,
  .evento-card__actions button {
    width: 100%;
  }
}
</style>