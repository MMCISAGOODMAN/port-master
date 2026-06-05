<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="端口监控告警"
    width="600px"
    @open="loadConfig"
    @close="stopMonitor"
  >
    <el-alert
      title="监控配置保存在浏览器本地，后端定时轮询探测端口状态变化"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 16px"
    />

    <div class="monitor-form">
      <el-input v-model="newPort" placeholder="端口号" style="width: 120px" @keyup.enter="addPort" />
      <el-input v-model="newRemark" placeholder="备注" style="width: 200px" />
      <el-button type="primary" @click="addPort">添加监控</el-button>
      <el-switch v-model="monitorEnabled" active-text="启用监控" @change="toggleMonitor" />
    </div>

    <el-table :data="monitorList" size="small" border style="margin-top: 12px">
      <el-table-column prop="port" label="端口" width="80" />
      <el-table-column prop="remark" label="备注" />
      <el-table-column prop="expectedState" label="期望状态" width="100">
        <template #default="{ row }">
          <el-select v-model="row.expectedState" size="small" @change="saveConfig">
            <el-option label="任意" value="any" />
            <el-option label="占用" value="occupied" />
            <el-option label="空闲" value="free" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="当前状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.lastOccupied ? 'danger' : 'success'" size="small">
            {{ row.lastOccupied ? '占用' : '空闲' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80">
        <template #default="{ $index }">
          <el-button link type="danger" size="small" @click="removePort($index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api'
import { loadFromStorage, saveToStorage, STORAGE_KEYS } from '@/utils/storage'

defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'alert'])

const monitorList = ref([])
const monitorEnabled = ref(false)
const newPort = ref('')
const newRemark = ref('')
let monitorTimer = null
let lastSnapshot = {}

function loadConfig() {
  const config = loadFromStorage(STORAGE_KEYS.MONITOR, { enabled: false, ports: [] })
  monitorList.value = config.ports || []
  monitorEnabled.value = config.enabled || false
  lastSnapshot = {}
  monitorList.value.forEach(p => {
    lastSnapshot[p.port] = p.lastOccupied
  })
  if (monitorEnabled.value) startMonitor()
}

function saveConfig() {
  saveToStorage(STORAGE_KEYS.MONITOR, {
    enabled: monitorEnabled.value,
    ports: monitorList.value
  })
}

function addPort() {
  const port = parseInt(newPort.value)
  if (!port || port < 1 || port > 65535) {
    ElMessage.warning('请输入有效端口号')
    return
  }
  if (monitorList.value.some(p => p.port === port)) {
    ElMessage.info('该端口已在监控列表中')
    return
  }
  monitorList.value.push({
    port,
    protocol: 'TCP',
    remark: newRemark.value || `端口 ${port}`,
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
  else stopMonitor()
}

function startMonitor() {
  stopMonitor()
  pollMonitor()
  monitorTimer = setInterval(pollMonitor, 5000)
}

function stopMonitor() {
  if (monitorTimer) {
    clearInterval(monitorTimer)
    monitorTimer = null
  }
}

async function pollMonitor() {
  if (monitorList.value.length === 0) return
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

      if (changed) {
        alerts.push(r)
      }

      // 期望状态告警
      if (item.expectedState === 'occupied' && !r.occupied) {
        alerts.push({ ...r, remark: item.remark, _reason: '期望占用但已释放' })
      } else if (item.expectedState === 'free' && r.occupied) {
        alerts.push({ ...r, remark: item.remark, _reason: '期望空闲但被占用' })
      }

      item.lastOccupied = r.occupied
      lastSnapshot[r.port] = r.occupied
    })

    saveConfig()
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
</style>
