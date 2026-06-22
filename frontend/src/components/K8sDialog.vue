<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :title="t('k8s.title')"
    width="820px"
    @open="loadData"
  >
    <div class="toolbar">
      <el-tag :type="kubectlAvailable ? 'success' : 'danger'">
        {{ kubectlAvailable ? t('k8s.available') : t('k8s.unavailable') }}
      </el-tag>
      <el-tag v-if="context" type="info">{{ t('k8s.context') }}: {{ context }}</el-tag>
      <el-input v-model="namespace" :placeholder="t('k8s.namespaceFilter')" clearable style="width: 160px" @keyup.enter="loadData" />
      <el-button :icon="Refresh" :loading="loading" @click="loadData">{{ t('common.refresh') }}</el-button>
    </div>

    <el-tabs v-model="activeTab" style="margin-top: 12px">
      <el-tab-pane :label="t('k8s.pods')" name="pods">
        <el-table :data="pods" v-loading="loading" size="small" border max-height="380">
          <el-table-column prop="namespace" :label="t('k8s.namespace')" width="120" />
          <el-table-column prop="name" :label="t('k8s.podName')" min-width="160" show-overflow-tooltip />
          <el-table-column prop="status" :label="t('k8s.status')" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 'Running' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="podIp" label="Pod IP" width="120" />
          <el-table-column prop="node" :label="t('k8s.node')" width="120" show-overflow-tooltip />
          <el-table-column :label="t('k8s.ports')" min-width="180">
            <template #default="{ row }">
              <el-tag v-for="(p, i) in row.ports" :key="i" size="small" style="margin: 2px">
                {{ p.containerPort }}/{{ p.protocol }}
              </el-tag>
              <span v-if="!row.ports?.length">-</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('common.action')" width="80">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="queryPorts(row)">{{ t('k8s.queryPort') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane :label="t('k8s.services')" name="services">
        <el-table :data="services" v-loading="loading" size="small" border max-height="380">
          <el-table-column prop="namespace" :label="t('k8s.namespace')" width="120" />
          <el-table-column prop="name" :label="t('k8s.serviceName')" min-width="140" show-overflow-tooltip />
          <el-table-column prop="type" :label="t('k8s.type')" width="100" />
          <el-table-column prop="clusterIp" label="Cluster IP" width="120" />
          <el-table-column :label="t('k8s.ports')" min-width="200">
            <template #default="{ row }">
              <el-tag v-for="(p, i) in row.ports" :key="i" size="small" style="margin: 2px">
                {{ p.port }}→{{ p.targetPort }}{{ p.nodePort ? `(node:${p.nodePort})` : '' }}
              </el-tag>
              <span v-if="!row.ports?.length">-</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('common.action')" width="80">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="querySvcPorts(row)">{{ t('k8s.queryPort') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import request from '@/api'

defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'query-port'])

const { t } = useI18n()

const activeTab = ref('pods')
const namespace = ref('')
const pods = ref([])
const services = ref([])
const loading = ref(false)
const kubectlAvailable = ref(false)
const context = ref('')

async function loadData() {
  loading.value = true
  try {
    const avail = await request.get('/k8s/available')
    kubectlAvailable.value = avail.data
    if (!avail.data) {
      pods.value = []
      services.value = []
      return
    }
    const ctx = await request.get('/k8s/context')
    context.value = ctx.data || ''
    const params = namespace.value ? { namespace: namespace.value } : {}
    const [podRes, svcRes] = await Promise.all([
      request.get('/k8s/pods', { params }),
      request.get('/k8s/services', { params })
    ])
    pods.value = podRes.data || []
    services.value = svcRes.data || []
  } finally {
    loading.value = false
  }
}

function queryPorts(row) {
  const ports = (row.ports || []).map(p => p.containerPort).filter(Boolean)
  if (!ports.length) {
    ElMessage.info(t('k8s.noPorts'))
    return
  }
  emit('query-port', ports.join(','))
  emit('update:modelValue', false)
}

function querySvcPorts(row) {
  const ports = (row.ports || []).map(p => p.nodePort || p.port).filter(Boolean)
  if (!ports.length) {
    ElMessage.info(t('k8s.noPorts'))
    return
  }
  emit('query-port', ports.join(','))
  emit('update:modelValue', false)
}
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
</style>
