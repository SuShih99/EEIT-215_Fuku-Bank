<template>
  <div class="passbook-page">
    <nav class="passbook-breadcrumb" aria-label="breadcrumb">
      <span>首頁</span>
      <span class="chevron">›</span>
      <span>我的帳戶</span>
      <span class="chevron">›</span>
      <strong>電子存摺</strong>
    </nav>

    <section class="passbook-heading">
      <h1>電子存摺</h1>
      <p>帳戶資料與存摺封面資訊</p>
      <div class="account-select">
        <span>選擇帳戶</span>
        <a-select
          v-model:value="selectedAccountNumber"
          :loading="loading"
          placeholder="請選擇帳戶"
          style="width: 100%"
        >
          <a-select-option v-for="account in passbookAccounts" :key="account.accountNumber" :value="account.accountNumber">
            {{ accountOptionLabel(account) }}
          </a-select-option>
        </a-select>
      </div>
    </section>

    <a-spin :spinning="loading">
      <a-empty v-if="!selectedAccount && !loading" description="尚無可顯示的帳戶" />

      <section v-else class="passbook-cover" aria-label="電子存摺封面">
        <div class="brand-block">
          <img src="/logo.png" alt="JAVA_BANK" class="brand-mark" />
          <div>
            <div class="brand-title">JAVA_BANK</div>
            <div class="brand-subtitle">E-PASSBOOK</div>
            <div class="brand-motto">CALM · BALANCE · TRUST</div>
          </div>
        </div>

        <div class="cover-divider"></div>

        <div class="cover-grid">
          <div class="info-row">
            <span>戶名</span>
            <strong>{{ ownerName }}</strong>
          </div>
          <div class="info-row">
            <span>幣別</span>
            <strong class="currency-pill">{{ currencyLabel(selectedAccount?.currency) }}</strong>
          </div>
          <div class="info-row">
            <span>帳號</span>
            <strong>{{ formatAccountNumber(selectedAccount?.accountNumber) }}</strong>
          </div>
          <div class="info-row">
            <span>銀行代號</span>
            <strong>909</strong>
          </div>
          <div class="info-row">
            <span>開戶分行</span>
            <strong>台北分行</strong>
          </div>
          <div class="info-row">
            <span>申請日期</span>
            <strong>{{ formatDate(selectedAccount?.createdAt) }}</strong>
          </div>
          <div class="info-row">
            <span>帳戶類型</span>
            <strong>{{ typeLabel(selectedAccount?.accountType, selectedAccount?.currency) }}</strong>
          </div>
          <div class="info-row">
            <span>狀態</span>
            <strong class="status-text">{{ statusLabel(selectedAccount?.status) }}</strong>
          </div>
        </div>

        <p class="cover-note">本電子存摺僅供參考，實際帳戶資訊以本行系統資料為準。</p>
        <div class="seal" aria-hidden="true">爪哇<br />銀行</div>
      </section>
    </a-spin>

    <div class="passbook-actions" v-if="selectedAccount">
      <button class="action-btn primary" :disabled="pdfLoading" @click="downloadPdf">
        <DownloadOutlined />
        下載 PDF
      </button>
      <button class="action-btn" :disabled="pdfLoading" @click="printCover">
        <PrinterOutlined />
        列印
      </button>
      <button class="action-btn" @click="copyAccount">
        <CopyOutlined />
        複製帳號
      </button>
      <button class="action-btn" @click="$router.push({ name: 'user-accounts' })">
        <ArrowLeftOutlined />
        返回帳戶總覽
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined, CopyOutlined, DownloadOutlined, PrinterOutlined } from '@ant-design/icons-vue'
import { downloadPassbookPdf, getMyAccounts } from '@/api/customerAccount'
import { customerGetProfile } from '@/api/customerAuth'
import { useCustomerAuthStore } from '@/stores/customerAuth'

const customerAuthStore = useCustomerAuthStore()
const loading = ref(false)
const accounts = ref([])
const profile = ref(null)
const selectedAccountNumber = ref(null)
const pdfLoading = ref(false)

const currencyPriority = { TWD: 0, USD: 1, JPY: 2, EUR: 3, GBP: 4 }
const passbookAccounts = computed(() => accounts.value
  .filter(account => account.accountType !== 'LOAN')
  .sort((a, b) => (currencyPriority[a.currency] ?? 99) - (currencyPriority[b.currency] ?? 99) || a.accountNumber.localeCompare(b.accountNumber)))
const selectedAccount = computed(() => passbookAccounts.value.find(account => account.accountNumber === selectedAccountNumber.value) || passbookAccounts.value[0])
const ownerName = computed(() => selectedAccount.value?.customerName || profile.value?.name || customerAuthStore.customer?.name || '會員')

onMounted(async () => {
  loading.value = true
  try {
    const [accountRes, profileRes] = await Promise.allSettled([getMyAccounts(), customerGetProfile()])
    if (accountRes.status === 'fulfilled') {
      accounts.value = accountRes.value || []
      selectedAccountNumber.value = passbookAccounts.value.find(account => account.currency === 'TWD')?.accountNumber || passbookAccounts.value[0]?.accountNumber || null
    }
    if (profileRes.status === 'fulfilled') {
      profile.value = profileRes.value?.data?.data || null
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})

function typeLabel(type, currency) {
  const map = {
    CHECKING: `${currencyLabel(currency)}活期存款`,
    SAVINGS: `${currencyLabel(currency)}儲蓄存款`,
    TIME_DEPOSIT: `${currencyLabel(currency)}定期存款`,
    LOAN: '貸款帳戶',
    SUB_ACCOUNT: `${currencyLabel(currency)}子帳戶`,
  }
  return map[type] || type || '-'
}

function currencyLabel(currency) {
  const map = {
    TWD: '臺幣',
    USD: '美元',
    EUR: '歐元',
    JPY: '日圓',
    GBP: '英鎊',
    CNY: '人民幣',
    AUD: '澳幣',
    CAD: '加幣',
    CHF: '瑞郎',
    HKD: '港幣',
  }
  return map[currency] || currency || '-'
}

function statusLabel(status) {
  const map = { ACTIVE: '正常', FROZEN: '凍結', DORMANT: '靜止', CLOSED: '已銷戶', PENDING: '待啟用' }
  return map[status] || status || '-'
}

function formatAccountNumber(accountNumber = '') {
  const raw = String(accountNumber)
  if (!raw) return '-'
  if (raw.length === 12) return `${raw.slice(0, 3)}-${raw.slice(3, 6)}-${raw.slice(6)}`
  return raw
}

function accountOptionLabel(account) {
  return `${typeLabel(account.accountType, account.currency)}　${formatAccountNumber(account.accountNumber)}`
}

function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return `${date.getFullYear()} / ${String(date.getMonth() + 1).padStart(2, '0')} / ${String(date.getDate()).padStart(2, '0')}`
}

async function fetchPassbookPdf() {
  if (!selectedAccount.value?.accountNumber) return null
  pdfLoading.value = true
  try {
    return await downloadPassbookPdf(selectedAccount.value.accountNumber)
  } catch (e) {
    console.error(e)
    message.error('電子存摺 PDF 產生失敗')
    return null
  } finally {
    pdfLoading.value = false
  }
}

async function printCover() {
  const blob = await fetchPassbookPdf()
  if (!blob) return
  const url = URL.createObjectURL(blob)
  const iframe = document.createElement('iframe')
  iframe.style.position = 'fixed'
  iframe.style.right = '0'
  iframe.style.bottom = '0'
  iframe.style.width = '0'
  iframe.style.height = '0'
  iframe.style.border = '0'
  iframe.src = url
  iframe.onload = () => {
    iframe.contentWindow?.focus()
    iframe.contentWindow?.print()
    setTimeout(() => {
      URL.revokeObjectURL(url)
      iframe.remove()
    }, 60000)
  }
  document.body.appendChild(iframe)
}

async function downloadPdf() {
  const blob = await fetchPassbookPdf()
  if (!blob) return
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `e-passbook-${selectedAccount.value.accountNumber}.pdf`
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

async function copyAccount() {
  if (!selectedAccount.value?.accountNumber) return
  await navigator.clipboard.writeText(selectedAccount.value.accountNumber)
  message.success('已複製帳號')
}
</script>

<style scoped>
.passbook-page {
  max-width: 1120px;
  margin: 0 auto;
  padding: 24px 16px 48px;
  color: var(--text-primary);
}

.passbook-breadcrumb {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 44px;
  color: var(--text-secondary);
  font-size: 14px;
}

.passbook-breadcrumb strong {
  color: var(--primary-dark);
  font-weight: 600;
}

.chevron {
  color: var(--text-disabled);
}

.passbook-heading {
  display: grid;
  justify-items: center;
  gap: 12px;
  margin-bottom: 32px;
  text-align: center;
}

.passbook-heading h1 {
  font-family: var(--font-heading);
  font-size: 40px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: 0;
  color: var(--text-primary);
}

.passbook-heading p {
  color: var(--text-secondary);
  font-size: 15px;
}

.account-select {
  display: grid;
  grid-template-columns: auto minmax(280px, 430px);
  align-items: center;
  gap: 16px;
  width: min(560px, 100%);
  margin-top: 12px;
  padding: 10px 14px;
  background: rgba(255, 249, 239, 0.72);
  border: 1px solid var(--border);
  border-radius: 8px;
  text-align: left;
}

.account-select span {
  color: var(--text-secondary);
  font-size: 14px;
  white-space: nowrap;
}

.passbook-cover {
  position: relative;
  overflow: hidden;
  width: min(100%, 980px);
  min-height: 430px;
  margin: 0 auto;
  padding: 52px 56px 44px;
  background:
    linear-gradient(120deg, rgba(255, 249, 239, 0.94), rgba(249, 244, 235, 0.86)),
    url('/washi-texture.png');
  background-size: auto, 320px 320px;
  border: 1px solid rgba(214, 206, 195, 0.92);
  border-radius: 16px;
  box-shadow: 0 18px 44px rgba(63, 74, 66, 0.18);
}

.passbook-cover::before,
.passbook-cover::after {
  content: '';
  position: absolute;
  pointer-events: none;
}

.passbook-cover::before {
  right: -42px;
  top: 42px;
  width: 330px;
  height: 330px;
  border: 34px solid rgba(92, 107, 95, 0.13);
  border-left-color: transparent;
  border-bottom-color: transparent;
  border-radius: 50%;
  transform: rotate(-18deg);
}

.passbook-cover::after {
  right: -20px;
  bottom: -80px;
  width: 520px;
  height: 240px;
  background:
    radial-gradient(ellipse at center, rgba(92, 107, 95, 0.14), rgba(92, 107, 95, 0) 64%),
    linear-gradient(155deg, transparent 20%, rgba(92, 107, 95, 0.1) 21%, rgba(92, 107, 95, 0.1) 38%, transparent 39%);
}

.brand-block {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 20px;
}

.brand-mark {
  width: 120px;
  height: 80px;
  object-fit: contain;
}

.brand-title {
  font-family: var(--font-display);
  font-size: 31px;
  line-height: 1;
  letter-spacing: 0;
}

.brand-subtitle {
  margin-top: 10px;
  font-family: var(--font-display);
  font-size: 18px;
  letter-spacing: 0;
}

.brand-motto {
  margin-top: 8px;
  color: var(--text-secondary);
  font-size: 13px;
}

.cover-divider {
  position: relative;
  z-index: 1;
  height: 1px;
  margin: 36px 0 30px;
  background: rgba(183, 173, 158, 0.72);
}

.cover-grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 88px;
  max-width: 760px;
}

.info-row {
  display: grid;
  grid-template-columns: 120px 1fr;
  align-items: center;
  min-height: 44px;
  border-bottom: 1px dashed rgba(183, 173, 158, 0.58);
}

.info-row span {
  color: var(--text-primary);
  font-weight: 600;
}

.info-row strong {
  color: var(--text-primary);
  font-size: 17px;
  font-weight: 500;
  text-align: right;
}

.currency-pill {
  justify-self: end;
  width: fit-content;
  padding: 3px 14px;
  color: #fff !important;
  background: var(--primary);
  border-radius: 999px;
  font-size: 14px !important;
}

.status-text {
  color: var(--primary) !important;
}

.cover-note {
  position: relative;
  z-index: 1;
  margin-top: 42px;
  color: var(--text-secondary);
  font-size: 12px;
}

.seal {
  position: absolute;
  right: 48px;
  bottom: 42px;
  z-index: 1;
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  color: var(--accent);
  border: 2px solid rgba(166, 90, 77, 0.72);
  font-family: var(--font-heading);
  font-size: 13px;
  line-height: 1.15;
}

.passbook-actions {
  display: flex;
  justify-content: center;
  gap: 24px;
  flex-wrap: wrap;
  margin-top: 34px;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-width: 160px;
  min-height: 52px;
  padding: 0 22px;
  color: var(--text-primary);
  background: rgba(255, 249, 239, 0.78);
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:hover {
  border-color: var(--primary);
  background: rgba(92, 107, 95, 0.08);
}

.action-btn:disabled {
  opacity: 0.58;
  cursor: wait;
}

.action-btn.primary {
  color: #fff;
  background: var(--primary);
  border-color: var(--primary);
}

.action-btn.primary:hover {
  background: var(--primary-dark);
}

@media (max-width: 760px) {
  .passbook-page {
    padding: 16px 0 40px;
  }

  .passbook-breadcrumb {
    margin-bottom: 28px;
    padding: 0 12px;
  }

  .passbook-heading h1 {
    font-size: 32px;
  }

  .account-select {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .passbook-cover {
    padding: 34px 22px 32px;
    border-radius: 12px;
  }

  .brand-mark {
    width: 96px;
  }

  .brand-title {
    font-size: 24px;
  }

  .cover-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .info-row {
    grid-template-columns: 96px 1fr;
  }

  .passbook-actions {
    gap: 12px;
  }

  .action-btn {
    width: 100%;
    min-width: 0;
  }
}

@media print {
  .passbook-breadcrumb,
  .passbook-heading,
  .passbook-actions {
    display: none;
  }

  .passbook-page {
    padding: 0;
  }

  .passbook-cover {
    box-shadow: none;
  }
}
</style>
