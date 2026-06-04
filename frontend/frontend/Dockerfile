FROM node:22-alpine AS build

WORKDIR /app

ARG VITE_API_URL
ARG VITE_TV_TOKEN
ENV VITE_API_URL=$VITE_API_URL
ENV VITE_TV_TOKEN=$VITE_TV_TOKEN

COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build

FROM nginx:1.27-alpine

RUN /bin/sh -c 'printf "%s\n" "server {" "  listen 80;" "  server_name _;" "  root /usr/share/nginx/html;" "  index index.html;" "" "  location / {" "    try_files $uri $uri/ /index.html;" "  }" "}" > /etc/nginx/conf.d/default.conf'

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
