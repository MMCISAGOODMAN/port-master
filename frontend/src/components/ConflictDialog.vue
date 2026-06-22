<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :title="t('conflict.title')"
    width="640px"
    @open="fetchConflicts"
  >
    <el-alert
      v-if="conflicts.length === 0 && !loading"
      :title="t('conflict.noConflict')"
      type="success"
      show-icon
      :closable="false"
    />
    <el-table v-else :data="conflicts" v-loading="loading" border size="small">
      <el-table-column prop="port" :label="t('conflict.port')" width="80" />
      <el-table-column prop="protocol" :label="t('conflict.protocol')" width="70" />
      <el-table-column :label="t('conflict.conflictProcesses')">
        <template #default="{ row }">
          <el-tag v-for="(name, i) in row.processNames" :key="i" size="small" type="danger" style="margin: 2px">
            {{ name }} (PID: {{ row.pids[i] }})
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.action')" width="100">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="$emit('query-port', row.port)">{{ t('conflict.view') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="fetchConflicts" :loading="loading">{{ t('conflict.redetect') }}</el-button>
      <el-button @click="$emit('update:modelValue', false)">{{ t('common.close') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import request from '@/api'

const { t } = useI18n()

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
