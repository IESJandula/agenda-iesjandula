# Vue 3 + Vite

This template should help get you started developing with Vue 3 in Vite. The template uses Vue 3 `<script setup>` SFCs, check out the [script setup docs](https://v3.vuejs.org/api/sfc-script-setup.html#sfc-script-setup) to learn more.

Learn more about IDE Support for Vue in the [Vue Docs Scaling up Guide](https://vuejs.org/guide/scaling-up/tooling.html#ide-support).

## Variables de entorno

El frontend lee el token de las pantallas TV desde `VITE_TV_TOKEN`.

En desarrollo, si no se define esa variable, se usa `agenda-tv-dev` para mantener el flujo local.

En producción define el valor real en `.env`:

```bash
VITE_TV_TOKEN=valor_real_del_token
```
