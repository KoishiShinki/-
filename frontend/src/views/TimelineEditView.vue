<template>
  <section class="page section">
    <div class="page-heading">
      <div>
        <h1 class="serif">时间线编辑</h1>
        <p>
          这里用来整理正式历史事件。事件越清楚，后续 AI
          划分阶段、生成小说和插图时越不容易跑偏。
        </p>
      </div>
      <RouterLink class="btn" :to="`/creator/worldline/${id}`"
        >返回详情</RouterLink
      >
    </div>

    <SchemaForm
      v-if="editing"
      :fields="schemas.event.fields"
      :initial="editing"
      title="编辑事件"
      :save="(value) => appApi.updateEvent(value)"
      closable
      @close="editing = null"
      @saved="
        editing = null;
        load();
      "
    />
    <div class="timeline-layout">
      <form class="paper-panel editor" @submit.prevent="save">
        <h2 class="serif form-section-title">新增历史事件</h2>
        <p class="hint">
          可以手动补充关键节点，也可以先去“截图识别”让 AI
          生成草稿。建议每个事件都写清楚时间、国家和影响。
        </p>
        <div class="grid two">
          <label class="field">
            <span>事件标题</span>
            <input
              class="input"
              v-model="form.eventTitle"
              required
              placeholder="例如：建立殖民政府"
            />
          </label>
          <label class="field">
            <span>年份</span>
            <input
              class="input"
              type="number"
              v-model.number="form.eventYear"
            />
          </label>
        </div>
        <div class="grid two">
          <label class="field">
            <span>事件日期</span>
            <input
              class="input"
              v-model="form.eventDate"
              placeholder="例如：1932年6月22日"
            />
          </label>
          <label class="field">
            <span>国家</span>
            <input
              class="input"
              v-model="form.country"
              placeholder="例如：安第斯"
            />
          </label>
        </div>
        <div class="grid two">
          <label class="field">
            <span>事件类型</span>
            <select class="input select" v-model="form.eventType">
              <option
                v-for="item in eventTypes"
                :key="item.value"
                :value="item.value"
              >
                {{ item.label }}
              </option>
            </select>
          </label>
          <label class="field">
            <span>重要度</span>
            <select class="input select" v-model="form.importanceLevel">
              <option
                v-for="item in importanceLevels"
                :key="item.value"
                :value="item.value"
              >
                {{ item.label }}
              </option>
            </select>
          </label>
        </div>
        <label class="field">
          <span>相关势力</span>
          <input
            class="input"
            v-model="form.relatedForces"
            placeholder="多个势力用逗号分隔，例如：英国, 法国, 殖民地叛军"
          />
        </label>
        <label class="field">
          <span>摘要</span>
          <textarea
            class="textarea"
            v-model="form.summary"
            placeholder="写清楚发生了什么、为什么重要、对后续局势有什么影响。"
          ></textarea>
        </label>
        <button class="btn primary" type="submit" :disabled="saving">
          {{ saving ? "保存中..." : "保存为时间线事件" }}
        </button>
      </form>

      <aside class="guide-panel side-guide">
        <h2 class="serif">怎么判断一个事件值得记录？</h2>
        <ul class="step-list">
          <li>
            <b>1</b>
            <div>
              <strong>改变局势</strong
              ><span>战争、法律、经济危机、政权更替都适合记录。</span>
            </div>
          </li>
          <li>
            <b>2</b>
            <div>
              <strong>能引出故事</strong
              ><span>事件里有人物、矛盾或选择，就更适合生成小说。</span>
            </div>
          </li>
          <li>
            <b>3</b>
            <div>
              <strong>标记分歧</strong
              ><span>如果它让历史走向明显不同，保存后点“标记为分歧点”。</span>
            </div>
          </li>
        </ul>
      </aside>
    </div>

    <section class="section">
      <div class="toolbar">
        <div>
          <h2 class="serif">事件列表</h2>
          <p class="muted">
            按年份从早到晚排列。每个事件都可以生成一段小说片段。
          </p>
        </div>
      </div>
      <TimelineNode v-for="event in events" :key="event.eventId" :event="event">
        <div class="node-actions">
          <button
            class="btn"
            @click="
              editing = { ...event };
              scrollTop();
            "
          >
            编辑事件</button
          ><ActionButton
            label="删除"
            danger
            :confirm="`确定删除事件“${event.eventTitle}”？`"
            :action="() => appApi.deleteEvent(event.eventId)"
            @done="load"
          />
          <button
            class="btn"
            :disabled="generatingEventId === event.eventId"
            @click="fragment(event)"
          >
            {{
              generatingEventId === event.eventId
                ? "生成中..."
                : event.aiDescription
                  ? "重新生成小说片段"
                  : "生成小说片段"
            }}
          </button>
          <button
            class="btn danger"
            :disabled="markingEventId === event.eventId"
            @click="mark(event)"
          >
            {{
              markingEventId === event.eventId ? "标记中..." : "标记为分歧点"
            }}
          </button>
        </div>
      </TimelineNode>
      <div v-if="!events.length" class="empty">
        暂无事件。建议先上传截图识别，或手动新增一个关键历史节点。
      </div>
      <p v-if="message" class="success">{{ message }}</p>
      <p v-if="error" class="error">{{ error }}</p>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import TimelineNode from "@/components/TimelineNode.vue";
import { appApi } from "@/api/timeline";
import SchemaForm from "@/components/SchemaForm.vue";
import ActionButton from "@/components/ActionButton.vue";
import { schemas } from "@/schemas";
const editing = ref(null);
function scrollTop() {
  window.scrollTo({ top: 0, behavior: "smooth" });
}

const route = useRoute();
const id = computed(() => route.params.id);
const events = ref([]);
const saving = ref(false);
const generatingEventId = ref(null);
const markingEventId = ref(null);
const message = ref("");
const error = ref("");
const eventTypes = [
  { label: "战争事件", value: "war" },
  { label: "外交事件", value: "diplomacy" },
  { label: "法律改革", value: "law" },
  { label: "经济危机", value: "economy" },
  { label: "革命叛乱", value: "revolution" },
  { label: "科技突破", value: "technology" },
  { label: "殖民扩张", value: "colony" },
  { label: "政权更替", value: "government" },
  { label: "重要人物", value: "character" },
  { label: "世界线分歧点", value: "divergence" },
  { label: "其他事件", value: "other" },
];
const importanceLevels = [
  { label: "普通：背景变化", value: "1" },
  { label: "重要：影响后续发展", value: "2" },
  { label: "转折点：改变阶段走向", value: "3" },
  { label: "世界线分歧点：明显偏离历史", value: "4" },
];
const form = reactive({
  worldlineId: null,
  eventTitle: "",
  eventYear: 1836,
  eventDate: "",
  eventType: "other",
  importanceLevel: "1",
  country: "",
  relatedForces: "",
  summary: "",
});

async function load() {
  const response = await appApi.events(id.value);
  events.value = response.data || [];
}

async function save() {
  message.value = "";
  error.value = "";
  saving.value = true;
  form.worldlineId = Number(id.value);
  try {
    await appApi.createEvent(form);
    Object.assign(form, {
      worldlineId: null,
      eventTitle: "",
      eventYear: form.eventYear,
      eventDate: "",
      eventType: "other",
      importanceLevel: "1",
      country: "",
      relatedForces: "",
      summary: "",
    });
    message.value = "事件已保存到时间线。";
    await load();
  } catch (err) {
    error.value = err.message;
  } finally {
    saving.value = false;
  }
}

async function mark(event) {
  message.value = "";
  error.value = "";
  markingEventId.value = event.eventId;
  try {
    await appApi.markDivergence(event.eventId);
    message.value = "已标记为分歧点。";
    await load();
  } catch (err) {
    error.value = err.message;
  } finally {
    markingEventId.value = null;
  }
}

async function fragment(event) {
  message.value = "";
  error.value = "";
  generatingEventId.value = event.eventId;
  try {
    await appApi.generateFragment(event.eventId);
    await load();
    message.value = "小说片段已生成，并显示在对应事件卡片中。";
  } catch (err) {
    error.value = err.message;
  } finally {
    generatingEventId.value = null;
  }
}

onMounted(load);
</script>

<style scoped>
.timeline-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
}

.editor {
  padding: 20px;
}

.select {
  appearance: none;
}

.side-guide {
  align-self: start;
  position: sticky;
  top: 88px;
}

.node-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 14px;
}

h2 {
  margin: 0;
}

@media (max-width: 980px) {
  .timeline-layout {
    grid-template-columns: 1fr;
  }

  .side-guide {
    position: relative;
    top: auto;
  }
}
</style>
