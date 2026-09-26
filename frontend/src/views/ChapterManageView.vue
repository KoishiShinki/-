<template>
  <section class="page section">
    <div class="page-heading">
      <div>
        <h1 class="serif">小说章节</h1>
        <p>
          这里负责把阶段档案写成小说正文。你可以用 AI
          按阶段生成第一版，再人工修改标题、风格和正文。
        </p>
      </div>
      <RouterLink class="btn" :to="`/creator/worldline/${id}`"
        >返回详情</RouterLink
      >
    </div>

    <div class="chapter-layout">
      <aside class="panel sidebar">
        <h2 class="serif">已有章节</h2>
        <p class="muted">点击章节可继续编辑。公开后访客才能在阅读页看到。</p>
        <button
          v-for="chapter in chapters"
          :key="chapter.chapterId"
          class="chapter-tab"
          :class="{ active: chapter.chapterId === form.chapterId }"
          @click="select(chapter)"
        >
          <strong>{{ chapter.chapterTitle }}</strong>
          <span>{{ statusText(chapter.status) }}</span>
        </button>
        <button class="btn primary wide" @click="newChapter">
          新建空白章节
        </button>
      </aside>

      <form class="paper-panel editor" @submit.prevent="save">
        <h2 class="serif form-section-title">
          {{ form.chapterId ? "编辑章节正文" : "新建章节正文" }}
        </h2>
        <p class="hint">
          正文保存后仍可随时修改。若要把章节展示给访客，请保存后点击“公开章节”。
        </p>
        <p v-if="route.query.chapterId && form.chapterId" class="success">
          已打开刚生成的章节，你可以在这里检查和修改正文。
        </p>
        <label class="field">
          <span>章节标题</span>
          <input
            class="input"
            v-model="form.chapterTitle"
            required
            placeholder="例如：铁轨穿过旧王朝"
          />
        </label>
        <label class="field">
          <span>写作风格</span>
          <select class="input select" v-model="form.writingStyle">
            <option
              v-for="style in styleOptions"
              :key="style.value"
              :value="style.value"
            >
              {{ style.label }}
            </option>
          </select>
        </label>
        <div class="grid two">
          <label class="field"
            ><span>章节顺序</span
            ><input
              class="input"
              type="number"
              v-model.number="form.chapterOrder" /></label
          ><label class="field"
            ><span>所属阶段</span
            ><select class="input" v-model.number="form.stageId">
              <option :value="null">未指定</option>
              <option
                v-for="stage in stages"
                :key="stage.stageId"
                :value="stage.stageId"
              >
                {{ stage.stageName }}
              </option>
            </select></label
          >
        </div>
        <label class="field">
          <span>正文</span>
          <textarea
            class="textarea content"
            v-model="form.content"
            placeholder="在这里编辑小说正文。AI生成的内容也会填入这里。"
          ></textarea>
        </label>
        <div class="toolbar-actions">
          <button class="btn primary" type="submit" :disabled="saving">
            {{ saving ? "保存中..." : "保存章节" }}
          </button>
          <button
            class="btn paper"
            type="button"
            @click="publish"
            :disabled="!form.chapterId || publishing"
          >
            {{
              publishing
                ? "更新中..."
                : form.status === "3"
                  ? "取消公开"
                  : "公开章节"
            }}
          </button>
          <button
            class="btn paper"
            type="button"
            @click="exportMarkdown"
            :disabled="!form.chapterId || exporting"
          >
            {{ exporting ? "导出中..." : "导出Markdown" }}
          </button>
          <button
            class="btn paper"
            type="button"
            @click="exportWord"
            :disabled="!form.chapterId || exporting"
          >
            导出Word
          </button>
        </div>
        <div v-if="form.chapterId" class="record-actions">
          <ActionButton
            label="删除章节"
            danger
            :confirm="`确定删除章节“${form.chapterTitle}”？`"
            :action="() => appApi.deleteChapter(form.chapterId)"
            @done="
              newChapter();
              load();
            "
          />
        </div>
        <p v-if="message" class="success">{{ message }}</p>
        <p v-if="error" class="error">{{ error }}</p>
        <pre v-if="markdown">{{ markdown }}</pre>
      </form>

      <aside class="panel sidebar">
        <h2 class="serif">按阶段生成</h2>
        <p class="muted">
          选择一个历史阶段，AI
          会读取该阶段事件、国家档案和人物档案生成章节草稿。
        </p>
        <button
          v-for="stage in stages"
          :key="stage.stageId"
          class="chapter-tab"
          :disabled="generatingStageId === stage.stageId"
          @click="generate(stage)"
        >
          <strong>{{ stage.stageName }}</strong>
          <span>{{
            generatingStageId === stage.stageId
              ? "小说生成中..."
              : `${stage.startYear || "-"} - ${stage.endYear || "-"}`
          }}</span>
        </button>
        <div v-if="!stages.length" class="empty compact">
          暂无阶段。请先到“阶段”页面划分历史阶段。
        </div>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import { appApi } from "@/api/timeline";
import ActionButton from "@/components/ActionButton.vue";

const route = useRoute();
const id = computed(() => route.params.id);
const chapters = ref([]);
const stages = ref([]);
const markdown = ref("");
const saving = ref(false);
const publishing = ref(false);
const exporting = ref(false);
const generatingStageId = ref(null);
const message = ref("");
const error = ref("");
const styleOptions = [
  {
    label: "严肃历史小说",
    value: "严肃历史小说",
    aliases: ["history_serious"],
  },
  { label: "编年史风格", value: "编年史风格", aliases: ["chronicle"] },
  { label: "档案纪实风", value: "档案纪实风", aliases: ["archive"] },
  {
    label: "维多利亚时代报纸风",
    value: "维多利亚时代报纸风",
    aliases: ["newspaper"],
  },
  { label: "史诗战争风", value: "史诗战争风", aliases: ["epic"] },
  { label: "蒸汽朋克风", value: "蒸汽朋克风", aliases: ["steampunk"] },
  { label: "人物视角叙事", value: "人物视角叙事", aliases: ["character_view"] },
  { label: "网文爽文风", value: "网文爽文风", aliases: ["webnovel"] },
];
const form = reactive({
  chapterId: null,
  worldlineId: null,
  chapterTitle: "",
  writingStyle: "严肃历史小说",
  content: "",
  status: "0",
});

async function load() {
  const [c, s] = await Promise.all([
    appApi.chapters(id.value),
    appApi.stages(id.value),
  ]);
  chapters.value = c.data || [];
  stages.value = s.data || [];
  const targetId = form.chapterId || route.query.chapterId;
  const targetChapter = chapters.value.find(
    (chapter) => String(chapter.chapterId) === String(targetId),
  );
  if (targetChapter) {
    select(targetChapter);
  } else if (!form.chapterId && chapters.value.length) {
    select(chapters.value[0]);
  }
}

function select(chapter) {
  markdown.value = "";
  Object.keys(form).forEach((key) => delete form[key]);
  Object.assign(form, chapter);
  form.writingStyle = normalizeStyle(form.writingStyle);
}

function newChapter() {
  markdown.value = "";
  Object.keys(form).forEach((key) => delete form[key]);
  Object.assign(form, {
    chapterId: null,
    worldlineId: Number(id.value),
    chapterTitle: "",
    writingStyle: "严肃历史小说",
    content: "",
    status: "0",
    chapterOrder: chapters.value.length + 1,
    stageId: null,
    relatedEventIds: "",
    aiPrompt: "",
    coverIllustrationId: null,
    coverUrl: "",
  });
}

function normalizeStyle(value) {
  const item = styleOptions.find(
    (style) => style.value === value || style.aliases.includes(value),
  );
  return item ? item.value : value;
}

function statusText(value) {
  return { 0: "草稿", 1: "生成中", 2: "已生成", 3: "已公开" }[value] || "草稿";
}

async function save() {
  clearStatus();
  saving.value = true;
  form.worldlineId = Number(id.value);
  try {
    if (form.chapterId) {
      await appApi.updateChapter(form);
    } else {
      const created = await appApi.createChapter(form);
      if (created.data) select(created.data);
    }
    message.value = "章节已保存。";
    await load();
  } catch (err) {
    error.value = err.message;
  } finally {
    saving.value = false;
  }
}

async function publish() {
  clearStatus();
  publishing.value = true;
  try {
    await appApi.setChapterPublic({
      chapterId: form.chapterId,
      publish: form.status !== "3",
    });
    message.value =
      form.status === "3" ? "章节已取消公开。" : "章节已公开，访客可以阅读。";
    await load();
  } catch (err) {
    error.value = err.message;
  } finally {
    publishing.value = false;
  }
}

async function exportMarkdown() {
  clearStatus();
  exporting.value = true;
  try {
    const response = await appApi.exportMarkdown(form.chapterId);
    markdown.value = response.data;
    message.value = "Markdown 已导出到下方预览。";
  } catch (err) {
    error.value = err.message;
  } finally {
    exporting.value = false;
  }
}

async function exportWord() {
  if (!form.chapterId) return;
  exporting.value = true;
  try {
    const response = await appApi.exportWord(form.chapterId);
    const url = URL.createObjectURL(response.data);
    const link = document.createElement("a");
    link.href = url;
    link.download = `${form.chapterTitle || "小说章节"}.docx`;
    link.click();
    URL.revokeObjectURL(url);
  } finally {
    exporting.value = false;
  }
}

async function generate(stage) {
  clearStatus();
  generatingStageId.value = stage.stageId;
  message.value = "正在生成小说章节，请稍候...";
  try {
    const response = await appApi.generateStageNovel(stage.stageId, {
      writingStyle: form.writingStyle,
    });
    await load();
    select(response.data);
    message.value = "小说章节已生成，可以继续修改正文。";
  } catch (err) {
    error.value = err.message;
    message.value = "";
  } finally {
    generatingStageId.value = null;
  }
}

function clearStatus() {
  message.value = "";
  error.value = "";
}

onMounted(load);
</script>

<style scoped>
.chapter-layout {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) 280px;
  gap: 18px;
  align-items: start;
}

.sidebar,
.editor {
  padding: 18px;
}

.sidebar h2 {
  margin: 0 0 8px;
}

.sidebar p {
  margin: 0 0 16px;
  line-height: 1.7;
}

.chapter-tab {
  display: grid;
  gap: 5px;
  width: 100%;
  margin-bottom: 8px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: transparent;
  color: var(--text);
  text-align: left;
}

.chapter-tab:hover,
.chapter-tab.active {
  border-color: rgba(38, 93, 229, 0.58);
  background: rgba(38, 93, 229, 0.1);
}

.chapter-tab span {
  color: var(--muted);
  font-size: 13px;
}

.select {
  appearance: none;
}

.content {
  min-height: 480px;
  line-height: 1.85;
}

.wide {
  width: 100%;
}

.compact {
  padding: 18px;
}

pre {
  white-space: pre-wrap;
  margin-top: 16px;
  padding: 12px;
  background: rgba(24, 50, 74, 0.08);
  border-radius: 8px;
}

@media (max-width: 1100px) {
  .chapter-layout {
    grid-template-columns: 1fr;
  }
}
</style>
