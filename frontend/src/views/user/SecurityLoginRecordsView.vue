<template>
  <div class="security-page">
    <nav class="security-breadcrumb" aria-label="breadcrumb">
      <span>首頁</span>
      <span class="chevron">›</span>
      <span>安全中心</span>
      <span class="chevron">›</span>
      <strong>登入紀錄</strong>
    </nav>

    <section class="security-heading">
      <div>
        <p>安全中心</p>
        <h1>登入紀錄</h1>
      </div>
      <button class="refresh-btn" @click="loadLoginLogs">
        <ReloadOutlined />
        重新整理
      </button>
    </section>

    <a-spin :spinning="loading">
      <a-empty v-if="!loginLogs.length" description="尚無登入紀錄" />
      <div v-else class="record-list">
        <article v-for="log in loginLogs" :key="log.loginLogId" class="record-row">
          <div class="record-main">
            <div class="record-title">
              <span>{{ log.deviceName || '未知裝置' }}</span>
              <a-tag :color="resultColor(log.result)">{{ formatResult(log.result) }}</a-tag>
            </div>
            <div class="record-meta">
              <span>{{ formatDateTime(log.loginTime) }}</span>
              <span>{{ log.ipAddress || '未知 IP' }}</span>
              <span>{{ log.username }}</span>
            </div>
          </div>
          <p v-if="log.failReason" class="record-reason">{{ log.failReason }}</p>
        </article>
      </div>
    </a-spin>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { getCustomerLoginLogs } from '@/api/customerAuth'

const loading = ref(false)
const loginLogs = ref([])

onMounted(loadLoginLogs)

async function loadLoginLogs() {
  loading.value = true
  try {
    loginLogs.value = await getCustomerLoginLogs()
  } catch (e) {
    console.error(e)
    message.error('登入紀錄讀取失敗')
  } finally {
    loading.value = false
  }
}

function formatDateTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()} / ${String(date.getMonth() + 1).padStart(2, '0')} / ${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function formatResult(value) {
  if (value === '成功' || value === 'SUCCESS') return '成功'
  if (value === '失敗' || value === 'FAILED' || value === 'FAIL') return '失敗'
  return '未知'
}

function resultColor(value) {
  if (value === '成功' || value === 'SUCCESS') return 'green'
  if (value === '失敗' || value === 'FAILED' || value === 'FAIL') return 'red'
  return 'default'
}
</script>

<style scoped>
.security-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px 16px 56px;
  color: var(--text-primary);
}

.security-breadcrumb {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 32px;
  color: var(--text-secondary);
  font-size: 14px;
}

.security-breadcrumb strong {
  color: var(--primary-dark);
  font-weight: 600;
}

.chevron {
  color: var(--text-disabled);
}

.security-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
}

.security-heading p {
  margin: 0 0 8px;
  color: var(--primary);
  font-weight: 700;
}

.security-heading h1 {
  margin: 0;
  font-family: var(--font-heading);
  font-size: 34px;
  letter-spacing: 0;
}

.refresh-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 42px;
  padding: 0 16px;
  color: var(--text-primary);
  background: rgba(255, 249, 239, 0.78);
  border: 1px solid var(--border);
  border-radius: 8px;
  cursor: pointer;
}

.record-list {
  display: grid;
  gap: 12px;
}

.record-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 20px;
  background: rgba(255, 249, 239, 0.72);
  border: 1px solid var(--border);
  border-radius: 8px;
}

.record-main {
  min-width: 0;
}

.record-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  font-size: 17px;
  font-weight: 700;
}

.record-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
  color: var(--text-secondary);
  font-size: 14px;
}

.record-reason {
  flex: 0 0 auto;
  margin: 0;
  color: #a65a4d;
  font-weight: 700;
}

@media (max-width: 680px) {
  .security-heading,
  .record-row {
    align-items: stretch;
    flex-direction: column;
  }

  .record-reason {
    flex: none;
  }
}
</style>
