<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="open" class="modal-layer" @mousedown.self="$emit('close')">
        <section
          class="modal-panel"
          ref="panel"
          tabindex="-1"
          :style="{ '--modal-width': width }"
          role="dialog"
          aria-modal="true"
          :aria-label="title"
          @mousedown.stop
        >
          <header class="modal-header">
            <div>
              <p v-if="eyebrow" class="modal-eyebrow">{{ eyebrow }}</p>
              <h2 class="serif">{{ title }}</h2>
            </div>
            <button
              class="modal-close"
              type="button"
              aria-label="关闭浮窗"
              @click="$emit('close')"
            >
              ×
            </button>
          </header>
          <div class="modal-body">
            <slot />
          </div>
          <footer v-if="$slots.footer" class="modal-footer">
            <slot name="footer" />
          </footer>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { onBeforeUnmount, nextTick, ref, watch } from "vue";

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: "详情" },
  eyebrow: { type: String, default: "" },
  width: { type: String, default: "820px" },
});

const emit = defineEmits(["close"]);
const panel = ref(null);
let previousFocus = null;

function handleKeydown(event) {
  if (event.key === "Escape" && props.open) emit("close");
  if (event.key === "Tab" && props.open) {
    const nodes = [
      ...panel.value.querySelectorAll(
        'button:not(:disabled),a[href],input:not(:disabled),select:not(:disabled),textarea:not(:disabled),[tabindex="0"]',
      ),
    ];
    const first = nodes[0],
      last = nodes[nodes.length - 1];
    if (!first) {
      event.preventDefault();
      panel.value.focus();
    } else if (
      event.shiftKey &&
      (document.activeElement === first ||
        document.activeElement === panel.value)
    ) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  }
}

watch(
  () => props.open,
  (value) => {
    document.body.style.overflow = value ? "hidden" : "";
    if (value) {
      previousFocus = document.activeElement;
      window.addEventListener("keydown", handleKeydown);
      nextTick(() => panel.value?.focus());
    } else {
      window.removeEventListener("keydown", handleKeydown);
      previousFocus?.focus?.();
    }
  },
);

onBeforeUnmount(() => {
  document.body.style.overflow = "";
  window.removeEventListener("keydown", handleKeydown);
});
</script>

<style scoped>
.modal-layer {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(4, 9, 18, 0.78);
  backdrop-filter: blur(5px);
}

.modal-panel {
  width: min(var(--modal-width), 100%);
  max-height: min(88vh, 900px);
  overflow: hidden;
  border: 1px solid rgba(38, 93, 229, 0.34);
  border-radius: 16px;
  background: #ffffff;
  color: var(--text);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.46);
}

.modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 20px 22px 16px;
  border-bottom: 1px solid var(--line);
}

.modal-header h2 {
  margin: 0;
  font-size: clamp(22px, 3vw, 30px);
}

.modal-eyebrow {
  margin: 0 0 5px;
  color: var(--gold);
  font-size: 12px;
  letter-spacing: 0.08em;
}

.modal-close {
  flex: 0 0 auto;
  width: 38px;
  height: 38px;
  padding: 0;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: transparent;
  color: var(--text);
  font-size: 25px;
  line-height: 1;
}

.modal-close:hover {
  border-color: rgba(38, 93, 229, 0.7);
  color: var(--gold);
}

.modal-body {
  max-height: calc(88vh - 150px);
  overflow: auto;
  padding: 22px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 22px;
  border-top: 1px solid var(--line);
  background: #edf3fb;
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.18s ease;
}

.modal-fade-enter-active .modal-panel,
.modal-fade-leave-active .modal-panel {
  transition: transform 0.18s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-from .modal-panel,
.modal-fade-leave-to .modal-panel {
  transform: translateY(10px) scale(0.985);
}

@media (max-width: 640px) {
  .modal-layer {
    align-items: end;
    padding: 0;
  }

  .modal-panel {
    width: 100%;
    max-height: 92vh;
    border-radius: 16px 16px 0 0;
  }

  .modal-body {
    max-height: calc(92vh - 145px);
    padding: 18px;
  }
}
</style>
