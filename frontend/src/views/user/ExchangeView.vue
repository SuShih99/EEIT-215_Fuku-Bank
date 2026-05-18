<template>
  <div class="exchange-page">
    <section class="exchange-header">
      <div>
        <p class="eyebrow">轉帳匯款</p>
        <h2>換匯</h2>
      </div>
      <button class="refresh-btn" type="button" :disabled="rateLoading" @click="refreshRates">
        <span :class="{ spinning: rateLoading }">↻</span>
        更新匯率
      </button>
    </section>

    <section class="exchange-shell">
      <a-form
        class="exchange-form"
        :model="form"
        layout="vertical"
        @finish="handleExchange"
      >
        <div class="form-grid">
          <a-form-item label="轉出帳戶" name="fromAccountNumber" :rules="[{ required: true, message: '請選擇轉出帳戶' }]">
            <a-select
              v-model:value="form.fromAccountNumber"
              placeholder="選擇扣款帳戶"
              :options="fromAccountOptions"
              @change="handleFromChange"
            />
          </a-form-item>

          <a-form-item label="轉入帳戶" name="toAccountNumber" :rules="[{ required: true, message: '請選擇轉入帳戶' }]">
            <a-select
              v-model:value="form.toAccountNumber"
              placeholder="選擇入帳帳戶"
              :options="toAccountOptions"
              @change="handleToChange"
            />
          </a-form-item>
        </div>

        <a-form-item label="換匯金額" name="fromAmount" :rules="[{ required: true, message: '請輸入換匯金額' }]">
          <a-input-number
            v-model:value="form.fromAmount"
            :min="minimumAmount"
            :max="selectedFromBalance || 999999999"
            :precision="selectedFromCurrency === 'JPY' ? 0 : 2"
            style="width: 100%"
            placeholder="請輸入轉出金額"
            :formatter="v => `${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')"
            :parser="v => v.replace(/,/g, '')"
          />
          <div class="hint" v-if="selectedFromAccount">
            可用餘額：{{ formatAmount(selectedFromBalance, selectedFromCurrency) }}
          </div>
        </a-form-item>

        <div class="quote-card">
          <div class="quote-row">
            <span>成交匯率</span>
            <strong>{{ rateText }}</strong>
          </div>
          <div class="quote-row">
            <span>轉出金額</span>
            <strong>{{ formatAmount(form.fromAmount, selectedFromCurrency) }}</strong>
          </div>
          <div class="quote-row total">
            <span>預計入帳</span>
            <strong>{{ formatAmount(estimatedToAmount, selectedToCurrency) }}</strong>
          </div>
          <p class="quote-time">資料時間：{{ exchangeTime }}</p>
        </div>

        <a-form-item label="備註">
          <a-input v-model:value="form.note" :maxlength="100" placeholder="選填，例如：旅費換匯" />
        </a-form-item>

        <a-button
          type="primary"
          html-type="submit"
          block
          size="large"
          :loading="submitting"
          :disabled="!canSubmit"
        >
          確認換匯
        </a-button>
      </a-form>

      <aside class="rate-panel" :class="{ breathing: rateBreathing }">
        <div class="panel-title">常用匯率</div>
        <div v-if="rateBreathing" class="rate-loading-breath">
          <span class="breath-orb" aria-hidden="true"></span>
          <strong>匯率更新中</strong>
        </div>
        <div v-if="rateRows.length === 0" class="empty-rate">尚無匯率資料</div>
        <div v-for="row in rateRows" :key="row.code" class="rate-row">
          <span>{{ row.name }} {{ row.code }}</span>
          <strong>{{ row.twdText }}</strong>
        </div>
      </aside>
    </section>

    <a-modal v-model:open="showResult" title="換匯結果" :footer="null" @cancel="closeResult">
      <a-result :status="resultStatus" :title="resultTitle" :sub-title="resultSub">
        <template #extra>
          <a-button type="primary" @click="closeResult">再換一筆</a-button>
          <a-button @click="$router.push({ name: 'user-transactions' })">查看紀錄</a-button>
        </template>
      </a-result>
    </a-modal>

    <!-- 無外幣帳戶提示 modal -->
    <transition name="modal-fade">
      <div v-if="showNoForeignModal" class="jb-modal-overlay">
        <div class="jb-modal jb-card">
          <h3 class="jb-modal-title">尚未擁有外幣帳戶</h3>
          <p class="jb-modal-content">您目前沒有任何外幣存款帳戶，無法使用換匯服務。<br/>請先前往開戶申請頁面申請外幣活期存款帳戶。</p>
          <div class="jb-modal-actions">
            <button class="jb-btn jb-btn-secondary" @click="goHome">返回首頁</button>
            <button class="jb-btn jb-btn-primary" @click="goToApply">前往申請</button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { doExchange, getExchangeRates, getMyAccounts } from '@/api/customerAccount'

const router = useRouter()

const currencyNames = {
  TWD: '臺幣',
  USD: '美元',
  EUR: '歐元',
  JPY: '日幣',
  GBP: '英鎊',
  CNY: '人民幣',
  AUD: '澳幣',
  CAD: '加幣',
  CHF: '瑞士法郎',
  HKD: '港幣',
}

const form = reactive({
  fromAccountNumber: undefined,
  toAccountNumber: undefined,
  fromAmount: null,
  note: '',
})

const accounts = ref([])
const rates = ref({})
const exchangeTime = ref('-')
const rateLoading = ref(false)
const rateBreathing = ref(false)
const submitting = ref(false)
const showResult = ref(false)
const resultStatus = ref('success')
const resultTitle = ref('')
const resultSub = ref('')
const showNoForeignModal = ref(false)

const exchangeAccounts = computed(() =>
  accounts.value.filter(a => a.status === 'ACTIVE' && a.accountType !== 'LOAN'),
)
const selectedFromAccount = computed(() => exchangeAccounts.value.find(a => a.accountNumber === form.fromAccountNumber))
const selectedToAccount = computed(() => exchangeAccounts.value.find(a => a.accountNumber === form.toAccountNumber))
const selectedFromCurrency = computed(() => selectedFromAccount.value?.currency || '-')
const selectedToCurrency = computed(() => selectedToAccount.value?.currency || '-')
const selectedFromBalance = computed(() => Number(selectedFromAccount.value?.balance || 0))
const minimumAmount = computed(() => selectedFromCurrency.value === 'JPY' ? 1 : 0.01)

const fromAccountOptions = computed(() => exchangeAccounts.value.map(accountOption))
const toAccountOptions = computed(() =>
  exchangeAccounts.value
    .filter(a => a.accountNumber !== form.fromAccountNumber && isSupportedCurrencyPair(selectedFromCurrency.value, a.currency))
    .map(accountOption),
)

const exchangeRate = computed(() => {
  if (!selectedFromAccount.value || !selectedToAccount.value) return null
  const fromRate = selectedFromCurrency.value === 'TWD' ? 1 : Number(rates.value[selectedFromCurrency.value])
  const toRate = selectedToCurrency.value === 'TWD' ? 1 : Number(rates.value[selectedToCurrency.value])
  if (!fromRate || !toRate) return null
  return toRate / fromRate
})

const estimatedToAmount = computed(() => {
  const amount = Number(form.fromAmount || 0)
  if (!exchangeRate.value || amount <= 0) return 0
  return roundByCurrency(amount * exchangeRate.value, selectedToCurrency.value)
})

const rateText = computed(() => {
  if (!exchangeRate.value || !selectedFromAccount.value || !selectedToAccount.value) return '-'
  return `1 ${selectedFromCurrency.value} = ${exchangeRate.value.toFixed(6)} ${selectedToCurrency.value}`
})

const canSubmit = computed(() =>
  form.fromAccountNumber &&
  form.toAccountNumber &&
  Number(form.fromAmount) > 0 &&
  exchangeRate.value &&
  isSupportedCurrencyPair(selectedFromCurrency.value, selectedToCurrency.value) &&
  Number(form.fromAmount) <= selectedFromBalance.value,
)

const rateRows = computed(() => ['USD', 'JPY', 'EUR', 'GBP', 'CNY']
  .map(code => {
    const twdRate = code === 'TWD' ? 1 : rates.value[code] ? 1 / Number(rates.value[code]) : null
    return {
      code,
      name: currencyNames[code] || code,
      twdText: twdRate ? `1 ${code} = ${twdRate.toFixed(4)} TWD` : '-',
    }
  }))

onMounted(async () => {
  await Promise.all([loadAccounts(), loadRates()])

  // 檢查是否有外幣帳戶（非 TWD 且 ACTIVE）
  const hasForeignAccount = accounts.value.some(
    a => a.status === 'ACTIVE' && a.currency !== 'TWD' && a.accountType !== 'LOAN'
  )
  if (!hasForeignAccount) {
    showNoForeignModal.value = true
    return
  }

  pickDefaultAccounts()
})

function goToApply() {
  router.push({ name: 'user-account-application' })
}

function goHome() {
  router.push({ name: 'user-home' })
}

async function loadAccounts() {
  try {
    accounts.value = await getMyAccounts()
  } catch (e) {
    message.error(e?.response?.data?.message || '帳戶資料讀取失敗')
  }
}

async function refreshRates() {
  await loadRates({ breathe: true })
}

async function loadRates({ breathe = false } = {}) {
  rateLoading.value = true
  if (breathe) {
    rateBreathing.value = true
  }
  const minimumBreath = breathe ? wait(800) : Promise.resolve()
  try {
    const data = await getExchangeRates()
    rates.value = data.rates || {}
    const d = new Date()
    exchangeTime.value = `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  } catch (e) {
    message.error(e?.response?.data?.message || '匯率資料讀取失敗')
  } finally {
    await minimumBreath
    rateLoading.value = false
    if (breathe) {
      rateBreathing.value = false
    }
  }
}

function wait(ms) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

function accountOption(a) {
  return {
    value: a.accountNumber,
    label: `${a.accountNumber} — ${currencyNames[a.currency] || a.currency} ${formatPlain(a.balance)}`,
  }
}

function pickDefaultAccounts() {
  const twdAccount = exchangeAccounts.value.find(a => a.currency === 'TWD')
  const foreignAccount = exchangeAccounts.value.find(a => a.currency !== 'TWD')
  form.fromAccountNumber = twdAccount?.accountNumber || exchangeAccounts.value[0]?.accountNumber
  form.toAccountNumber = foreignAccount?.accountNumber || exchangeAccounts.value.find(a => a.accountNumber !== form.fromAccountNumber)?.accountNumber
}

function handleFromChange() {
  if (
    form.toAccountNumber === form.fromAccountNumber ||
    !isSupportedCurrencyPair(selectedFromCurrency.value, selectedToCurrency.value)
  ) {
    form.toAccountNumber = toAccountOptions.value[0]?.value
  }
}

function handleToChange() {
  if (form.toAccountNumber === form.fromAccountNumber) {
    form.toAccountNumber = undefined
  }
}

function isSupportedCurrencyPair(fromCurrency, toCurrency) {
  if (!fromCurrency || !toCurrency || fromCurrency === '-' || toCurrency === '-') return false
  if (fromCurrency === toCurrency) return false
  return fromCurrency === 'TWD' || toCurrency === 'TWD'
}

async function handleExchange() {
  if (!canSubmit.value) {
    message.error('請確認帳戶、金額與可用餘額')
    return
  }

  submitting.value = true
  try {
    const result = await doExchange({
      fromAccountNumber: form.fromAccountNumber,
      toAccountNumber: form.toAccountNumber,
      fromAmount: form.fromAmount,
      note: form.note || undefined,
    })
    await loadAccounts()
    resultStatus.value = 'success'
    resultTitle.value = '換匯成功'
    resultSub.value = `已扣款 ${formatAmount(result.fromAmount, result.fromCurrency)}，入帳 ${formatAmount(result.toAmount, result.toCurrency)}`
    showResult.value = true
    form.fromAmount = null
    form.note = ''
  } catch (e) {
    resultStatus.value = 'error'
    resultTitle.value = '換匯失敗'
    resultSub.value = e?.response?.data?.message || '系統錯誤，請稍後再試'
    showResult.value = true
  } finally {
    submitting.value = false
  }
}

function closeResult() {
  showResult.value = false
}

function roundByCurrency(value, currency) {
  const digits = currency === 'JPY' ? 0 : 2
  return Number(value || 0).toFixed(digits)
}

function formatAmount(value, currency) {
  if (!currency || currency === '-') return '-'
  const digits = currency === 'JPY' ? 0 : 2
  return `${Number(value || 0).toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits })} ${currency}`
}

function formatPlain(value) {
  return Number(value || 0).toLocaleString('en-US', { maximumFractionDigits: 2 })
}
</script>

<style scoped>
.exchange-page {
  max-width: 1080px;
  margin: 0 auto;
  padding: 28px 24px 48px;
}

.exchange-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
}

.eyebrow {
  margin: 0 0 6px;
  color: var(--primary);
  font-size: 14px;
  font-weight: 700;
}

h2 {
  margin: 0;
  color: var(--text-primary);
  font-family: var(--font-heading);
  font-size: 42px;
}

.exchange-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 24px;
}

.exchange-shell > form,
.rate-panel {
  background: rgba(255, 249, 239, 0.9);
  border: 1px solid rgba(214, 206, 195, 0.9);
  border-radius: 16px;
  box-shadow: 0 14px 36px rgba(63, 74, 66, 0.08);
}

.exchange-shell > form {
  padding: 28px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.hint {
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 12px;
}

.quote-card {
  display: grid;
  gap: 10px;
  margin: 4px 0 24px;
  padding: 18px;
  background: rgba(250, 250, 247, 0.82);
  border: 1px solid rgba(214, 206, 195, 0.86);
  border-radius: 12px;
}

.quote-row,
.rate-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.quote-row {
  color: var(--text-secondary);
  font-size: 14px;
}

.quote-row strong {
  color: var(--text-primary);
}

.quote-row.total {
  padding-top: 10px;
  border-top: 1px solid rgba(214, 206, 195, 0.8);
  color: var(--primary-dark);
}

.quote-row.total strong {
  color: var(--primary-dark);
  font-size: 18px;
}

.quote-time {
  margin: 2px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
}

.rate-panel {
  position: relative;
  overflow: hidden;
  align-self: start;
  padding: 22px;
}

.rate-panel.breathing {
  animation: exchangeBreath 1.55s ease-in-out infinite;
}

.rate-loading-breath {
  position: absolute;
  inset: 0;
  z-index: 4;
  display: grid;
  place-items: center;
  gap: 10px;
  color: var(--primary-dark);
  text-align: center;
  background: rgba(255, 249, 239, 0.68);
  backdrop-filter: blur(1px);
}

.rate-loading-breath strong {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 700;
}

.breath-orb {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  border: 1px solid rgba(92, 107, 95, 0.28);
  background: radial-gradient(circle, rgba(92, 107, 95, 0.28), rgba(92, 107, 95, 0.06) 64%, transparent 68%);
  animation: breathOrb 1.55s ease-in-out infinite;
}

.panel-title {
  margin-bottom: 16px;
  color: var(--text-primary);
  font-weight: 700;
}

.rate-row {
  padding: 13px 0;
  border-top: 1px solid rgba(214, 206, 195, 0.72);
  color: var(--text-secondary);
  font-size: 14px;
}

.rate-row strong {
  color: var(--text-primary);
  font-weight: 600;
}

.empty-rate {
  padding: 28px 0;
  color: var(--text-secondary);
  text-align: center;
}

.refresh-btn {
  height: 42px;
  padding: 0 16px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--primary-dark);
  background: rgba(255, 249, 239, 0.7);
  border: 1px solid var(--border);
  border-radius: 10px;
  cursor: pointer;
}

.refresh-btn:disabled {
  cursor: wait;
  opacity: 0.7;
}

.spinning {
  display: inline-block;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes exchangeBreath {
  0%, 100% {
    box-shadow: 0 14px 36px rgba(63, 74, 66, 0.08);
  }

  50% {
    box-shadow: 0 18px 46px rgba(92, 107, 95, 0.18);
  }
}

@keyframes breathOrb {
  0%, 100% {
    opacity: 0.48;
    transform: scale(0.82);
  }

  50% {
    opacity: 1;
    transform: scale(1);
  }
}

/* === Modal Styles === */
.jb-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(43, 43, 43, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}
.jb-modal {
  width: 90%;
  max-width: 400px;
  padding: var(--space-6);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  border-radius: 12px;
  background: rgba(255, 249, 239, 0.96);
  border: 1px solid rgba(214, 206, 195, 0.86);
  box-shadow: 0 12px 36px rgba(63, 74, 66, 0.12);
}
.jb-modal-title {
  font-family: var(--font-heading);
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}
.jb-modal-content {
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.6;
}
.jb-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  margin-top: var(--space-2);
}
.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.2s var(--ease); }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
.jb-btn {
  padding: 8px 16px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s;
}
.jb-btn-primary {
  background: var(--primary);
  color: white;
}
.jb-btn-primary:hover {
  background: var(--primary-dark);
}
.jb-btn-secondary {
  background: transparent;
  color: var(--text-secondary);
  border: 1px solid var(--border);
}
.jb-btn-secondary:hover {
  border-color: var(--text-primary);
  color: var(--text-primary);
}

@media (max-width: 860px) {
  .exchange-page {
    padding: 20px 0 36px;
  }

  .exchange-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .exchange-shell,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
