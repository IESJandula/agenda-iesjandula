# agenda-iesjandula — Especificaciones del proyecto

> Proyecto para el módulo de **Proyecto Interdisciplinar** del ciclo de DAW.
> Modalidad: **individual**.
> Repositorio: `agenda-iesjandula`.
> Centro: **IES Jándula**.
> Documento de especificaciones funcionales y técnicas.

---

## 1. Contexto

El centro educativo dispone de un calendario escolar en el que se publican los eventos que afectan al profesorado y al funcionamiento del instituto: festivos, claustros, sesiones de evaluación, actividades complementarias y extraescolares, movilidades Erasmus, inicio y fin de periodo, etc.

La versión actual es estática: está hardcodeada en una página web y cada cambio requiere tocar el código. Además, en un mismo día pueden convivir **3 o 4 eventos de distinto tipo** (por ejemplo, dos extraescolares + un Erasmus + un claustro), lo que obliga a un diseño que sepa representar bien la coincidencia y la prioridad.

El centro dispone también de una **TV de 65" en la sala de profesores** donde se proyecta el **panel de guardias** (aplicación interna ya existente, que consulta el profesorado 5-10 minutos antes de cada cambio de clase). La idea es aprovechar esa pantalla para mostrar también el calendario de eventos en los momentos en que no se necesita el panel de guardias.

## 2. Objetivo

Desarrollar una aplicación web que:

1. Permita a los **administradores** gestionar todos los eventos del curso escolar.
2. Permita al **profesorado** consultar el calendario y **proponer nuevos eventos** que el administrador podrá aprobar o rechazar.
3. Proporcione varias **vistas** adaptadas al dispositivo: móvil, PC y TV 65" 4K horizontal.
4. **Exponga vistas específicas para TV** (sin menús ni cabeceras) que el panel de guardias existente pueda embeber en la pantalla de la sala de profesores cuando lo decida (p. ej. durante el recreo).

## 3. Usuarios y roles

| Rol | Permisos |
|---|---|
| **Administrador** | Crear, editar y eliminar eventos y tipos de evento. Aprobar o rechazar propuestas del profesorado. Gestionar usuarios. Importar festivos desde CSV. |
| **Profesorado** | Consultar el calendario en todas sus vistas. Proponer nuevos eventos (quedan en estado *pendiente de aprobación*). |

No hay acceso anónimo. Todo usuario debe autenticarse para entrar.

## 4. Autenticación

- **Fase de desarrollo**: autenticación local sencilla (usuario + contraseña en base de datos, con hash). Suficiente para probar la aplicación durante el desarrollo sin depender de servicios externos.
- **Fase de producción**: migración a **Firebase Authentication** con login vía **Google** (las cuentas son del Google Workspace del centro). El rol (admin / profesorado) se asigna internamente a partir del email autenticado.

El diseño debe contemplar desde el inicio la abstracción del proveedor de autenticación para que la migración sea sencilla (p. ej., una capa `AuthService` que internamente use uno u otro).

## 5. Modelo de datos

### 5.1 Entidades

**Usuario**
- `id`
- `email` (único, obligatorio)
- `nombre`
- `rol` (`admin` | `profesorado`)
- `fecha_alta`

**TipoEvento**
- `id`
- `nombre` (p. ej. "Festivo", "Claustro", "Extraescolar", "Erasmus", "Inicio periodo")
- `color` (hex)
- `icono` (opcional)
- `prioridad` (entero, menor = más prioritario)
- `activo` (booleano)

Gestionable por el administrador (puede crear nuevos tipos o editar los existentes).

**Evento**
- `id`
- `tipo_id` → TipoEvento
- `titulo`
- `descripcion`
- `fecha_inicio` (fecha + hora opcional)
- `fecha_fin` (fecha + hora opcional; permite eventos de varios días)
- `lugar`
- `grupos_afectados` (texto o relación, p. ej. "1º DAW, 2º DAM")
- `responsables` (uno o varios profesores; relación n:m con Usuario)
- `enlace_documento` (URL opcional)
- `num_asistentes` (entero opcional)
- `estado` (`aprobado` | `pendiente` | `rechazado`)
- `creador_id` → Usuario
- `aprobador_id` → Usuario (nullable)
- `fecha_creacion`
- `fecha_aprobacion`

### 5.2 Notas sobre el modelo

- **No hay recurrencia**: los claustros mensuales se crean como eventos individuales.
- **Sí hay rangos multi-día** (p. ej. una semana Erasmus del 12 al 16 de mayo).
- **Los tipos son editables** por el admin; no van hardcodeados.
- **Solo se maneja el curso académico actual** (no hay histórico multi-curso en esta versión).

## 6. Requisitos funcionales

### RF-01 — Gestión de eventos (admin)
El administrador accede a un panel donde puede crear, editar y eliminar eventos mediante un formulario. Puede filtrar por tipo, mes y estado.

### RF-02 — Gestión de tipos de evento (admin)
CRUD de tipos de evento con nombre, color, icono y prioridad.

### RF-03 — Propuesta de eventos (profesorado)
Desde su perfil, el profesorado puede crear propuestas de evento con los mismos campos que un evento normal. La propuesta queda en estado `pendiente` y no se muestra en el calendario público hasta que se apruebe.

### RF-04 — Aprobación de propuestas (admin)
El admin dispone de una bandeja de entrada con las propuestas pendientes. Puede aprobar (pasa a `aprobado` y aparece en el calendario), rechazar (con motivo opcional) o editar antes de aprobar.

### RF-05 — Importación de festivos desde CSV
El admin sube un CSV al inicio del curso con los festivos oficiales. Formato sugerido:

```csv
fecha_inicio,fecha_fin,titulo,descripcion
2025-10-13,2025-10-13,Día de la Hispanidad,Festivo nacional
2025-12-22,2026-01-07,Vacaciones de Navidad,
```

La aplicación valida el CSV, muestra preview, y tras confirmar crea los eventos con el tipo "Festivo".

### RF-06 — Vista curso completo
Muestra los 10 meses del curso en una parrilla (p. ej. 3×4). Similar a la imagen de referencia: cada día con pastillas de color indicando los eventos. Al hacer clic sobre un día se abre un modal con el detalle de los eventos. Navegable entre cursos (en esta versión solo existe el curso actual, pero la UI lo contempla).

### RF-07 — Vista mensual
Mes ampliado con celdas más grandes, donde se ven los eventos con título (no solo color). Si en un día hay más de N eventos, muestra "+X más" y al clicar se expande.

### RF-08 — Vista agenda ("próximos eventos")
Lista cronológica de los próximos eventos del día y de los 7 días siguientes, agrupados por día. Es la vista pensada para la TV y también útil en móvil.

### RF-09 — Vista móvil
Diseño responsive: vista mensual compacta + agenda. Navegación con gestos (swipe entre meses).

### RF-10 — Vistas para TV embebibles
Ver sección 8.

### RF-11 — Priorización visual
Cuando en un día coinciden varios eventos, se ordenan visualmente según la `prioridad` del tipo. Orden por defecto (menor número = más prioritario):

1. Festivo
2. Claustro / Sesión de evaluación
3. Actividad extraescolar / complementaria
4. Erasmus
5. Inicio / fin de periodo escolar

La prioridad solo afecta al **orden** (el más prioritario se muestra primero y, si no caben todos, los de menos prioridad se agrupan en "+N"). No se ocultan eventos: todos son accesibles haciendo clic.

### RF-12 — Gestión de usuarios (admin)
Alta, baja y cambio de rol. En producción con Firebase, el alta se hace automáticamente con el primer login; el admin solo decide el rol.

## 7. Vistas y diseño

### 7.1 Dispositivos

| Dispositivo | Resolución aprox | Vista por defecto |
|---|---|---|
| Móvil | 360–480 px | Agenda + mensual compacta |
| PC / portátil | 1280–1920 px | Curso completo + mensual |
| TV 65" | 3840 × 2160 (4K horizontal) | Orquestador (ver §8) |

### 7.2 Representación de eventos coincidentes

Se propone el siguiente patrón en la vista mensual/curso:

- **Pastillas apiladas** de colores en cada día, con el título del evento de más prioridad.
- Si hay más de 3 eventos, las 2 primeras pastillas son visibles con título y la tercera es un contador `+N`.
- Al hacer clic sobre el día se abre un modal con el **listado completo** ordenado por prioridad.

La decisión concreta de representación queda abierta a iteración durante el desarrollo, pero debe cumplir: (a) legibilidad desde la TV a varios metros, (b) accesibilidad de contraste AA, (c) no ocultar nunca información.

### 7.3 Identidad visual

- Paleta de colores a definir con el centro, coherente con los tipos de evento.
- Tipografía legible y de alto contraste (pensando en TV).
- Modo claro por defecto; opcional modo oscuro en iteraciones posteriores.

## 8. Modo TV — Vistas embebibles

La TV de la sala de profesores muestra actualmente el panel de guardias (aplicación interna ya existente). Ese panel **es la aplicación principal** de la TV, porque el profesorado lo consulta en los cambios de clase para ver ausencias y sustituciones.

**El programa de guardias es quien decide** cuándo embeber o abrir las vistas de `agenda-iesjandula`. Por tanto, **esta aplicación no implementa un orquestador**: su responsabilidad es **exponer vistas limpias, embebibles y pensadas para TV**, que el panel de guardias pueda mostrar cuando lo estime oportuno (por ejemplo, durante el recreo).

### 8.1 Horario del centro (contexto de referencia)

Horario lectivo vigente en el IES Jándula:

| Tramo | Hora |
|---|---|
| 1.ª clase | 08:00 – 09:00 |
| 2.ª clase | 09:00 – 10:00 |
| 3.ª clase | 10:00 – 11:00 |
| **Recreo** | **11:00 – 11:30** |
| 4.ª clase | 11:30 – 12:30 |
| 5.ª clase | 12:30 – 13:30 |
| 6.ª clase | 13:30 – 14:30 |

Este horario **no lo gestiona esta aplicación** (es el panel de guardias el que decide cuándo llamar a nuestras vistas). Se incluye aquí solo como contexto para diseñar las vistas pensando en los momentos de uso más probables (recreo de 30 min, final de jornada, etc.).

### 8.2 Vistas que expone la aplicación

La aplicación debe ofrecer **URLs públicas "de pantalla"** (sin cabecera de navegación ni menús), diseñadas para ser cargadas por el panel de guardias en pantalla completa o en un iframe. Requieren autenticación mínima (o token de servicio) para que solo dispositivos del centro las carguen.

Rutas propuestas:

| Ruta | Contenido | Uso previsto |
|---|---|---|
| `/pantalla/hoy-y-proximos` | "Hoy" + "Próximos 7 días" en formato agenda | Recreo, huecos largos |
| `/pantalla/mes-actual` | Vista mensual grande del mes en curso | Primera semana del mes, cambios de mes |
| `/pantalla/curso` | Vista curso completo (los 10 meses) | Inicio de curso, planificación |
| `/pantalla/proximo-evento` | Destacado a pantalla completa del siguiente evento relevante | Antesala de un claustro, sesión de evaluación, salida Erasmus |

El panel de guardias elegirá cuál cargar en cada momento.

### 8.3 Requisitos de las vistas de pantalla

- **Sin chrome**: nada de barra de navegación, botones de usuario ni menús. Contenido a pantalla completa.
- **Resolución objetivo**: 3840 × 2160 (4K horizontal). Deben ser legibles desde 4–5 metros.
- **Tipografía**: tamaño base mínimo 32 px; títulos ≥ 60 px.
- **Contraste**: AAA siempre que sea posible, AA como mínimo.
- **Autoactualización**: cada vista refresca sus datos cada 5 minutos sin recargar la página (polling o websocket, a elección).
- **Sin audio** ni elementos que requieran interacción.
- **Animación discreta**: rotaciones o fades suaves cada 30 s para evitar "quemado" de píxeles (burn-in) en pantallas que permanecen mucho tiempo con la misma imagen.
- **Estado vacío**: si no hay eventos próximos, mostrar un mensaje informativo (no una pantalla en blanco).

### 8.4 Diseño de referencia de `/pantalla/hoy-y-proximos`

- **Cabecera**: fecha de hoy en grande + logo del IES Jándula + reloj.
- **Columna izquierda (~40 %)**: bloque **"HOY"** con los eventos del día ordenados por prioridad y hora. Tarjetas grandes con color de tipo, título, hora, lugar y responsable.
- **Columna derecha (~60 %)**: bloque **"PRÓXIMOS 7 DÍAS"** en agenda, agrupado por día de la semana.
- **Pie**: próximos festivos o periodos vacacionales.

## 9. Requisitos no funcionales

- **Responsive**: móvil, PC y TV 4K.
- **Accesibilidad**: contraste AA mínimo; navegación por teclado en admin.
- **Idioma**: español (único idioma soportado en esta versión).
- **Rendimiento**: las vistas `/pantalla/*` deben renderizar en <2 s sobre una conexión de red local del centro y no bloquear la interfaz en cada refresco.
- **Seguridad**: contraseñas con hash (bcrypt/argon2) en fase de desarrollo; en producción, tokens Firebase. Protección CSRF en formularios. Validación de entrada en backend.
- **Persistencia**: base de datos relacional (PostgreSQL o MySQL, a elección de la alumna).
- **Mantenibilidad**: separación clara entre capa de presentación, lógica y datos.

## 10. Stack tecnológico

El stack concreto lo **elige la alumna** según lo que haya visto en el ciclo y se sienta cómoda. Recomendaciones razonables:

- **Frontend**: Vue 3 + Nuxt, React + Next.js, o Astro + Vue/React.
- **Backend**: Node.js (NestJS / Express), Laravel (PHP) o Django (Python).
- **Base de datos**: PostgreSQL (preferible) o MySQL.
- **Auth**: sistema propio en desarrollo, Firebase Auth en producción.
- **Contenedorización**: Docker + docker-compose.
- **Despliegue**: **Coolify** (infraestructura del centro). Esto implica entregar un `Dockerfile` y `docker-compose.yml` funcionales.

## 11. Despliegue

El entorno de producción corre sobre **Coolify** en el servidor del centro. La alumna debe entregar:

- `Dockerfile` para frontend y backend (o imagen única si es aplicación full-stack).
- `docker-compose.yml` para desarrollo local.
- Variables de entorno documentadas (`.env.example`).
- Migraciones de base de datos reproducibles.
- Documento breve de despliegue en Coolify.

## 12. Entregables

1. **Repositorio Git** con código fuente y `README.md`.
2. **Documento de especificaciones** (este).
3. **Manual de usuario** (uno para admin, uno breve para profesorado).
4. **Manual de despliegue** en Coolify.
5. **Diagrama entidad-relación** y **diagrama de casos de uso**.
6. **Vídeo demo** de 3-5 minutos.
7. **Presentación** para la defensa del proyecto.

## 13. Fases sugeridas

| Fase | Contenido | Duración estimada |
|---|---|---|
| 1 — Análisis | Revisión de este documento, diagrama ER, mockups | 1-2 semanas |
| 2 — Backend base | Modelo, migraciones, CRUD de eventos y tipos, auth local | 2-3 semanas |
| 3 — Frontend base | Vista curso, vista mensual, login | 2-3 semanas |
| 4 — Propuestas y admin | Flujo de aprobación, gestión de usuarios, importación CSV | 1-2 semanas |
| 5 — Vistas para TV | Rutas `/pantalla/*`, diseño 4K, autoactualización | 1-2 semanas |
| 6 — Producción | Migración a Firebase Auth, despliegue en Coolify, pruebas | 1 semana |
| 7 — Documentación y defensa | Manuales, vídeo, presentación | 1 semana |

## 14. Fuera del alcance (versión 1)

Estas funciones **no** forman parte del proyecto en esta primera entrega; se documentan aquí para tenerlas como posibles ampliaciones futuras:

- Notificaciones por email o Telegram.
- Exportación a iCal / Google Calendar.
- Histórico multi-curso.
- Varios idiomas (gallego, inglés).
- Recurrencia automática de eventos (p. ej. "todos los primeros lunes de mes").
- Integración con el sistema de gestión académica del centro.
- Asistencia / confirmación por parte del profesorado.

## 15. Glosario

- **Claustro**: reunión de todo el profesorado del centro.
- **Sesión de evaluación**: reunión del equipo docente de un grupo para evaluar.
- **Actividad complementaria**: actividad educativa dentro del horario lectivo (p. ej. una charla).
- **Actividad extraescolar**: actividad voluntaria fuera del horario lectivo.
- **Erasmus**: movilidades europeas de alumnado y/o profesorado.
- **Panel de guardias**: aplicación interna del centro que muestra las ausencias del profesorado y las sustituciones.
