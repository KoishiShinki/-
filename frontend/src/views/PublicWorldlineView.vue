<template>
  <section class="public-work">
    <div v-if="loading" class="page state">
      <span class="loader"></span>
      <p>正在打开作品…</p>
    </div>
    <div v-else-if="error" class="page state">
      <h1 class="serif">作品暂时无法打开</h1>
      <p>{{ error }}</p>
      <RouterLink class="btn primary" to="/explore">返回探索</RouterLink>
    </div>
    <template v-else-if="worldline">
      <header class="cover" :style="coverStyle">
        <div class="page cover-copy">
          <span class="eyebrow">{{
            worldline.gameName || "架空历史小说"
          }}</span>
          <h1 class="serif">{{ worldline.worldlineName }}</h1>
          <p>
            {{
              worldline.description || "一部根据世界线历史档案创作的图文小说。"
            }}
          </p>
          <RouterLink
            v-if="worldline.creatorId"
            class="btn"
            :to="`/authors/${worldline.creatorId}`"
            >创作者主页</RouterLink
          >
          <div class="meta">
            <span>{{ chapters.length }} 章</span
            ><span>{{ events.length }} 个历史节点</span
            ><span>{{ illustrations.length }} 幅插图</span
            ><span>偏离度 {{ worldline.divergenceScore || 0 }}</span>
          </div>
          <a class="btn primary" href="#catalog">查看章节目录</a>
        </div>
      </header>
      <main id="catalog" class="page catalog-layout">
        <article class="catalog paper">
          <header>
            <h2 class="serif">章节目录</h2>
            <p>像阅读网络小说一样，选择章节进入完整图文正文。</p>
          </header>
          <div v-if="chapters.length" class="chapter-list">
            <RouterLink
              v-for="(chapter, index) in chapters"
              :key="chapter.chapterId"
              :to="`/read/chapter/${chapter.chapterId}`"
              class="chapter-item"
              ><span class="chapter-no">{{ pad(index + 1) }}</span>
              <div>
                <h3 class="serif">
                  {{ chapter.chapterTitle || "未命名章节" }}
                </h3>
                <p>{{ excerpt(chapter.content) }}</p>
                <small
                  >{{ imageCount(chapter) }} 幅配图 · 点击阅读完整内容</small
                >
              </div>
              <span class="arrow">→</span></RouterLink
            >
          </div>
          <div v-else class="no-chapter">
            <h3 class="serif">作者尚未公开章节</h3>
            <p>
              世界线已经公开，但小说章节仍处于草稿或已完成状态。作者将章节设为公开后，就会出现在这里。
            </p>
          </div>
        </article>
        <aside class="archive-side">
          <section class="side-card">
            <span class="eyebrow">阅读说明</span>
            <h3 class="serif">从档案到故事</h3>
            <p>
              每章由一个历史阶段及其事件发展而来，正文内会穿插与章节绑定并已采用的原创插图。
            </p>
          </section>
          <details class="timeline-fold">
            <summary>
              查看历史节点年表 <span>{{ events.length }}</span>
            </summary>
            <div>
              <article v-for="event in events" :key="event.eventId">
                <time>{{ event.eventYear || "—" }}</time>
                <section>
                  <b>{{ event.eventTitle }}</b>
                  <p>{{ event.summary || event.aiDescription }}</p>
                </section>
              </article>
            </div>
          </details>
        </aside>
      </main>
      <section class="page public-gallery" aria-labelledby="gallery-title">
        <h2 id="gallery-title" class="serif">世界线插图库</h2>
        <p class="muted">查看这条世界线中已采用的插图。</p>
        <IllustrationGrid :items="illustrations" /></section
    ></template>
  </section>
</template>
<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { publicApi } from "@/api/timeline";
import { mediaOrFallback } from "@/utils/media";
import fallback from "@/assets/worldlines.svg";
import IllustrationGrid from "@/components/IllustrationGrid.vue";
const route = useRoute(),
  worldline = ref(null),
  events = ref([]),
  chapters = ref([]),
  illustrations = ref([]),
  loading = ref(true),
  error = ref("");
const coverStyle = computed(() => ({
  backgroundImage: `linear-gradient(90deg,rgba(7,13,24,.96),rgba(7,13,24,.3)),url(${JSON.stringify(mediaOrFallback(worldline.value?.coverUrl, fallback))})`,
}));
const pad = (n) => String(n).padStart(2, "0");
const excerpt = (t) =>
  (t || "本章暂无简介，点击进入阅读正文。").replace(/\s+/g, " ").slice(0, 100);
const imageCount = (c) =>
  illustrations.value.filter((i) => Number(i.chapterId) === Number(c.chapterId))
    .length;
onMounted(async () => {
  try {
    const id = route.params.id || route.params.worldlineId;
    const [w, e, c, i] = await Promise.all([
      publicApi.worldline(id),
      publicApi.timeline(id),
      publicApi.chapters(id),
      publicApi.illustrations(id),
    ]);
    worldline.value = w.data;
    events.value = e.data || [];
    chapters.value = (c.data || []).sort(
      (a, b) => (a.chapterOrder || 0) - (b.chapterOrder || 0),
    );
    illustrations.value = i.data || [];
  } catch (e) {
    error.value = e.response?.data?.msg || "请确认作品仍处于公开状态。";
  } finally {
    loading.value = false;
  }
});
</script>
<style scoped>
.public-gallery {
  padding-bottom: 64px;
}
.cover {
  background-color: #18324a;
  min-height: 530px;
  display: flex;
  align-items: center;
  background-size: cover;
  background-position: center;
}
.cover-copy {
  padding: 80px 0;
}
.eyebrow {
  color: var(--gold);
  font-size: 12px;
  letter-spacing: 0.2em;
}
.cover h1 {
  max-width: 850px;
  margin: 16px 0;
  font-size: clamp(48px, 7vw, 82px);
  line-height: 1.06;
}
.cover p {
  max-width: 650px;
  color: #d5d0c5;
  font-size: 17px;
  line-height: 1.8;
}
.meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin: 24px 0;
}
.meta span {
  padding: 6px 10px;
  border: 1px solid var(--line);
  border-radius: 20px;
  color: #c9c3b7;
  font-size: 12px;
}
.catalog-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 26px;
  align-items: start;
  padding: 58px 0;
}
.paper {
  background: #ffffff;
  color: var(--ink);
  border-radius: 8px;
  box-shadow: 0 22px 60px rgba(0, 0, 0, 0.25);
}
.catalog > header {
  padding: 38px 42px 26px;
  border-bottom: 1px solid rgba(42, 33, 24, 0.13);
}
.catalog h2 {
  margin: 8px 0;
  font-size: 40px;
}
.catalog header p {
  margin: 0;
  color: #64798a;
}
.chapter-item {
  display: grid;
  grid-template-columns: 48px 1fr 30px;
  gap: 18px;
  align-items: center;
  padding: 24px 42px;
  border-bottom: 1px solid rgba(42, 33, 24, 0.11);
  transition: 0.18s;
}
.chapter-item:hover {
  padding-left: 49px;
  background: rgba(38, 93, 229, 0.1);
}
.chapter-no {
  font: 700 20px Georgia;
  color: #265de5;
}
.chapter-item h3 {
  margin: 0 0 7px;
  font-size: 22px;
}
.chapter-item p {
  margin: 0 0 8px;
  color: #64798a;
  line-height: 1.6;
}
.chapter-item small {
  color: #265de5;
}
.arrow {
  font-size: 22px;
  color: var(--burgundy);
}
.no-chapter {
  padding: 48px 42px;
  text-align: center;
}
.no-chapter h3 {
  font-size: 26px;
}
.no-chapter p {
  color: #64798a;
  line-height: 1.8;
}
.archive-side {
  display: grid;
  gap: 16px;
  position: sticky;
  top: 90px;
}
.side-card,
.timeline-fold {
  padding: 20px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #eef3f9;
}
.side-card h3 {
  font-size: 24px;
  margin: 10px 0;
}
.side-card p {
  color: var(--muted);
  line-height: 1.75;
}
.timeline-fold summary {
  display: flex;
  justify-content: space-between;
  cursor: pointer;
  color: var(--gold);
}
.timeline-fold summary span {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(38, 93, 229, 0.12);
}
.timeline-fold article {
  display: grid;
  grid-template-columns: 46px 1fr;
  gap: 10px;
  padding: 15px 0;
  border-top: 1px solid var(--line);
}
.timeline-fold article:first-child {
  margin-top: 15px;
}
.timeline-fold time {
  color: var(--gold);
  font: 700 14px Georgia;
}
.timeline-fold b {
  font-size: 13px;
}
.timeline-fold p {
  margin: 5px 0 0;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.55;
}
.state {
  min-height: 75vh;
  display: grid;
  place-content: center;
  text-align: center;
}
.loader {
  width: 38px;
  height: 38px;
  margin: auto;
  border: 3px solid var(--line);
  border-top-color: var(--gold);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
@media (max-width: 850px) {
  .catalog-layout {
    grid-template-columns: 1fr;
  }
  .archive-side {
    position: static;
  }
  .catalog > header,
  .chapter-item {
    padding-left: 24px;
    padding-right: 24px;
  }
}
@media (max-width: 520px) {
  .chapter-item {
    grid-template-columns: 34px 1fr;
  }
  .arrow {
    display: none;
  }
  .cover {
    min-height: 450px;
  }
}
</style>
