<template>
  <article class="chronicle-card">
    <RouterLink
      :to="
        mode === 'creator'
          ? `/creator/worldline/${worldline.worldlineId}`
          : `/explore/worldline/${worldline.worldlineId}`
      "
      ><div class="card-cover">
        <img
          :src="mediaOrFallback(worldline.coverUrl, fallback)"
          :alt="worldline.worldlineName + '封面'"
          loading="lazy"
          referrerpolicy="no-referrer"
        /><span
          >{{ worldline.startYear || "—" }} —
          {{ worldline.currentYear || "—" }}</span
        >
      </div>
      <div class="card-copy">
        <div class="card-meta">
          <span>{{ worldline.gameName || "世界线" }}</span
          ><span>{{ worldline.mainCountry || "未指定国家" }}</span>
        </div>
        <h3>{{ worldline.worldlineName }}</h3>
        <p>{{ worldline.description || "故事正从这里开始。" }}</p>
        <div class="card-bottom">
          <span>{{ worldline.narrativeStyle || "历史档案" }}</span
          ><span
            >{{
              mode === "creator"
                ? worldline.visibility === "1"
                  ? "公开作品"
                  : "私人档案"
                : "阅读世界线"
            }}
            <b aria-hidden="true">↗</b></span
          >
        </div>
      </div></RouterLink
    >
  </article>
</template>
<script setup>
import { mediaOrFallback } from "@/utils/media";
import fallback from "@/assets/worldlines.svg";
defineProps({
  worldline: { type: Object, required: true },
  mode: { type: String, default: "public" },
});
</script>
