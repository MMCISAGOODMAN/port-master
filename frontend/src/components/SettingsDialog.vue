<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="设置"
    width="420px"
    @open="loadSettings"
    @closed="onDialogClosed"
  >
    <el-form label-width="120px">
      <el-form-item label="界面主题">
        <el-radio-group v-model="localSettings.theme">
          <el-radio value="light">浅色</el-radio>
          <el-radio value="dark">深色</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="自动刷新">
        <el-select v-model="localSettings.autoRefreshInterval" style="width: 100%">
          <el-option label="关闭" :value="0" />
          <el-option label="每 10 秒" :value="10" />
          <el-option label="每 30 秒" :value="30" />
          <el-option label="每 60 秒" :value="60" />
        </el-select>
      </el-form-item>
      <el-form-item label="默认仅监听">
        <el-switch v-model="localSettings.listenOnly" />
      </el-form-item>
      <el-form-item label="每页条数">
        <el-select v-model="localSettings.defaultPageSize" style="width: 100%">
          <el-option :value="20" label="20" />
          <el-option :value="50" label="50" />
          <el-option :value="100" label="100" />
          <el-option :value="200" label="200" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { loadFromStorage, saveToStorage, STORAGE_KEYS, getDefaultSettings } from '@/utils/storage'
import { applyTheme } from '@/utils/theme'

defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'change'])

const localSettings = ref(getDefaultSettings())

function loadSettings() {
  localSettings.value = { ...getDefaultSettings(), ...loadFromStorage(STORAGE_KEYS.SETTINGS, getDefaultSettings()) }
}

// 设置弹窗内切换主题时实时预览
watch(() => localSettings.value.theme, (theme) => {
  if (theme) applyTheme(theme)
})

function save() {
  saveToStorage(STORAGE_KEYS.SETTINGS, localSettings.value)
  applyTheme(localSettings.value.theme)
  emit('change', { ...localSettings.value })
  emit('update:modelValue', false)
  ElMessage.success('设置已保存')
}

/** 取消关闭时恢复已保存的主题 */
function onDialogClosed() {
  const persisted = loadFromStorage(STORAGE_KEYS.SETTINGS, getDefaultSettings())
  applyTheme(persisted.theme || 'light')
}
</script>
