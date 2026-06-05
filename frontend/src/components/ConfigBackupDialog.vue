<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="配置备份与恢复"
    width="520px"
  >
    <el-alert
      title="导出/导入分组、监控、历史、设置等全部 LocalStorage 配置"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 16px"
    />

    <div class="actions">
      <el-button type="primary" :icon="Download" @click="handleExport">导出配置 JSON</el-button>
      <el-upload
        :auto-upload="false"
        :show-file-list="false"
        accept=".json"
        @change="handleImport"
      >
        <el-button :icon="Upload">导入配置 JSON</el-button>
      </el-upload>
    </div>

    <el-divider />

    <div class="danger-zone">
      <p class="text-muted">危险操作</p>
      <el-button type="danger" plain size="small" @click="clearHistory">清空操作历史</el-button>
      <el-button type="danger" plain size="small" @click="resetAll">恢复默认配置</el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import { Download, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { downloadConfig, importConfig } from '@/utils/configBackup'
import { saveToStorage, STORAGE_KEYS, getDefaultGroups, getDefaultSettings } from '@/utils/storage'

defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'imported'])

function handleExport() {
  downloadConfig()
  ElMessage.success('配置已导出')
}

function handleImport(uploadFile) {
  const file = uploadFile.raw
  if (!file) return
  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      importConfig(e.target.result)
      ElMessage.success('配置导入成功，请刷新页面生效')
      emit('imported')
      emit('update:modelValue', false)
    } catch {
      ElMessage.error('配置文件格式错误')
    }
  }
  reader.readAsText(file)
}

function clearHistory() {
  ElMessageBox.confirm('确定清空全部操作历史？', '确认', { type: 'warning' })
    .then(() => {
      saveToStorage(STORAGE_KEYS.HISTORY, [])
      ElMessage.success('历史已清空')
      emit('imported')
    }).catch(() => {})
}

function resetAll() {
  ElMessageBox.confirm('将恢复默认分组和设置，自定义内容会丢失，确定继续？', '确认', { type: 'warning' })
    .then(() => {
      saveToStorage(STORAGE_KEYS.GROUPS, getDefaultGroups())
      saveToStorage(STORAGE_KEYS.SETTINGS, getDefaultSettings())
      saveToStorage(STORAGE_KEYS.MONITOR, { enabled: false, ports: [] })
      saveToStorage(STORAGE_KEYS.HISTORY, [])
      ElMessage.success('已恢复默认配置')
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
