<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="空闲端口生成器"
    width="520px"
  >
    <el-form label-width="100px">
      <el-form-item label="起始端口">
        <el-input-number v-model="startPort" :min="1" :max="65535" />
      </el-form-item>
      <el-form-item label="需要数量">
        <el-input-number v-model="count" :min="1" :max="100" />
      </el-form-item>
      <el-form-item label="快捷模板">
        <el-button
          v-for="tpl in templates"
          :key="tpl.label"
          size="small"
          @click="applyTemplate(tpl)"
        >{{ tpl.label }}</el-button>
      </el-form-item>
    </el-form>

    <el-button type="primary" :loading="loading" @click="generate" style="width: 100%">
      生成空闲端口
    </el-button>

    <div v-if="result" class="result-area">
      <el-alert :title="result.message" type="success" show-icon :closable="false" />
      <div class="port-list">
        <el-tag v-for="p in result.freePorts" :key="p" class="port-tag" effect="plain">{{ p }}</el-tag>
      </div>
      <el-button type="success" :icon="CopyDocument" @click="copyPorts" style="margin-top: 12px">
        一键复制
      </el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { CopyDocument } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/api'
import { PORT_TEMPLATES } from '@/utils/storage'

defineProps({ modelValue: Boolean })
defineEmits(['update:modelValue'])

const startPort = ref(8080)
const count = ref(5)
const loading = ref(false)
const result = ref(null)
const templates = PORT_TEMPLATES

function applyTemplate(tpl) {
  startPort.value = tpl.start
  count.value = tpl.count
}

async function generate() {
  loading.value = true
  result.value = null
  try {
    const res = await request.get('/ports/free', { params: { start: startPort.value, count: count.value } })
    result.value = res.data
  } finally { loading.value = false }
}

function copyPorts() {
  if (!result.value?.freePorts?.length) return
  const text = result.value.freePorts.join(', ')
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}
</script>

<style scoped>
.result-area {
  margin-top: 16px;
}

.port-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.port-tag {
  font-size: 14px;
  padding: 4px 12px;
}
</style>
