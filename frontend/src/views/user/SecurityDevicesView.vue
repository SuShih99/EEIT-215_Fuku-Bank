<template>
  <div class="security-page">
    <nav class="security-breadcrumb" aria-label="breadcrumb">
      <span>首頁</span>
      <span class="chevron">›</span>
      <span>安全中心</span>
      <span class="chevron">›</span>
      <strong>裝置管理</strong>
    </nav>

    <section class="security-heading">
      <div>
        <p>安全中心</p>
        <h1>裝置管理</h1>
      </div>
      <button class="refresh-btn" @click="loadDevices">
        <ReloadOutlined />
        重新整理
      </button>
    </section>

    <a-spin :spinning="loading">
      <a-empty v-if="!devices.length" description="尚無授權裝置" />
      <div v-else class="device-list">
        <article v-for="device in devices" :key="device.deviceId" class="device-row">
          <div class="device-icon">
            <LaptopOutlined />
          </div>
          <div class="device-main">
            <div class="device-title">
              <span>{{ device.deviceName || '未知裝置' }}</span>
              <a-tag :color="device.status === 'ACTIVE' ? 'green' : 'default'">{{ statusLabel(device.status) }}</a-tag>
              <a-tag v-if="device.deviceId === latestActiveDeviceId" color="blue">最近登入</a-tag>
            </div>
            <div class="device-meta">
              <span>{{ device.browserName || '未知瀏覽器' }}</span>
              <span>{{ device.operatingSystem || '未知系統' }}</span>
              <span>登入位置/IP {{ device.ipAddress || '未知 IP' }}</span>
              <span>最近使用 {{ formatDateTime(device.lastSeenAt) }}</span>
            </div>
          </div>
          <a-popconfirm
            v-if="device.status === 'ACTIVE'"
            title="確定要移除此裝置授權？"
            ok-text="移除"
            cancel-text="取消"
            @confirm="revokeDevice(device.deviceId)"
          >
            <button class="revoke-btn" :disabled="revokingId === device.deviceId">
              移除授權
            </button>
          </a-popconfirm>
        </article>
      </div>
    </a-spin>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { LaptopOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { getCustomerDevices, revokeCustomerDevice } from '@/api/customerAuth'

const loading = ref(false)
const revokingId = ref(null)
const devices = ref([])
const latestActiveDeviceId = computed(() => devices.value.find(device => device.status === 'ACTIVE')?.deviceId)

onMounted(loadDevices)

async function loadDevices() {
  loading.value = true
  try {
    const result = await getCustomerDevices()
    devices.value = [...result].sort((a, b) => new Date(b.lastSeenAt || 0) - new Date(a.lastSeenAt || 0))
  } catch (e) {
    console.error(e)
    message.error('授權裝置讀取失敗')
  } finally {
    loading.value = false
  }
}

async function revokeDevice(deviceId) {
  revokingId.value = deviceId
  try {
    await revokeCustomerDevice(deviceId)
    message.success('裝置授權已移除')
    await loadDevices()
  } catch (e) {
    console.error(e)
    message.error('裝置授權移除失敗')
  } finally {
    revokingId.value = null
  }
}

function statusLabel(status) {
  const map = { ACTIVE: '已授權', REVOKED: '已移除' }
  return map[status] || status || '-'
}

function formatDateTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()} / ${String(date.getMonth() + 1).padStart(2, '0')} / ${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
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

.refresh-btn,
.revoke-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 42px;
  padding: 0 16px;
  border: 1px solid var(--border);
  border-radius: 8px;
  cursor: pointer;
}

.refresh-btn {
  color: var(--text-primary);
  background: rgba(255, 249, 239, 0.78);
}

.revoke-btn {
  color: #a65a4d;
  background: #fffaf3;
}

.revoke-btn:disabled {
  opacity: .6;
  cursor: wait;
}

.device-list {
  display: grid;
  gap: 12px;
}

.device-row {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) auto;
  align-items: center;
  gap: 18px;
  padding: 18px 20px;
  background: rgba(255, 249, 239, 0.72);
  border: 1px solid var(--border);
  border-radius: 8px;
}

.device-icon {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  color: #fff;
  background: var(--primary);
  border-radius: 8px;
  font-size: 22px;
}

.device-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  font-size: 17px;
  font-weight: 700;
}

.device-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
  color: var(--text-secondary);
  font-size: 14px;
}

@media (max-width: 720px) {
  .security-heading {
    align-items: stretch;
    flex-direction: column;
  }

  .device-row {
    grid-template-columns: 48px minmax(0, 1fr);
  }

  .revoke-btn {
    grid-column: 1 / -1;
    width: 100%;
  }
}
</style>
