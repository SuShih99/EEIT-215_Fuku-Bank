<script setup>
import { RouterView } from 'vue-router'
</script>

<template>
  <!-- 讓RouterView放入元件的內容 -->
  <RouterView />
</template>

<style>
/* style 拿掉 scoped，設一個全域的 body reset，確保沒有多餘的邊距 */
body {
  margin: 0;
  padding: 0;
}
</style>