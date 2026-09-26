<template>
  <article class="reader paper-panel">
    <div
      v-if="mediaOrFallback(chapter.coverUrl, '')"
      class="cover"
      :style="{
        backgroundImage: `url(${JSON.stringify(mediaOrFallback(chapter.coverUrl, ''))})`,
      }"
    ></div>
    <h1 class="serif">{{ chapter.chapterTitle }}</h1>
    <div class="muted">{{ worldline?.worldlineName || "世界线档案" }}</div>
    <div class="content serif">
      <template v-for="(block, index) in blocks" :key="index"
        ><p>{{ block.text }}</p>
        <figure v-for="image in block.images" :key="image.illustrationId">
          <img
            :src="mediaOrFallback(image.imageUrl, '')"
            :alt="image.illustrationTitle || '章节插图'"
            loading="lazy"
          /></figure
      ></template>
    </div>
    <footer class="reader-footer">
      <RouterLink
        class="back-link"
        :to="`/explore/worldline/${chapter.worldlineId}`"
        >← 返回作品目录</RouterLink
      >
    </footer>
  </article>
</template>

<script setup>
import { computed } from "vue";
import { mediaOrFallback } from "@/utils/media";

const props = defineProps({
  chapter: { type: Object, required: true },
  worldline: { type: Object, default: null },
  illustrations: { type: Array, default: () => [] },
});
const blocks = computed(() => {
  const paragraphs = (props.chapter.content || "章节正文为空。")
    .split(/\n+/)
    .map((x) => x.trim())
    .filter(Boolean);
  const positions = new Map();
  props.illustrations.forEach((image, index) => {
    const position = Math.min(
      paragraphs.length - 1,
      Math.max(
        0,
        Math.round(
          ((index + 1) * paragraphs.length) / (props.illustrations.length + 1),
        ) - 1,
      ),
    );
    const bucket = positions.get(position) || [];
    bucket.push(image);
    positions.set(position, bucket);
  });
  return paragraphs.map((text, index) => ({
    text,
    images: positions.get(index) || [],
  }));
});
</script>

<style scoped>
.reader {
  max-width: 860px;
  margin: 0 auto;
  padding: 34px;
}

.cover {
  height: 320px;
  border-radius: 12px;
  margin-bottom: 26px;
  background-size: cover;
  background-position: center;
}

h1 {
  margin: 0 0 10px;
  font-size: clamp(30px, 5vw, 48px);
}

.content {
  margin-top: 28px;
  font-size: 18px;
  line-height: 1.95;
}
.content p {
  margin: 0 0 1.15em;
  text-indent: 2em;
  text-align: justify;
}
.content figure {
  margin: 32px 0;
}
.content img {
  display: block;
  width: 100%;
  max-height: 580px;
  object-fit: cover;
  border-radius: 8px;
}
.reader-footer {
  margin-top: 38px;
  padding-top: 22px;
  border-top: 1px solid rgba(42, 33, 24, 0.15);
}
.back-link {
  color: var(--burgundy);
  font-weight: 700;
}

@media (max-width: 720px) {
  .reader {
    padding: 22px;
  }

  .cover {
    height: 220px;
  }
}
</style>
