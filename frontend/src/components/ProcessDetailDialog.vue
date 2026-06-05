<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="进程详情"
    width="680px"
    destroy-on-close
    @open="startRefresh"
    @close="stopRefresh"
  >
    <div v-loading="loading">
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="PID">{{ detail.pid }}</el-descriptions-item>
        <el-descriptions-item label="进程名">{{ detail.processName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="CPU 占用">{{ detail.cpuPercent?.toFixed(1) || 0 }}%</el-descriptions-item>
        <el-descriptions-item label="内存占用">{{ detail.memoryUsage || (detail.memoryPercent?.toFixed(1) + '%') || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ detail.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="程序路径" :span="2">
          <span class="path-text">{{ detail.programPath || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="启动命令" :span="2">
          <span class="path-text">{{ detail.commandLine || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <div class="bound-ports" v-if="detail?.boundPorts?.length">
        <h4>绑定端口 ({{ detail.boundPorts.length }})</h4>
        <el-table :data="detail.boundPorts" size="small" border max-height="200">
          <el-table-column prop="protocol" label="协议" width="70" />
          <el-table-column prop="port" label="端口" width="80" />
          <el-table-column prop="localAddress" label="本地地址" />
          <el-table-column prop="state" label="状态" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ row.state }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div class="refresh-hint text-muted">
      <el-icon><Refresh /></el-icon> 数据每 2 秒自动刷新
    </div>

    <template #footer>
      <el-button @click="fetchDetail" :icon="Refresh">刷新</el-button>
      <el-button type="warning" @click="handleKill(false)">正常结束</el-button>
      <el-button type="danger" @click="handleKill(true)">强制杀死</el-button>
      <el-button @click="$emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api'

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
  const action = force ? '强制杀死' : '正常结束'
  try {
    await ElMessageBox.confirm(`确定${action}进程 PID: ${props.pid}？`, '确认', { type: 'warning' })
    const url = force ? `/process/${props.pid}/force` : `/process/${props.pid}`
    const res = await request.delete(url)
    ElMessage.success(res.message || '操作成功')
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
