<template>
  <section class="page section">
    <div class="page-heading">
      <div>
        <h1 class="serif">截图识别</h1>
        <p>
          先上传游戏截图，AI
          会提取其中可能发生的历史事件。识别结果只是草稿，确认前不会写入正式时间线。
        </p>
      </div>
      <RouterLink class="btn" :to="`/creator/worldline/${id}`"
        >返回详情</RouterLink
      >
    </div>

    <div class="grid three guide-row">
      <div class="guide-panel">
        <h3 class="serif">1. 上传截图</h3>
        <p>选择游戏画面，推荐使用包含日期、国家面板、战争或事件提示的截图。</p>
      </div>
      <div class="guide-panel">
        <h3 class="serif">2. AI识别草稿</h3>
        <p>系统会生成事件标题、年份、国家、类型、摘要和影响分析。</p>
      </div>
      <div class="guide-panel">
        <h3 class="serif">3. 确认为事件</h3>
        <p>确认后草稿会进入正式时间线，后续可用于阶段划分和小说生成。</p>
      </div>
    </div>

    <div class="upload-layout">
      <div class="panel uploader">
        <div>
          <h2 class="serif">上传新截图</h2>
          <p class="muted">
            上传后会先保存为截图记录，你可以马上识别，也可以稍后从历史截图中继续。
          </p>
        </div>
        <label class="file-picker">
          <input
            type="file"
            accept="image/png,image/jpeg,image/gif"
            @change="pickFile"
          />
          <span>{{ file ? file.name : "选择一张游戏截图" }}</span>
        </label>
        <img v-if="preview" :src="preview" alt="preview" />
        <button
          class="btn primary"
          :disabled="!file || uploading"
          @click="upload"
        >
          {{ uploading ? "上传中..." : "上传并设为当前截图" }}
        </button>
      </div>
      <div class="paper-panel result">
        <h2 class="serif">识别结果</h2>
        <div v-if="current" class="current-shot">
          <p>截图：{{ current.originalName }}</p>
          <button
            class="btn primary"
            @click="recognize"
            :disabled="recognizing"
          >
            {{ recognizing ? "AI识别中，请稍候" : "AI识别事件" }}
          </button>
          <p v-if="recognizing" class="muted">
            大图识别通常需要 1 到 3 分钟，请不要关闭页面。
          </p>
        </div>
        <div v-else class="empty soft">
          请先上传截图，或在下方历史截图里选择一张继续识别。
        </div>
        <pre v-if="draft">{{ draft }}</pre>
        <div v-if="draft" class="confirm-box">
          <p>请检查草稿内容是否符合截图。确认后它会成为时间线事件。</p>
          <div class="toolbar-actions">
            <button
              class="btn primary"
              :disabled="confirming || rejecting"
              @click="confirm"
            >
              {{ confirming ? "确认中..." : "确认为正式事件" }}
            </button>
            <button
              class="btn danger"
              :disabled="confirming || rejecting"
              @click="reject"
            >
              {{ rejecting ? "移除中..." : "废弃此草稿" }}
            </button>
          </div>
        </div>
        <div v-if="current" class="record-actions">
          <ActionButton
            label="删除当前截图"
            danger
            confirm="确定永久删除当前截图？"
            :action="
              () => http.delete(`/app/screenshot/${current.screenshotId}`)
            "
            @done="
              current = null;
              draft = '';
              load();
            "
          />
        </div>
        <p v-if="message" class="success">{{ message }}</p>
        <p v-if="error" class="error">{{ error }}</p>
      </div>
    </div>

    <section class="section">
      <h2 class="serif">历史截图</h2>
      <div class="grid three">
        <article
          v-for="item in screenshots"
          :key="item.screenshotId"
          class="panel screenshot-item"
          @click="selectScreenshot(item)"
        >
          <img :src="mediaOrFallback(item.imageUrl)" alt="" />
          <div class="shot-meta">
            <span class="badge">{{ statusText(item.aiStatus) }}</span>
            <strong>{{ item.originalName || "未命名截图" }}</strong>
          </div>
          <button class="btn paper" type="button">选择</button>
        </article>
      </div>
      <div v-if="!screenshots.length" class="empty">
        暂无历史截图。上传第一张截图后，它会出现在这里。
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, ref } from "vue";
import { useRoute } from "vue-router";
import { appApi } from "@/api/timeline";
import { http } from "@/api/http";
import { mediaOrFallback } from "@/utils/media";
import ActionButton from "@/components/ActionButton.vue";

const route = useRoute();
const id = computed(() => route.params.id);
const file = ref(null);
const preview = ref("");
const current = ref(null);
const draft = ref("");
const screenshots = ref([]);
const rejecting = ref(false);
const recognizing = ref(false);
const uploading = ref(false);
const confirming = ref(false);
const message = ref("");
const error = ref("");

function pickFile(event) {
  file.value = event.target.files[0];
  if (preview.value) URL.revokeObjectURL(preview.value);
  preview.value = file.value ? URL.createObjectURL(file.value) : "";
}

async function load() {
  const response = await appApi.screenshots(id.value);
  // 已驳回的草稿仅在后台保留，用户工作台不再展示。
  screenshots.value = (response.data || []).filter(
    (item) => String(item.aiStatus) !== "4",
  );
}

async function upload() {
  error.value = "";
  message.value = "";
  uploading.value = true;
  const data = new FormData();
  data.append("worldlineId", id.value);
  data.append("file", file.value);
  try {
    const response = await appApi.uploadScreenshot(data);
    current.value = response.data;
    message.value = "截图已上传，可以开始 AI 识别。";
    await load();
  } catch (err) {
    error.value = err.message;
  } finally {
    uploading.value = false;
  }
}

function selectScreenshot(item) {
  current.value = item;
  error.value = "";
  message.value = "";
  draft.value = formatDraft(item.draftEventJson);
}

function formatDraft(value) {
  if (!value) {
    return "";
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch (err) {
    return value;
  }
}

async function recognize() {
  message.value = "";
  error.value = "";
  recognizing.value = true;
  try {
    const response = await appApi.recognizeScreenshot(
      current.value.screenshotId,
    );
    draft.value = JSON.stringify(response.data.draft, null, 2);
    message.value = "AI 已生成事件草稿，请检查后确认。";
  } catch (err) {
    error.value = err.message;
  } finally {
    recognizing.value = false;
  }
}

async function confirm() {
  error.value = "";
  confirming.value = true;
  try {
    await appApi.confirmDraft(current.value.screenshotId);
    message.value = "事件已写入时间线。";
    draft.value = "";
    await load();
  } catch (err) {
    error.value = err.message;
  } finally {
    confirming.value = false;
  }
}

async function reject() {
  if (!current.value) return;
  error.value = "";
  rejecting.value = true;
  try {
    await appApi.rejectDraft(current.value.screenshotId);
    screenshots.value = screenshots.value.filter(
      (item) => item.screenshotId !== current.value.screenshotId,
    );
    current.value = null;
    draft.value = "";
    preview.value = "";
    message.value = "草稿已从工作台移除。";
  } catch (err) {
    error.value = err.message;
  } finally {
    rejecting.value = false;
  }
}

function statusText(value) {
  return (
    {
      0: "待识别",
      1: "识别中",
      2: "待审核",
      3: "已确认",
      4: "已驳回",
      5: "失败",
    }[value] || value
  );
}

onMounted(load);
onBeforeUnmount(() => {
  if (preview.value) URL.revokeObjectURL(preview.value);
});
</script>

<style scoped>
h1 {
  margin: 0 0 8px;
  font-size: 42px;
}

.upload-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.8fr);
  gap: 20px;
}

.guide-row {
  margin-bottom: 20px;
}

.uploader,
.result {
  display: grid;
  gap: 14px;
  padding: 20px;
}

.uploader h2,
.result h2 {
  margin: 0 0 8px;
}

.uploader p {
  margin: 0;
  line-height: 1.7;
}

.file-picker {
  display: grid;
  place-items: center;
  min-height: 74px;
  border: 1px dashed var(--line);
  border-radius: 12px;
  color: var(--gold);
  background: rgba(38, 93, 229, 0.08);
  cursor: pointer;
}

.file-picker input {
  display: none;
}

img {
  max-width: 100%;
  border-radius: 12px;
}

pre {
  white-space: pre-wrap;
  overflow: auto;
}

.screenshot-item {
  display: grid;
  gap: 10px;
  padding: 12px;
  cursor: pointer;
  transition: 0.18s ease;
}

.screenshot-item:hover {
  transform: translateY(-2px);
  border-color: rgba(38, 93, 229, 0.55);
}

.shot-meta {
  display: grid;
  gap: 8px;
}

.shot-meta strong {
  font-size: 14px;
  line-height: 1.5;
}

.current-shot p,
.confirm-box p {
  margin: 0 0 12px;
  line-height: 1.7;
}

.empty.soft {
  padding: 18px;
}

@media (max-width: 900px) {
  .upload-layout {
    grid-template-columns: 1fr;
  }
}
</style>
