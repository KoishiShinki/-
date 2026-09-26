<template>
  <section class="page section">
    <p v-if="error" class="error">{{ error }}</p>
    <template v-if="creator"
      ><h1 class="serif">
        {{ creator.profile?.nickname || creator.nickname || "创作者" }}
      </h1>
      <p class="muted">
        {{ creator.profile?.creatorTitle || creator.creatorTitle }}
      </p>
      <p>{{ creator.profile?.bio || creator.bio }}</p>
      <h2>
        公开作品
        <small class="muted"
          >{{
            creator.publicWorldlineCount ?? creator.worldlines?.length ?? 0
          }}
          部</small
        >
      </h2>
      <div class="grid three">
        <WorldlineCard
          v-for="item in creator.worldlines || []"
          :key="item.worldlineId"
          :worldline="item"
        />
      </div>
      <div
        v-if="Array.isArray(creator.worldlines) && !creator.worldlines.length"
        class="empty"
      >
        这位创作者还没有公开作品。
      </div></template
    >
  </section>
</template>
<script setup>
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { publicApi } from "@/api/timeline";
import WorldlineCard from "@/components/WorldlineCard.vue";
const id = useRoute().params.id,
  creator = ref(null),
  error = ref("");
onMounted(async () => {
  try {
    const r = await publicApi.creator(id);
    creator.value = r.data;
  } catch (e) {
    error.value = e.message;
  }
});
</script>
