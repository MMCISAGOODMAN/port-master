<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="端口连通性探测"
    width="560px"
    @open="onOpen"
  >
    <el-form inline>
      <el-form-item label="主机">
        <el-input v-model="host" placeholder="127.0.0.1" style="width: 140px" />
      </el-form-item>
      <el-form-item label="端口">
        <el-input v-model="portInput" placeholder="8080 或 8080,9090" style="width: 180px" @keyup.enter="runProbe" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="runProbe">探测</el-button>
      </el-form-item>
    </el-form>

    <el-table v-if="results.length" :data="results" size="small" border style="margin-top: 12px">
      <el-table-column prop="port" label="端口" width="80" />
      <el-table-column prop="host" label="主机" width="120" />
      <el-table-column label="结果" width="100">
        <template #default="{ row }">
          <el-tag :type="row.reachable ? 'success' : 'danger'" size="small">
            {{ row.reachable ? '可达' : '不可达' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="latencyMs" label="耗时(ms)" width="90" />
      <el-table-column prop="message" label="详情" show-overflow-tooltip />
    </el-table>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api'

const props = defineProps({
  modelValue: Boolean,
  initialPort: { type: [String, Number], default: '' }
})
const emit = defineEmits(['update:modelValue'])

const host = ref('127.0.0.1')
const portInput = ref('')
const loading = ref(false)
const results = ref([])

function onOpen() {
  results.value = []
  if (props.initialPort) {
    portInput.value = String(props.initialPort)
    runProbe()
  }
}

async function runProbe() {
  const ports = portInput.value.split(/[,，\s]+/).map(s => parseInt(s.trim())).filter(p => p > 0 && p <= 65535)
  if (ports.length === 0) {
    ElMessage.warning('请输入有效端口号')
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
</script>
