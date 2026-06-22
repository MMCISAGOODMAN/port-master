/**
 * WebSocket 监控告警客户端
 */
let ws = null
let reconnectTimer = null
let pingTimer = null
let alertCallback = null

function getWsUrl() {
  const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.host
  return `${proto}//${host}/ws/monitor`
}

export function connectMonitorWs(onAlert) {
  alertCallback = onAlert
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
    return
  }

  try {
    ws = new WebSocket(getWsUrl())

    ws.onopen = () => {
      startPing()
    }

    ws.onmessage = (event) => {
      if (event.data === 'pong') return
      try {
        const data = JSON.parse(event.data)
        if (data.type === 'alert' && data.alerts && alertCallback) {
          alertCallback(data.alerts)
        }
      } catch { /* ignore */ }
    }

    ws.onclose = () => {
      stopPing()
      scheduleReconnect()
    }

    ws.onerror = () => {
      ws?.close()
    }
  } catch {
    scheduleReconnect()
  }
}

export function disconnectMonitorWs() {
  stopPing()
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  if (ws) {
    ws.onclose = null
    ws.close()
    ws = null
  }
  alertCallback = null
}

function scheduleReconnect() {
  if (reconnectTimer) return
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    if (alertCallback) connectMonitorWs(alertCallback)
  }, 5000)
}

function startPing() {
  stopPing()
  pingTimer = setInterval(() => {
    if (ws?.readyState === WebSocket.OPEN) {
      ws.send('ping')
    }
  }, 30000)
}

function stopPing() {
  if (pingTimer) {
    clearInterval(pingTimer)
    pingTimer = null
  }
}

/** 同步监控配置到服务端（供后台 WebSocket 轮询） */
export async function syncMonitorConfig(request, config) {
  await request.post('/monitor/config', {
    enabled: config.enabled,
    ports: (config.ports || []).map(p => ({
      port: p.port,
      protocol: p.protocol || 'TCP',
      remark: p.remark,
      expectedState: p.expectedState || 'any'
    }))
  })
}

export function isMonitorWsConnected() {
  return ws?.readyState === WebSocket.OPEN
}
