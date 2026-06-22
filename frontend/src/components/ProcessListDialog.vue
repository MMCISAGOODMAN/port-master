<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :title="t('process.title')"
    width="900px"
    destroy-on-close
    @open="fetchProcesses"
  >
    <div class="toolbar">
      <el-input v-model="keyword" :placeholder="t('process.searchPlaceholder')" clearable style="width: 240px" :prefix-icon="Search" />
      <el-button :icon="Refresh" @click="fetchProcesses" :loading="loading">{{ t('common.refresh') }}</el-button>
      <span class="text-muted">{{ t('process.total', { count: filteredList.length }) }}</span>
    </div>

    <el-table :data="paginatedList" v-loading="loading" stripe border max-height="480" @row-click="handleRowClick">
      <el-table-column prop="pid" :label="t('table.pid')" width="90" sortable />
      <el-table-column prop="processName" :label="t('process.processName')" min-width="140" show-overflow-tooltip />
      <el-table-column prop="cpuPercent" :label="t('process.cpu')" width="80" sortable>
        <template #default="{ row }">{{ row.cpuPercent?.toFixed(1) || 0 }}</template>
      </el-table-column>
      <el-table-column prop="memoryPercent" :label="t('process.memory')" width="80" sortable>
        <template #default="{ row }">{{ row.memoryPercent?.toFixed(1) || 0 }}</template>
      </el-table-column>
      <el-table-column prop="memoryUsage" :label="t('process.memoryUsage')" width="100" />
      <el-table-column prop="portCount" :label="t('process.portCount')" width="80" sortable />
      <el-table-column :label="t('common.action')" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click.stop="$emit('view-detail', row.pid)">{{ t('process.detail') }}</el-button>
          <el-button link type="primary" size="small" @click.stop="$emit('view-ports', row.pid)">{{ t('process.viewPorts') }}</el-button>
          <el-button link type="warning" size="small" @click.stop="confirmKill(row.pid, false)">{{ t('table.kill') }}</el-button>
          <el-button link type="danger" size="small" @click.stop="confirmKill(row.pid, true)">{{ t('table.forceKill') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-bar">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="filteredList.length"
        layout="total, prev, pager, next"
        background
        small
      />
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search, Refresh } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import request from '@/api'

const { t } = useI18n()

defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'view-detail', 'view-ports', 'kill'])

const list = ref([])
const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(50)

const filteredList = computed(() => {
  if (!keyword.value) return list.value
  const kw = keyword.value.toLowerCase()
  return list.value.filter(p =>
    String(p.pid).includes(kw) ||
    (p.processName || '').toLowerCase().includes(kw)
  )
})

const paginatedList = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

async function fetchProcesses() {
  loading.value = true
  try {
    const res = await request.get('/process/list')
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

function handleRowClick(row) {
  emit('view-detail', row.pid)
}

function confirmKill(pid, force) {
  const action = force ? t('table.forceEnd') : t('table.normalEnd')
  ElMessageBox.confirm(t('table.confirmKill', { action, pid }), t('common.confirmTitle'), { type: 'warning' })
    .then(() => emit('kill', pid, force))
    .catch(() => {})
}
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
