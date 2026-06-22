<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :title="t('monitor.title')"
    width="600px"
    @open="loadConfig"
    @close="onMonitorDialogClose"
  >
    <el-alert
      :title="t('monitor.hint')"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 16px"
    />

    <div class="monitor-status">
      <el-tag :type="wsConnected ? 'success' : 'info'" size="small">
        WebSocket: {{ wsConnected ? 'Connected' : 'Disconnected' }}
      </el-tag>
    </div>

    <div class="monitor-form">
      <el-input v-model="newPort" :placeholder="t('monitor.portPlaceholder')" style="width: 120px" @keyup.enter="addPort" />
      <el-input v-model="newRemark" :placeholder="t('monitor.remarkPlaceholder')" style="width: 200px" />
      <el-button type="primary" @click="addPort">{{ t('monitor.add') }}</el-button>
      <el-switch v-model="monitorEnabled" :active-text="t('monitor.enable')" @change="toggleMonitor" />
    </div>

    <el-table :data="monitorList" size="small" border style="margin-top: 12px">
      <el-table-column prop="port" :label="t('monitor.port')" width="80" />
      <el-table-column prop="remark" :label="t('monitor.remark')" />
      <el-table-column prop="expectedState" :label="t('monitor.expectedState')" width="100">
        <template #default="{ row }">
          <el-select v-model="row.expectedState" size="small" @change="saveConfig">
            <el-option :label="t('monitor.any')" value="any" />
            <el-option :label="t('monitor.occupied')" value="occupied" />
            <el-option :label="t('monitor.free')" value="free" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column :label="t('monitor.currentState')" width="100">
        <template #default="{ row }">
          <el-tag :type="row.lastOccupied ? 'danger' : 'success'" size="small">
            {{ row.lastOccupied ? t('monitor.occupied') : t('monitor.free') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.action')" width="80">
        <template #default="{ $index }">
          <el-button link type="danger" size="small" @click="removePort($index)">{{ t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import request from '@/api'
import { loadFromStorage, saveToStorage, STORAGE_KEYS } from '@/utils/storage'
import { syncMonitorConfig, connectMonitorWs, disconnectMonitorWs, isMonitorWsConnected } from '@/utils/monitorWs'

defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'alert', 'monitor-change'])

const { t } = useI18n()

const monitorList = ref([])
const monitorEnabled = ref(false)
const newPort = ref('')
const newRemark = ref('')
const wsConnected = ref(false)
let monitorTimer = null
let lastSnapshot = {}
let pollIntervalMs = 5000

async function loadPollInterval() {
  try {
    const res = await request.get('/system/config')
    if (res.data?.monitorPollIntervalMs) {
      pollIntervalMs = res.data.monitorPollIntervalMs
    }
  } catch { /* use default */ }
}

function loadConfig() {
  loadPollInterval()
  const config = loadFromStorage(STORAGE_KEYS.MONITOR, { enabled: false, ports: [] })
  monitorList.value = config.ports || []
  monitorEnabled.value = config.enabled || false
  lastSnapshot = {}
  monitorList.value.forEach(p => {
    lastSnapshot[p.port] = p.lastOccupied
  })
  wsConnected.value = isMonitorWsConnected()
  if (monitorEnabled.value) startMonitor()
}

async function pushConfigToServer() {
  const config = { enabled: monitorEnabled.value, ports: monitorList.value }
  try {
    await syncMonitorConfig(request, config)
  } catch { /* ignore */ }
}

function saveConfig() {
  const config = { enabled: monitorEnabled.value, ports: monitorList.value }
  saveToStorage(STORAGE_KEYS.MONITOR, config)
  pushConfigToServer()
  emit('monitor-change', config)
}

function addPort() {
  const port = parseInt(newPort.value)
  if (!port || port < 1 || port > 65535) {
    ElMessage.warning(t('monitor.invalidPort'))
    return
  }
  if (monitorList.value.some(p => p.port === port)) {
    ElMessage.info(t('monitor.alreadyExists'))
    return
  }
  monitorList.value.push({
    port,
    protocol: 'TCP',
    remark: newRemark.value || `Port ${port}`,
    expectedState: 'any',
    lastOccupied: null
  })
  newPort.value = ''
  newRemark.value = ''
  saveConfig()
}

function removePort(index) {
  monitorList.value.splice(index, 1)
  saveConfig()
}

function toggleMonitor(enabled) {
  monitorEnabled.value = enabled
  saveConfig()
  if (enabled) startMonitor()
  else stopMonitorFully()
}

function stopLocalPoll() {
  if (monitorTimer) {
    clearInterval(monitorTimer)
    monitorTimer = null
  }
}

function stopMonitorFully() {
  stopLocalPoll()
  emit('monitor-change', { enabled: false, ports: monitorList.value })
}

function startMonitor() {
  stopLocalPoll()
  connectMonitorWs((alerts) => {
    wsConnected.value = true
    emit('alert', alerts)
  })
  pushConfigToServer()
  pollMonitor()
  monitorTimer = setInterval(pollMonitor, pollIntervalMs)
  wsConnected.value = isMonitorWsConnected()
}

function onMonitorDialogClose() {
  stopLocalPoll()
}

async function pollMonitor() {
  if (monitorList.value.length === 0) return
  wsConnected.value = isMonitorWsConnected()
  try {
    const res = await request.post('/ports/monitor', {
      ports: monitorList.value.map(p => ({
        port: p.port,
        protocol: p.protocol,
        remark: p.remark,
        expectedState: p.expectedState
      }))
    })
    const results = res.data || []
    const alerts = []

    results.forEach(r => {
      const item = monitorList.value.find(p => p.port === r.port)
      if (!item) return

      const prev = lastSnapshot[r.port]
      const changed = prev !== undefined && prev !== null && prev !== r.occupied

      if (changed) alerts.push(r)

      if (item.expectedState === 'occupied' && !r.occupied) {
        alerts.push({ ...r, remark: item.remark, _reason: 'expected_occupied' })
      } else if (item.expectedState === 'free' && r.occupied) {
        alerts.push({ ...r, remark: item.remark, _reason: 'expected_free' })
      }

      item.lastOccupied = r.occupied
      lastSnapshot[r.port] = r.occupied
    })

    saveToStorage(STORAGE_KEYS.MONITOR, { enabled: monitorEnabled.value, ports: monitorList.value })
    if (alerts.length > 0) emit('alert', alerts)
  } catch { /* ignore */ }
}
</script>

<style scoped>
.monitor-form {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.monitor-status {
  margin-bottom: 8px;
}
</style>
