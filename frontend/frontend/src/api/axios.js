import axios from 'axios'

const STORAGE_KEY = 'agenda-iesjandula-auth'

export const apiBaseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8081'

function readStoredSession() {
  if (typeof window === 'undefined') {
    return null
  }

  const rawSession = window.localStorage.getItem(STORAGE_KEY)

  if (!rawSession) {
    return null
  }

  try {
    return JSON.parse(rawSession)
  } catch {
    return null
  }
}

function clearStoredSession() {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.removeItem(STORAGE_KEY)
}

function getStoredToken() {
  const storedSession = readStoredSession()
  return {
    token: storedSession?.token ?? '',
    tokenType: storedSession?.tokenType ?? 'Bearer',
  }
}

export const api = axios.create({
  baseURL: apiBaseUrl,
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use((config) => {
  const { token, tokenType } = getStoredToken()

  if (token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `${tokenType} ${token}`.trim()
  }

  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status

    if (status === 401 || status === 403) {
      clearStoredSession()

      if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
        window.location.replace('/login')
      }
    }

    return Promise.reject(error)
  },
)

export function setAuthToken(token, tokenType = 'Bearer') {
  if (token) {
    api.defaults.headers.common.Authorization = `${tokenType} ${token}`.trim()
    return
  }

  delete api.defaults.headers.common.Authorization
}

export default api