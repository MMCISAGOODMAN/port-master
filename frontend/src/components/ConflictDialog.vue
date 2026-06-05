<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="端口冲突检测"
    width="640px"
    @open="fetchConflicts"
  >
    <el-alert
      v-if="conflicts.length === 0 && !loading"
      title="未检测到端口冲突"
      type="success"
      show-icon
      :closable="false"
    />
    <el-table v-else :data="conflicts" v-loading="loading" border size="small">
      <el-table-column prop="port" label="端口" width="80" />
      <el-table-column prop="protocol" label="协议" width="70" />
      <el-table-column label="冲突进程">
        <template #default="{ row }">
          <el-tag v-for="(name, i) in row.processNames" :key="i" size="small" type="danger" style="margin: 2px">
            {{ name }} (PID: {{ row.pids[i] }})
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="$emit('query-port', row.port)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="fetchConflicts" :loading="loading">重新检测</el-button>
      <el-button @click="$emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import request from '@/api'

defineProps({ modelValue: Boolean })
defineEmits(['update:modelValue', 'query-port'])

const conflicts = ref([])
const loading = ref(false)

async function fetchConflicts() {
  loading.value = true
  try {
    const res = await request.get('/ports/conflicts')
    conflicts.value = res.data || []
  } finally {
    loading.value = false
  }
}
</script>
