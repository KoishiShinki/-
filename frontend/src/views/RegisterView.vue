<template>
  <section class="auth-page">
    <form class="auth-box paper-panel" @submit.prevent="submit">
      <p class="muted">
        用户名使用 3 至 40 位字母、数字或下划线；密码至少 12 位。
      </p>
      <h1 class="serif">注册创作者</h1>
      <label class="field"
        ><span>用户名</span
        ><input class="input" v-model="form.username" required
      /></label>
      <label class="field"
        ><span>昵称</span><input class="input" v-model="form.nickname"
      /></label>
      <label class="field"
        ><span>密码</span
        ><input
          class="input"
          v-model="form.password"
          type="password"
          minlength="12"
          required
      /></label>
      <label class="field"
        ><span>确认密码</span
        ><input
          class="input"
          v-model="form.confirmPassword"
          type="password"
          minlength="12"
          required
      /></label>
      <button class="btn primary" type="submit" :disabled="busy">
        {{ busy ? "注册中…" : "注册" }}
      </button>
      <p class="muted">已有账号？<RouterLink to="/login">去登录</RouterLink></p>
      <p v-if="message" :class="ok ? 'success' : 'error'">{{ message }}</p>
    </form>
  </section>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { authApi } from "@/api/timeline";

const router = useRouter();
const busy = ref(false);
const message = ref("");
const ok = ref(false);
const form = reactive({
  username: "",
  nickname: "",
  password: "",
  confirmPassword: "",
});

async function submit() {
  if (busy.value) return;
  busy.value = true;
  message.value = "";
  ok.value = false;
  try {
    if (form.password !== form.confirmPassword)
      throw new Error("两次输入的密码不一致。");
    if (new TextEncoder().encode(form.password).length > 72)
      throw new Error("密码的 UTF-8 长度不能超过 72 字节。");
    if (form.password.length < 12) throw new Error("密码至少需要 12 个字符。");
    if (!/^[A-Za-z0-9_]{3,40}$/.test(form.username))
      throw new Error("用户名需为 3 至 40 位字母、数字或下划线。");
    await authApi.register(form);
    ok.value = true;
    message.value = "注册成功，请登录。";
    setTimeout(() => router.push("/login"), 600);
  } catch (err) {
    message.value = err.message;
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
