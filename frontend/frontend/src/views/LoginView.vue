<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '../stores/auth'
import logoIes from '../assets/image.png'

const authStore = useAuthStore()
const router = useRouter()

const form = reactive({
  email: '',
  password: '',
})

const localError = ref('')

async function handleSubmit() {
  localError.value = ''

  try {
    await authStore.login(form.email, form.password)

    await router.push('/eventos')
  } catch {
    localError.value = authStore.error || 'Revisa tus credenciales'
  }
}
</script>

<template>
  <main class="login-page">
    <header class="login-page__header" aria-label="Cabecera institucional">
      <div class="login-page__brand">
        <div class="login-page__brand-copy">
          <p class="login-page__brand-name">IES Jándula</p>
          <p class="login-page__brand-subtitle">Agenda Escolar</p>
        </div>
      </div>

      <div class="login-page__logo" aria-hidden="true">
        <img class="institution-logo" :src="logoIes" alt="Logo oficial del IES Jándula" />
        <div class="login-page__logo-text">
          <span>Centro educativo</span>
          <strong>Acceso institucional</strong>
        </div>
      </div>
    </header>

    <section class="login-page__content">
      <div class="login-page__intro" aria-hidden="true">
        <div class="login-page__icon">
          <svg viewBox="0 0 80 80" role="img" aria-hidden="true">
            <circle cx="40" cy="26" r="12"></circle>
            <path d="M18 62c2.8-14.8 14-22 22-22s19.2 7.2 22 22" fill="none" stroke-linecap="round"></path>
            <rect x="8" y="8" width="64" height="64" rx="18" fill="none" stroke-linecap="round"></rect>
          </svg>
        </div>

        <p class="login-page__eyebrow">Acceso privado</p>
        <h1 class="login-page__title">Inicia sesión</h1>
        <p class="login-page__subtitle">
          Gestiona los eventos del centro educativo con una experiencia limpia, profesional y segura.
        </p>
      </div>

      <section class="login-page__card" aria-labelledby="login-title">
        <div class="login-page__card-header">
          <p class="login-page__card-kicker">Acceso seguro</p>
          <h2 id="login-title">Bienvenido a la agenda</h2>
          <p class="login-page__card-subtitle">
            Introduce tus credenciales institucionales para continuar.
          </p>
        </div>

        <form class="login-page__form" @submit.prevent="handleSubmit">
          <label class="login-page__field" for="login-email">
            <span>Email</span>
            <input
              id="login-email"
              v-model="form.email"
              class="login-page__input"
              type="email"
              placeholder="profesor@iesjandula.es"
              autocomplete="email"
              required
            />
          </label>

          <label class="login-page__field" for="login-password">
            <span>Contraseña</span>
            <input
              id="login-password"
              v-model="form.password"
              class="login-page__input"
              type="password"
              placeholder="••••••••"
              autocomplete="current-password"
              required
            />
          </label>

          <p v-if="localError" class="login-page__error" role="alert">{{ localError }}</p>

          <button class="login-page__submit" type="submit" :disabled="authStore.loading">
            {{ authStore.loading ? 'Entrando...' : 'Entrar' }}
          </button>
        </form>
      </section>
    </section>

    <footer class="login-page__footer">
      <span>IES Jándula · Agenda Escolar</span>
    </footer>
  </main>
</template>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  padding: 96px 32px 40px;
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 28px;
  background:
    radial-gradient(circle at top left, rgba(97, 214, 167, 0.12), transparent 24%),
    radial-gradient(circle at bottom right, rgba(97, 214, 167, 0.08), transparent 22%),
    linear-gradient(180deg, #fbfdfc 0%, #f4fbf7 100%);
  color: var(--text);
  overflow: hidden;
}

.login-page::before,
.login-page::after {
  content: '';
  position: absolute;
  inset: auto;
  border-radius: 999px;
  pointer-events: none;
  opacity: 0.75;
}

.login-page::before {
  width: 320px;
  height: 320px;
  top: -110px;
  right: -90px;
  background: rgba(97, 214, 167, 0.08);
}

.login-page::after {
  width: 240px;
  height: 240px;
  bottom: -100px;
  left: -70px;
  background: rgba(15, 118, 110, 0.06);
}

.login-page__header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 80px;
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  box-shadow: 0 8px 28px rgba(15, 23, 42, 0.06);
  z-index: 2;
}

.login-page__brand {
  display: flex;
  align-items: center;
  gap: 16px;
}

.login-page__brand-copy {
  display: grid;
  gap: 2px;
}

.login-page__brand-name {
  margin: 0;
  font-size: 1.08rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #0f172a;
}

.login-page__brand-subtitle {
  margin: 0;
  font-size: 0.85rem;
  color: var(--muted);
}

.login-page__logo {
  display: flex;
  align-items: center;
  gap: 14px;
  color: #334155;
}

.institution-logo {
  height: 56px;
  width: auto;
  object-fit: contain;
  display: block;
  flex-shrink: 0;
}

.login-page__logo-text {
  display: grid;
  gap: 2px;
  text-align: right;
}

.login-page__logo-text span {
  font-size: 0.72rem;
  color: var(--muted);
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.login-page__logo-text strong {
  font-size: 0.9rem;
  color: #0f172a;
}

.login-page__content {
  width: min(100%, 1180px);
  margin: 0 auto;
  align-self: center;
  display: grid;
  justify-items: center;
  gap: 30px;
  text-align: center;
  position: relative;
  z-index: 1;
}

.login-page__intro {
  width: min(100%, 760px);
  display: grid;
  justify-items: center;
  gap: 16px;
}

.login-page__icon {
  width: 92px;
  height: 92px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(97, 214, 167, 0.24);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.08);
  display: grid;
  place-items: center;
  color: #0f766e;
}

.login-page__icon svg {
  width: 48px;
  height: 48px;
  stroke: currentColor;
  stroke-width: 2.2;
  fill: currentColor;
  opacity: 0.9;
}

.login-page__eyebrow {
  margin: 0;
  font-size: 0.85rem;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: #0f766e;
  text-transform: uppercase;
}

.login-page__title {
  margin: 0;
  font-size: clamp(3rem, 4.6vw, 3.5rem);
  line-height: 1.05;
  letter-spacing: -0.05em;
  font-weight: 700;
  color: #0f172a;
}

.login-page__subtitle {
  margin: 0;
  max-width: 58ch;
  color: var(--muted);
  font-size: 1.1rem;
  line-height: 1.7;
}

.login-page__card {
  width: min(100%, 500px);
  padding: 34px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 28px;
  box-shadow: 0 22px 60px rgba(15, 23, 42, 0.1);
  backdrop-filter: blur(8px);
  text-align: left;
  transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease;
}

.login-page__card:hover {
  transform: translateY(-2px);
  box-shadow: 0 28px 72px rgba(15, 23, 42, 0.12);
  border-color: rgba(97, 214, 167, 0.26);
}

.login-page__card-header {
  display: grid;
  gap: 8px;
  margin-bottom: 26px;
}

.login-page__card-kicker {
  margin: 0;
  font-size: 0.82rem;
  font-weight: 700;
  color: #0f766e;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.login-page__card h2 {
  margin: 0;
  font-size: 1.85rem;
  line-height: 1.12;
  letter-spacing: -0.03em;
  color: #0f172a;
}

.login-page__card-subtitle {
  margin: 0;
  color: var(--muted);
  font-size: 0.95rem;
  line-height: 1.6;
}

.login-page__form {
  display: grid;
  gap: 16px;
}

.login-page__field {
  display: grid;
  gap: 8px;
}

.login-page__field span {
  color: #0f172a;
  font-size: 0.875rem;
  font-weight: 600;
}

.login-page__input {
  width: 100%;
  min-height: 54px;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.42);
  background: #fff;
  color: #0f172a;
  font-size: 1rem;
  transition: border-color 160ms ease, box-shadow 160ms ease, transform 160ms ease, background-color 160ms ease;
}

.login-page__input::placeholder {
  color: #94a3b8;
}

.login-page__input:hover {
  border-color: rgba(97, 214, 167, 0.48);
}

.login-page__input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 4px rgba(97, 214, 167, 0.18);
  background: #fcfffd;
}

.login-page__input:focus-visible {
  outline: none;
}

.login-page__error {
  margin: 0;
  padding: 12px 14px;
  border-radius: 14px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #b91c1c;
  font-size: 0.92rem;
  line-height: 1.5;
}

.login-page__submit {
  margin-top: 8px;
  width: 100%;
  min-height: 54px;
  border: 1px solid transparent;
  border-radius: 16px;
  background: var(--primary);
  color: #ffffff;
  font-size: 1rem;
  font-weight: 700;
  letter-spacing: 0.01em;
  box-shadow: 0 14px 24px rgba(97, 214, 167, 0.26);
  transition: transform 160ms ease, box-shadow 160ms ease, background-color 160ms ease, opacity 160ms ease;
}

.login-page__submit:hover:not(:disabled) {
  background: var(--primary-strong);
  transform: translateY(-1px);
  box-shadow: 0 18px 28px rgba(97, 214, 167, 0.32);
}

.login-page__submit:active:not(:disabled) {
  transform: translateY(0);
}

.login-page__submit:disabled {
  cursor: not-allowed;
  opacity: 0.7;
  transform: none;
  box-shadow: none;
}

.login-page__submit:focus-visible {
  outline: 3px solid rgba(97, 214, 167, 0.28);
  outline-offset: 3px;
}

.login-page__footer {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: center;
  color: var(--muted);
  font-size: 0.88rem;
  text-align: center;
}

@media (max-width: 920px) {
  .login-page {
    padding: 88px 20px 28px;
  }

  .login-page__header {
    padding: 0 20px;
  }

  .login-page__logo-text {
    display: none;
  }

  .institution-logo {
    height: 48px;
  }

  .login-page__title {
    font-size: clamp(2.6rem, 8vw, 3.4rem);
  }

  .login-page__subtitle {
    font-size: 1rem;
  }

  .login-page__card {
    padding: 28px 22px;
    border-radius: 24px;
  }
}

@media (max-width: 560px) {
  .login-page {
    padding: 84px 16px 20px;
  }

  .login-page__header {
    height: 72px;
    padding: 0 16px;
  }

  .login-page__brand-name {
    font-size: 1rem;
  }

  .login-page__brand-subtitle {
    font-size: 0.8rem;
  }

  .institution-logo {
    height: 40px;
  }

  .login-page__icon {
    width: 80px;
    height: 80px;
    border-radius: 24px;
  }

  .login-page__icon svg {
    width: 42px;
    height: 42px;
  }

  .login-page__title {
    font-size: clamp(2.25rem, 11vw, 3rem);
  }

  .login-page__card h2 {
    font-size: 1.55rem;
  }

  .login-page__input,
  .login-page__submit {
    min-height: 52px;
    border-radius: 14px;
  }
}
</style>
