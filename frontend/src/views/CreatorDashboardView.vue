<template>
  <section class="page section">
    <div class="toolbar">
      <div>
        <h1 class="serif">创作者工作台</h1>
        <p class="muted">继续整理你的世界线故事。</p>
      </div>
      <RouterLink class="btn primary" to="/creator/worldline/create"
        >创建世界线</RouterLink
      >
    </div>
    <nav class="toolbar-actions" style="margin: 20px 0">
      <RouterLink class="btn" to="/creator/tasks">AI 任务记录</RouterLink
      ><RouterLink class="btn" to="/creator/profile">创作者资料</RouterLink>
    </nav>
    <p v-if="loading" role="status">正在读取你的世界线…</p>
    <p v-if="error" class="error">{{ error }}</p>
    <h2 class="serif">我的世界线</h2>
    <div class="grid two">
      <WorldlineCard
        v-for="item in worldlines"
        :key="item.worldlineId"
        :worldline="item"
        mode="creator"
      />
    </div>
    <div v-if="!loading && !error && !worldlines.length" class="empty">
      还没有世界线，先创建一个项目。
    </div>
  </section>
</template>
<script setup>
import { onMounted, ref } from "vue";
import WorldlineCard from "@/components/WorldlineCard.vue";
import { appApi } from "@/api/timeline";
const worldlines = ref([]),
  loading = ref(true),
  error = ref("");
async function loadWorldlines() {
  try {
    const response = await appApi.myWorldlines();
    worldlines.value = response.data || [];
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}
onMounted(loadWorldlines);
</script>
<style scoped>
h1 {
  margin: 0 0 8px;
  font-size: 42px;
}
h2 {
  margin: 0 0 18px;
}
</style>
