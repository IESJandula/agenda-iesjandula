# 🏢 Validaciones de Negocio en Service Layer - Spring Boot

## 📋 Resumen

Se han implementado validaciones de negocio profesionales en `EventoService` siguiendo patrones enterprise. Las validaciones se aplican **antes de persistir** en la base de datos, garantizando integridad de datos.

---

## 🎯 Reglas de Negocio Implementadas

### **1. Título del Evento - OBLIGATORIO Y VÁLIDO**

```java
private void validarTitulo(String titulo) {
    if (titulo == null || titulo.trim().isEmpty()) {
        throw new BusinessException("TITULO_VACIO", 
            "El título del evento es obligatorio y no puede estar vacío");
    }
    
    if (titulo.trim().length() < 3) {
        throw new BusinessException("TITULO_CORTO", 
            "El título debe tener al menos 3 caracteres");
    }
}
```

**Cuándo se activa:**
- ❌ Título nulo o vacío
- ❌ Título solo espacios en blanco
- ❌ Título menos de 3 caracteres

**Excepción lanzada:** `BusinessException`  
**Código HTTP:** 400 Bad Request

---

### **2. Tipo de Evento - DEBE EXISTIR Y ESTAR ACTIVO**

```java
private TipoEvento validarYObtenerTipoEvento(Long tipoId) {
    TipoEvento tipo = tipoEventoRepository.findById(tipoId)
        .orElseThrow(() -> new ResourceNotFoundException("TipoEvento", "id", tipoId));
    
    if (!tipo.isActivo()) {
        throw new BusinessException("TIPO_EVENTO_INACTIVO", 
            String.format("No se puede usar el tipo '%s' porque está inactivo", 
                tipo.getNombre()));
    }
    
    return tipo;
}
```

**Validaciones:**
1. ✅ TipoEvento existe en BD
2. ✅ TipoEvento.activo = true

**Excepciones posibles:**
- `ResourceNotFoundException` (404) - Si tipoId no existe
- `BusinessException` (400) - Si está inactivo

**Escenarios:**
```json
// ❌ Tipo no existe
{
  "tipoId": 999
}
→ HTTP 404: "TipoEvento no encontrado con id: 999"

// ❌ Tipo existe pero está inactivo
{
  "tipoId": 1,  // Existe pero activo=false
  ...
}
→ HTTP 400: "No se puede usar el tipo porque está inactivo"
```

---

### **3. Usuario Creador - DEBE EXISTIR**

```java
private Usuario validarYObtenerUsuario(Long usuarioId) {
    Usuario usuario = usuarioRepository.findById(usuarioId)
        .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", usuarioId));
    
    return usuario;
}
```

**Validación:**
- ✅ Usuario existe en BD

**Excepción:**
- `ResourceNotFoundException` (404) - Si usuarioId no existe

**Escenario:**
```json
{
  "creadorId": 999  // No existe
}
→ HTTP 404: "Usuario no encontrado con id: 999"
```

---

### **4. Rango de Fechas - COHERENCIA TEMPORAL**

```java
private void validarRangoFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
    if (fechaFin == null) {
        return;  // Válido: evento sin fecha de fin
    }
    
    if (fechaFin.isBefore(fechaInicio) || fechaFin.isEqual(fechaInicio)) {
        throw new BusinessException("RANGO_FECHAS_INVALIDO", 
            "La fecha de fin debe ser posterior a la fecha de inicio");
    }
}
```

**Reglas:**
- ✅ Si `fechaFin` es nula → Válido (evento sin hora de fin)
- ✅ Si `fechaFin` existe → Debe ser > `fechaInicio`
- ❌ Si `fechaFin` <= `fechaInicio` → Error

**Combinaciones de validación:**

| Caso | fechaInicio | fechaFin | Resultado |
|------|-------------|----------|-----------|
| Evento con rango válido | 2025-06-15 10:00 | 2025-06-15 11:00 | ✅ OK |
| Evento sin fecha fin | 2025-06-15 10:00 | null | ✅ OK |
| Fecha fin igual inicio | 2025-06-15 10:00 | 2025-06-15 10:00 | ❌ ERROR |
| Fecha fin anterior | 2025-06-15 10:00 | 2025-06-15 09:00 | ❌ ERROR |

**Excepción:** `BusinessException` (400)

---

## 📊 Flujo de Validación en crearEvento()

```
crearEvento(request)
     │
     ├─→ validarTitulo(titulo)
     │   ├─→ null/vacío → BusinessException
     │   └─→ length < 3 → BusinessException
     │
     ├─→ validarYObtenerTipoEvento(tipoId)
     │   ├─→ No existe → ResourceNotFoundException (404)
     │   ├─→ Inactivo → BusinessException (400)
     │   └─→ ✅ Retorna TipoEvento
     │
     ├─→ validarYObtenerUsuario(usuarioId)
     │   ├─→ No existe → ResourceNotFoundException (404)
     │   └─→ ✅ Retorna Usuario
     │
     ├─→ validarRangoFechas(fechaInicio, fechaFin)
     │   ├─→ fechaFin < fechaInicio → BusinessException (400)
     │   └─→ ✅ Válido
     │
     ├─→ ✅ Todas las validaciones pasaron
     │
     └─→ eventoRepository.save(evento)
         └─→ Retorna Evento persistido
```

---

## 🧪 Ejemplos de Uso

### **Test 1: Crear evento válido**

```bash
curl -X POST http://localhost:8080/api/eventos \
  -H "Content-Type: application/json" \
  -d '{
    "tipoId": 1,
    "creadorId": 5,
    "titulo": "Reunión Importante",
    "descripcion": "Discusión de Q2 results",
    "fechaInicio": "2025-06-20T14:00:00",
    "fechaFin": "2025-06-20T15:30:00",
    "lugar": "Sala A",
    "gruposAfectados": "2ºDAW",
    "numAsistentes": 25,
    "estado": "PENDIENTE"
  }'
```

**Respuesta: HTTP 201 Created** ✅

```json
{
  "id": 1,
  "titulo": "Reunión Importante",
  "estado": "PENDIENTE"
}
```

**Logs:**
```
INFO  - Creando evento: titulo=Reunión Importante, tipo=1, creador=5
DEBUG - Validaciones exitosas para evento: Reunión Importante
INFO  - Evento creado exitosamente: id=1, titulo=Reunión Importante
```

---

### **Test 2: Título vacío**

```bash
curl -X POST http://localhost:8080/api/eventos \
  -H "Content-Type: application/json" \
  -d '{
    "tipoId": 1,
    "creadorId": 5,
    "titulo": "",
    ...
  }'
```

**Respuesta: HTTP 400 Bad Request** ❌

```json
{
  "timestamp": "2025-05-12T10:35:22.456",
  "status": 400,
  "error": "Error de negocio",
  "message": "El título del evento es obligatorio y no puede estar vacío",
  "requestId": "uuid-xxx"
}
```

**Logs:**
```
INFO  - Creando evento: titulo=, tipo=1, creador=5
WARN  - Validación fallida: título vacío o nulo
```

---

### **Test 3: Tipo de evento inactivo**

```bash
curl -X POST http://localhost:8080/api/eventos \
  -H "Content-Type: application/json" \
  -d '{
    "tipoId": 5,  # Existe pero activo=false
    "creadorId": 5,
    "titulo": "Evento",
    ...
  }'
```

**Respuesta: HTTP 400 Bad Request** ❌

```json
{
  "status": 400,
  "error": "Error de negocio",
  "message": "No se puede usar el tipo 'Mantenimiento' porque está inactivo. Contacte al administrador para activarlo.",
  "requestId": "uuid-xxx"
}
```

**Logs:**
```
INFO  - Creando evento: titulo=Evento, tipo=5, creador=5
WARN  - Validación fallida: TipoEvento inactivo: id=5, nombre=Mantenimiento
```

---

### **Test 4: Usuario no existe**

```bash
curl -X POST http://localhost:8080/api/eventos \
  -H "Content-Type: application/json" \
  -d '{
    "tipoId": 1,
    "creadorId": 999,  # No existe
    "titulo": "Evento",
    ...
  }'
```

**Respuesta: HTTP 404 Not Found** ❌

```json
{
  "status": 404,
  "error": "Recurso no encontrado",
  "message": "Usuario no encontrado con id: 999",
  "requestId": "uuid-xxx"
}
```

**Logs:**
```
INFO  - Creando evento: titulo=Evento, tipo=1, creador=999
WARN  - Validación fallida: Usuario no existe con id=999
```

---

### **Test 5: Fechas inválidas (fin < inicio)**

```bash
curl -X POST http://localhost:8080/api/eventos \
  -H "Content-Type: application/json" \
  -d '{
    "tipoId": 1,
    "creadorId": 5,
    "titulo": "Evento",
    "fechaInicio": "2025-06-20T15:00:00",
    "fechaFin": "2025-06-20T14:00:00",  # Anterior a inicio
    ...
  }'
```

**Respuesta: HTTP 400 Bad Request** ❌

```json
{
  "status": 400,
  "error": "Error de negocio",
  "message": "La fecha de fin del evento debe ser posterior a la fecha de inicio",
  "requestId": "uuid-xxx"
}
```

**Logs:**
```
INFO  - Creando evento: titulo=Evento, tipo=1, creador=5
WARN  - Validación fallida: fechaFin (15:00) no es posterior a fechaInicio (14:00)
```

---

## 🏗️ Arquitectura de Validación

```
┌─────────────────────────────────────────────┐
│ Capa de Presentación (Controlador)         │
│ HTTP Request + @Valid (DTOs)               │
└─────────────────────┬───────────────────────┘
                      │
        Validaciones de sintaxis ✅
        (Ejecutadas por Spring)
                      │
                      ▼
┌─────────────────────────────────────────────┐
│ Capa de Negocio (Service)                   │
│ Validaciones de Reglas de Negocio          │
│                                             │
│ 1. validarTitulo()                          │
│ 2. validarYObtenerTipoEvento()             │
│ 3. validarYObtenerUsuario()                │
│ 4. validarRangoFechas()                    │
└─────────────────────┬───────────────────────┘
                      │
        Validaciones de coherencia ✅
        (Ejecutadas en Service)
                      │
                      ▼
┌─────────────────────────────────────────────┐
│ Capa de Persistencia (Repository)          │
│ Guardado en BD                              │
└─────────────────────────────────────────────┘
```

---

## 💡 Comparación: Antes vs Después

### **Antes (Sin validaciones):**

```java
@Transactional
public Evento crearEvento(CrearEventoRequest request) {
    Evento evento = Evento.builder()
        .tipo(buscarTipoPorId(request.getTipoId()))  // ❌ Sin validación de activo
        .creador(buscarUsuarioPorId(request.getCreadorId()))
        .titulo(request.getTitulo())  // ❌ Sin validación de contenido
        .fechaInicio(request.getFechaInicio())
        .fechaFin(request.getFechaFin())  // ❌ Sin validación de orden
        ...
        .build();
    
    return eventoRepository.save(evento);  // Problema en BD potencial
}
```

**Problemas:**
- ❌ Tipo inactivo podría ser utilizado
- ❌ Fechas incoherentes permitidas
- ❌ Sin logging
- ❌ Sin mensajes claros de error

---

### **Después (Con validaciones):**

```java
@Transactional
public Evento crearEvento(CrearEventoRequest request) {
    log.info("Creando evento: titulo={}, tipo={}, creador={}", 
        request.getTitulo(), request.getTipoId(), request.getCreadorId());
    
    // 1️⃣ Validar título
    validarTitulo(request.getTitulo());
    
    // 2️⃣ Validar tipo (existe + activo)
    TipoEvento tipo = validarYObtenerTipoEvento(request.getTipoId());
    
    // 3️⃣ Validar usuario (existe)
    Usuario creador = validarYObtenerUsuario(request.getCreadorId());
    
    // 4️⃣ Validar fechas (coherencia)
    validarRangoFechas(request.getFechaInicio(), request.getFechaFin());
    
    log.debug("Validaciones exitosas para evento: {}", request.getTitulo());
    
    Evento evento = Evento.builder()...build();
    
    Evento eventoPersistido = eventoRepository.save(evento);
    log.info("Evento creado exitosamente: id={}", eventoPersistido.getId());
    
    return eventoPersistido;
}
```

**Ventajas:**
- ✅ Tipos activos solamente
- ✅ Fechas válidas garantizadas
- ✅ Logging centralizado
- ✅ Mensajes claros y en español
- ✅ Código limpio y mantenible

---

## 🔍 Logging Implementado

### **Niveles de Log**

```
INFO  - Operaciones principales (crear, actualizar, eliminar)
DEBUG - Validaciones exitosas, detalles internos
WARN  - Validaciones fallidas, datos incoherentes
ERROR - Excepciones inesperadas
```

### **Ejemplos de Logs**

```
INFO  - Creando evento: titulo=Reunión Q2, tipo=1, creador=5
DEBUG - Validación exitosa: TipoEvento válido y activo: Reunión
DEBUG - Validación exitosa: Usuario válido: Juan Pérez
DEBUG - Validación exitosa: rango de fechas válido
INFO  - Evento creado exitosamente: id=123, titulo=Reunión Q2

---

INFO  - Actualizando evento: id=123, titulo=Reunión Q2
WARN  - Validación fallida: TipoEvento inactivo: id=5, nombre=Mantenimiento
ERROR - BusinessException lanzada: TIPO_EVENTO_INACTIVO

---

INFO  - Eliminando evento: id=123
INFO  - Evento eliminado exitosamente: id=123
```

---

## ✨ Buenas Prácticas Enterprise Implementadas

✅ **Validaciones en capas:**
- DTOs con @Valid (sintaxis)
- Service con lógica (negocio)

✅ **Excepciones específicas:**
- `ResourceNotFoundException` (404)
- `BusinessException` (400)

✅ **Logging profesional:**
- Trazabilidad completa
- Diferentes niveles (INFO, DEBUG, WARN)

✅ **Mensajes claros:**
- En español
- Descriptivos y específicos
- Guía al usuario en la solución

✅ **Clean Code:**
- Métodos privados de validación
- Responsabilidad única
- Documentación Javadoc

✅ **Transaccionalidad:**
- `@Transactional` en escrituras
- Rollback automático en errores

---

## 📚 Documentación Relacionada

- [VALIDACIONES.md](VALIDACIONES.md) - Validaciones en DTOs
- [VALIDACIONES_CONTROLADORES.md](VALIDACIONES_CONTROLADORES.md) - @Valid en controladores
- [MANEJO_ERRORES_ENTERPRISE.md](MANEJO_ERRORES_ENTERPRISE.md) - Excepciones personalizadas

---

## 🎓 Checklist de Implementación

- [x] Validaciones de título
- [x] Validaciones de TipoEvento (existe + activo)
- [x] Validaciones de Usuario (existe)
- [x] Validaciones de rango de fechas
- [x] Logging centralizado
- [x] Excepciones apropiadas
- [x] Mensajes en español
- [x] Métodos privados para cada validación
- [x] Documentación Javadoc
- [x] Código limpio y mantenible

**Tu EventoService tiene validaciones de negocio enterprise implementadas** 🚀
