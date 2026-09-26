<template>
  <section class="page section form-page">
    <form class="paper-panel form-panel" @submit.prevent="submit">
      <h1 class="serif">个人资料</h1>
      <label class="field"
        ><span>昵称</span><input class="input" v-model="profile.nickname"
      /></label>
      <label class="field"
        ><span>称号</span><input class="input" v-model="profile.creatorTitle"
      /></label>
      <label class="field"
        ><span>头像地址</span><input class="input" v-model="profile.avatarUrl"
      /></label>
      <label class="field"
        ><span>主页封面</span
        ><input class="input" v-model="profile.homepageCoverUrl"
      /></label>
      <label class="field"
        ><span>简介</span
        ><textarea class="textarea" v-model="profile.bio"></textarea>
      </label>
      <button class="btn primary" type="submit">保存</button>
      <p v-if="message" class="success">{{ message }}</p>
    </form>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { authApi } from "@/api/timeline";

const message = ref("");
const profile = reactive({
  nickname: "",
  creatorTitle: "",
  avatarUrl: "",
  homepageCoverUrl: "",
  bio: "",
});

async function load() {
  const response = await authApi.profile();
  Object.assign(profile, response.profile || response.data?.profile || {});
}

async function submit() {
  await authApi.updateProfile(profile);
  message.value = "已保存";
}

onMounted(load);
</script>

<style scoped>
.form-page {
  display: grid;
  place-items: start center;
}

.form-panel {
  width: min(720px, 100%);
  padding: 28px;
}

h1 {
  margin: 0 0 20px;
}
</style>
