<template>
  <div class="panel ai-task">
    <div class="toolbar">
      <h3 class="serif">AI任务</h3>
      <button class="btn" @click="$emit('refresh')">刷新</button>
    </div>
    <div v-if="!items.length" class="empty">暂无近期任务。</div>
    <div v-for="task in items" :key="task.taskId" class="task-row">
      <span>{{ task.taskType }}</span>
      <strong :class="statusClass(task.taskStatus)">{{
        statusText(task.taskStatus)
      }}</strong>
    </div>
  </div>
</template>

<script setup>
defineEmits(["refresh"]);
defineProps({
  items: { type: Array, default: () => [] },
});

function statusText(value) {
  return { 0: "待处理", 1: "处理中", 2: "成功", 3: "失败" }[value] || value;
}

function statusClass(value) {
  return value === "2" ? "success" : value === "3" ? "error" : "";
}
</script>

<style scoped>
.ai-task {
  padding: 18px;
}

.toolbar {
  margin: 0 0 14px;
}

h3 {
  margin: 0;
}

.task-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-top: 1px solid var(--line);
  color: var(--muted);
}
</style>
