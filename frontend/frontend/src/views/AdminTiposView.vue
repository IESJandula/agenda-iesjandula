<script setup>
import { computed, onMounted, reactive, ref } from 'vue'

import AppShell from '../components/layout/AppShell.vue'
import { createTipo, deleteTipo, getTiposAdmin, updateTipo } from '../api/tipos.api'

const tipos = ref([])
const loading = ref(false)
const submitting = ref(false)
const formError = ref('')
const fieldErrors = ref({})

const editingId = ref(null)

const form = reactive({
  nombre: '',
  color: '#1f7ae0',
  icono: '',
  prioridad: 0,
  activo: true,
})

const isEditMode = computed(() => editingId.value !== null)
const editingTipo = computed(() => tipos.value.find((tipo) => String(tipo.id) === String(editingId.value)) ?? null)
const editingTipoProtegido = computed(() => Boolean(editingTipo.value?.protegido))
const submitLabel = computed(() => {
  if (submitting.value) {
    return isEditMode.value ? 'Guardando...' : 'Creando...'
  }

  return isEditMode.value ? 'Guardar cambios' : 'Crear tipo'
})

function normalizeCollection(response) {
  if (Array.isArray(response)) {
    return response
  }

  return response?.content ?? response?.data ?? []
}

function resetForm() {
  form.nombre = ''
  form.color = '#1f7ae0'
  form.icono = ''
  form.prioridad = 0
  form.activo = true
  editingId.value = null
  fieldErrors.value = {}
  formError.value = ''
}

function startEdit(tipo) {
  editingId.value = tipo.id
  form.nombre = tipo.nombre ?? ''
  form.color = tipo.color ?? '#1f7ae0'
  form.icono = tipo.icono ?? ''
  form.prioridad = Number.isFinite(tipo.prioridad) ? tipo.prioridad : 0
  form.activo = Boolean(tipo.activo)
  fieldErrors.value = {}
  formError.value = ''
}

function toPayload() {
  return {
    nombre: form.nombre.trim(),
    color: form.color.trim(),
    icono: form.icono.trim() || null,
    prioridad: Number(form.prioridad),
    activo: Boolean(form.activo),
  }
}

async function loadTipos() {
  loading.value = true
  formError.value = ''

  try {
    const response = await getTiposAdmin()
    tipos.value = normalizeCollection(response)
  } catch (requestError) {
    formError.value = requestError?.response?.data?.message || 'No se pudieron cargar los tipos.'
    tipos.value = []
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  submitting.value = true
  fieldErrors.value = {}
  formError.value = ''

  try {
    if (isEditMode.value) {
      await updateTipo(editingId.value, toPayload())
    } else {
      await createTipo(toPayload())
    }

    await loadTipos()
    resetForm()
  } catch (requestError) {
    const responseData = requestError?.response?.data
    fieldErrors.value = responseData?.fieldErrors ?? {}
    formError.value = responseData?.message || 'No se pudo guardar el tipo.'
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id) {
  const tipo = tipos.value.find((item) => String(item.id) === String(id)) ?? null

  if (tipo?.protegido) {
    formError.value = 'Los tipos oficiales y la opción Otro no se pueden eliminar.'
    return
  }

  const confirmed = window.confirm('¿Seguro que quieres eliminar este tipo de evento?')

  if (!confirmed) {
    return
  }

  try {
    await deleteTipo(id)

    if (editingId.value === id) {
      resetForm()
    }
    await loadTipos()
  } catch (requestError) {
    formError.value = requestError?.response?.data?.message || 'No se pudo eliminar el tipo.'
  }
}

onMounted(loadTipos)
</script>

<template>
  <AppShell
    title="Administrar tipos"
    eyebrow="Admin"
    subtitle="Catálogo de tipos de evento con operaciones completas de alta, edición y borrado."
  >
    <section class="panel tipos-form-panel">
      <div class="tipos-form-panel__header">
        <h2>{{ isEditMode ? 'Editar tipo' : 'Nuevo tipo' }}</h2>
        <button v-if="isEditMode" class="btn btn--ghost" type="button" @click="resetForm">
          Cancelar edición
        </button>
      </div>

      <p v-if="formError" class="tipos-form-panel__error" role="alert">{{ formError }}</p>

      <form class="tipos-form" @submit.prevent="handleSubmit">
        <label class="tipos-form__field">
          <span>Nombre *</span>
          <input v-model="form.nombre" class="input" type="text" maxlength="80" :disabled="editingTipoProtegido" required />
          <small v-if="editingTipoProtegido" class="tipos-form__field-error">Los tipos oficiales no se pueden renombrar.</small>
          <small v-if="fieldErrors.nombre" class="tipos-form__field-error">{{ fieldErrors.nombre }}</small>
        </label>

        <label class="tipos-form__field">
          <span>Color *</span>
          <div class="tipos-form__color">
            <input v-model="form.color" class="input" type="text" placeholder="#1F7AE0" required />
            <input v-model="form.color" class="tipos-form__color-picker" type="color" />
          </div>
          <small v-if="fieldErrors.color" class="tipos-form__field-error">{{ fieldErrors.color }}</small>
        </label>

        <label class="tipos-form__field">
          <span>Icono</span>
          <input v-model="form.icono" class="input" type="text" placeholder="calendar" />
          <small v-if="fieldErrors.icono" class="tipos-form__field-error">{{ fieldErrors.icono }}</small>
        </label>

        <label class="tipos-form__field">
          <span>Prioridad *</span>
          <input v-model.number="form.prioridad" class="input" type="number" min="0" step="1" required />
          <small v-if="fieldErrors.prioridad" class="tipos-form__field-error">{{ fieldErrors.prioridad }}</small>
        </label>

        <label class="tipos-form__field tipos-form__field--toggle">
          <input v-model="form.activo" type="checkbox" />
          <span>Activo</span>
          <small v-if="fieldErrors.activo" class="tipos-form__field-error">{{ fieldErrors.activo }}</small>
        </label>

        <div class="tipos-form__actions">
          <button class="btn btn--primary" type="submit" :disabled="submitting">{{ submitLabel }}</button>
        </div>
      </form>
    </section>

    <section class="panel tipos-list-panel">
      <header class="tipos-list-panel__header">
        <h2>Listado de tipos</h2>
        <button class="btn btn--ghost" type="button" @click="loadTipos" :disabled="loading">
          {{ loading ? 'Actualizando...' : 'Actualizar' }}
        </button>
      </header>

      <p v-if="loading" class="muted">Cargando tipos...</p>
      <p v-else-if="tipos.length === 0" class="muted">No hay tipos registrados todavía.</p>

      <div v-else class="tipos-table-wrap table-wrap">
        <table class="table tipos-table">
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Color</th>
              <th>Icono</th>
              <th>Prioridad</th>
              <th>Activo</th>
              <th>Protegido</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="tipo in tipos" :key="tipo.id">
              <td>{{ tipo.nombre }}</td>
              <td>
                <span class="tipos-table__color">
                  <span class="tipos-table__swatch" :style="{ backgroundColor: tipo.color }"></span>
                  {{ tipo.color }}
                </span>
              </td>
              <td>{{ tipo.icono || '-' }}</td>
              <td>{{ tipo.prioridad }}</td>
              <td>
                <span :class="tipo.activo ? 'tipos-table__badge tipos-table__badge--on' : 'tipos-table__badge'">
                  {{ tipo.activo ? 'Sí' : 'No' }}
                </span>
              </td>
              <td>
                <span :class="tipo.protegido ? 'tipos-table__badge tipos-table__badge--on' : 'tipos-table__badge'">
                  {{ tipo.protegido ? 'Sí' : 'No' }}
                </span>
              </td>
              <td>
                <div class="tipos-table__actions">
                  <button class="btn btn--ghost" type="button" @click="startEdit(tipo)">Editar</button>
                  <button class="btn btn--ghost tipos-table__delete" type="button" :disabled="tipo.protegido" @click="handleDelete(tipo.id)">
                    Eliminar
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </AppShell>
</template>

<style scoped>
.tipos-form-panel,
.tipos-list-panel {
  padding: 28px;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: 12px;
}

.tipos-form-panel {
  display: grid;
  gap: 20px;
}

.tipos-form-panel__header,
.tipos-list-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.tipos-form-panel__header h2,
.tipos-list-panel__header h2 {
  margin: 0;
  font-size: 1.3rem;
  font-weight: 600;
}

.tipos-form-panel__error {
  margin: 0;
  padding: 12px 14px;
  border-radius: 8px;
  background: #fee2e2;
  border: 1px solid #fecaca;
  color: #dc2626;
}

.tipos-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.tipos-form__field {
  display: grid;
  gap: 8px;
  color: var(--muted);
  font-weight: 500;
}

.tipos-form__field span {
  color: var(--text);
  font-size: 0.9rem;
}

.tipos-form__field--toggle {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tipos-form__field-error {
  color: #dc2626;
  font-size: 0.85rem;
}

.tipos-form__color {
  display: flex;
  gap: 10px;
  align-items: center;
}

.tipos-form__color-picker {
  width: 48px;
  height: 48px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-soft);
  cursor: pointer;
}

.tipos-form__actions {
  grid-column: 1 / -1;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.tipos-table-wrap {
  margin-top: 20px;
}

.tipos-table__color {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.tipos-table__swatch {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  border: 1px solid var(--border);
}

.tipos-table__actions {
  display: flex;
  gap: 8px;
}

.tipos-table__badge {
  display: inline-flex;
  padding: 0.35rem 0.7rem;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  background: var(--bg-soft);
  color: var(--muted);
}

.tipos-table__badge--on {
  background: #dff8ed;
  color: #047857;
}

.tipos-table__delete {
  border-color: #fecaca;
  color: #dc2626;
  background: #fef2f2;
}

.tipos-table__delete:hover {
  background: #fee2e2;
}

@media (max-width: 900px) {
  .tipos-form,
  .tipos-form__actions,
  .tipos-form-panel__header,
  .tipos-list-panel__header,
  .tipos-table__actions {
    display: flex;
    flex-direction: column;
    align-items: stretch;
  }

  .tipos-form {
    gap: 12px;
  }

  .tipos-form__actions button,
  .tipos-form-panel__header button,
  .tipos-list-panel__header button,
  .tipos-table__actions button {
    width: 100%;
  }
}
</style>
