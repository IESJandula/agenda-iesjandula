# 🎓 Agenda Escolar Inteligente - IES Jándula

<div align="center">

# 📅 Agenda Escolar Inteligente

### Proyecto Final DAW · IES Jándula

Sistema web para la gestión integral de eventos académicos y organizativos de un centro educativo.

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green?style=for-the-badge)
![Vue](https://img.shields.io/badge/Vue-3-42b883?style=for-the-badge)
![MySQL](https://img.shields.io/badge/MySQL-8-blue?style=for-the-badge)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge)
![JWT](https://img.shields.io/badge/Security-JWT-red?style=for-the-badge)

</div>

---

## 📖 Descripción

Agenda Escolar Inteligente es una aplicación web desarrollada como Proyecto Final del ciclo de Desarrollo de Aplicaciones Web (DAW).

Permite gestionar de forma centralizada los eventos académicos y organizativos de un centro educativo mediante una arquitectura cliente-servidor moderna basada en Spring Boot y Vue 3.

La aplicación incorpora:

* Gestión de usuarios.
* Gestión de eventos.
* Gestión de tipos de eventos.
* Calendario mensual.
* Agenda semanal.
* Vista de curso completo.
* Propuestas del profesorado.
* Importación masiva mediante CSV.
* Pantallas TV embebibles.
* Autenticación JWT.
* Control de acceso por roles.

---

# 🚀 Funcionalidades principales

## 👤 Gestión de usuarios

* Alta de usuarios.
* Edición de usuarios.
* Eliminación de usuarios.
* Gestión de roles.
* Relación N:M entre usuarios y eventos responsables.

## 📅 Gestión de eventos

* Crear eventos.
* Editar eventos.
* Eliminar eventos.
* Visualización en calendario.
* Agenda semanal.
* Curso completo.
* Asociación de múltiples responsables.

## 🏷️ Gestión de tipos

* CRUD completo de tipos de evento.
* Clasificación visual de eventos.

## 💡 Propuestas del profesorado

Los usuarios con rol profesorado pueden proponer eventos para su posterior validación.

## 📂 Importación CSV

Importación masiva de festivos desde archivos CSV.

## 📺 Pantallas TV

Pantallas públicas optimizadas para televisores informativos:

* Próximo evento.
* Mes actual.
* Curso completo.
* Hoy y próximos eventos.

Actualización automática cada 5 minutos.

---

# 🏗️ Arquitectura

```text
Vue 3 + Vite
       │
       ▼
 REST API (JWT)
       │
       ▼
 Spring Boot
       │
       ▼
     MySQL
```

---

# 🛠️ Tecnologías utilizadas

| Tecnología      | Uso               |
| --------------- | ----------------- |
| Java 21         | Backend           |
| Spring Boot     | API REST          |
| Spring Security | Seguridad         |
| JWT             | Autenticación     |
| JPA / Hibernate | Persistencia      |
| MySQL           | Base de datos     |
| Vue 3           | Frontend          |
| Vue Router      | Navegación        |
| Axios           | Comunicación HTTP |
| Docker          | Despliegue        |

---

# 🔐 Seguridad

## Autenticación

La aplicación utiliza JWT (JSON Web Token).

## Roles disponibles

| Rol         | Permisos                      |
| ----------- | ----------------------------- |
| ADMIN       | Acceso total                  |
| PROFESORADO | Gestión limitada y propuestas |

## Protección de rutas

Las rutas privadas requieren token válido.

---

# 🗄️ Base de datos

## Relación usuarios-eventos

Relación N:M implementada mediante:

```sql
evento_responsables
```

```text
usuarios
   ▲
   │ N:M
   ▼
eventos
```

Un evento puede tener múltiples responsables y un usuario puede ser responsable de múltiples eventos.

---

# 📺 URLs de pantallas TV

```text
/pantalla/proximo-evento?tvToken=TOKEN
```

```text
/pantalla/mes-actual?tvToken=TOKEN
```

```text
/pantalla/curso?tvToken=TOKEN
```

```text
/pantalla/hoy-y-proximos?tvToken=TOKEN
```

---

# ⚙️ Instalación

## Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

Servidor:

```text
http://localhost:8080
```

---

## Frontend

```bash
cd frontend
npm install
npm run dev
```

Aplicación:

```text
http://localhost:5173
```

---

# 🐳 Docker

```bash
docker-compose up -d
```

---

# 📷 Capturas de pantalla

## Dashboard

*Añadir captura*

## Calendario mensual

*Añadir captura*

## Gestión de eventos

*Añadir captura*

## Pantalla TV

*Añadir captura*

---

# 🧪 Pruebas realizadas

* Autenticación JWT.
* CRUD de usuarios.
* CRUD de eventos.
* CRUD de tipos.
* Importación CSV.
* Relación N:M responsables.
* Pantallas TV.
* Responsive Design.

---

# 📚 Autor

**Cristina Mangue Mitogo Mangue**

Proyecto Final de Desarrollo de Aplicaciones Web (DAW)

IES Jándula · Curso 2025-2026

---

# 📄 Licencia

MIT License

