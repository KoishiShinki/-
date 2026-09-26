<template>
  <section class="page section">
    <div class="toolbar">
      <div>
        <h1 class="serif">AI 任务记录</h1>
        <p class="muted">查看结果和进度，或在调整配置后重试失败任务。</p>
      </div>
      <button class="btn" @click="load">刷新任务</button>
    </div>
    <p v-if="loading" role="status">正在读取任务…</p>
    <p v-if="error" class="error">{{ error }}</p>
    <div class="record-list">
      <article class="record" v-for="task in tasks" :key="task.taskId">
        <span class="badge">{{ status(task.taskStatus) }}</span>
        <h3>{{ task.taskType }} / 任务 {{ task.taskId }}</h3>
        <p>{{ task.finishTime || task.createTime }}</p>
        <p v-if="task.errorMsg" class="error">{{ task.errorMsg }}</p>
        <details>
          <summary>查看任务详情</summary>
          <h4>输入</h4>
          <p>{{ task.inputContent }}</p>
          <h4>结果</h4>
          <p>{{ task.resultContent }}</p>
        </details>
        <div class="record-actions">
          <ActionButton
            v-if="String(task.taskStatus) === '3'"
            label="重试任务"
            :action="() => http.post(`/app/aiTask/retry/${task.taskId}`)"
            @done="load"
          />
        </div>
      </article>
    </div>
    <div v-if="!loading && !tasks.length" class="empty">
      还没有 AI 任务。识别截图、生成小说或插图后，任务会显示在这里。
    </div>
  </section>
</template>
<script setup>
import { onMounted, ref } from "vue";
import { http } from "@/api/http";
import ActionButton from "@/components/ActionButton.vue";
const tasks = ref([]),
  loading = ref(true),
  error = ref("");
const status = (value) =>
  ({ 0: "等待中", 1: "处理中", 2: "已完成", 3: "失败" })[value] || value;
async function load() {
  loading.value = true;
  error.value = "";
  try {
    const r = await http.get("/app/aiTask/recent");
    tasks.value = r.data || [];
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}
onMounted(load);
</script>
