<template>
  <div class="stats-bar">
    <el-tag effect="plain">TCP {{ stats.tcp }}</el-tag>
    <el-tag effect="plain" type="success">UDP {{ stats.udp }}</el-tag>
    <el-tag effect="plain" type="success">监听 {{ stats.listen }}</el-tag>
    <el-tag effect="plain" type="primary">连接 {{ stats.established }}</el-tag>
    <el-tag effect="plain" type="info">空闲 {{ stats.free }}</el-tag>
    <el-tag v-if="stats.conflicts > 0" effect="dark" type="danger">
      冲突 {{ stats.conflicts }}
    </el-tag>
  </div>
</template>

<script setup>
defineProps({
  stats: {
    type: Object,
    default: () => ({ tcp: 0, udp: 0, listen: 0, established: 0, free: 0, conflicts: 0 })
  }
})
</script>

<style scoped>
.stats-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
</style>
