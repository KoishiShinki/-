<template>
  <article class="stage-card paper-panel">
    <div class="stage-years">
      {{ stage.startYear || "-" }} - {{ stage.endYear || "-" }}
    </div>
    <h3 class="serif">{{ stage.stageName }}</h3>
    <p>{{ stage.stageSummary || "尚未生成阶段摘要。" }}</p>
    <div class="meta">
      <span>{{ stage.stageTheme || "历史阶段" }}</span>
      <span>{{ stage.eventCount || 0 }} 个事件</span>
      <span>小说 {{ statusText(stage.novelStatus) }}</span>
    </div>
    <slot />
  </article>
</template>

<script setup>
defineProps({
  stage: { type: Object, required: true },
});

function statusText(value) {
  return (
    { 0: "未生成", 1: "生成中", 2: "已生成", 3: "失败" }[value] ||
    value ||
    "未生成"
  );
}
</script>

<style scoped>
.stage-card {
  padding: 18px;
}

.stage-years {
  color: var(--burgundy);
  font-family: Georgia, serif;
  font-size: 14px;
}

h3 {
  margin: 8px 0;
  font-size: 22px;
}

p {
  margin: 0 0 12px;
  line-height: 1.8;
}

.meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 13px;
  color: rgba(24, 50, 74, 0.62);
}
</style>
