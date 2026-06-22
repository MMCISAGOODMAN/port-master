<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :title="t('settings.title')"
    width="420px"
    @open="loadSettings"
    @closed="onDialogClosed"
  >
    <el-form label-width="120px">
      <el-form-item :label="t('settings.language')">
        <el-select v-model="localSettings.locale" style="width: 100%">
          <el-option :label="t('settings.langZh')" value="zh-CN" />
          <el-option :label="t('settings.langEn')" value="en" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('settings.theme')">
        <el-radio-group v-model="localSettings.theme">
          <el-radio value="light">{{ t('settings.light') }}</el-radio>
          <el-radio value="dark">{{ t('settings.dark') }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="t('settings.autoRefresh')">
        <el-select v-model="localSettings.autoRefreshInterval" style="width: 100%">
          <el-option :label="t('settings.refreshOff')" :value="0" />
          <el-option :label="t('settings.refresh10')" :value="10" />
          <el-option :label="t('settings.refresh30')" :value="30" />
          <el-option :label="t('settings.refresh60')" :value="60" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('settings.defaultListenOnly')">
        <el-switch v-model="localSettings.listenOnly" />
      </el-form-item>
      <el-form-item :label="t('settings.pageSize')">
        <el-select v-model="localSettings.defaultPageSize" style="width: 100%">
          <el-option :value="20" label="20" />
          <el-option :value="50" label="50" />
          <el-option :value="100" label="100" />
          <el-option :value="200" label="200" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" @click="save">{{ t('common.save') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import en from 'element-plus/dist/locale/en.mjs'
import { loadFromStorage, saveToStorage, STORAGE_KEYS, getDefaultSettings } from '@/utils/storage'
import { applyTheme } from '@/utils/theme'
import { setLocale } from '@/i18n'

defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'change'])

const { t, locale } = useI18n()
const localSettings = ref(getDefaultSettings())

function loadSettings() {
  localSettings.value = { ...getDefaultSettings(), ...loadFromStorage(STORAGE_KEYS.SETTINGS, getDefaultSettings()) }
}

watch(() => localSettings.value.theme, (theme) => {
  if (theme) applyTheme(theme)
})

function save() {
  saveToStorage(STORAGE_KEYS.SETTINGS, localSettings.value)
  applyTheme(localSettings.value.theme)
  if (localSettings.value.locale && localSettings.value.locale !== locale.value) {
    setLocale(localSettings.value.locale)
    window.location.reload()
    return
  }
  emit('change', { ...localSettings.value })
  emit('update:modelValue', false)
  ElMessage.success(t('settings.saved'))
}

function onDialogClosed() {
  const persisted = loadFromStorage(STORAGE_KEYS.SETTINGS, getDefaultSettings())
  applyTheme(persisted.theme || 'light')
}
</script>
