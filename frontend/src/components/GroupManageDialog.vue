<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="分组管理"
    width="560px"
  >
    <el-collapse v-model="activeNames">
      <el-collapse-item v-for="group in localGroups" :key="group.id" :name="group.id">
        <template #title>
          <el-input
            v-if="editingId === group.id"
            v-model="editName"
            size="small"
            style="width: 200px"
            @click.stop
            @keyup.enter="saveRename(group)"
          />
          <span v-else>{{ group.name }} ({{ group.ports.length }})</span>
        </template>

        <div class="group-actions">
          <el-button size="small" @click="startRename(group)">改名</el-button>
          <el-button size="small" type="danger" @click="deleteGroup(group)">删除分组</el-button>
        </div>

        <el-table :data="group.ports" size="small" border>
          <el-table-column prop="port" label="端口" width="80" />
          <el-table-column prop="remark" label="备注">
            <template #default="{ row }">
              <el-input v-model="row.remark" size="small" @change="emitUpdate" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="removePort(group, $index)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="add-port-row">
          <el-input v-model="group._newPort" placeholder="端口号" size="small" style="width: 100px" />
          <el-input v-model="group._newRemark" placeholder="备注" size="small" style="width: 200px" />
          <el-button size="small" type="primary" @click="addPortToGroup(group)">添加</el-button>
        </div>
      </el-collapse-item>
    </el-collapse>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { saveToStorage, STORAGE_KEYS } from '@/utils/storage'

const props = defineProps({
  modelValue: Boolean,
  groups: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:modelValue', 'update'])

const localGroups = ref([])
const activeNames = ref([])
const editingId = ref('')
const editName = ref('')

watch(() => props.groups, (val) => {
  localGroups.value = val.map(g => ({ ...g, _newPort: '', _newRemark: '' }))
  activeNames.value = val.map(g => g.id)
}, { immediate: true, deep: true })

function emitUpdate() {
  const cleaned = localGroups.value.map(({ _newPort, _newRemark, ...rest }) => rest)
  saveToStorage(STORAGE_KEYS.GROUPS, cleaned)
  emit('update')
}

function startRename(group) {
  editingId.value = group.id
  editName.value = group.name
}

function saveRename(group) {
  group.name = editName.value.trim() || group.name
  editingId.value = ''
  emitUpdate()
}

function deleteGroup(group) {
  ElMessageBox.confirm(`确定删除分组「${group.name}」？`, '确认', { type: 'warning' })
    .then(() => {
      localGroups.value = localGroups.value.filter(g => g.id !== group.id)
      emitUpdate()
      ElMessage.success('已删除')
    }).catch(() => {})
}

function removePort(group, index) {
  group.ports.splice(index, 1)
  emitUpdate()
}

function addPortToGroup(group) {
  const port = parseInt(group._newPort)
  if (!port) {
    ElMessage.warning('请输入有效端口')
    return
  }
  if (group.ports.some(p => p.port === port)) {
    ElMessage.info('端口已存在')
    return
  }
  group.ports.push({ port, remark: group._newRemark || `${port}` })
  group._newPort = ''
  group._newRemark = ''
  emitUpdate()
}
</script>

<style scoped>
.group-actions {
  margin-bottom: 8px;
  display: flex;
  gap: 8px;
}

.add-port-row {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  align-items: center;
}
</style>
