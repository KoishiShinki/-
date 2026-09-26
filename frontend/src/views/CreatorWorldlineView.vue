<template>
  <section v-if="worldline" class="creator-worldline">
    <div
      class="hero"
      :style="{
        backgroundImage: `linear-gradient(180deg, rgba(14,23,38,0.22), rgba(14,23,38,0.96)), url(${JSON.stringify(mediaOrFallback(worldline.coverUrl, hero))})`,
      }"
    >
      <div class="page hero-inner">
        <div class="hero-copy">
          <div class="badge">
            {{ worldline.visibility === "1" ? "公开作品" : "私人草稿" }}
          </div>
          <h1 class="serif">{{ worldline.worldlineName }}</h1>
          <p>
            {{
              worldline.description ||
              "这条世界线还没有简介。可以先上传截图识别事件，再逐步整理成阶段、章节和插图。"
            }}
          </p>
          <div class="hero-actions">
            <RouterLink class="btn primary" :to="recommendedAction.to">{{
              recommendedAction.label
            }}</RouterLink>
            <RouterLink
              v-if="worldline.visibility === '1'"
              class="btn"
              :to="`/explore/worldline/${id}`"
              >预览公开作品</RouterLink
            >
            <button class="btn" @click="toggleVisibility">
              {{ worldline.visibility === "1" ? "设为私有" : "公开世界线" }}
            </button>
          </div>
        </div>
        <div class="status-board paper-panel">
          <h2 class="serif">创作进度</h2>
          <div class="status-grid">
            <div>
              <strong>{{ stat.eventCount || 0 }}</strong
              ><span>事件</span>
            </div>
            <div>
              <strong>{{ stat.stageCount || 0 }}</strong
              ><span>阶段</span>
            </div>
            <div>
              <strong>{{ stat.chapterCount || 0 }}</strong
              ><span>章节</span>
            </div>
            <div>
              <strong>{{ stat.illustrationCount || 0 }}</strong
              ><span>插图</span>
            </div>
          </div>
          <div class="progress-note">
            <b>{{ stat.divergenceScore || 0 }}</b>
            <span
              >世界线偏离度会随关键分歧事件增加，用来衡量这条历史离原时间线有多远。</span
            >
          </div>
          <div class="completion">
            <div :style="{ width: completion + '%' }"></div>
          </div>
          <small>作品完成度 {{ completion }}% · {{ nextHint }}</small>
        </div>
      </div>
    </div>

    <section class="page section">
      <div class="page-heading">
        <div>
          <h1 class="serif">从这里继续创作</h1>
          <p>
            建议按顺序推进：先上传截图生成事件，再把事件整理成阶段，最后生成小说章节和插图。每个入口都可以随时回来修改。
          </p>
        </div>
      </div>

      <div class="workflow-grid">
        <RouterLink
          v-for="item in workflow"
          :key="item.key"
          class="feature-card workflow-card"
          :to="item.to"
        >
          <div class="workflow-head">
            <span class="step-number">{{ item.step }}</span>
            <span class="badge">{{ item.status }}</span>
          </div>
          <h3 class="serif">{{ item.title }}</h3>
          <p>{{ item.description }}</p>
          <div class="meta">
            <span>{{ item.detail }}</span>
          </div>
          <span class="card-action">{{ item.action }}</span>
        </RouterLink>
      </div>
    </section>

    <section class="page section snapshot-section">
      <div class="toolbar">
        <div>
          <h2 class="serif">作品结构</h2>
          <p class="muted">
            按阶段逐层展开，查看每个阶段包含的事件、小说章节和插图。
          </p>
        </div>
        <div class="toolbar-actions">
          <RouterLink class="btn" :to="`/creator/worldline/${id}/timeline`"
            >管理事件</RouterLink
          ><RouterLink class="btn" :to="`/creator/worldline/${id}/stages`"
            >管理阶段</RouterLink
          >
        </div>
      </div>
      <div class="structure-legend">
        <span>历史阶段</span><i>→</i><span>事件素材</span><i>→</i
        ><span>小说章节</span><i>→</i><span>章节插图</span>
      </div>
      <div v-if="stages.length" class="stage-tree">
        <details
          v-for="(stage, index) in stageTree"
          :key="stage.stageId"
          class="stage-node"
          :open="index === 0"
        >
          <summary>
            <span class="node-index">{{ index + 1 }}</span>
            <div>
              <small
                >{{ stage.startYear || "?" }}—{{
                  stage.endYear || "至今"
                }}</small
              >
              <h3 class="serif">{{ stage.stageName }}</h3>
              <p>
                {{
                  stage.stageSummary || stage.stageTheme || "尚未填写阶段摘要。"
                }}
              </p>
            </div>
            <span class="node-count"
              >{{ stage.events.length }} 事件 ·
              {{ stage.chapters.length }} 章节</span
            >
          </summary>
          <div class="node-children">
            <section>
              <h4>事件素材</h4>
              <div
                v-for="event in stage.events"
                :key="event.eventId"
                class="child-row"
              >
                <b>{{ event.eventYear }}</b
                ><span>{{ event.eventTitle }}</span
                ><em v-if="event.divergenceFlag === '1'">分歧点</em>
              </div>
              <p v-if="!stage.events.length" class="node-empty">
                这个阶段还没有绑定事件
              </p>
            </section>
            <section>
              <h4>小说章节</h4>
              <div
                v-for="chapter in stage.chapters"
                :key="chapter.chapterId"
                class="chapter-row"
              >
                <div>
                  <b>{{ chapter.chapterTitle }}</b
                  ><small
                    >{{
                      chapter.status === "3" ? "已公开" : "尚未公开，读者看不到"
                    }}
                    · {{ illustrationsFor(chapter).length }} 张关联插图</small
                  >
                </div>
                <button
                  v-if="chapter.status !== '3'"
                  class="mini-action"
                  @click="publishChapter(chapter)"
                >
                  一键公开</button
                ><RouterLink
                  v-else
                  class="mini-action"
                  :to="`/read/chapter/${chapter.chapterId}`"
                  >查看成品</RouterLink
                >
              </div>
              <p v-if="!stage.chapters.length" class="node-empty">
                尚未生成章节，可前往“写章节”
              </p>
            </section>
          </div>
        </details>
      </div>
      <div v-else class="empty">
        还没有历史阶段。请先整理事件，再创建或自动划分阶段。
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import hero from "@/assets/worldlines.svg";
import { appApi } from "@/api/timeline";
import { mediaOrFallback } from "@/utils/media";

const route = useRoute();
const id = computed(() => route.params.id);
const worldline = ref(null);
const stat = ref({});
const events = ref([]);
const stages = ref([]);
const chapters = ref([]);
const illustrations = ref([]);
const stageTree = computed(() =>
  stages.value.map((stage) => ({
    ...stage,
    events: events.value.filter(
      (e) => Number(e.stageId) === Number(stage.stageId),
    ),
    chapters: chapters.value.filter(
      (c) => Number(c.stageId) === Number(stage.stageId),
    ),
  })),
);
const illustrationsFor = (chapter) =>
  illustrations.value.filter(
    (i) => Number(i.chapterId) === Number(chapter.chapterId),
  );

const recommendedAction = computed(() => {
  if (!stat.value.eventCount) {
    return {
      label: "上传第一张截图",
      to: `/creator/worldline/${id.value}/upload`,
    };
  }
  if (!stat.value.stageCount) {
    return {
      label: "划分历史阶段",
      to: `/creator/worldline/${id.value}/stages`,
    };
  }
  if (!stat.value.chapterCount) {
    return {
      label: "生成第一章小说",
      to: `/creator/worldline/${id.value}/chapters`,
    };
  }
  if (!stat.value.illustrationCount) {
    return {
      label: "生成章节插图",
      to: `/creator/worldline/${id.value}/illustrations`,
    };
  }
  return { label: "查看时间线", to: `/creator/worldline/${id.value}/timeline` };
});
const completion = computed(
  () =>
    [
      stat.value.eventCount,
      stat.value.stageCount,
      stat.value.chapterCount,
      stat.value.illustrationCount,
    ].filter(Boolean).length * 25,
);
const nextHint = computed(() =>
  completion.value === 100
    ? "已经具备完整图文阅读结构"
    : recommendedAction.value.label,
);

const workflow = computed(() => [
  {
    key: "upload",
    step: "1",
    title: "上传截图识别事件",
    description:
      "把游戏截图交给 AI 读取，生成可编辑的事件草稿。适合记录战争、法律、外交、科技、殖民等关键节点。",
    detail: `${stat.value.eventCount || 0} 个事件已入库`,
    status: stat.value.eventCount ? "已有素材" : "建议先做",
    action: "进入截图识别",
    to: `/creator/worldline/${id.value}/upload`,
  },
  {
    key: "timeline",
    step: "2",
    title: "整理时间线",
    description: "检查事件年份、类型和摘要，标记真正改变历史走向的分歧点。",
    detail: `${stat.value.divergenceScore || 0} 偏离度`,
    status: stat.value.eventCount ? "可整理" : "等待事件",
    action: "编辑事件",
    to: `/creator/worldline/${id.value}/timeline`,
  },
  {
    key: "stages",
    step: "3",
    title: "划分历史阶段",
    description: "把零散事件分成几个阶段，作为小说章节的结构骨架。",
    detail: `${stat.value.stageCount || 0} 个阶段`,
    status: stat.value.stageCount ? "已划分" : "待划分",
    action: "管理阶段",
    to: `/creator/worldline/${id.value}/stages`,
  },
  {
    key: "chapters",
    step: "4",
    title: "生成和编辑小说",
    description:
      "按阶段生成正文，再像编辑器一样修改标题、风格、内容和公开状态。",
    detail: `${stat.value.chapterCount || 0} 章小说`,
    status: stat.value.chapterCount ? "可编辑" : "待生成",
    action: "写章节",
    to: `/creator/worldline/${id.value}/chapters`,
  },
  {
    key: "illustrations",
    step: "5",
    title: "制作原创插图",
    description: "选择小说章节后直接生成配图，满意后采用并展示在正文中。",
    detail: `${stat.value.illustrationCount || 0} 张插图`,
    status: stat.value.illustrationCount ? "有图库" : "待生成",
    action: "生成插图",
    to: `/creator/worldline/${id.value}/illustrations`,
  },
  {
    key: "chat",
    step: "6",
    title: "向世界线提问",
    description: "让 AI 基于当前档案分析分歧点、剧情走向、人物视角和插图建议。",
    detail: "适合卡文或查漏补缺",
    status: "随时可用",
    action: "开始问答",
    to: `/creator/worldline/${id.value}/chat`,
  },
]);

async function load() {
  const [w, s, e, st, c, ill] = await Promise.all([
    appApi.worldline(id.value),
    appApi.stat(id.value),
    appApi.events(id.value),
    appApi.stages(id.value),
    appApi.chapters(id.value),
    appApi.illustrations(id.value),
  ]);
  worldline.value = w.data;
  stat.value = s.data || {};
  events.value = e.data || [];
  stages.value = st.data || [];
  chapters.value = c.data || [];
  illustrations.value = (ill.data || []).filter(
    (item) => String(item.generateStatus) !== "5",
  );
}
async function publishChapter(chapter) {
  await appApi.setChapterPublic({
    chapterId: chapter.chapterId,
    publish: true,
  });
  await load();
}

async function toggleVisibility() {
  await appApi.setVisibility({
    worldlineId: id.value,
    visibility: worldline.value.visibility === "1" ? "0" : "1",
  });
  load();
}

onMounted(load);
</script>

<style scoped>
.hero {
  min-height: 500px;
  display: flex;
  align-items: end;
  padding: 92px 0 44px;
  background-size: cover;
  background-position: center;
}

.hero-inner {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 26px;
  align-items: end;
}

.hero-copy h1 {
  margin: 14px 0;
  font-size: clamp(38px, 6vw, 68px);
  line-height: 1.08;
}

.hero-copy p {
  max-width: 760px;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 20px;
}

.status-board {
  padding: 20px;
}

.status-board h2 {
  margin: 0 0 16px;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.status-grid div {
  padding: 12px;
  border: 1px solid rgba(24, 50, 74, 0.12);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.35);
}

.status-grid strong {
  display: block;
  font-size: 28px;
  line-height: 1;
}

.status-grid span {
  display: block;
  margin-top: 6px;
  color: rgba(24, 50, 74, 0.62);
  font-size: 13px;
}

.progress-note {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  margin-top: 14px;
  color: rgba(24, 50, 74, 0.68);
  line-height: 1.6;
}

.progress-note b {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: var(--burgundy);
  color: #fff;
}
.completion {
  height: 7px;
  margin: 18px 0 8px;
  overflow: hidden;
  border-radius: 10px;
  background: rgba(42, 33, 24, 0.12);
}
.completion div {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--burgundy), var(--gold));
  transition: width 0.35s;
}
.status-board small {
  color: rgba(42, 33, 24, 0.62);
}

.workflow-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.workflow-card {
  min-height: 248px;
}

.workflow-card:hover {
  transform: translateY(-2px);
  border-color: rgba(38, 93, 229, 0.55);
}

.workflow-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.step-number {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(38, 93, 229, 0.18);
  color: var(--gold);
  font-family: Georgia, serif;
  font-weight: 700;
}

.card-action {
  margin-top: auto;
  color: var(--gold);
  font-weight: 700;
}

.snapshot-section {
  padding-top: 18px;
}
.structure-legend {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 18px;
  color: var(--muted);
  font-size: 13px;
}
.structure-legend span {
  padding: 7px 10px;
  border: 1px solid var(--line);
  border-radius: 20px;
}
.structure-legend i {
  color: var(--gold);
}
.stage-tree {
  display: grid;
  gap: 12px;
}
.stage-node {
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: rgba(21, 31, 50, 0.78);
}
.stage-node summary {
  display: grid;
  grid-template-columns: 44px 1fr auto;
  gap: 16px;
  align-items: center;
  padding: 20px;
  cursor: pointer;
  list-style: none;
}
.stage-node summary::-webkit-details-marker {
  display: none;
}
.stage-node summary:hover {
  background: rgba(255, 255, 255, 0.025);
}
.node-index {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border: 1px solid var(--gold);
  border-radius: 50%;
  color: var(--gold);
  font: 700 18px Georgia;
}
.stage-node h3 {
  margin: 4px 0;
  font-size: 22px;
}
.stage-node p {
  margin: 0;
  color: var(--muted);
  line-height: 1.55;
}
.stage-node small {
  color: var(--gold);
}
.node-count {
  color: var(--muted);
  font-size: 13px;
}
.node-children {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1px;
  border-top: 1px solid var(--line);
  background: var(--line);
}
.node-children section {
  padding: 20px;
  background: var(--navy-2);
}
.node-children h4 {
  margin: 0 0 12px;
  color: var(--gold);
}
.child-row,
.chapter-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-top: 1px solid rgba(38, 93, 229, 0.12);
}
.child-row b {
  color: var(--gold);
}
.child-row span {
  flex: 1;
}
.child-row em {
  color: #ef9b9b;
  font-size: 11px;
  font-style: normal;
}
.chapter-row {
  justify-content: space-between;
}
.chapter-row div {
  display: grid;
  gap: 5px;
}
.chapter-row small {
  color: var(--muted);
}
.mini-action {
  padding: 6px 10px;
  border: 1px solid var(--line);
  border-radius: 7px;
  background: transparent;
  color: var(--gold);
}
.node-empty {
  font-size: 13px;
}
@media (max-width: 760px) {
  .stage-node summary {
    grid-template-columns: 40px 1fr;
  }
  .node-count {
    grid-column: 2;
  }
  .node-children {
    grid-template-columns: 1fr;
  }
}

h2 {
  margin: 0;
}

@media (max-width: 980px) {
  .hero-inner,
  .workflow-grid {
    grid-template-columns: 1fr;
  }
}
</style>
