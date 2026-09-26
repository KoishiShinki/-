<template>
  <AppHeader />
  <div
    v-if="pendingRequests"
    class="request-progress"
    role="status"
    aria-label="正在处理请求"
  />
  <div v-if="requestError" class="global-notice" role="alert">
    <span>{{ requestError }}</span
    ><button @click="requestError = ''" aria-label="关闭错误提示">×</button>
  </div>
  <WorldlineNav />
  <main id="main-content"><RouterView :key="$route.path" /></main>
  <AppFooter />
</template>
<script setup>
import AppHeader from "@/components/AppHeader.vue";
import AppFooter from "@/components/AppFooter.vue";
import WorldlineNav from "@/components/WorldlineNav.vue";
import { pendingRequests, requestError, sessionToken } from "@/api/http";
import { watch } from "vue";
import { useRouter, useRoute } from "vue-router";
const router = useRouter(),
  route = useRoute();
watch(sessionToken, (value) => {
  if (!value && route.meta.auth)
    router.replace({ path: "/login", query: { redirect: route.fullPath } });
});
</script>
