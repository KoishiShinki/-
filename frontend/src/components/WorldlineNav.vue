<template>
  <div v-if="id" class="world-nav">
    <RouterLink class="back-link" to="/creator">‹ 我的世界线</RouterLink>
    <nav aria-label="世界线功能">
      <RouterLink
        v-for="[path, label] in items"
        :key="path"
        :to="`/creator/worldline/${id}${path ? '/' + path : ''}`"
        :class="{ active: tail === path }"
        >{{ label }}</RouterLink
      >
    </nav>
  </div>
</template>
<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";
const route = useRoute();
const id = computed(
  () => route.path.startsWith("/creator/worldline/") && route.params.id,
);
const tail = computed(() => route.path.split("/")[4] || "");
const items = [
  ["", "总览"],
  ["upload", "截图识别"],
  ["timeline", "时间线"],
  ["stages", "阶段"],
  ["chapters", "章节"],
  ["illustrations", "插图"],
  ["nations", "国家"],
  ["characters", "人物"],
  ["relations", "事件关系"],
  ["chat", "问答"],
  ["settings", "设置"],
];
</script>
