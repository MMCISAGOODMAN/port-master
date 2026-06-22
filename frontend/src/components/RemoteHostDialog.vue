<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :title="t('remote.title')"
    width="720px"
    @open="onOpen"
  >
    <el-alert type="info" show-icon :closable="false" style="margin-bottom: 12px">
      {{ t('remote.hint') }}
    </el-alert>

    <el-form :model="form" label-width="80px" size="small">
      <el-row :gutter="12">
        <el-col :span="10">
          <el-form-item :label="t('remote.host')">
            <el-input v-model="form.host" placeholder="192.168.1.100" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item :label="t('remote.sshPort')">
            <el-input-number v-model="form.port" :min="1" :max="65535" controls-position="right" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="t('remote.username')">
            <el-input v-model="form.username" placeholder="root" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="8">
          <el-form-item :label="t('remote.authType')">
            <el-select v-model="form.authType" style="width: 100%">
              <el-option :label="t('remote.passwordAuth')" value="password" />
              <el-option :label="t('remote.keyAuth')" value="key" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="16">
          <el-form-item :label="form.authType === 'key' ? t('remote.privateKey') : t('remote.password')">
            <el-input v-model="form.credential" :type="form.authType === 'key' ? 'textarea' : 'password'"
              :rows="form.authType === 'key' ? 2 : 1" :placeholder="form.authType === 'key' ? t('remote.keyPlaceholder') : t('remote.passwordPlaceholder')" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="action-bar">
      <el-button :loading="testing" @click="testConnection">{{ t('remote.testConnection') }}</el-button>
      <el-button type="primary" :loading="scanning" @click="scanRemote">{{ t('remote.scanRemote') }}</el-button>
      <el-tag v-if="remoteInfo" type="success">{{ remoteInfo }}</el-tag>
    </div>

    <el-divider content-position="left">{{ t('remote.savedHosts') }}</el-divider>
    <div class="saved-hosts">
      <el-tag
        v-for="h in savedHosts"
        :key="h.id"
        class="host-tag"
        closable
        @click="loadHost(h)"
        @close="removeHost(h.id)"
      >
        {{ h.name || h.host }}:{{ h.port }}
      </el-tag>
      <el-button v-if="form.host" size="small" link type="primary" @click="saveCurrentHost">{{ t('remote.saveHost') }}</el-button>
    </div>

    <el-table v-if="remotePorts.length" :data="remotePorts" size="small" border max-height="320" style="margin-top: 12px">
      <el-table-column prop="protocol" :label="t('table.protocol')" width="60" />
      <el-table-column prop="port" :label="t('table.port')" width="80" sortable />
      <el-table-column prop="processName" :label="t('remote.process')" min-width="100" show-overflow-tooltip />
      <el-table-column prop="state" :label="t('table.state')" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.state === 'LISTEN' ? 'success' : 'info'">{{ row.state }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="pid" :label="t('table.pid')" width="70" />
      <el-table-column :label="t('common.action')" width="120" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.pid" link type="danger" size="small" @click="killRemote(row.pid, false)">{{ t('table.kill') }}</el-button>
          <el-button v-if="row.pid" link type="danger" size="small" @click="killRemote(row.pid, true)">{{ t('table.forceKill') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api'
import { loadFromStorage, saveToStorage, STORAGE_KEYS } from '@/utils/storage'

const { t } = useI18n()

defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'scan-result'])

const form = ref({ host: '', port: 22, username: 'root', credential: '', authType: 'password' })
const savedHosts = ref([])
const remotePorts = ref([])
const remoteInfo = ref('')
const testing = ref(false)
const scanning = ref(false)

function onOpen() {
  savedHosts.value = loadFromStorage(STORAGE_KEYS.REMOTE_HOSTS, [])
}

function getPayload() {
  return { ...form.value }
}

async function testConnection() {
  testing.value = true
  remoteInfo.value = ''
  try {
    const res = await request.post('/remote/test', getPayload())
    if (res.data) {
      ElMessage.success(t('remote.connectSuccess'))
      const infoRes = await request.post('/remote/info', getPayload())
      remoteInfo.value = infoRes.data || 'connected'
    } else {
      ElMessage.error(t('remote.connectFailed'))
    }
  } finally {
    testing.value = false
  }
}

async function scanRemote() {
  scanning.value = true
  try {
    const res = await request.post('/remote/scan', getPayload())
    remotePorts.value = res.data || []
    ElMessage.success(t('remote.scanDone', { count: remotePorts.value.length }))
    emit('scan-result', remotePorts.value)
  } finally {
    scanning.value = false
  }
}

async function killRemote(pid, force) {
  const action = force ? t('table.forceEnd') : t('table.kill')
  await ElMessageBox.confirm(t('remote.confirmKillRemote', { action, pid }), t('remote.remoteOperation'), { type: 'warning' })
  await request.post('/remote/kill', { ...getPayload(), pid, force })
  ElMessage.success(t('remote.killDone'))
  scanRemote()
}

function saveCurrentHost() {
  if (!form.value.host) return
  const hosts = [...savedHosts.value]
  const entry = {
    id: 'host_' + Date.now(),
    name: form.value.host,
    host: form.value.host,
    port: form.value.port,
    username: form.value.username,
    authType: form.value.authType
  }
  hosts.push(entry)
  savedHosts.value = hosts
  saveToStorage(STORAGE_KEYS.REMOTE_HOSTS, hosts)
  ElMessage.success(t('remote.hostSaved'))
}

function loadHost(h) {
  form.value = { ...form.value, host: h.host, port: h.port, username: h.username, authType: h.authType || 'password', credential: '' }
}

function removeHost(id) {
  savedHosts.value = savedHosts.value.filter(h => h.id !== id)
  saveToStorage(STORAGE_KEYS.REMOTE_HOSTS, savedHosts.value)
}
</script>

<style scoped>
.action-bar { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.saved-hosts { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; }
.host-tag { cursor: pointer; }
</style>
