import './assets/main.css'

// 載入Vue的運行環境
import { createApp } from 'vue'

// 載入Vue的根元件
import App from './App.vue'

// 載入插件
import router from './router' // 路由套件
import Antd from 'ant-design-vue' // 螞蟻骨架
import 'ant-design-vue/dist/reset.css' // 螞蟻css
import { createPinia } from 'pinia' // 狀態管理

// 將根元件掛載到變數 才能使用其方法
const app = createApp(App)

// app.use() -> 載入插件
app.use(createPinia())
app.use(router)
app.use(Antd)

// app.mount() 把vue掛載到html上
app.mount('#app')
