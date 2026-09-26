<template>
  <form class="schema-form paper-panel" @submit.prevent="submit">
    <div class="toolbar">
      <h2>{{ title }}</h2>
      <button v-if="closable" class="btn" type="button" @click="$emit('close')">
        关闭编辑
      </button>
    </div>
    <div class="fields">
      <label
        v-for="field in fields"
        :key="field.key"
        class="field"
        :class="{ wide: field.type === 'textarea' }"
        ><span>{{ field.label }}<b v-if="field.required"> *</b></span
        ><textarea
          v-if="field.type === 'textarea'"
          class="textarea"
          :rows="field.key === 'content' ? 18 : 4"
          v-model="value[field.key]"
          :required="field.required" /><select
          v-else-if="field.type === 'select'"
          class="input"
          v-model="value[field.key]"
          :required="field.required"
        >
          <option value="">请选择</option>
          <option
            v-for="option in field.options"
            :key="Array.isArray(option) ? option[0] : option"
            :value="Array.isArray(option) ? option[0] : option"
          >
            {{ Array.isArray(option) ? option[1] : option }}
          </option></select
        ><input
          v-else-if="field.type === 'number'"
          class="input"
          type="number"
          v-model.number="value[field.key]"
          :required="field.required" /><input
          v-else
          class="input"
          :type="field.type || 'text'"
          v-model="value[field.key]"
          :required="field.required"
      /></label>
    </div>
    <div class="actions">
      <button class="btn primary" :disabled="busy">
        {{ busy ? "正在保存…" : label }}</button
      ><span v-if="success" class="success" role="status">已保存</span>
    </div>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
  </form>
</template>
<script setup>
import { ref, watch } from "vue";
const props = defineProps({
  fields: { type: Array, required: true },
  initial: { type: Object, default: () => ({}) },
  title: String,
  label: { type: String, default: "保存" },
  save: { type: Function, required: true },
  closable: Boolean,
});
const emit = defineEmits(["saved", "close"]);
const value = ref({}),
  busy = ref(false),
  error = ref(""),
  success = ref(false);
watch(
  () => props.initial,
  (v) => {
    value.value = { ...v };
    success.value = false;
  },
  { immediate: true },
);
async function submit() {
  busy.value = true;
  error.value = "";
  success.value = false;
  try {
    await props.save({ ...value.value });
    success.value = true;
    emit("saved");
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
</script>
