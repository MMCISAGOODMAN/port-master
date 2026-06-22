import { loadFromStorage, saveToStorage, STORAGE_KEYS, getDefaultGroups, getDefaultSettings } from './storage'

/**
 * 导出全部本地配置为 JSON
 */
export function exportConfig() {
  return {
    version: '2.0',
    exportedAt: new Date().toISOString(),
    groups: loadFromStorage(STORAGE_KEYS.GROUPS, getDefaultGroups()),
    monitor: loadFromStorage(STORAGE_KEYS.MONITOR, { enabled: false, ports: [] }),
    history: loadFromStorage(STORAGE_KEYS.HISTORY, []),
    settings: loadFromStorage(STORAGE_KEYS.SETTINGS, getDefaultSettings()),
    remoteHosts: loadFromStorage(STORAGE_KEYS.REMOTE_HOSTS, []),
    scanHistory: loadFromStorage(STORAGE_KEYS.SCAN_HISTORY, [])
  }
}

export function downloadConfig() {
  const data = exportConfig()
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `portmaster-config-${new Date().toISOString().slice(0, 10)}.json`
  a.click()
  URL.revokeObjectURL(url)
}

/**
 * 导入配置 JSON
 */
export function importConfig(json) {
  const data = typeof json === 'string' ? JSON.parse(json) : json
  if (data.groups) saveToStorage(STORAGE_KEYS.GROUPS, data.groups)
  if (data.monitor) saveToStorage(STORAGE_KEYS.MONITOR, data.monitor)
  if (data.history) saveToStorage(STORAGE_KEYS.HISTORY, data.history)
  if (data.settings) saveToStorage(STORAGE_KEYS.SETTINGS, data.settings)
  if (data.remoteHosts) saveToStorage(STORAGE_KEYS.REMOTE_HOSTS, data.remoteHosts)
  if (data.scanHistory) saveToStorage(STORAGE_KEYS.SCAN_HISTORY, data.scanHistory)
  return data
}
