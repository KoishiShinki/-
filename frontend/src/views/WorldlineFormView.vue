<template>
  <section class="page section form-page">
    <div class="builder-shell">
      <aside class="paper-panel brief-panel">
        <p class="eyebrow">世界线档案</p>
        <h1 class="serif">创建世界线</h1>
        <p class="brief-copy">
          建立一个可持续扩写的游戏历史档案，后续可继续上传截图、生成事件、整理章节与插图。
        </p>
        <div class="brief-stats">
          <div>
            <strong>{{ form.startYear || "-" }}</strong>
            <span>起始年份</span>
          </div>
          <div>
            <strong>{{ form.currentYear || "-" }}</strong>
            <span>当前年份</span>
          </div>
        </div>
        <div class="archive-lines">
          <span>基础设定</span>
          <span>叙事风格</span>
          <span>截图与事件生成</span>
        </div>
      </aside>

      <form class="paper-panel form-panel" @submit.prevent="submit">
        <div class="form-heading">
          <div>
            <p class="eyebrow">Creator Console</p>
            <h2 class="serif">基础设定</h2>
          </div>
          <span class="status-pill">草稿</span>
        </div>

        <label class="field">
          <span>世界线名称</span>
          <input
            class="input"
            v-model="form.worldlineName"
            required
            placeholder="例如：大清工业化世界线"
          />
        </label>

        <div class="grid two">
          <label class="field">
            <span>游戏名称</span>
            <input class="input" v-model="form.gameName" />
          </label>
          <label class="field">
            <span>主控国家</span>
            <input
              class="input"
              v-model="form.mainCountry"
              placeholder="例如：大清"
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
            <span>当前年份</span>
            <input
              class="input"
              type="number"
              v-model.number="form.currentYear"
            />
          </label>
        </div>

        <label class="field">
          <span>叙事风格</span>
          <select class="input select" v-model="form.narrativeStyle">
            <option
              v-for="style in styleOptions"
              :key="style.value"
              :value="style.value"
            >
              {{ style.label }}
            </option>
          </select>
        </label>

        <div class="style-grid">
          <button
            v-for="style in styleOptions"
            :key="style.value"
            class="style-card"
            :class="{ active: form.narrativeStyle === style.value }"
            type="button"
            @click="form.narrativeStyle = style.value"
          >
            <strong>{{ style.label }}</strong>
            <span>{{ style.tip }}</span>
          </button>
        </div>

        <label class="field">
          <span>简介</span>
          <textarea
            class="textarea"
            v-model="form.description"
            placeholder="概述这条世界线的起点、核心矛盾或你想重点书写的国家命运。"
          ></textarea>
        </label>

        <div class="action-row">
          <button class="btn primary" type="submit" :disabled="busy">
            {{ busy ? "创建中…" : "创建" }}
          </button>
          <span>创建后可继续上传截图并生成事件。</span>
        </div>
        <p v-if="error" class="error">{{ error }}</p>
      </form>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { appApi } from "@/api/timeline";

const router = useRouter();
const busy = ref(false);
const error = ref("");
const styleOptions = [
  {
    label: "严肃历史小说",
    value: "严肃历史小说",
    tip: "适合宏观政治、战争与制度变迁。",
  },
  {
    label: "编年史风格",
    value: "编年史风格",
    tip: "按年份记录关键节点，信息密度更高。",
  },
  {
    label: "档案纪实风",
    value: "档案纪实风",
    tip: "偏报告、史料、档案摘要的口吻。",
  },
  {
    label: "维多利亚时代报纸风",
    value: "维多利亚时代报纸风",
    tip: "适合维多利亚 3 的新闻叙事。",
  },
  {
    label: "史诗战争风",
    value: "史诗战争风",
    tip: "突出战役、英雄与国家命运。",
  },
  {
    label: "蒸汽朋克风",
    value: "蒸汽朋克风",
    tip: "更强调工业奇观与时代氛围。",
  },
  {
    label: "人物视角叙事",
    value: "人物视角叙事",
    tip: "从君主、将军或平民视角展开。",
  },
  { label: "网文爽文风", value: "网文爽文风", tip: "节奏更强，适合轻松连载。" },
];
const form = reactive({
  worldlineName: "",
  gameName: "维多利亚3",
  mainCountry: "",
  startYear: 1836,
  currentYear: 1836,
  narrativeStyle: "严肃历史小说",
  description: "",
});

async function submit() {
  if (busy.value) return;
  busy.value = true;
  error.value = "";
  try {
    const response = await appApi.createWorldline(form);
    router.push(`/creator/worldline/${response.data.worldlineId}`);
  } catch (err) {
    error.value = err.message;
  } finally {
    busy.value = false;
  }
}
</script>

<style scoped>
.form-page {
  display: block;
}

.builder-shell {
  width: min(1120px, 100%);
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(260px, 340px) minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.brief-panel {
  position: sticky;
  top: 92px;
  overflow: hidden;
  background:
    linear-gradient(
      145deg,
      rgba(38, 93, 229, 0.18),
      rgba(255, 255, 255, 0) 42%
    ),
    var(--paper);
}

.brief-panel::after {
  content: "";
  position: absolute;
  right: -70px;
  bottom: -70px;
  width: 170px;
  height: 170px;
  border: 2px solid rgba(144, 104, 40, 0.18);
  border-radius: 50%;
}

.eyebrow {
  margin: 0 0 10px;
  color: #265de5;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

.brief-panel h1,
.form-heading h2 {
  margin: 0;
}

.brief-copy {
  margin: 18px 0 22px;
  color: var(--muted);
  line-height: 1.8;
}

.brief-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 22px;
}

.brief-stats div {
  padding: 14px;
  border: 1px solid rgba(82, 113, 151, 0.18);
  border-radius: 8px;
  background: rgba(243, 247, 253, 0.58);
}

.brief-stats strong {
  display: block;
  font-size: 26px;
  line-height: 1;
}

.brief-stats span {
  display: block;
  margin-top: 8px;
  color: var(--muted);
  font-size: 13px;
}

.archive-lines {
  display: grid;
  gap: 10px;
}

.archive-lines span {
  padding-left: 16px;
  border-left: 3px solid var(--gold);
  color: #64798a;
  line-height: 1.6;
}

.form-panel {
  min-width: 0;
  padding: 28px;
}

.form-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
}

.status-pill {
  flex: 0 0 auto;
  padding: 6px 12px;
  border: 1px solid rgba(82, 113, 151, 0.18);
  border-radius: 999px;
  background: rgba(243, 247, 253, 0.72);
  color: #265de5;
  font-size: 13px;
  font-weight: 700;
}

.select {
  appearance: none;
  background-image:
    linear-gradient(45deg, transparent 50%, #265de5 50%),
    linear-gradient(135deg, #265de5 50%, transparent 50%);
  background-position:
    calc(100% - 18px) 50%,
    calc(100% - 12px) 50%;
  background-repeat: no-repeat;
  background-size:
    6px 6px,
    6px 6px;
  padding-right: 42px;
}

.style-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 6px 0 20px;
}

.style-card {
  min-height: 92px;
  padding: 14px;
  border: 1px solid rgba(82, 113, 151, 0.16);
  border-radius: 8px;
  background: rgba(243, 247, 253, 0.62);
  color: var(--ink);
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.16s ease,
    background 0.16s ease,
    transform 0.16s ease;
}

.style-card:hover {
  transform: translateY(-1px);
  border-color: rgba(38, 93, 229, 0.55);
}

.style-card.active {
  border-color: rgba(38, 93, 229, 0.72);
  background: rgba(38, 93, 229, 0.18);
}

.style-card strong,
.style-card span {
  display: block;
}

.style-card strong {
  margin-bottom: 8px;
  font-size: 15px;
}

.style-card span {
  color: var(--muted);
  line-height: 1.55;
  font-size: 13px;
}

.action-row {
  display: flex;
  align-items: center;
  gap: 14px;
  color: var(--muted);
  font-size: 14px;
}

@media (max-width: 760px) {
  .builder-shell {
    grid-template-columns: 1fr;
  }

  .brief-panel {
    position: relative;
    top: auto;
  }

  .style-grid {
    grid-template-columns: 1fr;
  }

  .action-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
