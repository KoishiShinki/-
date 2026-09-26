<template>
  <section class="page section">
    <div class="page-heading">
      <div>
        <h1 class="serif">原创插图</h1>
        <p>选择小说章节和画面风格，直接生成该章节的插图。</p>
      </div>
      <RouterLink class="btn" :to="`/creator/worldline/${id}`"
        >返回详情</RouterLink
      >
    </div>

    <form class="paper-panel editor" @submit.prevent="generateDirect">
      <div class="form-heading">
        <div>
          <h2 class="serif form-section-title">插图生成工作台</h2>
          <p class="hint">选择章节和画面设置后，系统会直接生成图片。</p>
        </div>
        <span class="status-pill">{{
          form.chapterId ? "章节已选择" : "等待选择章节"
        }}</span>
      </div>

      <div class="control-grid">
        <label class="field fragment-select">
          <span>关联小说片段</span>
          <select class="input select" v-model.number="form.eventId" required>
            <option :value="null" disabled>
              请选择一段已生成的事件小说片段
            </option>
            <option
              v-for="event in fragmentEvents"
              :key="event.eventId"
              :value="event.eventId"
            >
              {{ event.eventYear || "未知年份" }} · {{ event.eventTitle }}
            </option>
          </select>
        </label>
        <label class="field">
          <span>小说章节</span>
          <select class="input select" v-model.number="form.chapterId">
            <option :value="null" disabled>请选择要配图的章节</option>
            <option
              v-for="chapter in chapters"
              :key="chapter.chapterId"
              :value="chapter.chapterId"
            >
              {{ chapter.chapterTitle }}
            </option>
          </select>
        </label>
        <label class="field">
          <span>插图类型</span>
          <select class="input select" v-model="form.illustrationType">
            <option
              v-for="item in typeOptions"
              :key="item.value"
              :value="item.value"
            >
              {{ item.label }}
            </option>
          </select>
        </label>
        <label class="field">
          <span>画面风格</span>
          <select class="input select" v-model="form.styleType">
            <option
              v-for="item in styleOptions"
              :key="item.value"
              :value="item.value"
            >
              {{ item.label }}
            </option>
          </select>
        </label>
      </div>

      <button
        v-if="selectedEvent"
        class="fragment-summary"
        type="button"
        @click="workspaceDialog = 'fragment'"
      >
        <span class="fragment-meta">
          <strong>{{ selectedEvent.eventTitle }}</strong>
          <small
            >{{ selectedEvent.country || "未知国家" }} ·
            {{
              selectedEvent.eventDate || `${selectedEvent.eventYear || "-"}年`
            }}</small
          >
        </span>
        <span class="summary-copy">{{
          shortText(selectedEvent.aiDescription, 84)
        }}</span>
        <span class="open-copy">查看全文 →</span>
      </button>
      <div v-else class="empty compact">
        请先到时间线为一个事件生成小说片段，再在上方选中它。<RouterLink
          :to="`/creator/worldline/${id}/timeline`"
          >打开时间线</RouterLink
        >
      </div>

      <section class="reference-box">
        <div class="reference-head">
          <div>
            <strong>主要人物来源</strong>
            <p>
              不指定时由 AI
              设计人物；选择人物档案后，该档案中的人设图会随生图请求发送。
            </p>
          </div>
          <div class="mode-switch">
            <button
              class="mode-button"
              :class="{ active: characterMode === 'ai' }"
              type="button"
              @click="setCharacterMode('ai')"
            >
              AI 自生成人物
            </button>
            <button
              class="mode-button"
              :class="{ active: characterMode === 'upload' }"
              type="button"
              @click="setCharacterMode('upload')"
            >
              使用人物档案
            </button>
          </div>
        </div>

        <div v-if="characterMode === 'upload'" class="reference-tools">
          <label class="field character-select">
            <span>本次使用的人物档案</span>
            <select class="input select" v-model.number="form.characterId">
              <option :value="null">请选择已有人物，或在右侧上传新人物</option>
              <option
                v-for="character in characters"
                :key="character.characterId"
                :value="character.characterId"
              >
                {{ character.characterName || "未命名人物"
                }}{{ character.portraitUrl ? "（有人设图）" : "（无图片）" }}
              </option>
            </select>
          </label>

          <div v-if="selectedCharacter" class="selected-character">
            <img
              v-if="selectedCharacter.portraitUrl"
              :src="mediaOrFallback(selectedCharacter.portraitUrl)"
              alt="当前人物参考图"
            />
            <span v-else class="no-portrait">无图</span>
            <div>
              <strong>{{ selectedCharacter.characterName }}</strong>
              <small>{{
                selectedCharacter.portraitUrl
                  ? "生成时将携带此参考图"
                  : "该档案没有人设图，请重新上传"
              }}</small>
            </div>
          </div>

          <div class="new-character">
            <input
              class="input"
              v-model="characterName"
              placeholder="新人物名称"
              aria-label="新人物名称"
            />
            <label class="file-picker">
              <input
                type="file"
                accept="image/png,image/jpeg,image/gif"
                @change="pickCharacterFile"
              />
              <span>{{
                characterFile ? characterFile.name : "选择新的人设图"
              }}</span>
            </label>
            <img
              v-if="characterPreview"
              class="reference-preview"
              :src="characterPreview"
              alt="待上传人物参考图"
            />
            <button
              class="btn paper"
              type="button"
              :disabled="!characterFile || busy.uploadReference"
              @click="uploadReference"
            >
              {{ busy.uploadReference ? "上传中..." : "上传并选中" }}
            </button>
          </div>
        </div>
        <p
          v-if="characterMode === 'upload' && !referenceReady"
          class="reference-warning"
        >
          请选择一份带人设图的人物档案，或先上传新的人设图。
        </p>
      </section>

      <div class="generation-bar">
        <span class="hint">可以先检查提示词，也可以直接生成章节配图。</span
        ><button
          class="btn"
          type="button"
          :disabled="busy.prompt || busy.image || !selectedEvent"
          @click="generatePrompt"
        >
          生成提示词</button
        ><button class="btn" type="button" @click="workspaceDialog = 'prompt'">
          编辑提示词 / 图片地址</button
        ><button
          class="btn primary generate-image"
          type="submit"
          :disabled="
            !form.chapterId ||
            !selectedEvent ||
            !referenceReady ||
            busy.prompt ||
            busy.image
          "
        >
          {{
            busy.prompt || busy.image ? "正在生成章节插图..." : "为本章生成插图"
          }}
        </button>
      </div>

      <p v-if="message" class="success status-message">{{ message }}</p>
      <p v-if="error" class="error status-message">{{ error }}</p>
    </form>

    <section class="gallery-section">
      <div class="guide-panel illustration-guide">
        <div>
          <h2 class="serif">候选图库</h2>
          <p>点击图片可以查看大图，满意后点击采用。采用后可以设为章节封面。</p>
        </div>
        <span>{{ illustrations.length }} 张</span>
      </div>
      <IllustrationGrid :items="illustrations">
        <template #default="{ item }">
          <div class="illustration-meta">
            <span class="badge">{{
              illustrationStatus(item.generateStatus)
            }}</span>
            <span v-if="item.chapterId">章节 {{ item.chapterId }}</span>
            <span v-if="item.eventId">事件 {{ item.eventId }}</span>
            <span v-if="item.characterId"
              >人物 {{ characterNameFor(item.characterId) }}</span
            >
          </div>
          <div class="toolbar-actions card-toolbar">
            <ActionButton
              v-if="
                item.chapterId &&
                item.imageUrl &&
                String(item.generateStatus) === '4'
              "
              label="设为章节封面"
              :action="
                () =>
                  appApi.setChapterCover({
                    chapterId: item.chapterId,
                    illustrationId: item.illustrationId,
                  })
              "
              @done="load"
            /><ActionButton
              label="删除"
              danger
              :confirm="'确定永久删除这张插图？'"
              :action="
                () => http.delete(`/app/illustration/${item.illustrationId}`)
              "
              @done="load"
            />
            <button
              class="btn"
              type="button"
              :disabled="
                !item.imageUrl || busy.adoptingId === item.illustrationId
              "
              @click="adopt(item)"
            >
              {{
                busy.adoptingId === item.illustrationId ? "采用中..." : "采用"
              }}
            </button>
            <button
              class="btn danger"
              type="button"
              :disabled="busy.discardingId === item.illustrationId"
              @click="discard(item)"
            >
              {{
                busy.discardingId === item.illustrationId ? "废弃中..." : "废弃"
              }}
            </button>
          </div>
        </template>
      </IllustrationGrid>
    </section>

    <ModalDialog
      :open="workspaceDialog === 'fragment'"
      :title="selectedEvent?.eventTitle || '小说片段'"
      eyebrow="关联小说片段"
      width="760px"
      @close="workspaceDialog = ''"
    >
      <div v-if="selectedEvent" class="fragment-dialog">
        <p class="dialog-meta">
          {{ selectedEvent.country || "未知国家" }} ·
          {{ selectedEvent.eventDate || `${selectedEvent.eventYear || "-"}年` }}
        </p>
        <p class="serif fragment-full">{{ selectedEvent.aiDescription }}</p>
      </div>
    </ModalDialog>

    <ModalDialog
      :open="workspaceDialog === 'prompt'"
      title="绘图生成内容"
      eyebrow="可在生成图片前继续修改"
      width="780px"
      @close="workspaceDialog = ''"
    >
      <div class="prompt-editor">
        <label class="field">
          <span>正向提示词</span>
          <textarea
            class="textarea prompt-area"
            v-model="form.prompt"
            placeholder="生成提示词后会出现在这里，也可以手动修改。"
          ></textarea>
        </label>
        <label class="field">
          <span>反向提示词</span>
          <textarea
            class="textarea negative-area"
            v-model="form.negativePrompt"
            placeholder="例如：game UI, screenshot frame, low quality"
          ></textarea>
        </label>
        <label class="field">
          <span>手动图片地址（可选）</span>
          <input
            class="input"
            v-model="form.imageUrl"
            placeholder="生图接口不可用时，可粘贴图片 URL 保存候选图"
          />
        </label>
        <p v-if="message" class="dialog-success">{{ message }}</p>
        <p v-if="error" class="dialog-error">{{ error }}</p>
      </div>
      <template #footer>
        <button class="btn" type="button" @click="workspaceDialog = ''">
          完成编辑
        </button>
        <button
          class="btn primary"
          type="button"
          :disabled="
            busy.image || (!form.prompt && !form.imageUrl) || !referenceReady
          "
          @click="generateImage"
        >
          {{ busy.image ? "候选图生成中..." : "生成候选图" }}
        </button>
      </template>
    </ModalDialog>
  </section>
</template>

<script setup>
import {
  computed,
  onMounted,
  onBeforeUnmount,
  reactive,
  ref,
  watch,
} from "vue";
import { useRoute } from "vue-router";
import IllustrationGrid from "@/components/IllustrationGrid.vue";
import ModalDialog from "@/components/ModalDialog.vue";
import { appApi } from "@/api/timeline";
import { mediaOrFallback } from "@/utils/media";
import ActionButton from "@/components/ActionButton.vue";
import { http } from "@/api/http";

const route = useRoute();
const id = computed(() => route.params.id);
const worldlineId = computed(() => Number(id.value));
const illustrations = ref([]);
const chapters = ref([]);
const events = ref([]);
const characters = ref([]);
const characterMode = ref("ai");
const characterName = ref("");
const characterFile = ref(null);
const characterPreview = ref("");
const workspaceDialog = ref("");
const message = ref("");
const error = ref("");
const busy = reactive({
  loading: false,
  prompt: false,
  image: false,
  uploadReference: false,
  adoptingId: null,
  discardingId: null,
});

const typeOptions = [
  { label: "章节插图", value: "chapter_scene" },
  { label: "关键事件插图", value: "event_scene" },
  { label: "人物肖像", value: "character_portrait" },
  { label: "阶段封面", value: "stage_cover" },
  { label: "报纸头版", value: "newspaper_front" },
  { label: "战争氛围图", value: "war_scene" },
  { label: "外交会议图", value: "diplomacy_scene" },
  { label: "城市风貌图", value: "city_scene" },
];
const styleOptions = [
  { label: "维多利亚时代插画风", value: "victorian_illustration" },
  { label: "写实历史风", value: "realistic_history" },
  { label: "油画风", value: "oil_painting" },
  { label: "复古档案照片风", value: "archive_old_photo" },
  { label: "黑暗史诗风", value: "dark_epic" },
  { label: "蒸汽朋克风", value: "steampunk" },
  { label: "旧报纸版画风", value: "newspaper_engraving" },
];
const form = reactive({
  worldlineId: null,
  stageId: null,
  chapterId: null,
  eventId: null,
  characterId: null,
  illustrationType: "chapter_scene",
  styleType: "victorian_illustration",
  prompt: "",
  negativePrompt: "",
  imageUrl: "",
});

const fragmentEvents = computed(() =>
  events.value.filter((event) => event.aiDescription),
);
const selectedEvent = computed(() =>
  events.value.find((event) => Number(event.eventId) === Number(form.eventId)),
);
const selectedCharacter = computed(() =>
  characters.value.find(
    (character) => Number(character.characterId) === Number(form.characterId),
  ),
);
const referenceReady = computed(
  () =>
    characterMode.value === "ai" ||
    Boolean(selectedCharacter.value?.portraitUrl),
);

watch(selectedEvent, (event) => {
  form.stageId = event?.stageId || null;
});

async function load() {
  busy.loading = true;
  try {
    const [illustrationResult, chapterResult, eventResult, characterResult] =
      await Promise.all([
        appApi.illustrations(id.value),
        appApi.chapters(id.value),
        appApi.events(id.value),
        appApi.characters(id.value),
      ]);
    // 废弃记录只保留在后台审计，用户创作工作台不再展示。
    illustrations.value = (illustrationResult.data || []).filter(
      (item) => String(item.generateStatus) !== "5",
    );
    chapters.value = chapterResult.data || [];
    events.value = eventResult.data || [];
    characters.value = characterResult.data || [];
    if (!form.eventId && fragmentEvents.value.length) {
      form.eventId = fragmentEvents.value[0].eventId;
    }
  } finally {
    busy.loading = false;
  }
}

function setCharacterMode(mode) {
  characterMode.value = mode;
  if (mode === "ai") {
    form.characterId = null;
  }
}

function pickCharacterFile(event) {
  const [file] = event.target.files;
  characterFile.value = file || null;
  if (characterPreview.value) URL.revokeObjectURL(characterPreview.value);
  characterPreview.value = file ? URL.createObjectURL(file) : "";
}

async function uploadReference() {
  clearStatus();
  if (!characterFile.value) {
    error.value = "请先选择一张人设图。";
    return;
  }
  busy.uploadReference = true;
  try {
    const data = new FormData();
    data.append("worldlineId", id.value);
    data.append("characterName", characterName.value || "主要人物参考");
    data.append("file", characterFile.value);
    const response = await appApi.uploadCharacterPortrait(data);
    form.characterId = response.data.characterId;
    message.value = "人设图已上传，并会作为这张插图的主要人物参考。";
    await load();
    characterFile.value = null;
    characterPreview.value = "";
    characterName.value = "";
  } catch (err) {
    error.value = err.message;
  } finally {
    busy.uploadReference = false;
  }
}

watch(
  () => form.chapterId,
  (chapterId) => {
    const chapter = chapters.value.find(
      (item) => Number(item.chapterId) === Number(chapterId),
    );
    const event =
      fragmentEvents.value.find(
        (item) => Number(item.stageId) === Number(chapter?.stageId),
      ) || fragmentEvents.value[0];
    form.eventId = event?.eventId || null;
    form.stageId = chapter?.stageId || event?.stageId || null;
    form.prompt = "";
    form.negativePrompt = "";
  },
);
async function generateDirect() {
  clearStatus();
  if (!form.chapterId) {
    error.value = "请先选择小说章节。";
    return;
  }
  if (!ensureFragmentSelected() || !ensureCharacterReady()) return;
  busy.prompt = true;
  message.value = "正在根据章节内容设计画面...";
  try {
    const promptResponse =
      await appApi.generateIllustrationPrompt(buildPayload());
    applyIllustration(promptResponse.data);
    busy.prompt = false;
    busy.image = true;
    message.value = "正在生成章节插图...";
    await appApi.generateIllustrationImage(buildPayload());
    message.value = "插图已生成，请在下方选择采用。";
    await load();
  } catch (err) {
    error.value = err.message;
    message.value = "";
  } finally {
    busy.prompt = false;
    busy.image = false;
  }
}
async function generatePrompt() {
  clearStatus();
  if (!ensureFragmentSelected()) return;
  if (!ensureCharacterReady()) return;
  busy.prompt = true;
  message.value = "正在生成绘图提示词，请稍候...";
  try {
    const response = await appApi.generateIllustrationPrompt(buildPayload());
    applyIllustration(response.data);
    message.value = "绘图提示词已生成，可以检查后继续生成候选图。";
    await load();
    workspaceDialog.value = "prompt";
  } catch (err) {
    error.value = err.message;
    message.value = "";
  } finally {
    busy.prompt = false;
  }
}

async function generateImage() {
  clearStatus();
  if (!ensureFragmentSelected()) return;
  if (!ensureCharacterReady()) return;
  if (!form.prompt && !form.imageUrl) {
    error.value = "请先生成或填写绘图提示词。";
    return;
  }
  busy.image = true;
  message.value = "正在生成候选图，图片模型可能需要一段时间...";
  try {
    await appApi.generateIllustrationImage(buildPayload());
    message.value = "候选图已保存到下方图库。满意后点击采用即可。";
    await load();
    workspaceDialog.value = "";
  } catch (err) {
    error.value = err.message;
    message.value = "";
  } finally {
    busy.image = false;
  }
}

async function adopt(item) {
  clearStatus();
  busy.adoptingId = item.illustrationId;
  try {
    await appApi.adoptIllustration(item.illustrationId);
    message.value = "插图已采用，公开图库会显示这张图。";
    await load();
  } catch (err) {
    error.value = err.message;
  } finally {
    busy.adoptingId = null;
  }
}

async function discard(item) {
  clearStatus();
  busy.discardingId = item.illustrationId;
  try {
    await appApi.discardIllustration(item.illustrationId);
    illustrations.value = illustrations.value.filter(
      (value) => value.illustrationId !== item.illustrationId,
    );
    message.value = "插图已从工作台移除。";
  } catch (err) {
    error.value = err.message;
  } finally {
    busy.discardingId = null;
  }
}

function buildPayload() {
  return {
    ...form,
    worldlineId: worldlineId.value,
    stageId: selectedEvent.value?.stageId || form.stageId || null,
    chapterId: form.chapterId || null,
    eventId: form.eventId || null,
    characterId:
      characterMode.value === "upload" ? form.characterId || null : null,
  };
}

function applyIllustration(illustration) {
  if (!illustration) return;
  form.stageId = illustration.stageId || form.stageId;
  form.chapterId = illustration.chapterId || form.chapterId;
  form.eventId = illustration.eventId || form.eventId;
  if (illustration.characterId) {
    form.characterId = illustration.characterId;
    characterMode.value = "upload";
  }
  form.illustrationType =
    illustration.illustrationType || form.illustrationType;
  form.styleType = illustration.styleType || form.styleType;
  form.prompt = illustration.prompt || form.prompt;
  form.negativePrompt = illustration.negativePrompt || form.negativePrompt;
}

function ensureFragmentSelected() {
  if (!selectedEvent.value) {
    error.value = "请先选择一段已经生成的小说片段。";
    return false;
  }
  return true;
}

function ensureCharacterReady() {
  if (!referenceReady.value) {
    error.value = "请选择一份带人设图的人物档案，或切换为“AI 自生成人物”。";
    return false;
  }
  return true;
}

function clearStatus() {
  message.value = "";
  error.value = "";
}

function shortText(value, length) {
  if (!value) return "暂无片段内容。";
  return value.length > length ? `${value.slice(0, length)}...` : value;
}

function illustrationStatus(value) {
  return (
    {
      0: "提示词",
      1: "生成中",
      2: "候选图",
      3: "失败",
      4: "已采用",
      5: "已废弃",
    }[value] || "候选"
  );
}

function characterNameFor(characterId) {
  return (
    characters.value.find(
      (item) => Number(item.characterId) === Number(characterId),
    )?.characterName || characterId
  );
}

onMounted(load);
onBeforeUnmount(() => {
  if (characterPreview.value) URL.revokeObjectURL(characterPreview.value);
});
</script>

<style scoped>
.editor {
  padding: 22px;
}

.form-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.form-heading .hint {
  margin-bottom: 0;
}

.status-pill {
  flex: 0 0 auto;
  padding: 6px 10px;
  border: 1px solid rgba(82, 113, 151, 0.18);
  border-radius: 999px;
  background: rgba(243, 247, 253, 0.72);
  color: #265de5;
  font-size: 12px;
  font-weight: 700;
}

.control-grid {
  display: grid;
  grid-template-columns: minmax(240px, 1.45fr) repeat(3, minmax(150px, 1fr));
  gap: 12px;
}

.control-grid .field {
  margin-bottom: 10px;
}

.fragment-summary,
.prompt-summary {
  width: 100%;
  border: 1px solid rgba(82, 113, 151, 0.16);
  border-radius: 10px;
  background: rgba(243, 247, 253, 0.62);
  color: var(--ink);
  text-align: left;
}

.fragment-summary {
  display: grid;
  grid-template-columns: minmax(150px, 0.75fr) minmax(240px, 1.5fr) auto;
  gap: 16px;
  align-items: center;
  margin-bottom: 12px;
  padding: 12px 14px;
}

.fragment-summary:hover,
.prompt-summary:hover {
  border-color: rgba(38, 93, 229, 0.72);
  background: rgba(243, 247, 253, 0.82);
}

.fragment-meta {
  display: grid;
  gap: 3px;
}

.fragment-meta small,
.summary-copy {
  color: rgba(24, 50, 74, 0.62);
  line-height: 1.55;
}

.summary-copy {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.open-copy {
  color: #265de5;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.compact {
  padding: 14px;
  margin-bottom: 12px;
}

.reference-box {
  display: grid;
  gap: 12px;
  margin: 0 0 14px;
  padding: 14px;
  border: 1px solid rgba(82, 113, 151, 0.16);
  border-radius: 10px;
  background: rgba(243, 247, 253, 0.42);
}

.reference-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.reference-head p {
  margin: 4px 0 0;
  color: rgba(24, 50, 74, 0.66);
  font-size: 13px;
  line-height: 1.55;
}

.mode-switch {
  display: flex;
  flex: 0 0 auto;
  gap: 8px;
}

.mode-button {
  min-height: 36px;
  padding: 0 12px;
  border: 1px solid rgba(82, 113, 151, 0.16);
  border-radius: 8px;
  background: rgba(243, 247, 253, 0.62);
  color: var(--ink);
  font-weight: 700;
}

.mode-button.active {
  border-color: rgba(38, 93, 229, 0.72);
  background: rgba(38, 93, 229, 0.22);
}

.reference-tools {
  display: grid;
  grid-template-columns: minmax(250px, 1.2fr) minmax(190px, 0.8fr) minmax(
      310px,
      1.4fr
    );
  gap: 12px;
  align-items: end;
}

.reference-tools .field {
  margin-bottom: 0;
}

.selected-character {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  min-height: 58px;
  padding: 6px 9px;
  border: 1px solid rgba(82, 113, 151, 0.14);
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.28);
}

.selected-character img,
.no-portrait {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  object-fit: cover;
}

.no-portrait {
  display: grid;
  place-items: center;
  background: rgba(24, 50, 74, 0.08);
  color: rgba(24, 50, 74, 0.5);
  font-size: 12px;
}

.selected-character div {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.selected-character strong,
.selected-character small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-character small {
  color: rgba(24, 50, 74, 0.58);
}

.new-character {
  display: grid;
  grid-template-columns: minmax(100px, 0.7fr) minmax(130px, 1fr) auto auto;
  gap: 8px;
  align-items: center;
}

.file-picker {
  display: flex;
  align-items: center;
  place-items: center;
  min-height: 42px;
  min-width: 0;
  padding: 0 10px;
  border: 1px dashed rgba(82, 113, 151, 0.28);
  border-radius: 9px;
  background: rgba(243, 247, 253, 0.5);
  color: #265de5;
  cursor: pointer;
}

.file-picker span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-picker input {
  display: none;
}

.reference-preview {
  width: 42px;
  height: 42px;
  object-fit: cover;
  border-radius: 8px;
}

.reference-warning {
  margin: -2px 0 0;
  color: #9b3038;
  font-size: 12px;
}

.generation-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.prompt-button {
  border-color: rgba(82, 113, 151, 0.2);
  color: var(--ink);
}

.timeline-link {
  color: rgba(24, 50, 74, 0.68);
  border-color: rgba(82, 113, 151, 0.16);
}

.generate-image {
  flex: 0 0 auto;
  border-color: rgba(82, 113, 151, 0.2);
}

.prompt-summary {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  margin-top: 12px;
  padding: 10px 12px;
  color: rgba(24, 50, 74, 0.66);
}

.prompt-summary > span:nth-child(2) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.prompt-state {
  color: #2f7655;
  font-size: 12px;
  font-weight: 700;
}

.prompt-summary strong {
  color: #265de5;
  font-size: 12px;
}

.status-message {
  margin: 12px 0 0;
}

.paper-panel .success {
  color: #2f7655;
}

.paper-panel .error {
  color: #9b3038;
}

.gallery-section {
  margin-top: 24px;
}

.illustration-guide {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.illustration-guide > span {
  flex: 0 0 auto;
  color: var(--gold);
  font-weight: 700;
}

.illustration-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin: 10px 0;
  color: var(--muted);
  font-size: 12px;
}

.card-toolbar .btn {
  min-height: 34px;
  padding: 0 12px;
  font-size: 13px;
}

.fragment-dialog {
  display: grid;
  gap: 14px;
}

.dialog-meta {
  margin: 0;
  color: var(--gold);
}

.fragment-full {
  margin: 0;
  white-space: pre-wrap;
  color: var(--text);
  font-size: 17px;
  line-height: 2;
}

.prompt-editor {
  display: grid;
  gap: 4px;
}

.prompt-editor .field span {
  color: var(--muted);
}

.dialog-success,
.dialog-error {
  margin: 0;
}

.dialog-success {
  color: #216e5b;
}

.dialog-error {
  color: #a13b48;
}

.prompt-area {
  min-height: 260px;
}

.negative-area {
  min-height: 100px;
}

@media (max-width: 1040px) {
  .control-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .fragment-select {
    grid-column: 1 / -1;
  }

  .reference-tools {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .new-character {
    grid-column: 1 / -1;
  }
}

@media (max-width: 760px) {
  .reference-head,
  .generation-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .fragment-summary {
    grid-template-columns: 1fr auto;
  }

  .summary-copy {
    display: none;
  }

  .reference-tools {
    grid-template-columns: 1fr;
  }

  .new-character {
    grid-column: auto;
    grid-template-columns: 1fr auto;
  }

  .new-character .input,
  .new-character .file-picker {
    grid-column: span 1;
  }

  .prompt-summary {
    grid-template-columns: 1fr auto;
  }

  .prompt-summary > span:nth-child(2) {
    display: none;
  }
}

@media (max-width: 560px) {
  .editor {
    padding: 16px;
  }

  .form-heading,
  .control-grid,
  .mode-switch {
    grid-template-columns: 1fr;
  }

  .form-heading,
  .reference-head,
  .mode-switch {
    display: grid;
  }

  .fragment-select {
    grid-column: auto;
  }

  .new-character {
    grid-template-columns: 1fr;
  }

  .toolbar-actions .btn,
  .generate-image {
    width: 100%;
  }
}
.hidden-source {
  display: none;
}
</style>
