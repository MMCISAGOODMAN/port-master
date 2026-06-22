<template>
  <div class="stats-bar">
    <el-tag effect="plain">{{ t('stats.tcp') }} {{ stats.tcp }}</el-tag>
    <el-tag effect="plain" type="success">{{ t('stats.udp') }} {{ stats.udp }}</el-tag>
    <el-tag effect="plain" type="success">{{ t('stats.listen') }} {{ stats.listen }}</el-tag>
    <el-tag effect="plain" type="primary">{{ t('stats.established') }} {{ stats.established }}</el-tag>
    <el-tag effect="plain" type="info">{{ t('stats.free') }} {{ stats.free }}</el-tag>
    <el-tag v-if="stats.conflicts > 0" effect="dark" type="danger">
      {{ t('stats.conflicts') }} {{ stats.conflicts }}
    </el-tag>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'

defineProps({
  stats: {
    type: Object,
    default: () => ({ tcp: 0, udp: 0, listen: 0, established: 0, free: 0, conflicts: 0 })
  }
})

const { t } = useI18n()
</script>

<style scoped>
.stats-bar { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 8px; }
</style>
