<template>
  <section class="auth-page">
    <form class="auth-box paper-panel" @submit.prevent="submit">
      <h1 class="serif">欢迎回到创作室</h1>
      <label class="field"
        ><span>用户名</span
        ><input class="input" v-model="form.username" required
      /></label>
      <label class="field"
        ><span>密码</span
        ><input class="input" v-model="form.password" type="password" required
      /></label>
      <button class="btn primary" type="submit" :disabled="busy">
        {{ busy ? "登录中…" : "登录" }}
      </button>
      <p class="muted">
        还没有账号？<RouterLink to="/register">注册创作者</RouterLink>
      </p>
      <p v-if="error" class="error">{{ error }}</p>
    </form>
  </section>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { authApi } from "@/api/timeline";
import { setSession } from "@/api/http";

const router = useRouter();
const busy = ref(false);
const route = useRoute();
const error = ref("");
const form = reactive({ username: "", password: "" });

async function submit() {
  if (busy.value) return;
  busy.value = true;
  error.value = "";
  try {
    const response = await authApi.login(form);
    setSession(response.token, response.user);
    const target = String(route.query.redirect || "");
    router.push(
      target.startsWith("/") && !target.startsWith("//") ? target : "/creator",
    );
  } catch (err) {
    error.value = err.message;
  } finally {
    busy.value = false;
  }
}
</script>

<style scoped>
.auth-page {
  min-height: calc(100vh - 136px);
  display: grid;
  place-items: center;
  padding: 48px 16px;
}

.auth-box {
  width: min(440px, 100%);
  padding: 28px;
}

h1 {
  margin: 0 0 20px;
}

.btn {
  width: 100%;
}
</style>
