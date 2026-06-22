<template>
  <div class="dashboard-stats">
    <el-row :gutter="16">
      <el-col :span="4">
        <div class="stat-card card-shadow">
          <div class="stat-icon cpu"><el-icon :size="28"><Cpu /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ formatPercent(stats.cpuUsage) }}%</div>
            <div class="stat-label">{{ t('stats.cpu') }}</div>
          </div>
          <el-progress :percentage="stats.cpuUsage || 0" :show-text="false" :stroke-width="4" />
        </div>
      </el-col>
      <el-col :span="4">
        <div class="stat-card card-shadow">
          <div class="stat-icon memory"><el-icon :size="28"><Coin /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ formatPercent(stats.memoryUsage) }}%</div>
            <div class="stat-label">{{ t('stats.memory') }}</div>
          </div>
          <el-progress :percentage="stats.memoryUsage || 0" :show-text="false" :stroke-width="4" status="success" />
          <div class="stat-sub text-muted">{{ formatMb(stats.memoryUsedMb) }} / {{ formatMb(stats.memoryTotalMb) }} MB</div>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="stat-card card-shadow">
          <div class="stat-icon ports"><el-icon :size="28"><Connection /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.listenPortCount || 0 }}</div>
            <div class="stat-label">{{ t('stats.listenPorts') }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="stat-card card-shadow">
          <div class="stat-icon active"><el-icon :size="28"><Link /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.activeConnectionCount || 0 }}</div>
            <div class="stat-label">{{ t('stats.activeConn') }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="stat-card card-shadow">
          <div class="stat-icon process"><el-icon :size="28"><SetUp /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.processCount || 0 }}</div>
            <div class="stat-label">{{ t('stats.processes') }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="stat-card card-shadow">
          <div class="stat-icon os"><el-icon :size="28"><Platform /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value os-name">{{ osShortName }}</div>
            <div class="stat-label">{{ t('stats.os') }}</div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Cpu, Coin, Connection, Link, SetUp, Platform } from '@element-plus/icons-vue'

const props = defineProps({
  stats: { type: Object, default: () => ({}) },
  loading: { type: Boolean, default: false }
})

const { t } = useI18n()

const osShortName = computed(() => {
  const os = props.stats.osType || ''
  if (os.toLowerCase().includes('windows')) return 'Windows'
  if (os.toLowerCase().includes('mac') || os.toLowerCase().includes('darwin')) return 'macOS'
  if (os.toLowerCase().includes('linux')) return 'Linux'
  return os.slice(0, 12) || '-'
})

function formatPercent(val) {
  return val != null ? val.toFixed(1) : '0.0'
}

function formatMb(val) {
  return val != null ? val.toFixed(0) : '0'
}
</script>

<style scoped>
.dashboard-stats { width: 100%; }
.stat-card {
  background: var(--pm-bg-card);
  border-radius: 8px;
  padding: 16px;
  position: relative;
  overflow: hidden;
  transition: background-color 0.25s;
}
.stat-card .el-progress { margin-top: 8px; }
.stat-icon {
  float: right;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.stat-icon.cpu { background: #ecf5ff; color: #409EFF; }
.stat-icon.memory { background: #f0f9eb; color: #67C23A; }
.stat-icon.ports { background: #fdf6ec; color: #E6A23C; }
.stat-icon.active { background: #fef0f0; color: #F56C6C; }
.stat-icon.process { background: #f4f4f5; color: #909399; }
.stat-icon.os { background: #ecf5ff; color: #409EFF; }
.stat-value { font-size: 24px; font-weight: 600; color: var(--pm-text-primary); line-height: 1.2; }
.stat-value.os-name { font-size: 16px; }
.stat-label { font-size: 13px; color: var(--pm-text-muted); margin-top: 4px; }
.stat-sub { margin-top: 4px; font-size: 12px; }
</style>
