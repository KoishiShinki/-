<template>
  <span class="inline-action"
    ><button
      type="button"
      class="btn"
      :class="{ danger }"
      :disabled="busy || disabled"
      @click="run"
    >
      {{ busy ? "处理中…" : label }}</button
    ><span v-if="error" role="alert" class="action-message error">{{
      error
    }}</span
    ><span v-if="success" role="status" class="visually-hidden"
      >操作已完成</span
    ></span
  >
</template>
<script setup>
import { ref } from "vue";
const props = defineProps({
  label: String,
  action: { type: Function, required: true },
  confirm: String,
  danger: Boolean,
  disabled: Boolean,
});
const emit = defineEmits(["done"]);
const busy = ref(false),
  error = ref(""),
  success = ref(false);
async function run() {
  if (props.confirm && !window.confirm(props.confirm)) return;
  busy.value = true;
  error.value = "";
  success.value = false;
  try {
    const value = await props.action();
    success.value = true;
    emit("done", value);
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
</script>
