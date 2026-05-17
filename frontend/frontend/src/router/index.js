import { createRouter, createWebHistory } from 'vue-router'

import { useAuthStore } from '../stores/auth'

const routes = [
  {
    path: '/',
    redirect: '/eventos',
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: {
      public: true,
    },
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('../views/DashboardView.vue'),
    meta: {
      requiresAuth: true,
      title: 'Dashboard',
    },
  },
  {
    path: '/pantalla/hoy-y-proximos',
    name: 'pantalla-hoy-proximos',
    component: () => import('../views/PantallaHoyProximosView.vue'),
    meta: {
      public: true,
      title: 'Pantalla hoy y próximos',
    },
  },
  {
    path: '/pantalla/mes-actual',
    name: 'pantalla-mes-actual',
    component: () => import('../views/PantallaMesActualView.vue'),
    meta: {
      public: true,
      title: 'Pantalla mes actual',
    },
  },
  {
    path: '/pantalla/curso',
    name: 'pantalla-curso',
    component: () => import('../views/PantallaCursoView.vue'),
    meta: {
      public: true,
      title: 'Pantalla curso',
    },
  },
  {
    path: '/pantalla/proximo-evento',
    name: 'pantalla-proximo-evento',
    component: () => import('../views/PantallaProximoEventoView.vue'),
    meta: {
      public: true,
      title: 'Pantalla próximo evento',
    },
  },
  {
    path: '/agenda',
    name: 'agenda-proximos',
    component: () => import('../views/AgendaProximosView.vue'),
    meta: {
      requiresAuth: true,
      title: 'Agenda',
    },
  },
  {
    path: '/mis-propuestas',
    name: 'mis-propuestas',
    component: () => import('../views/MisPropuestasView.vue'),
    meta: {
      requiresAuth: true,
      title: 'Mis propuestas',
    },
  },
  {
    path: '/admin/propuestas',
    name: 'propuestas-admin',
    component: () => import('../views/PropuestasAdminView.vue'),
    meta: {
      requiresAuth: true,
      roles: ['ADMIN'],
      title: 'Propuestas pendientes',
    },
  },
  {
    path: '/calendario/curso',
    name: 'curso-completo',
    component: () => import('../views/CursoCompletoView.vue'),
    meta: {
      requiresAuth: true,
      title: 'Curso completo',
    },
  },
  {
    path: '/calendario/mes',
    name: 'calendario-mes',
    component: () => import('../views/CalendarioMensualView.vue'),
    meta: {
      requiresAuth: true,
      title: 'Calendario mensual',
    },
  },
  {
    path: '/eventos',
    name: 'eventos',
    component: () => import('../views/EventosView.vue'),
    meta: {
      requiresAuth: true,
      title: 'Eventos',
    },
  },
  {
    path: '/eventos/nuevo',
    name: 'evento-nuevo',
    component: () => import('../views/EventoFormView.vue'),
    meta: {
      requiresAuth: true,
      title: 'Nuevo evento',
    },
  },
  {
    path: '/eventos/:id/editar',
    name: 'evento-editar',
    component: () => import('../views/EventoFormView.vue'),
    meta: {
      requiresAuth: true,
      title: 'Editar evento',
    },
    props: true,
  },
  {
    path: '/admin/tipos',
    name: 'admin-tipos',
    component: () => import('../views/AdminTiposView.vue'),
    meta: {
      requiresAuth: true,
      roles: ['ADMIN'],
      title: 'Tipos',
    },
  },
  {
    path: '/admin/usuarios',
    name: 'admin-usuarios',
    component: () => import('../views/AdminUsuariosView.vue'),
    meta: {
      requiresAuth: true,
      roles: ['ADMIN'],
      title: 'Usuarios',
    },
  },
  {
    path: '/admin/importar-festivos',
    name: 'importar-festivos',
    component: () => import('../views/ImportarFestivosView.vue'),
    meta: {
      requiresAuth: true,
      roles: ['ADMIN'],
      title: 'Importar festivos',
    },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)
  const requiredRoles = to.matched.flatMap((record) => record.meta.roles ?? [])

  if (requiresAuth && !authStore.isAuthenticated) {
    return {
      name: 'login',
      query: {
        redirect: to.fullPath,
      },
    }
  }

  if (requiredRoles.length > 0 && !requiredRoles.includes(authStore.rol)) {
    return { name: 'eventos' }
  }

  if (to.name === 'login' && authStore.isAuthenticated) {
    return { name: 'eventos' }
  }

  return true
})

export default router