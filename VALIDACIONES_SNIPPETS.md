# 💻 Snippets Rápidos - Validaciones en Spring Boot

## ⚡ Quick Reference

### **1. Activar Validaciones en Controlador**

```java
// ✅ CORRECTO
@PostMapping
public ResponseEntity<EventoResponseDTO> crearEvento(
    @Valid @RequestBody CrearEventoRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(eventoService.crearEvento(request));
}

// ❌ INCORRECTO (sin @Valid)
@PostMapping
public ResponseEntity<EventoResponseDTO> crearEvento(
    @RequestBody CrearEventoRequest request) {  // Falta @Valid
    return ResponseEntity.ok(eventoService.crearEvento(request));
}
```

---

### **2. Anotaciones de Validación Comunes**

```java
@NotNull(message = "Campo obligatorio")
private Long id;

@NotBlank(message = "No puede estar vacío")
private String titulo;

@Size(min = 3, max = 100, message = "Entre 3 y 100 caracteres")
private String descripcion;

@Email(message = "Email inválido")
private String email;

@Positive(message = "Debe ser mayor que 0")
private Integer cantidad;

@Min(value = 0, message = "Mínimo 0")
private Integer numero;

@Max(value = 100, message = "Máximo 100")
private Integer porcentaje;

@Future(message = "Debe ser una fecha futura")
private LocalDateTime fecha;

@FutureOrPresent(message = "No puede ser en el pasado")
private LocalDateTime fechaInicio;

@Past(message = "Debe ser una fecha pasada")
private LocalDateTime fechaNacimiento;

@PastOrPresent(message = "No puede ser en el futuro")
private LocalDateTime ultimaModificacion;

@Pattern(regexp = "^[A-Z]{2}\\d{3}$", message = "Formato: AB123")
private String codigo;

@DecimalMin(value = "0.01", message = "Mínimo 0.01")
private BigDecimal precio;

@DecimalMax(value = "99.99", message = "Máximo 99.99")
private BigDecimal descuento;

@AssertTrue(message = "El checkbox debe estar marcado")
private Boolean aceptaTerminos;

@Length(min = 6, max = 12, message = "Entre 6 y 12 caracteres")
private String password;
```

---

### **3. DTOs con Validaciones**

```java
@Data
public class CrearEventoRequest {
    
    @NotNull(message = "Tipo requerido")
    private Long tipoId;
    
    @NotBlank(message = "Título obligatorio")
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String titulo;
    
    @Size(max = 500)
    private String descripcion;
    
    @NotNull
    @FutureOrPresent(message = "No puede ser pasado")
    private LocalDateTime fechaInicio;
    
    @Positive(message = "Mayor que 0")
    private Integer numAsistentes;
}
```

---

### **4. Controlador Completo**

```java
@RestController
@RequestMapping("/api/eventos")
@RequiredArgsConstructor
public class EventoController {
    
    private final EventoService eventoService;
    
    @PostMapping
    public ResponseEntity<EventoResponseDTO> crearEvento(
        @Valid @RequestBody CrearEventoRequest request) {
        Evento creado = eventoService.crearEvento(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(eventoService.convertirAResponse(creado));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<EventoResponseDTO> actualizarEvento(
        @PathVariable Long id,
        @Valid @RequestBody CrearEventoRequest request) {
        EventoResponseDTO actualizado = eventoService.actualizarEvento(id, request);
        return ResponseEntity.ok(actualizado);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<EventoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return eventoService.obtenerPorId(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
```

---

### **5. GlobalExceptionHandler Básico**

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleValidationException(
        MethodArgumentNotValidException ex) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 400);
        response.put("error", "Validación fallida");
        
        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                error -> error.getField(),
                error -> error.getDefaultMessage(),
                (e1, e2) -> e1 + "; " + e2
            ));
        
        response.put("fieldErrors", errors);
        return ResponseEntity.badRequest().body(response);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
        IllegalArgumentException ex) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 400);
        response.put("error", "Error");
        response.put("message", ex.getMessage());
        
        return ResponseEntity.badRequest().body(response);
    }
}
```

---

### **6. Respuesta JSON de Error**

```json
{
  "timestamp": "2025-05-12T10:30:45.123",
  "status": 400,
  "error": "Validación fallida",
  "message": "Los datos enviados no cumplen con las validaciones",
  "fieldErrors": {
    "titulo": "El título es obligatorio y no puede estar vacío",
    "fechaInicio": "La fecha de inicio no puede estar en el pasado",
    "numAsistentes": "El número de asistentes debe ser mayor que 0"
  },
  "errorCount": 3
}
```

---

### **7. Respuesta JSON de Éxito**

```json
{
  "id": 1,
  "tipoId": 1,
  "tipoNombre": "Reunión",
  "titulo": "Junta Directiva",
  "descripcion": "Reunión trimestral",
  "fechaInicio": "2025-06-20T15:00:00",
  "fechaFin": "2025-06-20T16:30:00",
  "lugar": "Sala Principal",
  "gruposAfectados": "Directivos",
  "estado": "CONFIRMADO",
  "creadorId": 5,
  "creadorNombre": "Admin"
}
```

---

## 🧪 Pruebas con cURL

### **POST Válido**

```bash
curl -X POST http://localhost:8080/api/eventos \
  -H "Content-Type: application/json" \
  -d '{
    "tipoId": 1,
    "creadorId": 5,
    "titulo": "Evento",
    "fechaInicio": "2025-12-31T18:00:00",
    "lugar": "Sala",
    "gruposAfectados": "Grupo1",
    "numAsistentes": 20,
    "estado": "PENDIENTE"
  }' | jq
```

### **POST Inválido - Título vacío**

```bash
curl -X POST http://localhost:8080/api/eventos \
  -H "Content-Type: application/json" \
  -d '{
    "tipoId": 1,
    "creadorId": 5,
    "titulo": "",
    "fechaInicio": "2025-12-31T18:00:00",
    "lugar": "Sala",
    "gruposAfectados": "Grupo1",
    "numAsistentes": 20,
    "estado": "PENDIENTE"
  }' | jq
```

### **POST Inválido - Fecha en pasado**

```bash
curl -X POST http://localhost:8080/api/eventos \
  -H "Content-Type: application/json" \
  -d '{
    "tipoId": 1,
    "creadorId": 5,
    "titulo": "Evento",
    "fechaInicio": "2024-01-01T10:00:00",
    "lugar": "Sala",
    "gruposAfectados": "Grupo1",
    "numAsistentes": 20,
    "estado": "PENDIENTE"
  }' | jq
```

### **POST Inválido - Múltiples errores**

```bash
curl -X POST http://localhost:8080/api/eventos \
  -H "Content-Type: application/json" \
  -d '{
    "tipoId": null,
    "creadorId": 5,
    "titulo": "",
    "fechaInicio": "2024-01-01T10:00:00",
    "lugar": null,
    "gruposAfectados": null,
    "numAsistentes": 0,
    "estado": null
  }' | jq
```

---

## 📝 Checklist de Validación

- [ ] ¿Tengo `@Valid` en todos los `@PostMapping` y `@PutMapping`?
- [ ] ¿Están `@Valid` y `@RequestBody` en el orden correcto?
- [ ] ¿Tengo GlobalExceptionHandler implementado?
- [ ] ¿Tengo @ExceptionHandler(MethodArgumentNotValidException.class)?
- [ ] ¿Están los mensajes en el idioma del negocio?
- [ ] ¿Devuelvo HTTP 400 en errores de validación?
- [ ] ¿Devuelvo HTTP 201 en creaciones exitosas?
- [ ] ¿He probado con datos inválidos?
- [ ] ¿He probado con múltiples errores?
- [ ] ¿He documentado las validaciones?

---

## 🔧 Troubleshooting

### **Problema: Las validaciones no se activan**

```
❌ Solución: Verificar que tienes @Valid
✅ @PostMapping
   public void create(@Valid @RequestBody DTO dto)
```

### **Problema: El mensaje de error es incorrecto**

```
❌ Solución: Verificar el atributo 'message' en la anotación
✅ @NotBlank(message = "El campo es obligatorio")
   private String campo;
```

### **Problema: Spring no devuelve errores en JSON**

```
❌ Solución: Verificar que tengo GlobalExceptionHandler
✅ @ControllerAdvice
   public class GlobalExceptionHandler {
       @ExceptionHandler(MethodArgumentNotValidException.class)
       ...
   }
```

### **Problema: Recibo HTTP 500 en lugar de 400**

```
❌ Solución: Verificar @ResponseStatus(HttpStatus.BAD_REQUEST)
✅ @ExceptionHandler(MethodArgumentNotValidException.class)
   @ResponseStatus(HttpStatus.BAD_REQUEST)
   public ResponseEntity<...> handleValidation(...) {
       ...
   }
```

---

## 📚 Documentación Asociada

- [VALIDACIONES.md](VALIDACIONES.md) - Explicación detallada de cada validación
- [VALIDACIONES_CONTROLADORES.md](VALIDACIONES_CONTROLADORES.md) - Integración en controladores
- [VALIDACIONES_ARQUITECTURA.md](VALIDACIONES_ARQUITECTURA.md) - Arquitectura completa

---

## ✨ Comandos Git

```bash
# Ver cambios en validaciones
git diff HEAD~1 src/main/java/agenda/dto/CrearEventoRequest.java
git diff HEAD~1 src/main/java/agenda/controller/EventoController.java
git diff HEAD~1 src/main/java/agenda/exception/GlobalExceptionHandler.java

# Commit de validaciones
git add -A
git commit -m "feat: Add Jakarta Validation with @Valid in controllers"

# Ver histórico
git log --oneline -- src/main/java/agenda/dto/
```

---

**Quick Remember:**

```
@Valid + @RequestBody = Validación automática ✅
MethodArgumentNotValidException = GlobalExceptionHandler captura ✅
HTTP 400 = Errores de validación ✅
HTTP 201 = Creación exitosa ✅
```

**Ready to validate!** 🚀
