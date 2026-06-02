<script setup>
import { computed, ref } from 'vue'

import AppShell from '../components/layout/AppShell.vue'
import { TV_SCREEN_OPTIONS, TV_TOKEN, buildTvUrl, copyUrlToClipboard } from '../config/tv'

const feedbackMessage = ref('')
const feedbackTone = ref('info')
const copyLoadingId = ref('')

const screens = computed(() =>
  TV_SCREEN_OPTIONS.map((screen) => {
    const url = buildTvUrl(screen.path)

    return {
      ...screen,
      url,
      canOpen: Boolean(url),
    }
  }),
)

const hasTvToken = computed(() => Boolean(TV_TOKEN))

function openTv(url) {
  if (!url) {
    feedbackTone.value = 'error'
    feedbackMessage.value = 'Falta configurar el token TV para abrir esta pantalla.'
    return
  }

  window.open(url, '_blank', 'noopener,noreferrer')
  feedbackTone.value = 'success'
  feedbackMessage.value = 'La pantalla TV se ha abierto en una nueva pestaña.'
}

async function copyUrl(screen) {
  if (!screen.url) {
    feedbackTone.value = 'error'
    feedbackMessage.value = 'Falta configurar el token TV para copiar la URL.'
    return
  }

  copyLoadingId.value = screen.id

  try {
    await copyUrlToClipboard(screen.url)
    feedbackTone.value = 'success'
    feedbackMessage.value = 'URL copiada al portapapeles.'
  } catch (error) {
    feedbackTone.value = 'error'
    feedbackMessage.value = error?.message || 'No se pudo copiar la URL.'
  } finally {
    copyLoadingId.value = ''
  }
}

function statusClass() {
  return feedbackTone.value === 'error' ? 'tv-notice--error' : 'tv-notice--success'
}
</script>

<template>
  <AppShell
    title="Pantallas TV"
    eyebrow="Admin"
    subtitle="Selecciona una vista para abrirla en modo pantalla completa o copiar su URL para usarla en una televisión informativa."
  >
    <section class="tv-admin-hero panel">
      <div>
        <p class="eyebrow">Modo TV</p>
        <h2>Selecciona una vista para abrirla en modo pantalla completa.</h2>
        <p class="muted">
          Estas vistas están pensadas para mostrarse en pantallas informativas del centro.
        </p>
      </div>

      <p v-if="!hasTvToken" class="tv-warning" role="alert">
        No hay un token TV configurado para este entorno. Define <strong>VITE_TV_TOKEN</strong> en producción.
      </p>

      <p v-else-if="feedbackMessage" class="tv-notice" :class="statusClass()" role="status">
        {{ feedbackMessage }}
      </p>
    </section>

    <section class="tv-grid" aria-label="Pantallas TV disponibles">
      <article v-for="screen in screens" :key="screen.id" class="tv-card panel">
        <div class="tv-card__header">
          <div>
            <p class="tv-card__eyebrow">Pantalla TV</p>
            <h3>{{ screen.label }}</h3>
          </div>
          <span class="tv-card__badge">TV</span>
        </div>

        <p class="tv-card__description muted">{{ screen.description }}</p>

        <div class="tv-card__url">
          <span class="tv-card__url-label">URL</span>
          <span class="tv-card__url-value">{{ screen.url || 'Configura VITE_TV_TOKEN para generar la URL' }}</span>
        </div>

        <div class="tv-card__actions">
          <button class="btn btn--primary" type="button" :disabled="!screen.canOpen" @click="openTv(screen.url)">
            Abrir en modo TV
          </button>
          <button
            class="btn btn--ghost"
            type="button"
            :disabled="!screen.canOpen || copyLoadingId === screen.id"
            @click="copyUrl(screen)"
          >
            {{ copyLoadingId === screen.id ? 'Copiando...' : 'Copiar URL' }}
          </button>
        </div>
      </article>
    </section>
  </AppShell>
</template>

<style scoped>
.tv-admin-hero {
  padding: 28px;
  display: grid;
  gap: 16px;
  border-radius: 12px;
}

.tv-admin-hero h2 {
  margin: 0;
  font-size: 1.6rem;
  letter-spacing: -0.02em;
}

.tv-warning,
.tv-notice {
  margin: 0;
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid transparent;
  line-height: 1.5;
}

.tv-warning {
  color: #92400e;
  background: #fffbeb;
  border-color: #fcd34d;
}

.tv-notice--success {
  color: #065f46;
  background: #ecfdf5;
  border-color: #a7f3d0;
}

.tv-notice--error {
  color: #991b1b;
  background: #fef2f2;
  border-color: #fecaca;
}

.tv-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 20px;
}

.tv-card {
  padding: 24px;
  display: grid;
  gap: 16px;
  border-radius: 12px;
}

.tv-card__header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
}

.tv-card__eyebrow {
  margin: 0 0 6px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--muted);
}

.tv-card h3 {
  margin: 0;
  font-size: 1.2rem;
  line-height: 1.25;
}

.tv-card__badge {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(97, 214, 167, 0.14);
  color: var(--primary-strong);
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.tv-card__description {
  margin: 0;
  min-height: 3em;
}

.tv-card__url {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 10px;
  background: var(--bg-soft);
  border: 1px solid var(--border);
}

.tv-card__url-label {
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--muted);
}

.tv-card__url-value {
  word-break: break-all;
  font-size: 0.92rem;
  line-height: 1.5;
  color: var(--text);
}

.tv-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.tv-card__actions .btn {
  flex: 1 1 180px;
}

@media (max-width: 720px) {
  .tv-admin-hero,
  .tv-card {
    padding: 20px;
  }

  .tv-card__actions .btn {
    flex-basis: 100%;
  }
}
</style>