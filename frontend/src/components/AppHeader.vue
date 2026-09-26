<template>
  <a href="#main-content" class="skip-link">跳到主要内容</a>
  <header class="site-header">
    <RouterLink class="site-brand" to="/"
      ><span class="site-mark" aria-hidden="true">⌁</span
      ><span>Chronicle<small>世界线创作室</small></span></RouterLink
    >
    <nav aria-label="主导航">
      <RouterLink to="/explore">探索作品</RouterLink
      ><RouterLink to="/creator">创作工作台</RouterLink
      ><RouterLink v-if="admin" to="/admin">内容管理</RouterLink>
    </nav>
    <div class="header-account">
      <template v-if="token"
        ><RouterLink to="/creator/profile">{{
          user.nickname || user.username || "我的账号"
        }}</RouterLink
        ><button class="text-button" :disabled="busy" @click="logout">
          退出
        </button></template
      ><RouterLink v-else class="btn" to="/login">登录 / 注册</RouterLink>
    </div>
  </header>
</template>
<script setup>
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import {
  clearSession,
  sessionToken,
  sessionUser,
  http,
  cancelReads,
} from "@/api/http";
const token = sessionToken,
  user = sessionUser,
  router = useRouter(),
  busy = ref(false);
const admin = computed(() => user.value.roles?.includes("ADMIN"));
async function logout() {
  busy.value = true;
  try {
    await http.post("/app/auth/logout");
    cancelReads();
    clearSession();
    router.push("/");
  } finally {
    busy.value = false;
  }
}
</script>
