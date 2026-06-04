import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import './assets/seneca-theme.css'
import App from './App.vue'
import router from './router'

createApp(App).use(createPinia()).use(router).mount('#app')
