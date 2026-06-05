import { saveToStorage, loadFromStorage, STORAGE_KEYS, getDefaultSettings } from './storage'

/** 应用主题 */
export function applyTheme(theme) {
  const isDark = theme === 'dark'
  document.documentElement.classList.toggle('dark', isDark)
  document.documentElement.setAttribute('data-theme', theme)
}

/** 启动时从 LocalStorage 恢复主题 */
export function initTheme() {
  const settings = loadFromStorage(STORAGE_KEYS.SETTINGS, getDefaultSettings())
  applyTheme(settings.theme || 'light')
}

/** 切换主题并持久化 */
export function toggleTheme() {
  const settings = { ...getDefaultSettings(), ...loadFromStorage(STORAGE_KEYS.SETTINGS, getDefaultSettings()) }
  const next = settings.theme === 'dark' ? 'light' : 'dark'
  settings.theme = next
  saveToStorage(STORAGE_KEYS.SETTINGS, settings)
  applyTheme(next)
  return next
}

export function getCurrentTheme() {
  const settings = loadFromStorage(STORAGE_KEYS.SETTINGS, getDefaultSettings())
  return settings.theme || 'light'
}
