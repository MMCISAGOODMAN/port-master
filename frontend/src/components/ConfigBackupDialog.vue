<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :title="t('backup.title')"
    width="520px"
  >
    <el-alert
      :title="t('backup.hint')"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 16px"
    />

    <div class="actions">
      <el-button type="primary" :icon="Download" @click="handleExport">{{ t('backup.exportJson') }}</el-button>
      <el-upload
        :auto-upload="false"
        :show-file-list="false"
        accept=".json"
        @change="handleImport"
      >
        <el-button :icon="Upload">{{ t('backup.importJson') }}</el-button>
      </el-upload>
    </div>

    <el-divider />

    <div class="danger-zone">
      <p class="text-muted">{{ t('backup.dangerZone') }}</p>
      <el-button type="danger" plain size="small" @click="clearHistory">{{ t('backup.clearHistory') }}</el-button>
      <el-button type="danger" plain size="small" @click="resetAll">{{ t('backup.resetDefault') }}</el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { Download, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { downloadConfig, importConfig } from '@/utils/configBackup'
import { saveToStorage, STORAGE_KEYS, getDefaultGroups, getDefaultSettings } from '@/utils/storage'

const { t } = useI18n()

defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'imported'])

function handleExport() {
  downloadConfig()
  ElMessage.success(t('backup.exported'))
}

function handleImport(uploadFile) {
  const file = uploadFile.raw
  if (!file) return
  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      importConfig(e.target.result)
      ElMessage.success(t('backup.importSuccess'))
      emit('imported')
      emit('update:modelValue', false)
    } catch {
      ElMessage.error(t('backup.importError'))
    }
  }
  reader.readAsText(file)
}

function clearHistory() {
  ElMessageBox.confirm(t('backup.confirmClearHistory'), t('common.confirmTitle'), { type: 'warning' })
    .then(() => {
      saveToStorage(STORAGE_KEYS.HISTORY, [])
      ElMessage.success(t('backup.historyCleared'))
      emit('imported')
    }).catch(() => {})
}

function resetAll() {
  ElMessageBox.confirm(t('backup.confirmReset'), t('common.confirmTitle'), { type: 'warning' })
    .then(() => {
      saveToStorage(STORAGE_KEYS.GROUPS, getDefaultGroups())
      saveToStorage(STORAGE_KEYS.SETTINGS, getDefaultSettings())
      saveToStorage(STORAGE_KEYS.MONITOR, { enabled: false, ports: [] })
      saveToStorage(STORAGE_KEYS.HISTORY, [])
      ElMessage.success(t('backup.resetDone'))
      emit('imported')
      emit('update:modelValue', false)
    }).catch(() => {})
}
</script>

<style scoped>
.actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.danger-zone {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
