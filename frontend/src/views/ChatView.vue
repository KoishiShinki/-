<template>
  <section class="page section">
    <div class="page-heading">
      <div>
        <h1 class="serif">向世界线提问</h1>
        <p class="muted">
          从现有事件与章节中寻找线索、理解分歧，或寻找下一章的方向。
        </p>
      </div>
    </div>
    <div class="toolbar-actions" style="margin-bottom: 20px">
      <button
        v-for="q in suggestions"
        :key="q"
        class="btn"
        @click="question = q"
      >
        {{ q }}
      </button>
    </div>
    <form class="paper-panel schema-form" @submit.prevent="ask">
      <label class="field"
        ><span>你的问题</span
        ><textarea
          class="textarea"
          v-model="question"
          required
          placeholder="想从这条世界线了解什么？"
        /></label
      ><button class="btn primary" :disabled="loading">
        {{ loading ? "正在梳理档案…" : "向 AI 提问" }}
      </button>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
    </form>
    <article v-if="answer" class="record" style="margin-top: 24px">
      <h2>档案中的线索</h2>
      <p>{{ answer }}</p>
    </article>
    <div class="toolbar" style="margin-top: 36px">
      <h2>对话记录</h2>
      <button class="btn" @click="loadHistory">刷新记录</button>
    </div>
    <div class="record-list">
      <article v-for="item in history" :key="item.taskId" class="record">
        <span class="record-id">{{ item.createTime }}</span>
        <h3>{{ item.inputContent || "世界线问答" }}</h3>
        <p>{{ item.resultContent || item.errorMsg || "任务处理中" }}</p>
        <ActionButton
          label="删除记录"
          danger
          confirm="确定删除这条对话记录？"
          :action="() => http.delete(`/app/chat/history/${item.taskId}`)"
          @done="loadHistory"
        />
      </article>
    </div>
    <div v-if="!history.length" class="empty">你的问题和回答会保存在这里。</div>
  </section>
</template>
<script setup>
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { appApi } from "@/api/timeline";
import { http } from "@/api/http";
import ActionButton from "@/components/ActionButton.vue";
const id = useRoute().params.id,
  question = ref(""),
  answer = ref(""),
  loading = ref(false),
  error = ref(""),
  history = ref([]);
const suggestions = [
  "这条世界线最大的分歧点是什么？",
  "当前阶段有哪些人物冲突？",
  "下一章可以如何展开？",
];
async function loadHistory() {
  try {
    const r = await appApi.chatHistory(id);
    history.value = r.data || [];
  } catch (e) {
    error.value = e.message;
  }
}
async function ask() {
  if (!question.value.trim()) return;
  loading.value = true;
  error.value = "";
  try {
    const r = await appApi.ask({
      worldlineId: Number(id),
      question: question.value,
    });
    answer.value = r.data || "";
    await loadHistory();
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}
onMounted(loadHistory);
</script>
