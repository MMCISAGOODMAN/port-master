<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :title="t('process.detailTitle')"
    width="680px"
    destroy-on-close
    @open="startRefresh"
    @close="stopRefresh"
  >
    <div v-loading="loading">
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item :label="t('table.pid')">{{ detail.pid }}</el-descriptions-item>
        <el-descriptions-item :label="t('process.processName')">{{ detail.processName || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('process.cpuUsage')">{{ detail.cpuPercent?.toFixed(1) || 0 }}%</el-descriptions-item>
        <el-descriptions-item :label="t('process.memoryUsage')">{{ detail.memoryUsage || (detail.memoryPercent?.toFixed(1) + '%') || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('process.createTime')" :span="2">{{ detail.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('process.programPath')" :span="2">
          <span class="path-text">{{ detail.programPath || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('process.commandLine')" :span="2">
          <span class="path-text">{{ detail.commandLine || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <div class="bound-ports" v-if="detail?.boundPorts?.length">
        <h4>{{ t('process.boundPorts', { count: detail.boundPorts.length }) }}</h4>
        <el-table :data="detail.boundPorts" size="small" border max-height="200">
          <el-table-column prop="protocol" :label="t('process.protocol')" width="70" />
          <el-table-column prop="port" :label="t('process.port')" width="80" />
          <el-table-column prop="localAddress" :label="t('process.localAddress')" />
          <el-table-column prop="state" :label="t('process.state')" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ row.state }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div class="refresh-hint text-muted">
      <el-icon><Refresh /></el-icon> {{ t('process.autoRefresh') }}
    </div>

    <template #footer>
      <el-button @click="fetchDetail" :icon="Refresh">{{ t('common.refresh') }}</el-button>
      <el-button type="warning" @click="handleKill(false)">{{ t('table.normalEnd') }}</el-button>
      <el-button type="danger" @click="handleKill(true)">{{ t('table.forceEnd') }}</el-button>
      <el-button @click="$emit('update:modelValue', false)">{{ t('common.close') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api'

const { t } = useI18n()

const props = defineProps({
  modelValue: Boolean,
  pid: Number
})

const emit = defineEmits(['update:modelValue'])

const detail = ref(null)
const loading = ref(false)
let refreshTimer = null

watch(() => props.pid, () => {
  if (props.modelValue && props.pid) fetchDetail()
})

async function fetchDetail() {
  if (!props.pid) return
  loading.value = true
  try {
    const res = await request.get(`/process/${props.pid}`)
    detail.value = res.data
  } catch { /* handled */ }
  finally { loading.value = false }
}

function startRefresh() {
  fetchDetail()
  refreshTimer = setInterval(fetchDetail, 2000)
}

function stopRefresh() {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
  detail.value = null
}

async function handleKill(force) {
  const action = force ? t('table.forceEnd') : t('table.normalEnd')
  try {
    await ElMessageBox.confirm(t('table.confirmKill', { action, pid: props.pid }), t('common.confirmTitle'), { type: 'warning' })
    const url = force ? `/process/${props.pid}/force` : `/process/${props.pid}`
    const res = await request.delete(url)
    ElMessage.success(res.message || t('common.success'))
    emit('update:modelValue', false)
  } catch { /* cancelled or error */ }
}
</script>

<style scoped>
.path-text {
  word-break: break-all;
  font-size: 13px;
  font-family: monospace;
}

.bound-ports {
  margin-top: 16px;
}

.bound-ports h4 {
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
}

.refresh-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 12px;
  font-size: 12px;
}
</style>
