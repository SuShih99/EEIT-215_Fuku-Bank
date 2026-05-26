<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  getCustomerNotificationPreferences,
  updateCustomerNotificationPreference,
} from '@/api/notificationPreference'
import { useRouter } from 'vue-router'

const router = useRouter()

const loading = ref(false)
const savingType = ref('')
const loadError = ref('')
const toast = ref({
  visible: false,
  type: 'success',
  text: '',
  timer: null,
})

const typeOrder = [
  'ACCOUNT_APPLICATION',
  'ACCOUNT_SUPPLEMENT_REQUIRED',
  'TRANSFER',
  'LOAN',
  'CREDIT_CARD',
  'PASSBOOK',
  'SYSTEM',
  'SECURITY',
]

const preferences = ref([])

const hasPreferences = computed(() => preferences.value.length > 0)

function showToast(text, type = 'success') {
  toast.value.text = text
  toast.value.type = type
  toast.value.visible = true

  if (toast.value.timer) clearTimeout(toast.value.timer)
  toast.value.timer = setTimeout(() => {
    toast.value.visible = false
  }, 2800)
}

function normalizePreferences(rows = []) {
  const map = new Map()
  rows.forEach((row) => {
    if (row?.type) map.set(row.type, { ...row })
  })

  return typeOrder
    .map((type) => map.get(type))
    .filter(Boolean)
    .map((row) => ({
      type: row.type,
      label: row.label || typeLabelMap[row.type] || row.type,
      enabled: Boolean(row.enabled),
      locked: Boolean(row.locked),
      updatedAt: row.updatedAt || null,
    }))
}

const typeLabelMap = {
  ACCOUNT_APPLICATION: '開戶申請通知',
  ACCOUNT_SUPPLEMENT_REQUIRED: '開戶補件通知',
  TRANSFER: '轉帳通知',
  LOAN: '貸款通知',
  CREDIT_CARD: '信用卡通知',
  PASSBOOK: '電子存摺通知',
  SECURITY: '安全通知',
  SYSTEM: '系統通知',
}

const typeHintMap = {
  ACCOUNT_APPLICATION: '開戶申請、審核與狀態更新提醒',
  ACCOUNT_SUPPLEMENT_REQUIRED: '開戶申請需要補件時的提醒',
  TRANSFER: '轉帳、預約轉帳與交易進度提醒',
  LOAN: '貸款申請、補件、撥款與還款提醒',
  CREDIT_CARD: '信用卡帳單、申辦與交易提醒',
  PASSBOOK: '電子存摺與帳戶資料提醒',
  SYSTEM: '系統公告與重要功能通知',
  SECURITY: '登入、驗證與安全事件提醒',
}

async function loadPreferences() {
  loading.value = true
  loadError.value = ''

  try {
    const res = await getCustomerNotificationPreferences()
    const rows = res.data?.data ?? res.data ?? []
    preferences.value = normalizePreferences(Array.isArray(rows) ? rows : [])
  } catch (error) {
    console.error('載入通知偏好失敗', error)
    loadError.value = '通知設定載入失敗，請稍後再試。'
    preferences.value = []
  } finally {
    loading.value = false
  }
}

async function handleToggle(preference, event) {
  if (preference.locked) {
    event.target.checked = true
    return
  }

  const nextEnabled = event.target.checked
  const prevEnabled = preference.enabled
  preference.enabled = nextEnabled
  savingType.value = preference.type
  loadError.value = ''

  try {
    await updateCustomerNotificationPreference({
      type: preference.type,
      enabled: nextEnabled,
    })
    showToast(`${preference.label}已更新`, 'success')
  } catch (error) {
    console.error('更新通知偏好失敗', error)
    preference.enabled = prevEnabled
    event.target.checked = prevEnabled
    showToast(error.response?.data?.message || '設定更新失敗，請稍後再試。', 'error')
  } finally {
    savingType.value = ''
  }
}

function goBackToHome() {
  router.push({ name: 'user-home' })
}

onMounted(loadPreferences)

onBeforeUnmount(() => {
  if (toast.value.timer) clearTimeout(toast.value.timer)
})
</script>

<template>
  <div class="customer-page notification-settings-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">通知設定</h2>
        <p class="page-subtitle">選擇您希望接收的站內通知類型。</p>
      </div>
      <button class="jb-btn jb-btn-secondary page-back-btn" type="button" @click="goBackToHome">
        返回首頁
      </button>
    </div>

    <section class="notification-settings-card jb-card">
      <div class="notification-settings-note">
        <div class="note-title">設定說明</div>
        <p>
          關閉後，該類型的站內通知將不再顯示於通知中心。<br />
          安全通知因涉及帳戶安全，將永遠保持開啟。
        </p>
      </div>

      <div v-if="loading" class="state-panel">通知設定載入中...</div>
      <div v-else-if="loadError && !hasPreferences" class="state-panel state-error">
        {{ loadError }}
      </div>
      <div v-else-if="!hasPreferences" class="state-panel">
        目前沒有可設定的通知類型。
      </div>
      <div v-else class="preference-list">
        <div v-for="item in preferences" :key="item.type" class="preference-row" :class="{ locked: item.locked }">
          <div class="preference-copy">
            <div class="preference-title-row">
              <h3 class="preference-title">{{ item.label }}</h3>
              <span v-if="savingType === item.type" class="preference-saving">更新中...</span>
            </div>
            <p class="preference-description">{{ typeHintMap[item.type] || '站內通知提醒' }}</p>
            <p v-if="item.locked" class="preference-lock-hint">基於帳戶安全，無法關閉。</p>
          </div>

          <label class="toggle-switch" :class="{ disabled: item.locked }">
            <input
              type="checkbox"
              :checked="item.enabled"
              :disabled="item.locked || savingType === item.type"
              @change="handleToggle(item, $event)"
            />
            <span class="toggle-track">
              <span class="toggle-thumb"></span>
            </span>
          </label>
        </div>
      </div>
    </section>

    <transition name="toast-fade">
      <div v-if="toast.visible" class="jb-toast" :class="`toast-${toast.type}`">
        {{ toast.text }}
      </div>
    </transition>
  </div>
</template>

<style scoped>
.notification-settings-page {
  min-height: 100vh;
  padding: 40px 8vw 96px;
}

.page-head {
  max-width: 760px;
  margin: 0 auto 18px;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
}

.page-title {
  margin: 0 0 8px;
  font-family: var(--font-heading);
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
}

.page-subtitle {
  margin: 0;
  color: var(--text-secondary);
  font-size: 15px;
  line-height: 1.7;
}

.page-back-btn {
  flex-shrink: 0;
}

.notification-settings-card {
  max-width: 720px;
  margin: 0 auto;
  padding: 28px 28px 18px;
  background-color: rgba(255, 249, 239, 0.78);
  border: 1px solid rgba(214, 206, 195, 0.92);
  border-radius: 22px;
  box-shadow: 0 10px 26px rgba(63, 74, 66, 0.06);
}

.notification-settings-note {
  margin-bottom: 20px;
  padding: 16px 18px;
  background: rgba(92, 107, 95, 0.06);
  border: 1px solid rgba(92, 107, 95, 0.14);
  border-radius: 16px;
}

.note-title {
  margin-bottom: 6px;
  color: var(--primary-dark);
  font-size: 14px;
  font-weight: 700;
}

.notification-settings-note p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.state-panel {
  padding: 28px 20px;
  border: 1px dashed rgba(214, 206, 195, 0.9);
  border-radius: 16px;
  color: var(--text-secondary);
  text-align: center;
}

.state-error {
  border-color: rgba(166, 90, 77, 0.28);
  color: var(--accent);
  background: rgba(166, 90, 77, 0.05);
}

.preference-list {
  display: grid;
}

.preference-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 16px 0;
  border-bottom: 1px solid rgba(214, 206, 195, 0.62);
}

.preference-row:last-child {
  border-bottom: none;
  padding-bottom: 4px;
}

.preference-copy {
  min-width: 0;
  flex: 1 1 auto;
}

.preference-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.preference-title {
  margin: 0;
  color: var(--text-primary);
  font-size: 17px;
  font-weight: 700;
}

.preference-saving {
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid rgba(92, 107, 95, 0.18);
  background: rgba(92, 107, 95, 0.06);
  color: var(--primary-dark);
  font-size: 12px;
  font-weight: 600;
}

.preference-description {
  margin: 5px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.preference-lock-hint {
  margin: 8px 0 0;
  color: var(--accent);
  font-size: 12px;
  font-weight: 600;
}

.toggle-switch {
  position: relative;
  flex: 0 0 auto;
  width: 58px;
  height: 32px;
  cursor: pointer;
}

.toggle-switch input {
  position: absolute;
  inset: 0;
  opacity: 0;
  margin: 0;
  cursor: pointer;
}

.toggle-switch.disabled {
  cursor: not-allowed;
  opacity: 1;
}

.toggle-track {
  position: absolute;
  inset: 0;
  border-radius: 999px;
  background: rgba(214, 206, 195, 0.72);
  border: 1px solid rgba(214, 206, 195, 0.98);
  transition: background-color 0.2s ease, border-color 0.2s ease;
}

.toggle-thumb {
  position: absolute;
  top: 50%;
  left: 4px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  transform: translateY(-50%);
  background: #fffdf9;
  box-shadow: 0 2px 8px rgba(63, 74, 66, 0.14);
  transition: transform 0.2s ease, background-color 0.2s ease;
}

.toggle-switch input:checked + .toggle-track {
  background: rgba(92, 107, 95, 0.86);
  border-color: rgba(92, 107, 95, 0.92);
}

.toggle-switch input:checked + .toggle-track .toggle-thumb {
  transform: translate(26px, -50%);
}

.toggle-switch input:disabled + .toggle-track {
  cursor: not-allowed;
}

.toggle-switch input:focus-visible + .toggle-track {
  box-shadow: 0 0 0 3px rgba(92, 107, 95, 0.14);
}

.jb-toast {
  position: fixed;
  top: 88px;
  left: 50%;
  z-index: 2200;
  min-width: min(320px, calc(100vw - 32px));
  max-width: min(520px, calc(100vw - 32px));
  padding: 13px 18px;
  transform: translateX(-50%);
  border: 1px solid rgba(214, 206, 195, 0.92);
  border-radius: 16px;
  background: rgba(255, 249, 239, 0.96);
  box-shadow: 0 14px 34px rgba(63, 74, 66, 0.12);
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.45;
  text-align: center;
  backdrop-filter: blur(10px);
  animation: toast-breathe 1.8s ease-in-out infinite;
}

.jb-toast.toast-success {
  border-color: rgba(92, 107, 95, 0.34);
  color: var(--primary-dark);
}

.jb-toast.toast-error {
  border-color: rgba(166, 90, 77, 0.34);
  color: var(--accent);
  background: rgba(255, 249, 239, 0.98);
}

@keyframes toast-breathe {
  0%,
  100% {
    box-shadow: 0 14px 34px rgba(63, 74, 66, 0.11);
    transform: translateX(-50%) scale(1);
  }
  50% {
    box-shadow: 0 18px 42px rgba(63, 74, 66, 0.16);
    transform: translateX(-50%) scale(1.015);
  }
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.28s var(--ease);
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translate(-50%, -14px) scale(0.98);
}

@media (max-width: 768px) {
  .notification-settings-page {
    padding: 20px 16px 80px;
  }

  .page-head {
    flex-direction: column;
    align-items: stretch;
  }

  .page-title {
    font-size: 26px;
  }

  .notification-settings-card {
    padding: 20px 16px 12px;
  }

  .preference-row {
    gap: 14px;
    align-items: flex-start;
  }

  .preference-title {
    font-size: 16px;
  }

  .preference-description,
  .preference-lock-hint {
    line-height: 1.65;
  }

  .toggle-switch {
    margin-top: 4px;
    flex-shrink: 0;
  }

  .jb-toast {
    top: 72px;
    padding: 12px 14px;
    font-size: 13px;
  }
}
</style>
