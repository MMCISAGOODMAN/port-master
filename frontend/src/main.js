import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import en from 'element-plus/dist/locale/en.mjs'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import './styles/global.css'
import { initTheme } from './utils/theme'
import i18n, { getLocale } from './i18n'

initTheme()

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

const elementLocale = getLocale() === 'en' ? en : zhCn

app.use(i18n)
app.use(ElementPlus, { locale: elementLocale })
app.mount('#app')
