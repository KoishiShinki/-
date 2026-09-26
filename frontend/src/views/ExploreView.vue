<template>
  <section class="page section">
    <div class="toolbar">
      <div>
        <h1 class="serif">公开探索</h1>
        <p class="muted">浏览创作者公开的架空历史世界线。</p>
      </div>
    </div>

    <div class="filters panel">
      <input
        class="input"
        v-model="query.worldlineName"
        aria-label="搜索世界线名称"
        placeholder="搜索世界线名称"
        @keyup.enter="load"
      />
      <input
        class="input"
        v-model="query.gameName"
        aria-label="游戏名称"
        placeholder="游戏名称"
        @keyup.enter="load"
      />
      <input
        class="input"
        v-model="query.mainCountry"
        aria-label="主控国家"
        placeholder="主控国家"
        @keyup.enter="load"
      />
      <button class="btn primary" @click="load">搜索</button>
    </div>

    <p v-if="loading" role="status">正在读取公开档案…</p>
    <p v-if="error" class="error">{{ error }}</p>
    <div class="grid three">
      <WorldlineCard
        v-for="item in worldlines"
        :key="item.worldlineId"
        :worldline="item"
      />
    </div>
    <div v-if="!loading && !error && !worldlines.length" class="empty">
      没有找到公开世界线。
    </div>
    <nav v-if="total > 12" class="pagination" aria-label="分页">
      <button
        class="btn"
        :disabled="query.pageNum <= 1 || loading"
        @click="
          query.pageNum--;
          load();
        "
      >
        上一页</button
      ><span>第 {{ query.pageNum }} 页 / 共 {{ total }} 条</span
      ><button
        class="btn"
        :disabled="query.pageNum * 12 >= total || loading"
        @click="
          query.pageNum++;
          load();
        "
      >
        下一页
      </button>
    </nav>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import WorldlineCard from "@/components/WorldlineCard.vue";
import { publicApi } from "@/api/timeline";

const worldlines = ref([]),
  loading = ref(true),
  error = ref(""),
  total = ref(0);
const query = reactive({
  pageNum: 1,
  pageSize: 12,
  worldlineName: "",
  gameName: "",
  mainCountry: "",
});

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const response = await publicApi.worldlines(query);
    worldlines.value = response.rows || response.data || [];
    total.value = response.total ?? worldlines.value.length;
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
h1 {
  margin: 0 0 8px;
  font-size: 42px;
}

.filters {
  display: grid;
  grid-template-columns: 1.4fr 1fr 1fr auto;
  gap: 12px;
  margin-bottom: 24px;
  padding: 16px;
}

@media (max-width: 900px) {
  .filters {
    grid-template-columns: 1fr;
  }
}
</style>
