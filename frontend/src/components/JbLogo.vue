<template>
  <component
    :is="clickable ? 'a' : 'div'"
    :href="clickable ? '/' : undefined"
    class="jb-logo"
    :class="[sizeClass, { clickable }]"
    @click.prevent="clickable && $emit('navigate')"
  >
    <img
      src="/logo.webp"
      alt="福庫銀行 Logo"
      class="jb-logo-img"
      draggable="false"
    />
  </component>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  size: {
    type: String,
    default: 'sm',
    validator: v => ['sm', 'md', 'lg'].includes(v),
  },
  clickable: {
    type: Boolean,
    default: false,
  },
})

defineEmits(['navigate'])

const sizeClass = computed(() => `jb-logo--${props.size}`)
</script>

<style scoped>
.jb-logo {
  display: inline-flex;
  align-items: center;
  text-decoration: none;
  color: inherit;
  flex-shrink: 0;
}

.jb-logo.clickable {
  cursor: pointer;
  transition: opacity var(--duration, 0.2s) var(--ease, ease);
}

.jb-logo.clickable:hover {
  opacity: 0.82;
}

.jb-logo-img {
  display: block;
  object-fit: contain;
}

/* sm：header 用 */
.jb-logo--sm .jb-logo-img {
  height: 80px;
  width: auto;
}

/* md：register / reset 頁頂部 */
.jb-logo--md .jb-logo-img {
  height: 120px;
  width: auto;
}

/* lg：login 左側裝飾 */
.jb-logo--lg .jb-logo-img {
  height: 240px;
  width: auto;
}

@media (max-width: 700px) {
  .jb-logo--sm .jb-logo-img { height: 64px; }
  .jb-logo--md .jb-logo-img { height: 96px; }
  .jb-logo--lg .jb-logo-img { height: 180px; }
}
</style>
