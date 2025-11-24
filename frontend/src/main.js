import './assets/main.css'

import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import router from './router.js';

import App from './App.vue'
import { init as initDebugLogger } from './utils/debugLogger.js'

const app = createApp(App);
app.use(ElementPlus);
app.use(router);
initDebugLogger(app)
app.mount('#app');
