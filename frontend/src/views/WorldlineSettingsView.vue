<template>
  <section class="page section">
    <h1 class="serif">世界线设置</h1>
    <SchemaForm
      v-if="worldline"
      :fields="schemas.worldline.fields"
      :initial="worldline"
      title="基础设定"
      :save="save"
    />
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <form class="paper-panel upload-simple" @submit.prevent="upload">
      <h3>更新世界线封面</h3>
      <p class="muted">PNG、JPEG 或 GIF，最大 20 MB。</p>
      <input
        type="file"
        accept="image/png,image/jpeg,image/gif"
        @change="file = $event.target.files[0]"
        required
      /><button class="btn primary" :disabled="busy">
        {{ busy ? "上传中…" : "上传封面" }}
      </button>
      <p v-if="uploaded" class="success">封面已更新</p>
    </form>
    <div class="danger-zone">
      <h3>删除世界线</h3>
      <p>世界线中的事件、章节和插图会一并删除，无法撤销。</p>
      <ActionButton
        label="删除这条世界线"
        danger
        :confirm="`确定永久删除“${worldline?.worldlineName || '这条世界线'}”及全部内容？`"
        :action="remove"
      />
    </div>
  </section>
</template>
<script setup>
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { http } from "@/api/http";
import { schemas } from "@/schemas";
import SchemaForm from "@/components/SchemaForm.vue";
import ActionButton from "@/components/ActionButton.vue";
const id = useRoute().params.id,
  router = useRouter(),
  worldline = ref(null),
  error = ref(""),
  file = ref(null),
  busy = ref(false),
  uploaded = ref(false);
onMounted(async () => {
  try {
    const r = await http.get("/app/worldline/" + id);
    worldline.value = r.data;
  } catch (e) {
    error.value = e.message;
  }
});
async function save(value) {
  await http.put("/app/worldline", value);
}
async function upload() {
  busy.value = true;
  error.value = "";
  try {
    if (!file.value || file.value.size > 20 * 1024 * 1024)
      throw new Error("请选择小于 20 MB 的图片。");
    const data = new FormData();
    data.append("worldlineId", id);
    data.append("file", file.value);
    await http.post("/app/worldline/uploadCover", data);
    uploaded.value = true;
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
async function remove() {
  await http.delete("/app/worldline/" + id);
  router.push("/creator");
}
</script>
