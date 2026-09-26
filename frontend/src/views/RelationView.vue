<template>
  <section class="page section">
    <h1 class="serif">事件关系</h1>
    <p class="muted">把历史的起因与结果连接起来，理解世界线的转折。</p>
    <SchemaForm
      :fields="fields"
      :initial="{ relationType: 'cause' }"
      title="建立事件关系"
      label="连接事件"
      :save="save"
      @saved="load"
    />
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="loading">正在读取事件关系…</p>
    <article v-for="r in relations" :key="r.relationId" class="relation-item">
      <strong>{{ eventName(r.sourceEventId) }}</strong
      ><span aria-label="关联到">⟶</span
      ><strong>{{ eventName(r.targetEventId) }}</strong>
      <p>{{ r.relationDesc || r.relationType }}</p>
    </article>
    <div v-if="!loading && !relations.length" class="empty">
      先记录两个事件，再建立它们的联系。
    </div>
  </section>
</template>
<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { http } from "@/api/http";
import SchemaForm from "@/components/SchemaForm.vue";
const id = useRoute().params.id,
  events = ref([]),
  relations = ref([]),
  error = ref(""),
  loading = ref(true);
const eventMap = computed(
  () => new Map(events.value.map((e) => [Number(e.eventId), e])),
);
const fields = computed(() => [
  {
    key: "sourceEventId",
    label: "起因事件",
    type: "select",
    required: true,
    options: events.value.map((e) => [
      e.eventId,
      `${e.eventYear} ${e.eventTitle}`,
    ]),
  },
  {
    key: "targetEventId",
    label: "结果事件",
    type: "select",
    required: true,
    options: events.value.map((e) => [
      e.eventId,
      `${e.eventYear} ${e.eventTitle}`,
    ]),
  },
  {
    key: "relationType",
    label: "关系类型",
    type: "select",
    required: true,
    options: [
      ["cause", "因果"],
      ["influence", "影响"],
      ["continuation", "延续"],
      ["conflict", "冲突"],
    ],
  },
  { key: "relationDesc", label: "关系说明", type: "textarea" },
]);
function eventName(id) {
  return eventMap.value.get(Number(id))?.eventTitle || id;
}
async function load() {
  try {
    const [e, r] = await Promise.all([
      http.get(`/app/event/list/${id}`),
      http.get(`/app/event/relation/${id}`),
    ]);
    events.value = e.data || [];
    relations.value = r.data || [];
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}
async function save(value) {
  if (String(value.sourceEventId) === String(value.targetEventId))
    throw new Error("请选择两个不同的事件。");
  await http.post("/app/event/relation", {
    ...value,
    worldlineId: Number(id),
    sourceEventId: Number(value.sourceEventId),
    targetEventId: Number(value.targetEventId),
  });
}
onMounted(load);
</script>
