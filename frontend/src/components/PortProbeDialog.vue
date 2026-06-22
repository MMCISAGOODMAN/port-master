<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :title="t('probe.title')"
    width="680px"
    @open="onOpen"
  >
    <el-tabs v-model="probeType">
      <el-tab-pane :label="t('probe.tcp')" name="tcp">
        <el-form inline>
          <el-form-item :label="t('probe.host')">
            <el-input v-model="host" placeholder="127.0.0.1" style="width: 140px" />
          </el-form-item>
          <el-form-item :label="t('probe.port')">
            <el-input v-model="portInput" placeholder="8080 或 8080,9090" style="width: 180px" @keyup.enter="runProbe" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="runProbe">{{ t('probe.probe') }}</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      <el-tab-pane :label="t('probe.http')" name="http">
        <el-form inline>
          <el-form-item :label="t('probe.host')">
            <el-input v-model="host" placeholder="127.0.0.1" style="width: 120px" />
          </el-form-item>
          <el-form-item :label="t('probe.port')">
            <el-input-number v-model="httpPort" :min="1" :max="65535" controls-position="right" />
          </el-form-item>
          <el-form-item :label="t('probe.path')">
            <el-input v-model="httpPath" placeholder="/" style="width: 120px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="runHttpProbe">{{ t('probe.probe') }}</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      <el-tab-pane :label="t('probe.tls')" name="tls">
        <el-form inline>
          <el-form-item :label="t('probe.host')">
            <el-input v-model="host" placeholder="127.0.0.1" style="width: 140px" />
          </el-form-item>
          <el-form-item :label="t('probe.port')">
            <el-input-number v-model="tlsPort" :min="1" :max="65535" controls-position="right" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="runTlsProbe">{{ t('probe.probe') }}</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>

    <el-table v-if="results.length" :data="results" size="small" border style="margin-top: 12px">
      <el-table-column prop="probeType" :label="t('probe.type')" width="70" />
      <el-table-column prop="port" :label="t('probe.port')" width="70" />
      <el-table-column prop="host" :label="t('probe.host')" width="110" />
      <el-table-column :label="t('probe.result')" width="90">
        <template #default="{ row }">
          <el-tag :type="row.reachable ? 'success' : 'danger'" size="small">
            {{ row.reachable ? t('probe.success') : t('probe.failed') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="httpStatus" label="HTTP" width="70">
        <template #default="{ row }">{{ row.httpStatus || '-' }}</template>
      </el-table-column>
      <el-table-column prop="latencyMs" :label="t('probe.latency')" width="85" />
      <el-table-column prop="message" :label="t('probe.detail')" min-width="120" show-overflow-tooltip />
      <el-table-column prop="certInfo" :label="t('probe.cert')" min-width="140" show-overflow-tooltip />
    </el-table>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import request from '@/api'

const { t } = useI18n()

const props = defineProps({
  modelValue: Boolean,
  initialPort: { type: [String, Number], default: '' }
})
defineEmits(['update:modelValue'])

const probeType = ref('tcp')
const host = ref('127.0.0.1')
const portInput = ref('')
const httpPort = ref(8080)
const httpPath = ref('/')
const tlsPort = ref(443)
const loading = ref(false)
const results = ref([])

function onOpen() {
  results.value = []
  if (props.initialPort) {
    portInput.value = String(props.initialPort)
    httpPort.value = parseInt(props.initialPort) || 8080
    tlsPort.value = parseInt(props.initialPort) || 443
    runProbe()
  }
}

async function runProbe() {
  const ports = portInput.value.split(/[,，\s]+/).map(s => parseInt(s.trim())).filter(p => p > 0 && p <= 65535)
  if (ports.length === 0) {
    ElMessage.warning(t('probe.invalidPort'))
    return
  }
  loading.value = true
  results.value = []
  try {
    if (ports.length === 1) {
      const res = await request.get('/ports/probe', { params: { port: ports[0], host: host.value } })
      results.value = [res.data]
    } else {
      const res = await request.post('/ports/probe/batch', { host: host.value, ports, timeout: 3000 })
      results.value = res.data || []
    }
  } finally {
    loading.value = false
  }
}

async function runHttpProbe() {
  loading.value = true
  try {
    const res = await request.get('/ports/probe/http', {
      params: { port: httpPort.value, host: host.value, path: httpPath.value }
    })
    results.value = [res.data]
  } finally {
    loading.value = false
  }
}

async function runTlsProbe() {
  loading.value = true
  try {
    const res = await request.get('/ports/probe/tls', {
      params: { port: tlsPort.value, host: host.value }
    })
    results.value = [res.data]
  } finally {
    loading.value = false
  }
}
</script>
