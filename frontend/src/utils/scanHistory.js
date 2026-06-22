import { loadFromStorage, saveToStorage, STORAGE_KEYS } from './storage'

const MAX_HISTORY = 30

/**
 * 记录扫描快照
 */
export function recordScanSnapshot(portData, conflictCount = 0) {
  const history = loadScanHistory()
  const snapshot = {
    timestamp: new Date().toISOString(),
    total: portData.length,
    listen: portData.filter(p => p.state === 'LISTEN').length,
    established: portData.filter(p => p.state === 'ESTABLISHED').length,
    tcp: portData.filter(p => p.protocol === 'TCP').length,
    udp: portData.filter(p => p.protocol === 'UDP').length,
    conflicts: conflictCount
  }
  history.unshift(snapshot)
  saveToStorage(STORAGE_KEYS.SCAN_HISTORY, history.slice(0, MAX_HISTORY))
  return snapshot
}

export function loadScanHistory() {
  return loadFromStorage(STORAGE_KEYS.SCAN_HISTORY, [])
}

export function clearScanHistory() {
  saveToStorage(STORAGE_KEYS.SCAN_HISTORY, [])
}

export function removeScanHistoryItem(index) {
  const history = loadScanHistory()
  history.splice(index, 1)
  saveToStorage(STORAGE_KEYS.SCAN_HISTORY, history)
  return history
}
