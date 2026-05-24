<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const showButton = ref(false)

let scrollTarget = window

function checkScroll(e) {
  scrollTarget = e.target.nodeType === 9 ? window : e.target
  const scrollTop = e.target.scrollTop || window.scrollY || document.documentElement.scrollTop || 0
  showButton.value = scrollTop > 150
}

function scrollToTop() {
  if (scrollTarget && typeof scrollTarget.scrollTo === 'function') {
    scrollTarget.scrollTo({ top: 0, behavior: 'smooth' })
  }
  // Fallback to ensure window scrolls
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  window.addEventListener('scroll', checkScroll, { capture: true, passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', checkScroll, { capture: true })
})
</script>

<template>
  <transition name="fade-slide">
    <button
      v-if="showButton"
      class="scroll-to-top"
      @click="scrollToTop"
      aria-label="回到最頂"
    >
      ↑
    </button>
  </transition>
</template>

<style scoped>
.scroll-to-top {
  display: none; /* Default hidden on desktop */
}

@media (max-width: 900px) {
  .scroll-to-top {
    display: flex;
    align-items: center;
    justify-content: center;
    position: fixed;
    bottom: 84px;
    right: 24px;
    width: 48px;
    height: 48px;
    background-color: var(--primary, #5c6b5f);
    color: white;
    border: none;
    border-radius: 50%;
    font-size: 20px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    cursor: pointer;
    z-index: 9999;
  }
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(20px);
}
</style>
