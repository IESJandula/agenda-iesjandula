<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppShell from '../components/layout/AppShell.vue'
import { createEvento, getEventoById, updateEvento } from '../api/eventos.api'
import { getTipos } from '../api/tipos.api'
import { getUsuarios } from '../api/usuarios.api'

const route = useRoute()
const router = useRouter()

const estados = ['PENDIENTE', 'CONFIRMADO', 'RECHAZADO', 'CANCELADO']
const editingId = computed(() => route.params.id)
const isEditMode = computed(() => Boolean(editingId.value))

const form = reactive({
  tipoId: '',
  creadorId: '',
  responsablesIds: [],
  titulo: '',
  descripcion: '',
  fechaInicio: '',
  fechaFin: '',
  lugar: '',
  gruposAfectados: '',
  enlaceDocumento: '',
  numAsistentes: '',
  estado: 'PENDIENTE',
})

const tipos = ref([])
const usuarios = ref([])
const eventoOriginal = ref(null)

const loadingCatalogs = ref(false)
const loadingEvento = ref(false)
const submitting = ref(false)
const formError = ref('')
const fieldErrors = ref({})

const canSubmit = computed(() => !loadingCatalogs.value && !loadingEvento.value && !submitting.value)
const tiposDisponibles = computed(() => {
  const visibles = tipos.value
    .filter((tipo) => {
      const nombreNormalizado = String(tipo?.nombre ?? '').trim().toLowerCase()
      return Boolean(tipo?.activo) && (tipo?.protegido || nombreNormalizado === 'otro')
    })
    .sort((tipoA, tipoB) => {
      const prioridadA = Number.isFinite(Number(tipoA?.prioridad)) ? Number(tipoA.prioridad) : Number.MAX_SAFE_INTEGER
      const prioridadB = Number.isFinite(Number(tipoB?.prioridad)) ? Number(tipoB.prioridad) : Number.MAX_SAFE_INTEGER

      if (prioridadA !== prioridadB) {
        return prioridadA - prioridadB
      }

      return String(tipoA?.nombre ?? '').localeCompare(String(tipoB?.nombre ?? ''), 'es', { sensitivity: 'base' })
    })

  if (!isEditMode.value || !eventoOriginal.value?.tipoId) {
    return visibles
  }

  const tipoActualId = String(eventoOriginal.value.tipoId)
  const existeTipoActual = visibles.some((tipo) => String(tipo.id) === tipoActualId)

  if (existeTipoActual) {
    return visibles
  }

  return [
    ...visibles,
    {
      id: eventoOriginal.value.tipoId,
      nombre: eventoOriginal.value.tipoNombre ?? 'Tipo actual',
      color: eventoOriginal.value.tipoColor ?? '#9CA3AF',
      prioridad: Number.isFinite(Number(eventoOriginal.value.tipoPrioridad)) ? Number(eventoOriginal.value.tipoPrioridad) : 99,
      activo: true,
      protegido: false,
      esTipoExistente: true,
    },
  ]
})
const viewTitle = computed(() => (isEditMode.value ? 'Editar evento' : 'Nuevo evento'))
const formTitle = computed(() => (isEditMode.value ? 'Editar evento' : 'Crear evento'))
const submitLabel = computed(() => {
  if (submitting.value) {
    return isEditMode.value ? 'Guardando cambios...' : 'Guardando...'
  }

  return isEditMode.value ? 'Guardar cambios' : 'Crear evento'
})
const viewSubtitle = computed(() =>
  isEditMode.value
    ? 'Actualiza los datos del evento y guarda los cambios en la agenda del centro.'
    : 'Completa el formulario para crear un evento en la agenda del centro.',
)

function normalizeCollection(response) {
  if (Array.isArray(response)) {
    return response
  }

  return response?.content ?? response?.data ?? []
}

function toLocalDateTime(value) {
  if (!value) {
    return null
  }

  return value.length === 16 ? `${value}:00` : value
}

function fromBackendDateTime(value) {
  if (!value) {
    return ''
  }

  const sanitizedValue = String(value).replace(' ', 'T')
  return sanitizedValue.slice(0, 16)
}

function patchFormWithEvento(evento) {
  eventoOriginal.value = evento ?? null
  form.tipoId = evento?.tipoId != null ? String(evento.tipoId) : ''
  form.creadorId = evento?.creadorId != null ? String(evento.creadorId) : ''
  form.responsablesIds = extractResponsablesIds(evento)
  form.titulo = evento?.titulo ?? ''
  form.descripcion = evento?.descripcion ?? ''
  form.fechaInicio = fromBackendDateTime(evento?.fechaInicio)
  form.fechaFin = fromBackendDateTime(evento?.fechaFin)
  form.lugar = evento?.lugar ?? ''
  form.gruposAfectados = evento?.gruposAfectados ?? ''
  form.enlaceDocumento = evento?.enlaceDocumento ?? ''
  form.numAsistentes =
    evento?.numAsistentes == null || Number.isNaN(Number(evento.numAsistentes))
      ? ''
      : String(evento.numAsistentes)
  form.estado = evento?.estado ?? 'PENDIENTE'
}

function extractResponsablesIds(evento) {
  if (Array.isArray(evento?.responsablesIds)) {
    return evento.responsablesIds.filter((id) => id != null).map((id) => String(id))
  }

  if (Array.isArray(evento?.responsables)) {
    return evento.responsables
      .map((responsable) => responsable?.id)
      .filter((id) => id != null)
      .map((id) => String(id))
  }

  return []
}

function toPayload() {
  return {
    tipoId: Number(form.tipoId),
    creadorId: Number(form.creadorId),
    responsablesIds: form.responsablesIds.map((id) => Number(id)),
    titulo: form.titulo.trim(),
    descripcion: form.descripcion.trim() || null,
    fechaInicio: toLocalDateTime(form.fechaInicio),
    fechaFin: toLocalDateTime(form.fechaFin),
    lugar: form.lugar.trim(),
    gruposAfectados: form.gruposAfectados.trim(),
    enlaceDocumento: form.enlaceDocumento.trim() || null,
    numAsistentes: form.numAsistentes === '' ? null : Number(form.numAsistentes),
    estado: form.estado,
  }
}

function resetErrors() {
  formError.value = ''
  fieldErrors.value = {}
}

async function loadEventoForEdit() {
  if (!isEditMode.value) {
    eventoOriginal.value = null
    return
  }

  loadingEvento.value = true
  formError.value = ''

  try {
    const evento = await getEventoById(editingId.value)
    patchFormWithEvento(evento)
  } catch (requestError) {
    formError.value = requestError?.response?.data?.message || 'No se pudo cargar el evento a editar.'
  } finally {
    loadingEvento.value = false
  }
}

async function loadCatalogs() {
  loadingCatalogs.value = true
  formError.value = ''

  try {
    const [tiposResponse, usuariosResponse] = await Promise.all([getTipos(), getUsuarios()])
    tipos.value = normalizeCollection(tiposResponse)
    usuarios.value = normalizeCollection(usuariosResponse)

    if (!form.tipoId && tiposDisponibles.value.length > 0) {
      form.tipoId = String(tiposDisponibles.value[0].id)
    }

    if (!form.creadorId && usuarios.value.length > 0) {
      form.creadorId = String(usuarios.value[0].id)
    }
  } catch (requestError) {
    formError.value = requestError?.response?.data?.message || 'No se pudieron cargar tipos y usuarios.'
  } finally {
    loadingCatalogs.value = false
  }
}

async function handleSubmit() {
  if (!canSubmit.value) {
    return
  }

  submitting.value = true
  resetErrors()

  try {
    if (isEditMode.value) {
      await updateEvento(editingId.value, toPayload())
    } else {
      await createEvento(toPayload())
    }

    await router.push('/eventos')
  } catch (requestError) {
    const responseData = requestError?.response?.data
    fieldErrors.value = responseData?.fieldErrors ?? {}
    formError.value = responseData?.message || 'No se pudo guardar el evento.'
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadCatalogs(), loadEventoForEdit()])
})
</script>

<template>
  <AppShell
    :title="viewTitle"
    eyebrow="Eventos"
    :subtitle="viewSubtitle"
  >
    <section class="panel form-panel">
      <div class="form-panel__header">
        <h2>{{ formTitle }}</h2>
        <p class="muted">Los campos marcados con * son obligatorios.</p>
      </div>

      <p v-if="formError" class="form-panel__error" role="alert">{{ formError }}</p>

      <p v-if="loadingCatalogs || loadingEvento" class="muted">Cargando datos del formulario...</p>

      <form v-else class="evento-form" @submit.prevent="handleSubmit">
        <label class="evento-form__field">
          <span>Tipo de evento *</span>
          <select v-model="form.tipoId" class="select" required>
            <option value="" disabled>Selecciona un tipo</option>
            <option v-for="tipo in tiposDisponibles" :key="tipo.id" :value="String(tipo.id)">
              {{ tipo.nombre }}
            </option>
          </select>
          <small class="evento-form__hint">Solo se muestran los tipos oficiales y la opción Otro.</small>
          <small v-if="fieldErrors.tipoId" class="evento-form__field-error">{{ fieldErrors.tipoId }}</small>
        </label>

        <label class="evento-form__field">
          <span>Creador *</span>
          <select v-model="form.creadorId" class="select" required>
            <option value="" disabled>Selecciona un usuario</option>
            <option v-for="usuario in usuarios" :key="usuario.id" :value="String(usuario.id)">
              {{ usuario.nombre }} ({{ usuario.email }})
            </option>
          </select>
          <small v-if="fieldErrors.creadorId" class="evento-form__field-error">{{ fieldErrors.creadorId }}</small>
        </label>

        <label class="evento-form__field evento-form__field--full">
          <span>Responsables</span>
          <select v-model="form.responsablesIds" class="select evento-form__multiselect" multiple size="6">
            <option v-for="usuario in usuarios" :key="usuario.id" :value="String(usuario.id)">
              {{ usuario.nombre }} ({{ usuario.email }})
            </option>
          </select>
          <small class="evento-form__hint">Mantén pulsado Ctrl o Cmd para seleccionar varios usuarios. Puedes dejarlo vacío.</small>
          <small v-if="fieldErrors.responsablesIds" class="evento-form__field-error">{{ fieldErrors.responsablesIds }}</small>
        </label>

        <label class="evento-form__field evento-form__field--full">
          <span>Título *</span>
          <input v-model="form.titulo" class="input" type="text" maxlength="100" required />
          <small v-if="fieldErrors.titulo" class="evento-form__field-error">{{ fieldErrors.titulo }}</small>
        </label>

        <label class="evento-form__field evento-form__field--full">
          <span>Descripción</span>
          <textarea v-model="form.descripcion" class="textarea" rows="4" maxlength="500"></textarea>
          <small v-if="fieldErrors.descripcion" class="evento-form__field-error">{{ fieldErrors.descripcion }}</small>
        </label>

        <label class="evento-form__field">
          <span>Fecha inicio</span>
          <input v-model="form.fechaInicio" class="input" type="datetime-local" step="1" />
          <small v-if="fieldErrors.fechaInicio" class="evento-form__field-error">{{ fieldErrors.fechaInicio }}</small>
        </label>

        <label class="evento-form__field">
          <span>Fecha fin</span>
          <input v-model="form.fechaFin" class="input" type="datetime-local" step="1" />
          <small v-if="fieldErrors.fechaFin" class="evento-form__field-error">{{ fieldErrors.fechaFin }}</small>
        </label>

        <label class="evento-form__field">
          <span>Lugar *</span>
          <input v-model="form.lugar" class="input" type="text" required />
          <small v-if="fieldErrors.lugar" class="evento-form__field-error">{{ fieldErrors.lugar }}</small>
        </label>

        <label class="evento-form__field">
          <span>Estado *</span>
          <select v-model="form.estado" class="select" required>
            <option v-for="estado in estados" :key="estado" :value="estado">
              {{ estado }}
            </option>
          </select>
          <small v-if="fieldErrors.estado" class="evento-form__field-error">{{ fieldErrors.estado }}</small>
        </label>

        <label class="evento-form__field evento-form__field--full">
          <span>Grupos afectados *</span>
          <input v-model="form.gruposAfectados" class="input" type="text" required />
          <small v-if="fieldErrors.gruposAfectados" class="evento-form__field-error">{{ fieldErrors.gruposAfectados }}</small>
        </label>

        <label class="evento-form__field evento-form__field--full">
          <span>Enlace documento</span>
          <input v-model="form.enlaceDocumento" class="input" type="url" placeholder="https://..." />
          <small v-if="fieldErrors.enlaceDocumento" class="evento-form__field-error">{{ fieldErrors.enlaceDocumento }}</small>
        </label>

        <label class="evento-form__field">
          <span>Número de asistentes</span>
          <input v-model="form.numAsistentes" class="input" type="number" min="1" step="1" />
          <small v-if="fieldErrors.numAsistentes" class="evento-form__field-error">{{ fieldErrors.numAsistentes }}</small>
        </label>

        <div class="evento-form__actions evento-form__field--full">
          <button class="btn btn--ghost" type="button" @click="router.push('/eventos')">Cancelar</button>
          <button class="btn btn--primary" type="submit" :disabled="!canSubmit">
            {{ submitLabel }}
          </button>
        </div>
      </form>
    </section>
  </AppShell>
</template>

<style scoped>
.form-panel {
  padding: 28px;
  display: grid;
  gap: 20px;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: 12px;
}

.form-panel__header h2 {
  margin: 0 0 6px;
  font-size: 1.3rem;
  font-weight: 600;
}

.form-panel__header p {
  margin: 0;
}

.form-panel__error {
  margin: 0;
  padding: 12px 14px;
  border-radius: 8px;
  background: #fee2e2;
  border: 1px solid #fecaca;
  color: #dc2626;
}

.evento-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.evento-form__field {
  display: grid;
  gap: 8px;
  color: var(--muted);
  font-weight: 500;
  font-size: 0.9rem;
}

.evento-form__field span {
  color: var(--text);
}

.evento-form__field--full {
  grid-column: 1 / -1;
}

.evento-form__field-error {
  color: #dc2626;
  font-size: 0.85rem;
}

.evento-form__hint {
  color: var(--muted);
  font-size: 0.8rem;
  line-height: 1.4;
}

.evento-form__multiselect {
  min-height: 180px;
}

.evento-form__actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  grid-column: 1 / -1;
}

@media (max-width: 860px) {
  .evento-form {
    grid-template-columns: 1fr;
  }

  .evento-form__actions {
    flex-direction: column;
  }

  .evento-form__actions button {
    width: 100%;
  }
}
</style>
