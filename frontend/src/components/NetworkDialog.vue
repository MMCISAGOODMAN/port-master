<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :title="t('network.title')"
    width="620px"
    @open="loadInterfaces"
  >
    <el-button :icon="Refresh" :loading="loading" @click="loadInterfaces" style="margin-bottom: 12px">{{ t('common.refresh') }}</el-button>

    <el-table :data="interfaces" v-loading="loading" size="small" border max-height="400">
      <el-table-column prop="name" :label="t('network.name')" width="120" />
      <el-table-column prop="ipAddress" :label="t('network.ip')" width="140" />
      <el-table-column prop="macAddress" :label="t('network.mac')" width="150" />
      <el-table-column prop="status" :label="t('network.status')" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 'UP' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="type" :label="t('network.desc')" min-width="140" show-overflow-tooltip />
      <el-table-column :label="t('common.action')" width="80">
        <template #default="{ row }">
          <el-button v-if="row.ipAddress && row.ipAddress !== '-'" link type="primary" size="small"
            @click="copyIp(row.ipAddress)">{{ t('common.copy') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import request from '@/api'

const { t } = useI18n()

defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue'])

const interfaces = ref([])
const loading = ref(false)

async function loadInterfaces() {
  loading.value = true
  try {
    const res = await request.get('/network/interfaces')
    interfaces.value = res.data || []
  } finally {
    loading.value = false
  }
}

function copyIp(ip) {
  navigator.clipboard.writeText(ip).then(() => ElMessage.success(t('network.ipCopied')))
}
</script>
