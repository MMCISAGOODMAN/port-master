<template>
  <el-container class="app-container">
    <!-- 左侧分组导航 -->
    <el-aside width="240px" class="sidebar card-shadow">
      <div class="sidebar-header">
        <el-icon :size="24" color="#409EFF"><Monitor /></el-icon>
        <span class="logo-text">Port Master</span>
        <el-tag size="small" type="success" class="version-tag">v2.1</el-tag>
      </div>

      <el-menu :default-active="activeGroup" class="group-menu" @select="handleGroupSelect">
        <el-menu-item index="all">
          <el-icon><Grid /></el-icon>
          <span>{{ t('sidebar.allPorts') }}</span>
        </el-menu-item>
        <el-divider content-position="left">{{ t('sidebar.portGroups') }}</el-divider>
        <el-menu-item v-for="group in groups" :key="group.id" :index="group.id">
          <el-icon><Folder /></el-icon>
          <span>{{ group.name }}</span>
          <el-tag size="small" :type="getGroupActiveCount(group) > 0 ? 'success' : 'info'" class="group-count">
            {{ getGroupActiveCount(group) }}/{{ group.ports.length }}
          </el-tag>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-actions">
        <el-button type="primary" size="small" :icon="Plus" @click="showGroupDialog = true">{{ t('sidebar.newGroup') }}</el-button>
        <el-button size="small" :icon="Setting" @click="showGroupManage = true">{{ t('sidebar.manageGroups') }}</el-button>
      </div>

      <el-divider content-position="left">{{ t('sidebar.commonPorts') }}</el-divider>
      <div class="common-ports">
        <el-tag
          v-for="item in commonPorts"
          :key="item.port + item.name"
          class="common-port-tag"
          effect="plain"
          @click="quickSearchPort(item.port)"
        >
          {{ item.name }}:{{ item.port }}
        </el-tag>
      </div>

      <el-divider content-position="left">{{ t('sidebar.history') }}</el-divider>
      <div class="history-list">
        <div
          v-for="(item, idx) in historyList.slice(0, 10)"
          :key="idx"
          class="history-item"
          @click="handleHistoryClick(item)"
        >
          <el-icon><Clock /></el-icon>
          <span>{{ item.label }}</span>
        </div>
        <div v-if="historyList.length === 0" class="text-muted" style="padding: 8px">{{ t('sidebar.noHistory') }}</div>
      </div>
    </el-aside>

    <!-- 主内容区 -->
    <el-container class="main-panel">
      <!-- 顶部仪表盘 -->
      <el-header height="auto" class="dashboard-header">
        <DashboardStats :stats="systemStats" :loading="statsLoading" />
      </el-header>

      <!-- 权限提示 -->
      <el-alert
        v-if="permissionHint && !hintDismissed"
        class="permission-banner"
        :title="permissionHint"
        type="warning"
        show-icon
        closable
        @close="hintDismissed = true"
      />

      <!-- 顶部操作栏 -->
      <div class="toolbar card-shadow">
        <div class="toolbar-left">
          <el-button type="primary" :icon="Search" :loading="scanning" @click="handleScanAll()">
            {{ t('toolbar.fullScan') }}
          </el-button>
          <el-input
            v-model="searchKeyword"
            :placeholder="t('toolbar.searchPlaceholder')"
            clearable
            style="width: 260px"
            :prefix-icon="Search"
            @input="handleSearch"
          />
          <el-input
            v-model="queryPort"
            :placeholder="t('toolbar.portQuery')"
            clearable
            style="width: 200px"
            @keyup.enter="handlePortQuery"
          />
          <el-input
            v-model="queryProcess"
            :placeholder="t('toolbar.processQuery')"
            clearable
            style="width: 150px"
            @keyup.enter="handleProcessQuery"
          />
          <el-input
            v-model="queryPid"
            :placeholder="t('toolbar.pidQuery')"
            clearable
            style="width: 120px"
            @keyup.enter="handlePidQuery"
          />
        </div>
        <div class="toolbar-right">
          <el-button :icon="Refresh" @click="handleScanAll()">{{ t('common.refresh') }}</el-button>
          <el-dropdown @command="handleExport">
            <el-button :icon="Download">
              {{ t('common.export') }}<el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="excel">{{ t('toolbar.exportExcel') }}</el-dropdown-item>
                <el-dropdown-item command="markdown">{{ t('toolbar.exportMarkdown') }}</el-dropdown-item>
                <el-dropdown-item command="txt">{{ t('toolbar.exportTxt') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button :icon="Bell" @click="showMonitorDialog = true">
            {{ t('toolbar.monitor') }}
            <el-badge v-if="alertCount > 0" :value="alertCount" class="monitor-badge" />
          </el-button>
          <el-button :icon="MagicStick" @click="showFreePortDialog = true">{{ t('toolbar.freePort') }}</el-button>
          <el-button :icon="Connection" @click="showPortProbe = true">{{ t('toolbar.probe') }}</el-button>
          <el-button :icon="Monitor" @click="showRemoteDialog = true">{{ t('toolbar.remoteSsh') }}</el-button>
          <el-button :icon="Box" @click="showDockerDialog = true">{{ t('toolbar.docker') }}</el-button>
          <el-button :icon="Platform" @click="showK8sDialog = true">{{ t('toolbar.k8s') }}</el-button>
          <el-button :icon="TrendCharts" @click="showScanHistory = true">{{ t('toolbar.scanHistory') }}</el-button>
          <el-button :icon="Link" @click="showNetworkDialog = true">{{ t('toolbar.network') }}</el-button>
          <el-button :icon="List" @click="showProcessList = true">{{ t('toolbar.processList') }}</el-button>
          <el-button :icon="Warning" @click="showConflictDialog = true">{{ t('toolbar.conflicts') }}</el-button>
          <el-button :icon="FolderOpened" @click="showConfigBackup = true">{{ t('toolbar.backup') }}</el-button>
          <el-tooltip :content="isDark ? t('toolbar.switchLight') : t('toolbar.switchDark')" placement="bottom">
            <el-button :icon="isDark ? Sunny : Moon" circle @click="handleToggleTheme" />
          </el-tooltip>
          <el-button :icon="Setting" @click="showSettings = true">{{ t('toolbar.settings') }}</el-button>
        </div>
      </div>

      <!-- 筛选栏 -->
      <div class="filter-bar card-shadow">
        <el-select v-model="filterProtocol" :placeholder="t('filter.protocol')" clearable style="width: 100px" @change="applyFilter">
          <el-option label="TCP" value="TCP" />
          <el-option label="UDP" value="UDP" />
        </el-select>
        <el-select v-model="filterState" :placeholder="t('filter.state')" clearable style="width: 130px" @change="applyFilter">
          <el-option label="LISTEN" value="LISTEN" />
          <el-option label="ESTABLISHED" value="ESTABLISHED" />
          <el-option label="TIME_WAIT" value="TIME_WAIT" />
          <el-option label="FREE" value="FREE" />
        </el-select>
        <el-checkbox v-model="listenOnly" @change="applyFilter">{{ t('filter.listenOnly') }}</el-checkbox>
        <el-select v-model="filterAddress" :placeholder="t('filter.bindAddress')" clearable style="width: 130px" @change="applyFilter">
          <el-option :label="t('filter.localhost')" value="localhost" />
          <el-option :label="t('filter.allInterface')" value="all-if" />
        </el-select>
        <el-checkbox v-model="showDiffOnly" @change="applyFilter" :disabled="!hasDiff">
          {{ t('filter.diffOnly') }} <span v-if="hasDiff" class="diff-hint">(+{{ diffStats.newCount }} / ~{{ diffStats.changed }})</span>
        </el-checkbox>
        <el-button v-if="filterProtocol || filterState || listenOnly || filterAddress || showDiffOnly" link type="primary" @click="clearFilters">{{ t('filter.clearFilters') }}</el-button>
      </div>

      <!-- 扫描对比提示 -->
      <div v-if="hasDiff" class="diff-bar card-shadow">
        <span>{{ t('diff.compare') }}</span>
        <el-tag type="success" size="small">{{ t('diff.new') }} {{ diffStats.newCount }}</el-tag>
        <el-tag type="warning" size="small">{{ t('diff.changed') }} {{ diffStats.changed }}</el-tag>
        <el-tag type="info" size="small">{{ t('diff.removed') }} {{ diffStats.removed }}</el-tag>
      </div>

      <!-- 数据表格 -->
      <el-main class="main-content">
        <PortStatsBar :stats="tableStats" />
        <PortTable
          :data="filteredData"
          :loading="scanning"
          :conflict-ports="conflictPorts"
          :diff-map="scanDiffMap"
          :default-page-size="settings.defaultPageSize"
          @row-click="handleRowClick"
          @kill="handleKill"
          @batch-kill="handleBatchKill"
          @kill-by-port="handleKillByPort"
          @add-to-group="handleAddToGroup"
          @probe="handleQuickProbe"
        />
      </el-main>

      <!-- 底部统计 -->
      <el-footer height="40px" class="footer-stats">
        <span>{{ t('footer.total', { count: filteredData.length }) }}</span>
        <span class="text-muted">| {{ t('footer.listen') }} {{ listenCount }} | {{ t('footer.active') }} {{ activeCount }}</span>
      </el-footer>
    </el-container>

    <!-- 进程详情弹窗 -->
    <ProcessDetailDialog
      v-model="showProcessDetail"
      :pid="selectedPid"
    />

    <!-- 空闲端口生成器 -->
    <FreePortDialog v-model="showFreePortDialog" />

    <!-- 端口监控配置 -->
    <MonitorDialog
      v-model="showMonitorDialog"
      @alert="handleMonitorAlert"
      @monitor-change="handleMonitorChange"
    />

    <!-- 分组管理 -->
    <GroupManageDialog
      v-model="showGroupManage"
      :groups="groups"
      @update="loadGroups"
    />

    <!-- 设置 -->
    <SettingsDialog v-model="showSettings" @change="handleSettingsChange" />

    <!-- 进程列表 -->
    <ProcessListDialog
      v-model="showProcessList"
      @view-detail="openProcessDetail"
      @view-ports="handleViewPortsByPid"
      @kill="handleKill"
    />

    <!-- 端口冲突 -->
    <ConflictDialog
      v-model="showConflictDialog"
      @query-port="quickSearchPort"
    />

    <!-- 端口连通探测 -->
    <PortProbeDialog v-model="showPortProbe" :initial-port="probeInitialPort" />

    <!-- 配置备份 -->
    <ConfigBackupDialog v-model="showConfigBackup" @imported="handleConfigImported" />

    <!-- v2 新功能 -->
    <RemoteHostDialog v-model="showRemoteDialog" @scan-result="handleRemoteScanResult" />
    <DockerDialog v-model="showDockerDialog" @query-port="handleDockerQueryPort" />
    <ScanHistoryDialog v-model="showScanHistory" />
    <NetworkDialog v-model="showNetworkDialog" />
    <K8sDialog v-model="showK8sDialog" @query-port="handleDockerQueryPort" />

    <!-- 新建分组 -->
    <el-dialog v-model="showGroupDialog" :title="t('group.newTitle')" width="400px">
      <el-form @submit.prevent="createGroup">
        <el-form-item :label="t('group.nameLabel')">
          <el-input v-model="newGroupName" :placeholder="t('group.namePlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showGroupDialog = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="createGroup">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 添加到分组 -->
    <el-dialog v-model="showAddToGroupDialog" :title="t('group.addTitle')" width="400px">
      <el-select v-model="targetGroupId" :placeholder="t('group.selectGroup')" style="width: 100%">
        <el-option v-for="g in groups" :key="g.id" :label="g.name" :value="g.id" />
      </el-select>
      <el-input v-model="portRemark" :placeholder="t('group.remarkPlaceholder')" style="margin-top: 12px" />
      <template #footer>
        <el-button @click="showAddToGroupDialog = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="confirmAddToGroup">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElNotification } from 'element-plus'
import {
  Search, Refresh, Download, Bell, MagicStick, Plus, Setting,
  Monitor, Grid, Folder, Clock, Warning, ArrowDown, List, Moon, Sunny,
  Connection, FolderOpened, Box, TrendCharts, Link, Platform
} from '@element-plus/icons-vue'
import { applyTheme } from '@/utils/theme'
import { diffScans, getDiffStats } from '@/utils/scanDiff'
import { recordScanSnapshot } from '@/utils/scanHistory'
import { getServiceName } from '@/utils/portServices'
import { exportToExcel, exportToMarkdown, exportToTxt } from '@/utils/export'
import DashboardStats from '@/components/DashboardStats.vue'
import PortTable from '@/components/PortTable.vue'
import PortStatsBar from '@/components/PortStatsBar.vue'
import ProcessDetailDialog from '@/components/ProcessDetailDialog.vue'
import ProcessListDialog from '@/components/ProcessListDialog.vue'
import FreePortDialog from '@/components/FreePortDialog.vue'
import MonitorDialog from '@/components/MonitorDialog.vue'
import GroupManageDialog from '@/components/GroupManageDialog.vue'
import SettingsDialog from '@/components/SettingsDialog.vue'
import ConflictDialog from '@/components/ConflictDialog.vue'
import PortProbeDialog from '@/components/PortProbeDialog.vue'
import ConfigBackupDialog from '@/components/ConfigBackupDialog.vue'
import RemoteHostDialog from '@/components/RemoteHostDialog.vue'
import DockerDialog from '@/components/DockerDialog.vue'
import ScanHistoryDialog from '@/components/ScanHistoryDialog.vue'
import NetworkDialog from '@/components/NetworkDialog.vue'
import K8sDialog from '@/components/K8sDialog.vue'
import request from '@/api'
import { connectMonitorWs, disconnectMonitorWs, syncMonitorConfig } from '@/utils/monitorWs'
import { loadFromStorage, saveToStorage, STORAGE_KEYS, getDefaultGroups, getDefaultSettings, COMMON_PORTS } from '@/utils/storage'

const { t } = useI18n()

const portData = ref([])
const filteredData = ref([])
const scanning = ref(false)
const searchKeyword = ref('')
const queryPort = ref('')
const queryProcess = ref('')
const queryPid = ref('')
const systemStats = ref({})
const statsLoading = ref(false)
const permissionHint = ref('')
const activeGroup = ref('all')
const groups = ref([])
const commonPorts = COMMON_PORTS
const historyList = ref([])

const showProcessDetail = ref(false)
const selectedPid = ref(null)
const showFreePortDialog = ref(false)
const showMonitorDialog = ref(false)
const showGroupManage = ref(false)
const showGroupDialog = ref(false)
const showAddToGroupDialog = ref(false)
const newGroupName = ref('')
const targetGroupId = ref('')
const portRemark = ref('')
const addPortItem = ref(null)
const alertCount = ref(0)
const showSettings = ref(false)
const showProcessList = ref(false)
const showConflictDialog = ref(false)
const settings = ref(getDefaultSettings())
const filterProtocol = ref('')
const filterState = ref('')
const filterAddress = ref('')
const listenOnly = ref(false)
const showDiffOnly = ref(false)
const conflicts = ref([])
const hintDismissed = ref(false)
const previousScan = ref([])
const scanDiffMap = ref(new Map())
const showPortProbe = ref(false)
const showConfigBackup = ref(false)
const showRemoteDialog = ref(false)
const showDockerDialog = ref(false)
const showScanHistory = ref(false)
const showNetworkDialog = ref(false)
const showK8sDialog = ref(false)
const probeInitialPort = ref('')

let statsTimer = null
let autoRefreshTimer = null

const listenCount = computed(() =>
  filteredData.value.filter(p => p.state === 'LISTEN').length
)
const activeCount = computed(() =>
  filteredData.value.filter(p => p.state === 'ESTABLISHED').length
)

const conflictPorts = computed(() => new Set(conflicts.value.map(c => c.port)))

const tableStats = computed(() => ({
  tcp: filteredData.value.filter(p => p.protocol === 'TCP').length,
  udp: filteredData.value.filter(p => p.protocol === 'UDP').length,
  listen: filteredData.value.filter(p => p.state === 'LISTEN').length,
  established: filteredData.value.filter(p => p.state === 'ESTABLISHED').length,
  free: filteredData.value.filter(p => p.state === 'FREE').length,
  conflicts: conflicts.value.length
}))

const isDark = computed(() => settings.value.theme === 'dark')

const diffStats = computed(() => getDiffStats(scanDiffMap.value))
const hasDiff = computed(() => diffStats.value.newCount + diffStats.value.changed + diffStats.value.removed > 0)

onMounted(() => {
  loadSettings()
  loadGroups()
  loadHistory()
  fetchSystemStats()
  fetchSystemInfo()
  handleScanAll()
  statsTimer = setInterval(fetchSystemStats, 5000)
  setupAutoRefresh()
  initMonitorWs()
})

onUnmounted(() => {
  if (statsTimer) clearInterval(statsTimer)
  if (autoRefreshTimer) clearInterval(autoRefreshTimer)
  disconnectMonitorWs()
})

function initMonitorWs() {
  const config = loadFromStorage(STORAGE_KEYS.MONITOR, { enabled: false, ports: [] })
  if (config.enabled && config.ports?.length) {
    connectMonitorWs(handleMonitorAlert)
    syncMonitorConfig(request, config).catch(() => {})
  }
}

function handleMonitorChange(config) {
  if (config.enabled) {
    connectMonitorWs(handleMonitorAlert)
  } else {
    disconnectMonitorWs()
  }
}

function loadSettings() {
  settings.value = { ...getDefaultSettings(), ...loadFromStorage(STORAGE_KEYS.SETTINGS, getDefaultSettings()) }
  listenOnly.value = settings.value.listenOnly || false
  applyTheme(settings.value.theme || 'light')
}

function handleSettingsChange(newSettings) {
  settings.value = newSettings
  listenOnly.value = newSettings.listenOnly
  applyTheme(newSettings.theme || 'light')
  setupAutoRefresh()
  applyFilter()
}

function handleToggleTheme() {
  const next = settings.value.theme === 'dark' ? 'light' : 'dark'
  settings.value = { ...settings.value, theme: next }
  applyTheme(next)
  saveToStorage(STORAGE_KEYS.SETTINGS, settings.value)
  ElMessage.success(next === 'dark' ? t('settings.themeDark') : t('settings.themeLight'))
}

function setupAutoRefresh() {
  if (autoRefreshTimer) clearInterval(autoRefreshTimer)
  autoRefreshTimer = null
  const interval = settings.value.autoRefreshInterval
  if (interval > 0) {
    autoRefreshTimer = setInterval(() => handleScanAll(true), interval * 1000)
  }
}

async function fetchConflicts() {
  try {
    const res = await request.get('/ports/conflicts')
    conflicts.value = res.data || []
  } catch {
    conflicts.value = []
  }
}

function clearFilters() {
  filterProtocol.value = ''
  filterState.value = ''
  filterAddress.value = ''
  listenOnly.value = false
  showDiffOnly.value = false
  applyFilter()
}

function handleConfigImported() {
  loadSettings()
  loadGroups()
  loadHistory()
  setupAutoRefresh()
  handleScanAll()
}

function loadGroups() {
  groups.value = loadFromStorage(STORAGE_KEYS.GROUPS, getDefaultGroups())
}

function loadHistory() {
  historyList.value = loadFromStorage(STORAGE_KEYS.HISTORY, [])
}

function addHistory(label, action) {
  const item = { label, action, time: new Date().toISOString() }
  historyList.value.unshift(item)
  historyList.value = historyList.value.slice(0, 50)
  saveToStorage(STORAGE_KEYS.HISTORY, historyList.value)
}

async function fetchSystemStats() {
  statsLoading.value = true
  try {
    const res = await request.get('/system/stats')
    systemStats.value = res.data
  } catch { /* ignore */ }
  finally { statsLoading.value = false }
}

async function fetchSystemInfo() {
  try {
    const res = await request.get('/system/info')
    permissionHint.value = res.data.permissionHint
  } catch { /* ignore */ }
}

async function handleScanAll(silent = false) {
  scanning.value = true
  try {
    const res = await request.get('/ports/scan', { params: { refresh: silent ? false : true } })
    const newData = res.data || []
    if (portData.value.length > 0) {
      previousScan.value = [...portData.value]
      scanDiffMap.value = diffScans(previousScan.value, newData)
    } else {
      scanDiffMap.value = new Map()
    }
    portData.value = newData
    await fetchConflicts()
    applyFilter()
    if (!silent) {
      recordScanSnapshot(portData.value, conflicts.value.length)
      addHistory('全量端口扫描', 'scan')
      const diffHint = hasDiff.value ? ` (+${diffStats.value.newCount}/~${diffStats.value.changed})` : ''
      ElMessage.success(t('messages.scanDone', { count: portData.value.length, diff: diffHint }))
    }
  } catch { /* handled by interceptor */ }
  finally { scanning.value = false }
}

function handleRemoteScanResult(data) {
  portData.value = data || []
  activeGroup.value = 'all'
  applyFilter()
  ElMessage.info(t('remote.loadedToTable'))
}

function handleDockerQueryPort(ports) {
  queryPort.value = ports
  handlePortQuery()
}

function handleQuickProbe(port) {
  probeInitialPort.value = String(port)
  showPortProbe.value = true
}

/** 统计分组内当前有占用记录的端口数 */
function getGroupActiveCount(group) {
  if (!group?.ports?.length) return 0
  const portSet = new Set(group.ports.map(p => p.port))
  const activePorts = new Set(
    portData.value.filter(p => portSet.has(p.port)).map(p => p.port)
  )
  return activePorts.size
}

function applyFilter() {
  let data = [...portData.value]

  // 分组筛选：展示分组内每个端口的实时状态（含未占用占位行）
  if (activeGroup.value !== 'all') {
    const group = groups.value.find(g => g.id === activeGroup.value)
    if (group) {
      const portSet = new Set(group.ports.map(p => p.port))
      const matched = data.filter(p => portSet.has(p.port))
      const matchedPorts = new Set(matched.map(p => p.port))
      const placeholders = group.ports
        .filter(gp => !matchedPorts.has(gp.port))
        .map(gp => ({
          protocol: '-',
          port: gp.port,
          localAddress: '-',
          foreignAddress: '-',
          pid: null,
          processName: gp.remark || '-',
          programPath: '-',
          state: 'FREE'
        }))
      data = [...matched, ...placeholders]
    }
  }

  // 协议 / 状态 / 仅监听 筛选
  if (filterProtocol.value) {
    data = data.filter(p => p.protocol === filterProtocol.value)
  }
  if (filterState.value) {
    data = data.filter(p => p.state === filterState.value)
  }
  if (listenOnly.value) {
    data = data.filter(p => p.state === 'LISTEN')
  }

  // 绑定地址筛选
  if (filterAddress.value === 'localhost') {
    data = data.filter(p => {
      const addr = (p.localAddress || '').toLowerCase()
      return addr.includes('127.0.0.1') || addr.includes('localhost')
    })
  } else if (filterAddress.value === 'all-if') {
    data = data.filter(p => {
      const addr = p.localAddress || ''
      return addr.startsWith('*:') || addr.startsWith('0.0.0.0:') || addr.includes('*.')
    })
  }

  // 仅看扫描变化
  if (showDiffOnly.value && scanDiffMap.value.size > 0) {
    data = data.filter(p => {
      const key = `${p.protocol}:${p.port}:${p.localAddress}:${p.foreignAddress}:${p.state}:${p.pid || ''}`
      const t = scanDiffMap.value.get(key)
      return t === 'new' || t === 'changed'
    })
  }

  // 全局搜索（含服务名）
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    data = data.filter(p =>
      String(p.port).includes(kw) ||
      String(p.pid || '').includes(kw) ||
      (p.processName || '').toLowerCase().includes(kw) ||
      getServiceName(p.port).toLowerCase().includes(kw)
    )
  }

  filteredData.value = data
}

function handleSearch() {
  applyFilter()
}

function handleGroupSelect(index) {
  activeGroup.value = index
  applyFilter()
  if (index !== 'all') {
    const group = groups.value.find(g => g.id === index)
    if (group) addHistory(`查看分组: ${group.name}`, 'group')
  }
}

async function handlePortQuery() {
  if (!queryPort.value) return
  scanning.value = true
  try {
    const res = await request.get('/ports/query', { params: { ports: queryPort.value } })
    portData.value = res.data || []
    activeGroup.value = 'all'
    applyFilter()
    addHistory(`端口查询: ${queryPort.value}`, 'query')
  } finally { scanning.value = false }
}

async function handleProcessQuery() {
  if (!queryProcess.value) return
  scanning.value = true
  try {
    const res = await request.get('/ports/query/process', { params: { name: queryProcess.value } })
    portData.value = res.data || []
    activeGroup.value = 'all'
    applyFilter()
    addHistory(`进程查询: ${queryProcess.value}`, 'query')
  } finally { scanning.value = false }
}

async function handlePidQuery() {
  if (!queryPid.value) return
  scanning.value = true
  try {
    const res = await request.get(`/ports/query/pid/${queryPid.value}`)
    portData.value = res.data || []
    activeGroup.value = 'all'
    applyFilter()
    addHistory(`PID 查询: ${queryPid.value}`, 'query')
  } finally { scanning.value = false }
}

function quickSearchPort(port) {
  queryPort.value = String(port)
  handlePortQuery()
}

function handleHistoryClick(item) {
  if (item.action === 'scan') handleScanAll()
  else if (item.action === 'query') {
    const match = item.label.match(/:\s*(.+)$/)
    if (match) {
      queryPort.value = match[1]
      handlePortQuery()
    }
  }
}

function handleRowClick(row) {
  if (row.pid) {
    openProcessDetail(row.pid)
  }
}

function openProcessDetail(pid) {
  selectedPid.value = pid
  showProcessDetail.value = true
}

async function handleViewPortsByPid(pid) {
  showProcessList.value = false
  queryPid.value = String(pid)
  await handlePidQuery()
}

async function handleKillByPort(port, force) {
  try {
    const url = force ? `/process/by-port/${port}/force` : `/process/by-port/${port}`
    const res = await request.delete(url)
    ElMessage.success(t('messages.freePortDone'))
    res.data.forEach(msg => ElMessage.info(msg))
    addHistory(`${force ? '强杀' : '释放'}端口 ${port}`, 'kill')
    handleScanAll()
  } catch { /* handled */ }
}

async function handleKill(pid, force) {
  try {
    const url = force ? `/process/${pid}/force` : `/process/${pid}`
    const res = await request.delete(url)
    ElMessage.success(res.message || t('messages.killSuccess'))
    addHistory(`${force ? '强制杀死' : '结束'}进程 PID:${pid}`, 'kill')
    handleScanAll()
  } catch { /* handled */ }
}

async function handleBatchKill(pids, force) {
  try {
    const res = await request.post('/process/kill/batch', { pids, force })
    ElMessage.success(t('messages.batchDone'))
    res.data.forEach(msg => ElMessage.info(msg))
    addHistory(`批量${force ? '强杀' : '结束'} ${pids.length} 个进程`, 'kill')
    handleScanAll()
  } catch { /* handled */ }
}

function handleExport(format) {
  if (filteredData.value.length === 0) {
    ElMessage.warning(t('messages.noExport'))
    return
  }
  const filename = `ports_${new Date().toISOString().slice(0, 10)}`
  if (format === 'excel') exportToExcel(filteredData.value, filename)
  else if (format === 'markdown') exportToMarkdown(filteredData.value, filename)
  else exportToTxt(filteredData.value, filename)
  ElMessage.success(t('messages.exportSuccess'))
}

function handleMonitorAlert(alerts) {
  alertCount.value = alerts.length
  alerts.forEach(a => {
    const msg = a.occupied
      ? t('monitor.alertOccupied', { port: a.port })
      : t('monitor.alertReleased', { port: a.port })
    ElNotification({
      title: t('monitor.alertTitle'),
      message: msg + (a.processName ? ' - ' + a.processName : ''),
      type: 'warning',
      duration: 8000,
      position: 'top-right'
    })
  })
}

function createGroup() {
  if (!newGroupName.value.trim()) {
    ElMessage.warning(t('group.nameRequired'))
    return
  }
  groups.value.push({
    id: 'custom_' + Date.now(),
    name: newGroupName.value.trim(),
    ports: []
  })
  saveToStorage(STORAGE_KEYS.GROUPS, groups.value)
  newGroupName.value = ''
  showGroupDialog.value = false
  ElMessage.success(t('group.created'))
}

function handleAddToGroup(row) {
  addPortItem.value = row
  portRemark.value = `${row.port}-${row.processName || '未命名'}`
  showAddToGroupDialog.value = true
}

function confirmAddToGroup() {
  if (!targetGroupId.value) {
    ElMessage.warning(t('group.selectRequired'))
    return
  }
  const group = groups.value.find(g => g.id === targetGroupId.value)
  if (group) {
    const exists = group.ports.some(p => p.port === addPortItem.value.port)
    if (!exists) {
      group.ports.push({ port: addPortItem.value.port, remark: portRemark.value })
      saveToStorage(STORAGE_KEYS.GROUPS, groups.value)
      ElMessage.success(t('group.added'))
    } else {
      ElMessage.info(t('group.alreadyInGroup'))
    }
  }
  showAddToGroupDialog.value = false
}
</script>

<style scoped>
.app-container {
  height: 100vh;
}

.sidebar {
  background: var(--pm-bg-card);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  border-right: 1px solid var(--pm-border);
  transition: background-color 0.25s, border-color 0.25s;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  border-bottom: 1px solid var(--pm-border);
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: var(--pm-text-primary);
}

.version-tag {
  margin-left: auto;
}

.group-menu {
  border-right: none;
  flex: 1;
}

.group-count {
  margin-left: auto;
}

.sidebar-actions {
  padding: 8px 12px;
  display: flex;
  gap: 8px;
}

.common-ports {
  padding: 0 12px 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.common-port-tag {
  cursor: pointer;
}

.common-port-tag:hover {
  background: var(--pm-bg-accent);
  border-color: #409EFF;
  color: #409EFF;
}

.history-list {
  padding: 0 12px 16px;
  max-height: 200px;
  overflow-y: auto;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  cursor: pointer;
  border-radius: 4px;
  font-size: 13px;
  color: var(--pm-text-regular);
}

.history-item:hover {
  background: var(--pm-bg-hover);
  color: #409EFF;
}

.dashboard-header {
  padding: 12px 16px 0;
  background: var(--pm-bg-page);
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  margin: 8px 16px 0;
  background: var(--pm-bg-card);
  border-radius: 8px;
  transition: background-color 0.25s;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  margin: 12px 16px 0;
  background: var(--pm-bg-card);
  border-radius: 8px;
  flex-wrap: wrap;
  gap: 8px;
  transition: background-color 0.25s;
}

.toolbar-left, .toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.main-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.permission-banner {
  margin: 8px 16px 0;
  flex-shrink: 0;
}

.main-content {
  flex: 1;
  min-height: 0;
  padding: 12px 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.main-content :deep(.stats-bar) {
  flex-shrink: 0;
}

.main-content :deep(.port-table-wrapper) {
  flex: 1;
  min-height: 0;
}

.footer-stats {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 16px;
  background: var(--pm-bg-card);
  border-top: 1px solid var(--pm-border);
  font-size: 13px;
  transition: background-color 0.25s, border-color 0.25s;
}

.monitor-badge {
  margin-left: 4px;
}

.diff-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  margin: 8px 16px 0;
  background: var(--pm-bg-card);
  border-radius: 8px;
  font-size: 13px;
}

.diff-hint {
  color: var(--pm-text-muted);
  font-size: 12px;
}
</style>
