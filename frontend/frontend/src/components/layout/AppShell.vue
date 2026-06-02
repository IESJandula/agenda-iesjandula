<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '../../stores/auth'

const props = defineProps({
  title: {
    type: String,
    default: 'Agenda IES Jándula',
  },
  subtitle: {
    type: String,
    default: '',
  },
  eyebrow: {
    type: String,
    default: 'Panel interno',
  },
})

const authStore = useAuthStore()
const router = useRouter()

const navigation = computed(() => [
  { label: 'Dashboard', name: 'dashboard' },
  { label: 'Agenda', name: 'agenda-proximos' },
  { label: 'Curso', name: 'curso-completo' },
  { label: 'Calendario', name: 'calendario-mes' },
  { label: 'Mis propuestas', name: 'mis-propuestas', roles: ['PROFESORADO'] },
  { label: 'Propuestas', name: 'propuestas-admin', roles: ['ADMIN'] },
  { label: 'Importar festivos', name: 'importar-festivos', roles: ['ADMIN'] },
  { label: 'Pantallas TV', name: 'pantallas-tv', roles: ['ADMIN'] },
  { label: 'Eventos', name: 'eventos' },
  { label: 'Tipos', name: 'admin-tipos' },
  { label: 'Usuarios', name: 'admin-usuarios' },
].filter((item) => !item.roles || item.roles.includes(authStore.rol)))

function handleLogout() {
  authStore.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="shell page-shell">
    <aside class="shell__sidebar">
      <div class="shell__sidebar-inner">
        <div class="shell__brand">
          <div class="shell__logo">AJ</div>
          <div class="shell__brand-text">
            <p class="shell__brand-name">Agenda</p>
            <p class="shell__brand-subtitle">IES Jándula</p>
          </div>
        </div>

        <nav class="shell__nav">
          <RouterLink
            v-for="item in navigation"
            :key="item.name"
            :to="{ name: item.name }"
            class="shell__nav-link"
            active-class="shell__nav-link--active"
          >
            {{ item.label }}
          </RouterLink>
        </nav>

        <section class="shell__session">
          <p class="shell__session-label">Sesión</p>
          <strong class="shell__session-name">{{ authStore.displayName }}</strong>
          <span class="shell__session-role">{{ authStore.roleLabel }}</span>
        </section>
      </div>

      <button class="shell__logout" type="button" @click="handleLogout">
        Salir
      </button>
    </aside>

    <main class="shell__main">
      <header class="shell__header">
        <div class="shell__header-content">
          <p class="eyebrow">{{ props.eyebrow }}</p>
          <h1 class="title">{{ props.title }}</h1>
          <p v-if="props.subtitle" class="subtitle">{{ props.subtitle }}</p>
        </div>

        <div class="shell__header-actions">
          <slot name="actions" />
        </div>
      </header>

      <section class="shell__content">
        <slot />
      </section>
    </main>
  </div>
</template>

<style scoped>
.shell {
  display: grid;
  grid-template-columns: 240px 1fr;
  min-height: 100vh;
  gap: 0;
}

.shell__sidebar {
  background: var(--bg-elevated);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  padding: 0;
  position: fixed;
  left: 0;
  top: 0;
  width: 240px;
  height: 100vh;
}

.shell__sidebar-inner {
  flex: 1;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 28px;
  overflow-y: auto;
}

.shell__brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.shell__logo {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  color: #ffffff;
  font-weight: 700;
  font-size: 1rem;
  background: var(--primary);
}

.shell__brand-text {
  display: grid;
  gap: 0;
}

.shell__brand-name,
.shell__brand-subtitle {
  margin: 0;
}

.shell__brand-name {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--text);
}

.shell__brand-subtitle {
  color: var(--muted);
  font-size: 0.75rem;
}

.shell__nav {
  display: grid;
  gap: 6px;
}

.shell__nav-link {
  display: block;
  padding: 0.75rem 0.9rem;
  border-radius: 8px;
  color: var(--text);
  background: transparent;
  border: 1px solid transparent;
  font-size: 0.9rem;
  font-weight: 500;
  transition: all 0.2s ease;
  text-decoration: none;
}

.shell__nav-link:hover {
  background: var(--bg-soft);
  color: var(--primary);
}

.shell__nav-link--active {
  color: #ffffff;
  background: var(--primary);
  font-weight: 600;
}

.shell__session {
  padding: 14px 12px;
  background: var(--bg-soft);
  border-radius: 8px;
  display: grid;
  gap: 4px;
}

.shell__session-label {
  margin: 0;
  color: var(--muted);
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-weight: 600;
}

.shell__session-name {
  margin: 0;
  color: var(--text);
  font-size: 0.9rem;
  font-weight: 600;
}

.shell__session-role {
  color: var(--muted);
  font-size: 0.8rem;
}

.shell__logout {
  margin: 0 16px 20px;
  padding: 0.85rem 1rem;
  border: 1px solid var(--border);
  background: var(--bg-soft);
  color: var(--text);
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 600;
  transition: all 0.2s ease;
  text-align: center;
  width: calc(100% - 32px);
}

.shell__logout:hover {
  color: var(--danger);
  border-color: var(--danger);
  background: rgba(220, 38, 38, 0.08);
}

.shell__logout:focus-visible {
  outline: 2px solid var(--primary);
  outline-offset: 2px;
}

.shell__main {
  grid-column: 2;
  background: var(--bg);
  display: flex;
  flex-direction: column;
}

.shell__header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 24px;
  padding: 40px 48px;
  border-bottom: 1px solid var(--border);
  background: var(--bg);
}

.shell__header-content {
  flex: 1;
}

.shell__header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.shell__content {
  flex: 1;
  padding: 40px 48px;
  display: grid;
  gap: 24px;
}

@media (max-width: 1100px) {
  .shell {
    grid-template-columns: 1fr;
  }

  .shell__sidebar {
    position: relative;
    width: 100%;
    height: auto;
    border-right: none;
    border-bottom: 1px solid var(--border);
  }

  .shell__sidebar-inner {
    flex-direction: row;
    padding: 16px;
    gap: 20px;
  }

  .shell__nav {
    flex-direction: row;
    gap: 8px;
  }

  .shell__session {
    display: none;
  }

  .shell__logout {
    display: none;
  }

  .shell__main {
    grid-column: 1;
  }

  .shell__header {
    padding: 32px 24px;
    flex-direction: column;
  }

  .shell__content {
    padding: 32px 24px;
  }
}

@media (max-width: 720px) {
  .shell__header {
    padding: 24px 16px;
  }

  .shell__content {
    padding: 24px 16px;
  }

  .shell__header-actions {
    width: 100%;
  }
}
</style>