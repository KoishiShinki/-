<template>
  <section class="page section">
    <div class="archive-toolbar">
      <div>
        <h1 class="serif">{{ schema.title }}</h1>
        <p class="muted">
          {{
            resource === "nation"
              ? "记录不同时期的政体、经济、外交与社会变化。"
              : "整理推动历史的人物，保持叙事中的身份与形象一致。"
          }}
        </p>
      </div>
      <button
        class="btn primary"
        @click="editing = { worldlineId: Number(id) }"
      >
        ＋ 新增{{ schema.title }}
      </button>
    </div>
    <SchemaForm
      v-if="editing"
      :fields="schema.fields"
      :initial="editing"
      :title="
        editing[schema.id] ? '编辑' + schema.title : '新增' + schema.title
      "
      :save="save"
      closable
      @close="editing = null"
      @saved="
        editing = null;
        load();
      "
    />
    <form
      v-if="resource === 'character'"
      class="paper-panel upload-simple"
      @submit.prevent="upload"
    >
      <h3>上传人物参考图</h3>
      <div class="filter-row">
        <label class="field"
          ><span>人物名称</span
          ><input
            class="input"
            v-model="name"
            maxlength="100"
            required /></label
        ><label class="field"
          ><span>肖像图片（最大 20 MB）</span
          ><input
            type="file"
            accept="image/png,image/jpeg,image/gif"
            @change="file = $event.target.files[0]"
            required
        /></label>
      </div>
      <button class="btn" :disabled="uploading">
        {{ uploading ? "上传中…" : "上传肖像" }}
      </button>
    </form>
    <p v-if="loading" role="status">正在读取档案…</p>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <div class="record-list">
      <article class="record" v-for="item in records" :key="item[schema.id]">
        <span class="record-id"
          >编号 {{ item[schema.id] }}　{{
            item.recordYear || item.country || ""
          }}</span
        >
        <h3>{{ item[schema.name] }}</h3>
        <img
          v-if="item.portraitUrl"
          class="record-image"
          :src="mediaOrFallback(item.portraitUrl)"
          :alt="item.characterName"
          loading="lazy"
        />
        <p
          v-for="field in schema.fields.filter(
            (f) => !['portraitUrl', schema.name].includes(f.key) && item[f.key],
          )"
          :key="field.key"
        >
          <b>{{ field.label }}：</b>{{ item[field.key] }}
        </p>
        <div class="record-actions">
          <button
            class="btn"
            @click="
              editing = { ...item };
              scrollTop();
            "
          >
            编辑</button
          ><ActionButton
            label="删除"
            danger
            :confirm="`确定删除“${item[schema.name]}”？`"
            :action="() => http.delete(`/app/${resource}/${item[schema.id]}`)"
            @done="load"
          />
        </div>
      </article>
    </div>
    <div v-if="!loading && !records.length" class="empty">
      还没有{{ schema.title }}，从第一条记录开始。
    </div>
  </section>
</template>
<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { http } from "@/api/http";
import { schemas } from "@/schemas";
import { mediaOrFallback } from "@/utils/media";
import SchemaForm from "@/components/SchemaForm.vue";
import ActionButton from "@/components/ActionButton.vue";
const route = useRoute(),
  id = route.params.id;
const resource = computed(() =>
    route.path.endsWith("nations") ? "nation" : "character",
  ),
  schema = computed(() => schemas[resource.value]);
const records = ref([]),
  editing = ref(null),
  loading = ref(true),
  error = ref(""),
  name = ref(""),
  file = ref(null),
  uploading = ref(false);
async function load() {
  loading.value = true;
  error.value = "";
  try {
    const r = await http.get(`/app/${resource.value}/list/${id}`);
    records.value = r.data || [];
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}
async function save(value) {
  await http[value[schema.value.id] ? "put" : "post"](
    `/app/${resource.value}`,
    { ...value, worldlineId: Number(id) },
  );
}
async function upload() {
  uploading.value = true;
  error.value = "";
  try {
    if (!file.value || file.value.size > 20 * 1024 * 1024)
      throw new Error("请选择小于 20 MB 的图片。");
    const data = new FormData();
    data.append("worldlineId", id);
    data.append("characterName", name.value);
    data.append("file", file.value);
    await http.post("/app/character/uploadPortrait", data);
    await load();
  } catch (e) {
    error.value = e.message;
  } finally {
    uploading.value = false;
  }
}
function scrollTop() {
  window.scrollTo({ top: 0, behavior: "smooth" });
}
onMounted(load);
</script>
