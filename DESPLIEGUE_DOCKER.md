# Despliegue Docker - Resumen de acciones realizadas

Fecha: 2026-06-03
Proyecto: Agenda Escolar IES Jándula
Objetivo: dejar preparado un despliegue Docker minimo y seguro sin romper funcionamiento local.

## 1) Archivos modificados

- frontend/frontend/src/api/axios.js
- docker-compose.yml
- frontend/frontend/Dockerfile
- .env.example
- README.md

## 2) Cambios aplicados

### 2.1 Frontend (Axios)

Archivo: frontend/frontend/src/api/axios.js

- Antes: URL fija hardcodeada.
  - http://localhost:8080
- Ahora: URL leida desde variable de entorno con fallback local.
  - import.meta.env.VITE_API_URL || 'http://localhost:8080'

No se tocaron endpoints, stores ni logica de negocio.

### 2.2 docker-compose

Archivo: docker-compose.yml

Cambios minimos:

- MySQL:
  - MYSQL_DATABASE alineada con variable:
    - MYSQL_DATABASE: ${MYSQL_DATABASE:-agenda_db}
  - Se mantiene volumen persistente:
    - mysql_data:/var/lib/mysql
  - Se mantiene healthcheck existente.

- Backend:
  - Se mantiene en puerto 8080 publicado en localhost.
  - Se mantiene conexion a MySQL por nombre de servicio interno (mysql).

- Frontend:
  - Se anaden args de build para Vite:
    - VITE_API_URL
    - VITE_TV_TOKEN
  - Puerto publicado cambiado a 5173 por defecto:
    - ${FRONTEND_PORT:-5173}:80

No se cambiaron nombres de servicios.

### 2.3 Dockerfile del frontend

Archivo: frontend/frontend/Dockerfile

- Se mantuvo estrategia multi-stage (Node build + Nginx runtime).
- Se anadieron variables de build para Vite:
  - ARG VITE_API_URL
  - ARG VITE_TV_TOKEN
  - ENV VITE_API_URL=$VITE_API_URL
  - ENV VITE_TV_TOKEN=$VITE_TV_TOKEN

Nginx sigue sirviendo dist en puerto interno 80.

### 2.4 Variables de entorno documentadas

Archivo: .env.example

Se dejaron documentadas/ajustadas:

- MYSQL_DATABASE=agenda_db
- MYSQL_ROOT_PASSWORD=toor
- SPRING_DATASOURCE_USERNAME=root
- SPRING_DATASOURCE_PASSWORD=toor
- SECURITY_JWT_SECRET=clave-super-secreta-agenda-ies-jandula-cambia-esta-clave-en-produccion
- SECURITY_JWT_EXPIRATION_MS=86400000
- APP_TV_TOKEN=agenda-tv-dev
- FRONTEND_PORT=5173
- VITE_API_URL=http://localhost:8080
- VITE_TV_TOKEN=agenda-tv-dev

### 2.5 README

Archivo: README.md

Se anadio seccion breve "Ejecucion con Docker" con:

- docker compose up --build
- docker compose down
- Frontend: http://localhost:5173
- Backend: http://localhost:8080

## 3) Validaciones ejecutadas

### 3.1 Frontend build

Comando:

npm run build

Resultado:

- OK
- Vite build finalizado correctamente.

### 3.2 Backend compile

Comando:

mvn clean compile

Resultado:

- OK
- Compilacion correcta.
- Verificacion adicional con marcador BUILD_OK en compilacion corta.

### 3.3 Compose config

Comando:

docker compose config

Resultado:

- OK
- Compose renderiza correctamente con:
  - mysql:3306
  - backend:8080
  - frontend:5173
  - args de build VITE_API_URL y VITE_TV_TOKEN
  - volumen mysql_data
  - healthcheck en mysql

### 3.4 Levantar stack

Comando:

docker compose up --build -d

Resultado en este entorno:

- No completado por Docker daemon detenido.
- Error observado:
  - failed to connect to docker API at npipe:////./pipe/dockerDesktopLinuxEngine
  - The system cannot find the file specified.

## 4) Estado final

- Configuracion Docker lista para usar.
- Funcionamiento local preservado (frontend y backend compilan fuera de Docker).
- Bloqueo actual: Docker Desktop debe estar iniciado para ejecutar "docker compose up --build".

## 5) Pasos para ejecutar ahora

1. Iniciar Docker Desktop.
2. Desde la raiz del proyecto ejecutar:
   - docker compose up --build
3. Comprobar:
   - Frontend: http://localhost:5173
   - Backend: http://localhost:8080
4. Para parar:
   - docker compose down
