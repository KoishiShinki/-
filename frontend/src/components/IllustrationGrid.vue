<template>
  <div class="illustration-grid">
    <article
      v-for="item in items"
      :key="item.illustrationId"
      class="illustration"
      role="button"
      tabindex="0"
      aria-label="打开插图详情"
      @click="openDetail(item)"
      @keydown.enter="openDetail(item)"
      @keydown.space.prevent="openDetail(item)"
    >
      <div
        class="image"
        :style="{
          backgroundImage: `url(${JSON.stringify(mediaOrFallback(item.imageUrl, fallback))})`,
        }"
      ></div>
      <div class="info">
        <h3 class="serif">{{ item.illustrationTitle || "未命名插图" }}</h3>
        <span class="detail-hint">点击查看大图 →</span>
        <div class="card-actions" @click.stop @keydown.stop>
          <slot :item="item" />
        </div>
      </div>
    </article>
    <div v-if="!items.length" class="empty">暂无插图。</div>
  </div>

  <ModalDialog
    :open="Boolean(activeItem)"
    :title="activeItem?.illustrationTitle || '未命名插图'"
    eyebrow="插图详情"
    width="940px"
    @close="activeItem = null"
  >
    <div v-if="activeItem" class="illustration-detail">
      <img
        :src="mediaOrFallback(activeItem.imageUrl, fallback)"
        :alt="activeItem.illustrationTitle || '插图'"
      />
    </div>
  </ModalDialog>
</template>

<script setup>
import { ref } from "vue";
import fallback from "@/assets/worldlines.svg";
import ModalDialog from "@/components/ModalDialog.vue";
import { mediaOrFallback } from "@/utils/media";

defineProps({
  items: { type: Array, default: () => [] },
});

const activeItem = ref(null);

function openDetail(item) {
  activeItem.value = item;
}
</script>

<style scoped>
.illustration-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.illustration {
  display: grid;
  grid-template-rows: auto 1fr;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--paper);
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    transform 0.18s ease,
    box-shadow 0.18s ease;
}

.illustration:hover,
.illustration:focus-visible {
  outline: none;
  border-color: rgba(38, 93, 229, 0.62);
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.2);
}

.image {
  aspect-ratio: 16 / 10;
  background-size: cover;
  background-position: center;
}

.info {
  display: flex;
  flex-direction: column;
  padding: 14px;
}

h3 {
  margin: 0 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

p {
  margin: 0;
  color: var(--muted);
  line-height: 1.6;
}

.info > p {
  display: -webkit-box;
  min-height: 3.2em;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.detail-hint {
  margin-top: 10px;
  color: var(--gold);
  font-size: 12px;
}

.card-actions {
  margin-top: auto;
}

.illustration-detail {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.illustration-detail img {
  display: block;
  width: 100%;
  max-height: 65vh;
  object-fit: contain;
  border-radius: 12px;
  background: rgba(4, 9, 18, 0.35);
}

@media (max-width: 900px) {
  .illustration-detail {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .illustration-grid {
    grid-template-columns: 1fr;
  }
}
</style>
