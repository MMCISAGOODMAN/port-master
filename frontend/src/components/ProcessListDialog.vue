<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="进程列表"
    width="900px"
    destroy-on-close
    @open="fetchProcesses"
  >
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索 PID / 进程名" clearable style="width: 240px" :prefix-icon="Search" />
      <el-button :icon="Refresh" @click="fetchProcesses" :loading="loading">刷新</el-button>
      <span class="text-muted">共 {{ filteredList.length }} 个进程</span>
    </div>

    <el-table :data="paginatedList" v-loading="loading" stripe border max-height="480" @row-click="handleRowClick">
      <el-table-column prop="pid" label="PID" width="90" sortable />
      <el-table-column prop="processName" label="进程名" min-width="140" show-overflow-tooltip />
      <el-table-column prop="cpuPercent" label="CPU%" width="80" sortable>
        <template #default="{ row }">{{ row.cpuPercent?.toFixed(1) || 0 }}</template>
      </el-table-column>
      <el-table-column prop="memoryPercent" label="内存%" width="80" sortable>
        <template #default="{ row }">{{ row.memoryPercent?.toFixed(1) || 0 }}</template>
      </el-table-column>
      <el-table-column prop="memoryUsage" label="内存占用" width="100" />
      <el-table-column prop="portCount" label="端口数" width="80" sortable />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click.stop="$emit('view-detail', row.pid)">详情</el-button>
          <el-button link type="primary" size="small" @click.stop="$emit('view-ports', row.pid)">查端口</el-button>
          <el-button link type="warning" size="small" @click.stop="confirmKill(row.pid, false)">结束</el-button>
          <el-button link type="danger" size="small" @click.stop="confirmKill(row.pid, true)">强杀</el-button>
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
import { Search, Refresh } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import request from '@/api'

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
  const action = force ? '强制杀死' : '正常结束'
  ElMessageBox.confirm(`确定${action}进程 PID: ${pid}？`, '确认', { type: 'warning' })
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
