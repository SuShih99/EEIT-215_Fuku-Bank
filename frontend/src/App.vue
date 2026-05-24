<script setup>
import { RouterView } from 'vue-router'
import ScrollToTop from '@/components/ScrollToTop.vue'

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
    <ScrollToTop />
  </a-config-provider>
</template>

<style>
/* style 拿掉 scoped，設一個全域的 body reset，確保沒有多餘的邊距 */
body {
  margin: 0;
  padding: 0;
  background-color: #f5f1ea !important;
}

body.customer-ant-scope .ant-message {
  top: 86px !important;
  font-family: var(--font-body, 'Noto Sans TC', -apple-system, BlinkMacSystemFont, sans-serif);
}

body.customer-ant-scope .ant-message .ant-message-notice-wrapper {
  padding: 0;
}

body.customer-ant-scope .ant-message .ant-message-notice-content {
  min-height: 64px;
  padding: 16px 28px;
  display: inline-flex;
  align-items: center;
  border-radius: 12px;
  background: rgba(255, 249, 239, 0.96);
  border: 1px solid rgba(214, 206, 195, 0.86);
  box-shadow: 0 12px 36px rgba(63, 74, 66, 0.12);
  color: var(--text-primary, #2b2b2b);
}

body.customer-ant-scope .ant-message .ant-message-custom-content {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
}

body.customer-ant-scope .ant-message .anticon {
  margin-inline-end: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 999px;
  font-size: 16px;
  color: #fff;
}

body.customer-ant-scope .ant-message .ant-message-success .anticon {
  background: var(--primary, #5c6b5f);
}

body.customer-ant-scope .ant-message .ant-message-warning .anticon {
  background: #a65a4d;
}

body.customer-ant-scope .ant-message .ant-message-error .anticon {
  background: var(--accent, #a65a4d);
}

body.customer-ant-scope .ant-message .ant-message-info .anticon,
body.customer-ant-scope .ant-message .ant-message-loading .anticon {
  background: var(--primary, #5c6b5f);
}

@media (max-width: 640px) {
  body.customer-ant-scope .ant-message {
    left: 16px !important;
    right: 16px !important;
    transform: none !important;
  }

  body.customer-ant-scope .ant-message .ant-message-notice-content {
    width: 100%;
    justify-content: center;
    padding: 14px 18px;
  }

  body.customer-ant-scope .ant-message .ant-message-custom-content {
    font-size: 16px;
  }
}

body.customer-ant-scope .ant-btn {
  min-height: 40px;
  padding: 8px 18px;
  border-radius: 8px;
  border-color: rgba(214, 206, 195, 0.86);
  color: var(--primary-dark, #3f4a42);
  background: rgba(255, 249, 239, 0.68);
  box-shadow: none;
  font-family: var(--font-body, 'Noto Sans TC', -apple-system, BlinkMacSystemFont, sans-serif);
  font-weight: 600;
}

body.customer-ant-scope .ant-btn:hover,
body.customer-ant-scope .ant-btn:focus-visible {
  border-color: var(--primary, #5c6b5f) !important;
  color: var(--primary-dark, #3f4a42) !important;
  background: var(--primary-light, rgba(92, 107, 95, 0.08)) !important;
}

body.customer-ant-scope .ant-btn-primary {
  border-color: var(--primary, #5c6b5f) !important;
  color: #fff !important;
  background: var(--primary, #5c6b5f) !important;
  box-shadow: 0 6px 14px rgba(63, 74, 66, 0.14);
}

body.customer-ant-scope .ant-btn-primary:hover,
body.customer-ant-scope .ant-btn-primary:focus-visible {
  border-color: var(--primary-dark, #3f4a42) !important;
  color: #fff !important;
  background: var(--primary-dark, #3f4a42) !important;
}

body.customer-ant-scope .ant-btn-link {
  min-height: auto;
  padding: 0;
  border: 0;
  color: var(--primary, #5c6b5f) !important;
  background: transparent;
  box-shadow: none;
}

body.customer-ant-scope .ant-card,
body.customer-ant-scope .ant-modal-content {
  border-radius: 12px;
  border: 1px solid rgba(214, 206, 195, 0.86);
  background: rgba(255, 249, 239, 0.96);
  box-shadow: 0 12px 36px rgba(63, 74, 66, 0.10);
}

body.customer-ant-scope .ant-card-head,
body.customer-ant-scope .ant-modal-header {
  border-bottom-color: rgba(214, 206, 195, 0.72);
  background: transparent;
}

body.customer-ant-scope .ant-card-head-title,
body.customer-ant-scope .ant-modal-title {
  color: var(--text-primary, #2b2b2b);
  font-family: var(--font-heading, 'Noto Serif TC', serif);
  font-weight: 700;
}

body.customer-ant-scope .ant-modal-close {
  color: var(--text-secondary, #5c564e);
}

body.customer-ant-scope .ant-modal-footer {
  border-top-color: rgba(214, 206, 195, 0.72);
}

body.customer-ant-scope .ant-form-item-label > label {
  color: var(--text-primary, #2b2b2b);
  font-weight: 600;
}

body.customer-ant-scope .ant-input,
body.customer-ant-scope .ant-input-number,
body.customer-ant-scope .ant-picker,
body.customer-ant-scope .ant-select-selector {
  min-height: 42px;
  border-color: rgba(198, 188, 174, 0.92) !important;
  border-radius: 8px !important;
  background: rgba(250, 250, 247, 0.84) !important;
  color: var(--text-primary, #2b2b2b);
  box-shadow: none !important;
}

body.customer-ant-scope .ant-input:hover,
body.customer-ant-scope .ant-input-number:hover,
body.customer-ant-scope .ant-picker:hover,
body.customer-ant-scope .ant-select-selector:hover {
  border-color: var(--text-disabled, #a8a199) !important;
}

body.customer-ant-scope .ant-input:focus,
body.customer-ant-scope .ant-input-focused,
body.customer-ant-scope .ant-input-number-focused,
body.customer-ant-scope .ant-picker-focused,
body.customer-ant-scope .ant-select-focused .ant-select-selector {
  border-color: var(--primary, #5c6b5f) !important;
  box-shadow: 0 0 0 3px var(--primary-light, rgba(92, 107, 95, 0.08)) !important;
}

body.customer-ant-scope .ant-input-number-input,
body.customer-ant-scope .ant-select-selection-item,
body.customer-ant-scope .ant-picker-input > input {
  color: var(--text-primary, #2b2b2b);
}

body.customer-ant-scope .ant-input::placeholder,
body.customer-ant-scope .ant-picker-input > input::placeholder,
body.customer-ant-scope .ant-select-selection-placeholder {
  color: var(--text-placeholder, #b5ada3) !important;
}

body.customer-ant-scope .ant-select-dropdown,
body.customer-ant-scope .ant-picker-dropdown .ant-picker-panel-container,
body.customer-ant-scope .ant-popover-inner {
  border: 1px solid rgba(214, 206, 195, 0.86);
  border-radius: 12px;
  background: rgba(255, 249, 239, 0.98);
  box-shadow: 0 12px 36px rgba(63, 74, 66, 0.12);
}

body.customer-ant-scope .ant-select-item-option-selected:not(.ant-select-item-option-disabled),
body.customer-ant-scope .ant-picker-cell-in-view.ant-picker-cell-selected .ant-picker-cell-inner,
body.customer-ant-scope .ant-picker-cell-in-view.ant-picker-cell-range-start .ant-picker-cell-inner,
body.customer-ant-scope .ant-picker-cell-in-view.ant-picker-cell-range-end .ant-picker-cell-inner {
  color: #fff !important;
  background: var(--primary, #5c6b5f) !important;
}

body.customer-ant-scope .ant-select-item-option-selected .ant-select-item-option-content,
body.customer-ant-scope .ant-select-item-option-selected .ant-select-item-option-state {
  color: #fff !important;
}

body.customer-ant-scope .ant-select-item-option-active:not(.ant-select-item-option-disabled),
body.customer-ant-scope .ant-picker-cell-in-view.ant-picker-cell-today .ant-picker-cell-inner::before,
body.customer-ant-scope .ant-picker-cell .ant-picker-cell-inner:hover {
  border-color: var(--primary, #5c6b5f);
  background: var(--primary-light, rgba(92, 107, 95, 0.08));
}

body.customer-ant-scope .ant-select-item-option-active:not(.ant-select-item-option-disabled):not(.ant-select-item-option-selected),
body.customer-ant-scope .ant-select-item-option-active:not(.ant-select-item-option-disabled):not(.ant-select-item-option-selected) .ant-select-item-option-content {
  color: var(--text-primary, #2b2b2b) !important;
}

body.customer-ant-scope .ant-list-item {
  border-block-end-color: rgba(214, 206, 195, 0.72);
}

body.customer-ant-scope .ant-list-item-meta-title,
body.customer-ant-scope .ant-empty-description,
body.customer-ant-scope .ant-popover-message-title {
  color: var(--text-primary, #2b2b2b);
}

body.customer-ant-scope .ant-list-item-meta-description {
  color: var(--text-secondary, #5c564e);
}

body.customer-ant-scope .ant-empty-img-simple-ellipse {
  fill: rgba(214, 206, 195, 0.72);
}

body.customer-ant-scope .ant-empty-img-simple-g {
  stroke: rgba(166, 90, 77, 0.18);
}

body.customer-ant-scope .ant-empty-img-simple-path {
  fill: rgba(250, 250, 247, 0.84);
}

body.customer-ant-scope .ant-spin-dot-item {
  background-color: var(--primary, #5c6b5f);
}

body.customer-ant-scope .ant-tag {
  border-color: rgba(214, 206, 195, 0.86);
  border-radius: 999px;
  background: rgba(250, 250, 247, 0.84);
  color: var(--text-secondary, #5c564e);
  font-weight: 600;
}

body.customer-ant-scope .ant-tag-green,
body.customer-ant-scope .ant-tag-blue {
  border-color: rgba(92, 107, 95, 0.24);
  background: rgba(92, 107, 95, 0.08);
  color: var(--primary-dark, #3f4a42);
}

body.customer-ant-scope .ant-tag-red,
body.customer-ant-scope .ant-tag-error {
  border-color: rgba(166, 90, 77, 0.24);
  background: rgba(166, 90, 77, 0.10);
  color: var(--accent, #a65a4d);
}

body.customer-ant-scope .ant-popover-buttons .ant-btn-sm {
  min-height: 32px;
  padding: 4px 12px;
}
</style>
