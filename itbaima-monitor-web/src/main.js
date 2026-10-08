import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from "axios";

import '@/assets/css/element.less'
import 'flag-icon-css/css/flag-icons.min.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import {createPinia} from "pinia";
import piniaPluginPersistedstate from "pinia-plugin-persistedstate"
// import '@/assets/quill.css'
import API_BASE from "@/net/config.js"

axios.defaults.baseURL = API_BASE

const app = createApp(App)

const pinia = createPinia()

app.use(pinia)
pinia.use(piniaPluginPersistedstate)
app.use(router)

app.mount('#app')
