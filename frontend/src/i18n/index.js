import { createI18n } from 'vue-i18n'
import zhCN from './locales/zh-CN'
import en from './locales/en'
import { loadFromStorage, STORAGE_KEYS, getDefaultSettings } from '@/utils/storage'

function getInitialLocale() {
  const settings = loadFromStorage(STORAGE_KEYS.SETTINGS, getDefaultSettings())
  return settings.locale || 'zh-CN'
}

const i18n = createI18n({
  legacy: false,
  locale: getInitialLocale(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    en
  }
})

export default i18n

export function setLocale(locale) {
  i18n.global.locale.value = locale
}

export function getLocale() {
  return i18n.global.locale.value
}
