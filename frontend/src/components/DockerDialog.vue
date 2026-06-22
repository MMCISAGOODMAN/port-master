<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :title="t('docker.title')"
    width="760px"
    @open="loadContainers"
  >
    <div class="toolbar">
      <el-tag :type="dockerAvailable ? 'success' : 'danger'">
        {{ dockerAvailable ? t('docker.available') : t('docker.unavailable') }}
      </el-tag>
      <el-checkbox v-model="showAll" @change="loadContainers">{{ t('docker.showStopped') }}</el-checkbox>
      <el-button :icon="Refresh" :loading="loading" @click="loadContainers">{{ t('common.refresh') }}</el-button>
    </div>

    <el-table :data="containers" v-loading="loading" size="small" border max-height="420" style="margin-top: 12px">
      <el-table-column prop="containerId" :label="t('docker.id')" width="100">
        <template #default="{ row }">{{ row.containerId?.slice(0, 12) }}</template>
      </el-table-column>
      <el-table-column prop="name" :label="t('docker.name')" min-width="120" show-overflow-tooltip />
      <el-table-column prop="image" :label="t('docker.image')" min-width="140" show-overflow-tooltip />
      <el-table-column prop="status" :label="t('docker.status')" width="120" show-overflow-tooltip />
      <el-table-column :label="t('docker.portMapping')" min-width="180">
        <template #default="{ row }">
          <el-tag v-for="(m, i) in row.portMappings" :key="i" size="small" style="margin: 2px">
            {{ m.hostPort }}→{{ m.containerPort }}/{{ m.protocol }}
          </el-tag>
          <span v-if="!row.portMappings?.length" class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.action')" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="queryPort(row)">{{ t('docker.queryPort') }}</el-button>
          <el-button link type="warning" size="small" @click="restart(row)">{{ t('docker.restart') }}</el-button>
          <el-button link type="danger" size="small" @click="stop(row)">{{ t('docker.stop') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import request from '@/api'

const { t } = useI18n()

defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'query-port'])

const containers = ref([])
const loading = ref(false)
const dockerAvailable = ref(false)
const showAll = ref(false)

async function loadContainers() {
  loading.value = true
  try {
    const avail = await request.get('/docker/available')
    dockerAvailable.value = avail.data
    if (!avail.data) {
      containers.value = []
      return
    }
    const res = await request.get('/docker/containers', { params: { all: showAll.value } })
    containers.value = res.data || []
  } finally {
    loading.value = false
  }
}

function queryPort(row) {
  const ports = (row.portMappings || []).map(m => m.hostPort).filter(Boolean)
  if (ports.length === 0) {
    ElMessage.info(t('docker.noPorts'))
    return
  }
  emit('query-port', ports.join(','))
  emit('update:modelValue', false)
}

async function stop(row) {
  await ElMessageBox.confirm(t('docker.confirmStop', { name: row.name }), t('common.confirmTitle'), { type: 'warning' })
  await request.post('/docker/stop', { containerId: row.containerId })
  ElMessage.success(t('docker.stopped'))
  loadContainers()
}

async function restart(row) {
  await request.post('/docker/restart', { containerId: row.containerId })
  ElMessage.success(t('docker.restarted'))
  loadContainers()
}
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 12px; }
</style>
