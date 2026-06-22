<template>
  <div class="port-table-wrapper card-shadow">
    <div class="table-toolbar">
      <el-button type="danger" size="small" :disabled="selectedRows.length === 0" @click="handleBatchKill(false)">
        {{ t('table.batchKill') }}
      </el-button>
      <el-button type="danger" size="small" plain :disabled="selectedRows.length === 0" @click="handleBatchKill(true)">
        {{ t('table.batchForceKill') }}
      </el-button>
      <span v-if="selectedRows.length" class="text-muted">{{ t('table.selected', { count: selectedRows.length }) }}</span>
    </div>

    <div class="table-body">
      <el-table
        :data="paginatedData"
        v-loading="loading"
        stripe
        border
        height="100%"
        :row-class-name="rowClassName"
        @selection-change="handleSelectionChange"
        @row-click="(row) => $emit('row-click', row)"
        @sort-change="handleSortChange"
        style="width: 100%"
      >
        <el-table-column type="selection" width="45" :selectable="row => !!row.pid" />
        <el-table-column prop="protocol" :label="t('table.protocol')" width="68" sortable="custom" />
        <el-table-column prop="port" :label="t('table.port')" width="100" sortable="custom">
          <template #default="{ row }">
            <span>{{ row.port }}</span>
            <el-tag v-if="getDiffType(row) === 'new'" size="small" type="success" class="diff-tag">{{ t('table.tagNew') }}</el-tag>
            <el-tag v-else-if="getDiffType(row) === 'changed'" size="small" type="warning" class="diff-tag">{{ t('table.tagChanged') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('table.service')" width="110" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="service-name">{{ getServiceName(row.port) || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="localAddress" :label="t('table.localAddress')" min-width="140" show-overflow-tooltip />
        <el-table-column prop="foreignAddress" :label="t('table.foreignAddress')" min-width="130" show-overflow-tooltip />
        <el-table-column prop="pid" :label="t('table.pid')" width="80" sortable="custom" />
        <el-table-column prop="processName" :label="t('table.processName')" min-width="110" show-overflow-tooltip />
        <el-table-column prop="state" :label="t('table.state')" width="110" sortable="custom">
          <template #default="{ row }">
            <el-tag :type="stateTagType(row.state)" size="small">{{ row.state }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.action')" width="340" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="copyRow(row)">{{ t('common.copy') }}</el-button>
            <el-button v-if="canOpen(row)" link type="primary" size="small" @click.stop="openUrl(row)">{{ t('table.open') }}</el-button>
            <el-button link type="primary" size="small" @click.stop="$emit('probe', row.port)" :disabled="row.state === 'FREE'">{{ t('table.probe') }}</el-button>
            <el-button link type="primary" size="small" @click.stop="$emit('row-click', row)" :disabled="!row.pid">{{ t('table.detail') }}</el-button>
            <el-button link type="primary" size="small" @click.stop="$emit('add-to-group', row)" :disabled="row.state === 'FREE'">{{ t('table.favorite') }}</el-button>
            <el-button v-if="row.state === 'LISTEN' && row.port" link type="warning" size="small" @click.stop="confirmKillByPort(row.port, false)">{{ t('table.freePort') }}</el-button>
            <el-button v-if="row.pid" link type="warning" size="small" @click.stop="confirmKill(row.pid, false)">{{ t('table.kill') }}</el-button>
            <el-button v-if="row.pid" link type="danger" size="small" @click.stop="confirmKill(row.pid, true)">{{ t('table.forceKill') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pagination-bar">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[20, 50, 100, 200]"
        :total="sortedData.length"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getServiceName, isWebPort, getOpenUrl } from '@/utils/portServices'
import { rowKey } from '@/utils/scanDiff'

const { t } = useI18n()

const props = defineProps({
  data: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  conflictPorts: { type: Set, default: () => new Set() },
  diffMap: { type: Map, default: () => new Map() },
  defaultPageSize: { type: Number, default: 50 }
})

const emit = defineEmits(['row-click', 'kill', 'batch-kill', 'add-to-group', 'kill-by-port', 'probe'])

const selectedRows = ref([])
const currentPage = ref(1)
const pageSize = ref(props.defaultPageSize)
const sortProp = ref('')
const sortOrder = ref('')

watch(() => props.data, () => { currentPage.value = 1 })
watch(() => props.defaultPageSize, (val) => { pageSize.value = val })

const sortedData = computed(() => {
  if (!sortProp.value) return props.data
  const data = [...props.data]
  const prop = sortProp.value
  const order = sortOrder.value
  data.sort((a, b) => {
    let va = a[prop], vb = b[prop]
    if (va == null) va = ''
    if (vb == null) vb = ''
    if (typeof va === 'number' && typeof vb === 'number') {
      return order === 'ascending' ? va - vb : vb - va
    }
    return order === 'ascending'
      ? String(va).localeCompare(String(vb))
      : String(vb).localeCompare(String(va))
  })
  return data
})

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return sortedData.value.slice(start, start + pageSize.value)
})

function getDiffType(row) {
  return props.diffMap.get(rowKey(row))
}

function rowClassName({ row }) {
  if (row.state === 'LISTEN' && props.conflictPorts.has(row.port)) return 'conflict-row'
  const diff = getDiffType(row)
  if (diff === 'new') return 'diff-new-row'
  if (diff === 'changed') return 'diff-changed-row'
  return ''
}

function canOpen(row) {
  return row.state === 'LISTEN' && isWebPort(row.port)
}

function openUrl(row) {
  const url = getOpenUrl(row)
  if (url) window.open(url, '_blank')
}

function handleSelectionChange(rows) { selectedRows.value = rows }
function handleSortChange({ prop, order }) { sortProp.value = prop; sortOrder.value = order }

function stateTagType(state) {
  const map = { LISTEN: 'success', ESTABLISHED: 'primary', TIME_WAIT: 'warning', CLOSE_WAIT: 'danger', FREE: 'info' }
  return map[state] || 'info'
}

function copyRow(row) {
  const svc = getServiceName(row.port)
  const text = [row.protocol, row.port, svc, row.localAddress, row.foreignAddress,
    row.pid || '', row.processName, row.state].join('\t')
  navigator.clipboard.writeText(text).then(() => ElMessage.success(t('table.copied')))
}

function confirmKill(pid, force) {
  const action = force ? t('table.forceEnd') : t('table.normalEnd')
  ElMessageBox.confirm(t('table.confirmKill', { action, pid }), t('common.confirmTitle'), { type: 'warning' })
    .then(() => emit('kill', pid, force)).catch(() => {})
}

function confirmKillByPort(port, force) {
  const action = force ? t('table.forceFreePort') : t('table.freePortAction')
  ElMessageBox.confirm(t('table.confirmKillByPort', { action, port }), t('table.freePort'), { type: 'warning' })
    .then(() => emit('kill-by-port', port, force)).catch(() => {})
}

function handleBatchKill(force) {
  const pids = selectedRows.value.map(r => r.pid).filter(Boolean)
  if (!pids.length) return
  const action = force ? t('table.forceEnd') : t('table.normalEnd')
  ElMessageBox.confirm(t('table.confirmBatchKill', { action, count: pids.length }), t('table.batchOperation'), { type: 'warning' })
    .then(() => emit('batch-kill', pids, force)).catch(() => {})
}
</script>

<style scoped>
.port-table-wrapper {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: var(--pm-bg-card);
  border-radius: 8px;
  padding: 12px;
  transition: background-color 0.25s;
}

.table-toolbar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.table-body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.pagination-bar {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
  padding-top: 4px;
}

.diff-tag { margin-left: 4px; transform: scale(0.85); }
.service-name { color: var(--pm-text-muted); font-size: 12px; }
:deep(.conflict-row) { background-color: var(--pm-conflict-bg) !important; }
:deep(.diff-new-row) { background-color: rgba(103, 194, 58, 0.12) !important; }
:deep(.diff-changed-row) { background-color: rgba(230, 162, 60, 0.12) !important; }
html.dark :deep(.diff-new-row) { background-color: rgba(103, 194, 58, 0.18) !important; }
html.dark :deep(.diff-changed-row) { background-color: rgba(230, 162, 60, 0.18) !important; }
</style>
