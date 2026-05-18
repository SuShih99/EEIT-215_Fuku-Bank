<script setup>
import { RouterView } from 'vue-router'

// 核心解法：透過 Ant Design Vue 4 的 ConfigProvider 全域覆寫 Token
const themeConfig = {
  token: {
    colorPrimary: '#5C6B5F',       // 主色調改為墨綠色
    colorPrimaryHover: '#4A574D',  // Hover 狀態加深
    colorLink: '#5C6B5F',          // 連結顏色
    colorLinkHover: '#4A574D',     
    borderRadius: 8,               // 系統預設圓角基數
  }
}
</script>

<template>
  <!-- 將全站用 ConfigProvider 包裹，確保所有元件預設都是墨綠色 -->
  <a-config-provider :theme="themeConfig">
    <RouterView />
  </a-config-provider>
</template>

<style>
/* style 拿掉 scoped，設一個全域的 body reset，確保沒有多餘的邊距 */
body {
  margin: 0;
  padding: 0;
  background-color: #f5f1ea !important;
}
</style>