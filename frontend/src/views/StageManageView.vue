<template>
  <section class="page section">
    <div class="page-heading">
      <div>
        <h1 class="serif">历史阶段</h1>
        <p>
          阶段用来把一连串事件整理成“章节骨架”。比如危机爆发、改革展开、战争转折、帝国崛起，都可以成为一个阶段。
        </p>
      </div>
      <RouterLink class="btn" :to="`/creator/worldline/${id}`"
        >返回详情</RouterLink
      >
    </div>

    <SchemaForm
      v-if="editing"
      :fields="schemas.stage.fields"
      :initial="editing"
      title="编辑阶段"
      :save="(value) => appApi.updateStage(value)"
      closable
      @close="editing = null"
      @saved="
        editing = null;
        load();
      "
    />
    <div v-if="binding" class="paper-panel upload-simple">
      <h3>绑定事件到《{{ binding.stageName }}》</h3>
      <p class="muted">勾选要归入该阶段的事件，未选事件保持现有阶段。</p>
      <div class="checkbox-list">
        <label v-for="event in events" :key="event.eventId"
          ><input
            type="checkbox"
            :value="event.eventId"
            v-model="selectedEvents"
          />{{ event.eventYear }}　{{ event.eventTitle }}</label
        >
      </div>
      <ActionButton
        label="保存事件绑定"
        :action="
          () =>
            appApi.bindEvents({
              stageId: binding.stageId,
              eventIds: selectedEvents,
            })
        "
        @done="
          binding = null;
          load();
        "
      /><button class="btn" @click="binding = null">关闭</button>
    </div>
    <div class="stage-workbench">
      <aside class="guide-panel">
        <h2 class="serif">推荐流程</h2>
        <ul class="step-list">
          <li>
            <b>1</b>
            <div>
              <strong>先积累事件</strong
              ><span>建议至少有 3 到 5 个事件后再划分阶段。</span>
            </div>
          </li>
          <li>
            <b>2</b>
            <div>
              <strong>用 AI 初分</strong
              ><span>AI 会按年份和关键转折点生成阶段草案。</span>
            </div>
          </li>
          <li>
            <b>3</b>
            <div>
              <strong>再手动补正</strong
              ><span>你可以新增更贴合剧情的阶段，之后用它生成小说。</span>
            </div>
          </li>
        </ul>
        <button
          class="btn primary wide"
          :disabled="splitting"
          @click="autoSplit"
        >
          {{ splitting ? "AI划分中..." : "AI自动划分阶段" }}
        </button>
        <p v-if="splitting" class="muted action-note">
          正在读取时间线并划分阶段，请稍候。
        </p>
      </aside>

      <form class="paper-panel editor" @submit.prevent="save">
        <h2 class="serif form-section-title">手动新增阶段</h2>
        <p class="hint">
          如果 AI
          划分不够贴合你的剧情，可以自己写一个阶段。阶段摘要会成为小说生成的重要上下文。
        </p>
        <div class="grid two">
          <label class="field">
            <span>阶段名称</span>
            <input
              class="input"
              v-model="form.stageName"
              required
              placeholder="例如：南方殖民地的火种"
            />
          </label>
          <label class="field">
            <span>阶段主题</span>
            <input
              class="input"
              v-model="form.stageTheme"
              placeholder="例如：殖民扩张 / 工业化 / 内战危机"
            />
          </label>
        </div>
        <div class="grid two">
          <label class="field">
            <span>起始年份</span>
            <input
              class="input"
              type="number"
              v-model.number="form.startYear"
            />
          </label>
          <label class="field">
            <span>结束年份</span>
            <input class="input" type="number" v-model.number="form.endYear" />
          </label>
        </div>
        <label class="field">
          <span>阶段摘要</span>
          <textarea
            class="textarea"
            v-model="form.stageSummary"
            placeholder="概括这个阶段的主要矛盾、关键事件和结果。"
          ></textarea>
        </label>
        <button class="btn primary" type="submit" :disabled="saving">
          {{ saving ? "保存中..." : "保存阶段" }}
        </button>
      </form>
    </div>

    <section class="section">
      <div class="toolbar">
        <div>
          <h2 class="serif">阶段列表</h2>
          <p class="muted">
            选择一个阶段生成小说章节。生成后可到“章节”页面继续编辑正文。
          </p>
        </div>
      </div>
      <div class="grid two">
        <StageCard v-for="stage in stages" :key="stage.stageId" :stage="stage">
          <div class="stage-actions record-actions">
            <button
              class="btn"
              @click="
                editing = { ...stage };
                scrollTop();
              "
            >
              编辑</button
            ><button class="btn" @click="bind(stage)">绑定事件</button
            ><ActionButton
              label="整理摘要"
              :action="
                () => http.post(`/app/stage/generateSummary/${stage.stageId}`)
              "
              @done="load"
            /><ActionButton
              label="删除"
              danger
              :confirm="`确定删除阶段“${stage.stageName}”？`"
              :action="() => appApi.deleteStage(stage.stageId)"
              @done="load"
            />
            <button
              class="btn paper"
              :disabled="generatingStageId === stage.stageId"
              @click="generateNovel(stage)"
            >
              {{
                generatingStageId === stage.stageId
                  ? "小说生成中..."
                  : "用这个阶段生成小说"
              }}
            </button>
          </div>
        </StageCard>
      </div>
      <div v-if="!stages.length" class="empty">
        暂无阶段。可以先点击“AI自动划分阶段”，或手动新增一个阶段。
      </div>
      <p v-if="message" class="success">{{ message }}</p>
      <p v-if="error" class="error">{{ error }}</p>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import StageCard from "@/components/StageCard.vue";
import { appApi } from "@/api/timeline";
import { http } from "@/api/http";
import SchemaForm from "@/components/SchemaForm.vue";
import ActionButton from "@/components/ActionButton.vue";
import { schemas } from "@/schemas";
const editing = ref(null),
  binding = ref(null),
  events = ref([]),
  selectedEvents = ref([]);
function scrollTop() {
  window.scrollTo({ top: 0, behavior: "smooth" });
}
async function bind(stage) {
  binding.value = stage;
  selectedEvents.value = [];
  const r = await appApi.events(id.value);
  events.value = r.data || [];
  scrollTop();
}

const route = useRoute();
const router = useRouter();
const id = computed(() => route.params.id);
const stages = ref([]);
const saving = ref(false);
const splitting = ref(false);
const generatingStageId = ref(null);
const message = ref("");
const error = ref("");
const form = reactive({
  worldlineId: null,
  stageName: "",
  startYear: 1836,
  endYear: 1845,
  stageTheme: "",
  stageSummary: "",
});

async function load() {
  const response = await appApi.stages(id.value);
  stages.value = response.data || [];
}

async function save() {
  clearStatus();
  saving.value = true;
  form.worldlineId = Number(id.value);
  try {
    await appApi.createStage(form);
    Object.assign(form, {
      worldlineId: null,
      stageName: "",
      startYear: form.endYear || 1836,
      endYear: (form.endYear || 1836) + 10,
      stageTheme: "",
      stageSummary: "",
    });
    message.value = "阶段已保存。";
    await load();
  } catch (err) {
    error.value = err.message;
  } finally {
    saving.value = false;
  }
}

async function autoSplit() {
  clearStatus();
  splitting.value = true;
  try {
    await appApi.autoSplitStages(id.value);
    message.value = "AI 已完成阶段划分。";
    await load();
  } catch (err) {
    error.value = err.message;
  } finally {
    splitting.value = false;
  }
}

async function generateNovel(stage) {
  clearStatus();
  generatingStageId.value = stage.stageId;
  message.value = "正在生成小说章节，完成后会自动打开章节编辑页。";
  try {
    const response = await appApi.generateStageNovel(stage.stageId);
    await load();
    const chapterId = response.data && response.data.chapterId;
    router.push({
      path: `/creator/worldline/${id.value}/chapters`,
      query: chapterId ? { chapterId } : {},
    });
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
.stage-workbench {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 20px;
}

.editor {
  padding: 20px;
}

.wide {
  width: 100%;
  margin-top: 18px;
}

.stage-actions {
  margin-top: 14px;
}

.action-note {
  margin: 12px 0 0;
  line-height: 1.6;
}

h2 {
  margin: 0;
}

@media (max-width: 980px) {
  .stage-workbench {
    grid-template-columns: 1fr;
  }
}
</style>
