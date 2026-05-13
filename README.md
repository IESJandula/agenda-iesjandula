# agenda-iesjandula
Agenda del instituto IES Jándula, para mostrar los eventos del centro.

## Formato de fecha en eventos

Las peticiones a `POST /api/eventos` y `PUT /api/eventos/{id}` deben enviar `fechaInicio` y `fechaFin` en formato ISO-8601 con segundos:

```json
"fechaInicio": "2025-06-15T14:00:00",
"fechaFin": "2025-06-15T15:30:00"
```

Si se envía otro formato, la API devolverá un error de validación antes de procesar el evento.
