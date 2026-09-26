<template>
  <section class="chronicle-hero page">
    <div class="hero-copy">
      <span class="hero-note"><i />为游戏里的历史，留一份档案</span>
      <h1>那些未曾发生的历史，<br />由你继续书写。</h1>
      <p>
        将游戏截图化作事件，把关键选择连成世界线。<br />与 AI
        一起，写成有来处的故事。
      </p>
      <div class="hero-buttons">
        <RouterLink class="btn primary" to="/creator/worldline/create"
          >开始一条世界线</RouterLink
        ><RouterLink class="text-link" to="/explore"
          >探索公开作品 <span aria-hidden="true">↗</span></RouterLink
        >
      </div>
    </div>
    <div class="branch-illustration">
      <svg
        viewBox="0 0 620 410"
        role="img"
        aria-label="历史在选择之间分叉的示意图"
      >
        <defs>
          <pattern
            id="hero-grid"
            width="26"
            height="26"
            patternUnits="userSpaceOnUse"
          >
            <path
              d="M26 0H0V26"
              fill="none"
              stroke="#d7e1ed"
              stroke-width=".7"
            />
          </pattern>
        </defs>
        <rect width="620" height="410" rx="18" fill="url(#hero-grid)" />
        <g fill="none" stroke-linecap="round">
          <path
            d="M30 290H180C235 290 225 195 290 195H580"
            stroke="#265de5"
            stroke-width="4"
          />
          <path
            d="M180 290C265 290 245 338 350 338H560"
            stroke="#90a9ba"
            stroke-width="2"
            stroke-dasharray="6 7"
          />
          <path
            d="M290 195C335 195 320 85 400 85H570"
            stroke="#2b867d"
            stroke-width="3"
          />
          <path
            d="M390 195C440 195 430 250 505 250H570"
            stroke="#7f9bdd"
            stroke-width="2"
          />
        </g>
        <g fill="white" stroke="#265de5" stroke-width="3">
          <circle cx="86" cy="290" r="8" />
          <circle cx="180" cy="290" r="8" />
          <circle cx="290" cy="195" r="10" />
          <circle cx="435" cy="195" r="7" />
          <circle cx="530" cy="195" r="7" />
        </g>
        <circle
          cx="405"
          cy="85"
          r="8"
          fill="white"
          stroke="#2b867d"
          stroke-width="3"
        />
        <g fill="#18324a" font-size="14">
          <text x="55" y="325">共同起点</text>
          <text x="260" y="164">你的选择</text>
          <text x="391" y="56">另一种未来</text>
          <text x="415" y="228">新的历史</text>
          <text x="365" y="372" fill="#64798a">未曾发生的可能</text>
        </g>
        <rect x="26" y="28" width="134" height="32" rx="16" fill="white" />
        <text x="44" y="49" fill="#64798a" font-size="13">世界线分叉示意</text>
      </svg>
    </div>
  </section>
  <section class="page section home-catalogue">
    <div class="toolbar">
      <div>
        <h2>打开一条世界线</h2>
        <p class="muted">阅读其他创作者记录的选择、分歧与故事。</p>
      </div>
      <RouterLink class="text-link" to="/explore">查看全部作品</RouterLink>
    </div>
    <p v-if="loading" class="empty" role="status">正在读取公开档案…</p>
    <div v-else-if="error" class="empty">
      <p>{{ error }}</p>
      <button class="btn" @click="load">重新加载</button>
    </div>
    <div v-else-if="worldlines.length" class="grid three">
      <WorldlineCard
        v-for="item in worldlines"
        :key="item.worldlineId"
        :worldline="item"
      />
    </div>
    <div v-else class="empty">
      <span class="empty-symbol">◇</span>
      <h3>第一部作品，从你开始</h3>
      <p>公开后的世界线会出现在这里。</p>
      <RouterLink class="btn" to="/creator/worldline/create"
        >创建世界线</RouterLink
      >
    </div>
  </section>
  <section class="workflow-band">
    <div class="page">
      <h2>从一帧画面，到一部编年史。</h2>
      <ol>
        <li v-for="(step, index) in steps" :key="step">
          <span>{{ index + 1 }}</span
          >{{ step }}
        </li>
      </ol>
      <p>让 AI 协助梳理素材，由你决定故事的方向。</p>
    </div>
  </section>
</template>
<script setup>
import { onMounted, ref } from "vue";
import { publicApi } from "@/api/timeline";
import WorldlineCard from "@/components/WorldlineCard.vue";
const worldlines = ref([]),
  loading = ref(true),
  error = ref("");
const steps = ["上传游戏截图", "整理历史事件", "划分故事阶段", "书写图文小说"];
async function load() {
  loading.value = true;
  error.value = "";
  try {
    const r = await publicApi.worldlines({ pageNum: 1, pageSize: 6 });
    worldlines.value = r.rows || r.data || [];
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}
onMounted(load);
</script>
