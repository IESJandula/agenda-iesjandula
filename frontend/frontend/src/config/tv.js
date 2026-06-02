const DEFAULT_TV_TOKEN = 'agenda-tv-dev'

function normalizeToken(value) {
  return typeof value === 'string' ? value.trim() : ''
}

export const TV_TOKEN = (() => {
  const configuredToken = normalizeToken(import.meta.env.VITE_TV_TOKEN)

  if (configuredToken) {
    return configuredToken
  }

  return import.meta.env.DEV ? DEFAULT_TV_TOKEN : ''
})()

export const TV_SCREEN_OPTIONS = [
  {
    id: 'hoy-proximos',
    label: 'Hoy y próximos 7 días',
    description: 'Muestra el resumen diario y la agenda de la próxima semana.',
    path: '/pantalla/hoy-y-proximos',
  },
  {
    id: 'mes-actual',
    label: 'Calendario mensual',
    description: 'Abre la vista mensual con los eventos distribuidos por día.',
    path: '/pantalla/mes-actual',
  },
  {
    id: 'curso-completo',
    label: 'Curso completo',
    description: 'Visualiza la agenda completa del curso en formato de pantalla.',
    path: '/pantalla/curso',
  },
  {
    id: 'proximo-evento',
    label: 'Próximo evento',
    description: 'Muestra el siguiente evento programado con el máximo detalle.',
    path: '/pantalla/proximo-evento',
  },
]

export function buildTvUrl(path) {
  if (!TV_TOKEN || typeof window === 'undefined') {
    return ''
  }

  const url = new URL(path, window.location.origin)
  url.searchParams.set('tvToken', TV_TOKEN)

  return `${url.origin}${url.pathname}${url.search}`
}

export async function copyUrlToClipboard(url) {
  if (!url) {
    throw new Error('No hay URL disponible para copiar.')
  }

  if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(url)
      return
    } catch {
      // Fallback below for browsers without clipboard permissions.
    }
  }

  if (typeof document === 'undefined') {
    throw new Error('No se pudo copiar la URL en este entorno.')
  }

  const textarea = document.createElement('textarea')
  textarea.value = url
  textarea.setAttribute('readonly', 'true')
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  textarea.style.pointerEvents = 'none'
  document.body.appendChild(textarea)
  textarea.focus()
  textarea.select()

  const copied = document.execCommand('copy')
  document.body.removeChild(textarea)

  if (!copied) {
    throw new Error('No se pudo copiar la URL.')
  }
}