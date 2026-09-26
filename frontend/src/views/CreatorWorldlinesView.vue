<template>
  <section class="page section">
    <div class="toolbar">
      <div>
        <h1 class="serif">我的世界线</h1>
        <p class="muted">管理所有创作项目。</p>
      </div>
      <RouterLink class="btn primary" to="/creator/worldline/create"
        >创建</RouterLink
      >
    </div>
    <div class="grid three">
      <WorldlineCard
        v-for="item in worldlines"
        :key="item.worldlineId"
        :worldline="item"
        mode="creator"
      />
    </div>
    <div v-if="!worldlines.length" class="empty">还没有世界线。</div>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import WorldlineCard from "@/components/WorldlineCard.vue";
import { appApi } from "@/api/timeline";

const worldlines = ref([]);
onMounted(async () => {
  const response = await appApi.myWorldlines();
  worldlines.value = response.data || [];
});
</script>

<style scoped>
h1 {
  margin: 0 0 8px;
  font-size: 42px;
}
</style>
