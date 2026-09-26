<template>
  <article
    class="timeline-node"
    :class="{ divergence: event.divergenceFlag === '1' }"
  >
    <div class="year">{{ event.eventYear || "未知" }}</div>
    <div class="node-body">
      <div class="node-head">
        <span class="badge">{{ eventTypeName(event.eventType) }}</span>
        <span v-if="event.divergenceFlag === '1'" class="badge warn"
          >分歧点</span
        >
      </div>
      <h3 class="serif">{{ event.eventTitle }}</h3>
      <p>{{ event.summary }}</p>
      <div class="muted">{{ event.country }} · {{ event.eventDate }}</div>
      <section v-if="event.aiDescription" class="fragment-box">
        <div class="fragment-title">已生成小说片段</div>
        <div class="fragment-content serif">{{ event.aiDescription }}</div>
      </section>
      <slot />
    </div>
  </article>
</template>

<script setup>
defineProps({
  event: { type: Object, required: true },
});

const eventTypeMap = {
  war: "战争事件",
  diplomacy: "外交事件",
  law: "法律改革",
  economy: "经济危机",
  revolution: "革命叛乱",
  technology: "科技突破",
  colony: "殖民扩张",
  government: "政权更替",
  character: "重要人物",
  divergence: "世界线分歧点",
  other: "其他事件",
};

function eventTypeName(value) {
  return eventTypeMap[value] || value || "其他事件";
}
</script>

<style scoped>
.timeline-node {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 18px;
  position: relative;
  padding: 6px 0;
}

.year {
  font-family: Georgia, serif;
  color: var(--gold);
  font-size: 24px;
  text-align: right;
  padding-top: 14px;
}

.node-body {
  border: 1px solid var(--line);
  border-left: 3px solid var(--gold);
  border-radius: 12px;
  padding: 18px;
  background: rgba(21, 31, 50, 0.76);
}

.divergence .node-body {
  border-left-color: var(--burgundy);
}

.node-head {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.warn {
  color: #a13b48;
  border-color: rgba(124, 45, 54, 0.5);
}

h3 {
  margin: 0 0 8px;
}

p {
  margin: 0 0 12px;
  line-height: 1.8;
  color: var(--text);
}

.fragment-box {
  margin-top: 16px;
  padding: 16px;
  border: 1px solid rgba(38, 93, 229, 0.24);
  border-radius: 10px;
  background: rgba(243, 231, 208, 0.07);
}

.fragment-title {
  margin-bottom: 10px;
  color: var(--gold);
  font-size: 13px;
  font-weight: 700;
}

.fragment-content {
  white-space: pre-wrap;
  color: var(--text);
  line-height: 1.95;
}

@media (max-width: 720px) {
  .timeline-node {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .year {
    text-align: left;
  }
}
</style>
