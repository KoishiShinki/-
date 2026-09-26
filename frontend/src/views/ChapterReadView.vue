<template>
  <section class="page section">
    <div v-if="error" class="empty">
      <h2>这一章暂时无法阅读</h2>
      <p>{{ error }}</p>
      <RouterLink class="btn primary" to="/explore">返回探索</RouterLink>
    </div>
    <ChapterReader
      v-else-if="chapter"
      :chapter="chapter"
      :worldline="worldline"
      :illustrations="illustrations"
    />
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import ChapterReader from "@/components/ChapterReader.vue";
import { publicApi } from "@/api/timeline";

const route = useRoute();
const chapter = ref(null);
const worldline = ref(null);
const illustrations = ref([]);
const error = ref("");

onMounted(async () => {
  try {
    const response = await publicApi.chapter(route.params.id);
    chapter.value = response.data;
    if (chapter.value?.worldlineId) {
      const [w, i] = await Promise.all([
        publicApi.worldline(chapter.value.worldlineId),
        publicApi.illustrations(chapter.value.worldlineId),
      ]);
      worldline.value = w.data;
      illustrations.value = (i.data || []).filter(
        (x) => Number(x.chapterId) === Number(chapter.value.chapterId),
      );
    }
  } catch (e) {
    error.value = e.response?.data?.msg || "章节可能尚未公开。";
  }
});
</script>
