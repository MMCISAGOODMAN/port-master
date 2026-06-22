<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :title="t('scanHistory.title')"
    width="640px"
    @open="loadHistory"
  >
    <el-alert type="info" show-icon :closable="false" style="margin-bottom: 12px">
      {{ t('scanHistory.hint') }}
    </el-alert>

    <div v-if="history.length === 0" class="text-muted" style="padding: 24px; text-align: center">
      {{ t('scanHistory.noHistory') }}
    </div>

    <el-table v-else :data="history" size="small" border max-height="400">
      <el-table-column :label="t('scanHistory.time')" width="170">
        <template #default="{ row }">{{ formatTime(row.timestamp) }}</template>
      </el-table-column>
      <el-table-column prop="total" :label="t('scanHistory.total')" width="80" />
      <el-table-column prop="listen" :label="t('scanHistory.listen')" width="70" />
      <el-table-column prop="established" :label="t('scanHistory.established')" width="90" />
      <el-table-column prop="tcp" :label="t('scanHistory.tcp')" width="70" />
      <el-table-column prop="udp" :label="t('scanHistory.udp')" width="70" />
      <el-table-column prop="conflicts" :label="t('scanHistory.conflicts')" width="70" />
      <el-table-column :label="t('common.action')" width="80">
        <template #default="{ $index }">
          <el-button link type="danger" size="small" @click="remove($index)">{{ t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="history.length >= 2" class="trend-summary">
      <span>{{ t('scanHistory.trend') }}</span>
      <el-tag :type="listenTrend >= 0 ? 'success' : 'warning'" size="small">
        {{ t('scanHistory.listenTrend', { sign: listenTrend >= 0 ? '+' : '', count: listenTrend }) }}
      </el-tag>
      <el-tag :type="totalTrend >= 0 ? 'primary' : 'info'" size="small">
        {{ t('scanHistory.totalTrend', { sign: totalTrend >= 0 ? '+' : '', count: totalTrend }) }}
      </el-tag>
    </div>

    <template #footer>
      <el-button type="danger" plain @click="clearAll">{{ t('scanHistory.clearAll') }}</el-button>
      <el-button @click="$emit('update:modelValue', false)">{{ t('common.close') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { loadScanHistory, clearScanHistory, removeScanHistoryItem } from '@/utils/scanHistory'

const { t } = useI18n()

defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue'])

const history = ref([])

const listenTrend = computed(() => {
  if (history.value.length < 2) return 0
  return history.value[0].listen - history.value[1].listen
})

const totalTrend = computed(() => {
  if (history.value.length < 2) return 0
  return history.value[0].total - history.value[1].total
})

function loadHistory() {
  history.value = loadScanHistory()
}

function formatTime(ts) {
  return new Date(ts).toLocaleString('zh-CN')
}

async function remove(index) {
  history.value = removeScanHistoryItem(index)
  ElMessage.success(t('scanHistory.deleted'))
}

async function clearAll() {
  await ElMessageBox.confirm(t('scanHistory.confirmClear'), t('common.confirmTitle'), { type: 'warning' })
  clearScanHistory()
  history.value = []
  ElMessage.success(t('scanHistory.cleared'))
}
</script>

<style scoped>
.trend-summary {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}
</style>
