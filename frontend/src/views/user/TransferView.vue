<template>
  <div class="transfer-page">
    <h2>轉帳匯款</h2>
    <div style="display: flex; gap: 10px; margin-bottom: 20px;">
      <a-button type="primary" class="rounded-btn" style="margin-bottom: 0 !important;" @click="demoTransfer">示範轉帳1000</a-button>
      <a-button type="primary" danger class="rounded-btn" style="margin-bottom: 0 !important;" @click="riskTestTransfer">連續轉帳測試 (觸發風險)</a-button>
    </div>

    <a-card class="transfer-form-card" v-show="step === 'form'">
      <a-form :model="form" layout="vertical" @finish="handleTransfer">
        <a-form-item
          label="轉出帳戶"
          name="fromAccount"
          :rules="[{ required: true, message: '請選擇轉出帳戶' }]"
        >
          <a-select
            v-model:value="form.fromAccount"
            placeholder="選擇轉出帳戶"
            @change="onFromChange"
          >
            <a-select-option
              v-for="a in twdAccounts"
              :key="a.accountNumber"
              :value="a.accountNumber"
            >
              {{ transferAccountLabel(a) }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item
          label="轉入銀行"
          name="toBankCode"
          :rules="[{ required: true, message: '請選擇轉入銀行' }]"
        >
          <a-select
            v-model:value="form.toBankCode"
            show-search
            placeholder="搜尋或選擇銀行"
            :loading="bankLoading"
            :options="bankSelectOptions"
            option-filter-prop="label"
          />
        </a-form-item>

        <a-form-item
          label="轉入帳號"
          name="toAccount"
          :rules="[{ required: true, message: '請輸入轉入帳號' }]"
        >
          <div class="account-input-row">
            <a-input
              v-model:value="form.toAccount"
              :maxlength="20"
              :placeholder="toAccountPlaceholder"
              inputmode="numeric"
              @input="normalizeToAccount"
            />
            <a-button @click="showFavorites = true">常用帳號</a-button>
          </div>
          <div class="hint">轉入帳號限 6 到 20 碼數字，本行帳號為 12 碼。</div>
        </a-form-item>

        <a-form-item
          label="轉帳金額"
          name="amount"
          :rules="[{ required: true, message: '請輸入金額' }]"
        >
          <a-input-number
            v-model:value="form.amount"
            :min="1"
            :max="maxAmount"
            style="width: 100%"
            :formatter="formatAmountInput"
            :parser="parseAmountInput"
            placeholder="請輸入金額"
          />
          <div class="hint" v-if="selectedBalance !== null">
            可用餘額：{{ formatNum(selectedBalance) }} TWD
          </div>
        </a-form-item>

        <div class="fee-preview" v-if="Number(form.amount) > 0">
          <div class="fee-row">
            <span>轉帳金額</span>
            <strong>{{ formatNum(form.amount) }} TWD</strong>
          </div>
          <div class="fee-row">
            <span>跨行手續費</span>
            <strong>{{ formatNum(feeAmount) }} TWD</strong>
          </div>
          <div class="fee-row total">
            <span>預計扣款金額</span>
            <strong>{{ formatNum(totalDebitAmount) }} TWD</strong>
          </div>
        </div>

        <a-form-item label="備註">
          <a-input
            v-model:value="form.note"
            placeholder="選填，例如：房租、貨款"
            :maxlength="100"
          />
        </a-form-item>

        <a-form-item>
          <a-button type="primary" html-type="submit" :loading="submitting" block size="large">
            下一步 (發送驗證碼)
          </a-button>
          <a-button @click="handleTransferBypassOtp" html-type="button" :loading="submitting" block size="large" style="margin-top: 10px;">
            直接轉帳（免驗證碼，For Demo）
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <a-modal v-model:open="showOtpModal" title="OTP 驗證碼" :footer="null" :closable="false" :maskClosable="false">
      <p>系統已發送一封包含 6 位數 OTP 驗證碼的信件至您的電子信箱，請輸入該驗證碼以完成轉帳交易。</p>
      <div style="display: flex; gap: 10px; margin-bottom: 20px;">
        <a-input v-model:value="otpCode" placeholder="請輸入 6 位數 OTP" :maxlength="6" />
        <a-button @click="fillDemoOtp" type="dashed">一鍵帶入剛寄出的 OTP</a-button>
      </div>
      <a-button type="primary" :loading="submitting" @click="submitOtpAndTransfer" block size="large">
        驗證並送出轉帳
      </a-button>
      <a-button type="link" @click="cancelOtp" block style="margin-top: 10px;">
        取消並回上一步修改
      </a-button>
    </a-modal>

    <a-card v-if="step === 'success'" class="transfer-form-card result-card">
      <div class="result-icon">
        <check-circle-filled style="color: #5b6b55; font-size: 64px;" />
      </div>
      <h3 class="result-title">{{ resultTitle }}</h3>
      <p class="result-message">{{ resultSub }}</p>
      
      <div class="result-actions">
        <a-button type="primary" size="large" @click="resetForm" style="min-width: 140px;">
          {{ resultPrimaryText }}
        </a-button>
        <a-button size="large" @click="$router.push({ name: 'user-transactions' })" style="min-width: 140px;">
          查看紀錄
        </a-button>
      </div>
    </a-card>



    <a-modal v-model:open="showFavorites" title="常用帳號" :footer="null" width="500px">
      <div v-if="favorites.length === 0 && !favLoading" class="empty-favorites">
        尚無常用帳號，請至「常用帳號管理」新增
      </div>
      <a-list v-else :data-source="favorites" :loading="favLoading">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta :title="item.alias" :description="favoriteDescription(item)" />
            <template #actions>
              <a-button size="small" type="link" @click="selectFavorite(item)">選用</a-button>
            </template>
          </a-list-item>
        </template>
      </a-list>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { CheckCircleFilled } from '@ant-design/icons-vue'
import { getMyAccounts, doTransfer, getTransferBanks, requestTransferOtp } from '@/api/customerAccount'
import { getFavoriteAccounts } from '@/api/favoriteAccount'

const JAVA_BANK_CODE = '909'
const fallbackBanks = [{ code: JAVA_BANK_CODE, name: '福庫銀行', label: '福庫銀行 909' }]

const route = useRoute()
const form = ref({
  fromAccount: undefined,
  toBankCode: JAVA_BANK_CODE,
  toAccount: '',
  amount: null,
  note: '',
})
const accounts = ref([])
const banks = ref(fallbackBanks)
const favorites = ref([])
const favLoading = ref(false)
const bankLoading = ref(false)
const submitting = ref(false)
const showFavorites = ref(false)
const showOtpModal = ref(false)

const step = ref('form') // 'form', 'success'
const otpCode = ref('')
const demoOtp = ref('')

const resultStatus = ref('success')
const resultTitle = ref('')
const resultSub = ref('')
const selectedBalance = ref(null)
const accountTypeLabels = {
  CHECKING: '活期存款',
  SAVINGS: '儲蓄存款',
  TIME_DEPOSIT: '定期存款',
  SUB_ACCOUNT: '子帳戶',
  CREDIT_CARD: '信用卡帳戶',
}

const twdAccounts = computed(() =>
  accounts.value.filter(
    (a) => a.currency === 'TWD' && a.status === 'ACTIVE' && a.accountType !== 'LOAN',
  ),
)
const maxAmount = computed(() => selectedBalance.value || 999999999)
const selectedBank = computed(
  () => banks.value.find((bank) => bank.code === form.value.toBankCode) || fallbackBanks[0],
)
const isInterbank = computed(() => form.value.toBankCode !== JAVA_BANK_CODE)
const feeAmount = computed(() => {
  const amount = Number(form.value.amount || 0)
  if (!isInterbank.value || amount <= 0) return 0
  return amount <= 1000 ? 10 : 15
})
const totalDebitAmount = computed(() => Number(form.value.amount || 0) + feeAmount.value)
const toAccountPlaceholder = computed(() =>
  isInterbank.value ? '請輸入 6 到 20 碼帳號' : '請輸入 12 碼本行帳號',
)
const bankSelectOptions = computed(() =>
  banks.value.map((bank) => ({
    value: bank.code,
    label: bankOptionLabel(bank),
    name: bank.name,
    code: bank.code,
  })),
)
const resultPrimaryText = computed(() => (resultStatus.value === 'error' ? '重新填寫' : '再轉一筆'))

onMounted(async () => {
  await Promise.all([loadAccounts(), loadBanks()])
  if (route.query.from) {
    form.value.fromAccount = route.query.from
    onFromChange(route.query.from)
  }
  // prefill toAccount, toBankCode, amount if provided
  if (route.query.toAccount) {
    form.value.toAccount = route.query.toAccount
    if (route.query.toBankCode) form.value.toBankCode = route.query.toBankCode
    if (route.query.amount) form.value.amount = Number(route.query.amount)
    // normalize after setting
    normalizeToAccount()
  }
  loadFavorites()
})

async function loadAccounts() {
  try {
    accounts.value = await getMyAccounts()
  } catch (e) {
    console.error(e)
  }
}

async function loadBanks() {
  bankLoading.value = true
  try {
    const res = await getTransferBanks()
    banks.value = Array.isArray(res) && res.length ? res : fallbackBanks
  } catch (e) {
    console.error(e)
    banks.value = fallbackBanks
  } finally {
    bankLoading.value = false
  }
}

async function loadFavorites() {
  favLoading.value = true
  try {
    favorites.value = await getFavoriteAccounts()
  } catch (e) {
    console.error(e)
  } finally {
    favLoading.value = false
  }
}

function onFromChange(val) {
  const acct = accounts.value.find((a) => a.accountNumber === val)
  selectedBalance.value = acct ? Number(acct.balance) : null
}

function normalizeToAccount() {
  form.value.toAccount = String(form.value.toAccount || '')
    .replace(/\D/g, '')
    .slice(0, 20)
}

function selectFavorite(item) {
  form.value.toAccount = item.accountNumber
  if (item.bankCode) form.value.toBankCode = item.bankCode
  showFavorites.value = false
}

function favoriteDescription(item) {
  const bank = bankDisplayName(item)
  return bank ? `${bank} ${item.accountNumber}` : item.accountNumber
}

function bankOptionLabel(bank) {
  return bank.label || `${bank.name} ${bank.code}`
}

function bankDisplayName(item) {
  return item.bankName ? `${item.bankName} ${item.bankCode || ''}`.trim() : item.bankCode
}

function parseAmountInput(value) {
  return String(value || '').replace(/[^\d.]/g, '')
}

function formatAmountInput(value) {
  const raw = parseAmountInput(value)
  if (!raw) return ''
  const [integerPart, decimalPart] = raw.split('.')
  const formattedInteger = integerPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return decimalPart !== undefined ? `${formattedInteger}.${decimalPart}` : formattedInteger
}

async function handleTransfer() {
  normalizeToAccount()
  if (!isInterbank.value && form.value.fromAccount === form.value.toAccount) {
    message.error('轉出與轉入帳戶不可相同')
    return
  }
  if (form.value.toAccount.length < 6 || form.value.toAccount.length > 20) {
    message.error('轉入帳號長度須為 6 到 20 碼')
    return
  }
  if (!isInterbank.value && form.value.toAccount.length !== 12) {
    message.error('本行轉帳目的帳號須為 12 碼')
    return
  }
  if (selectedBalance.value !== null && totalDebitAmount.value > selectedBalance.value) {
    message.error(`可用餘額不足，預計扣款 ${formatNum(totalDebitAmount.value)} TWD`)
    return
  }

  submitting.value = true
  try {
    const res = await requestTransferOtp()
    demoOtp.value = res.demoOtp
    otpCode.value = ''
    showOtpModal.value = true
    message.success('已發送 OTP 驗證碼至您的信箱')
  } catch (e) {
    message.error(e?.response?.data?.message || '發送 OTP 失敗')
  } finally {
    submitting.value = false
  }
}

async function handleTransferBypassOtp() {
  normalizeToAccount()
  if (!form.value.fromAccount || !form.value.toAccount || !form.value.amount) {
    message.warning('請先填寫完整轉帳資訊')
    return
  }
  if (!isInterbank.value && form.value.fromAccount === form.value.toAccount) {
    message.error('轉出與轉入帳戶不可相同')
    return
  }
  if (form.value.toAccount.length < 6 || form.value.toAccount.length > 20) {
    message.error('轉入帳號長度須為 6 到 20 碼')
    return
  }
  if (!isInterbank.value && form.value.toAccount.length !== 12) {
    message.error('本行轉帳目的帳號須為 12 碼')
    return
  }
  if (selectedBalance.value !== null && totalDebitAmount.value > selectedBalance.value) {
    message.error(`可用餘額不足，預計扣款 ${formatNum(totalDebitAmount.value)} TWD`)
    return
  }

  otpCode.value = '000000'
  await submitOtpAndTransfer()
}

function fillDemoOtp() {
  if (demoOtp.value) {
    otpCode.value = demoOtp.value
    message.success('已帶入驗證碼')
  } else {
    message.warning('尚未獲取到驗證碼')
  }
}

function cancelOtp() {
  showOtpModal.value = false
  otpCode.value = ''
  demoOtp.value = ''
}

async function submitOtpAndTransfer() {
  if (!otpCode.value || otpCode.value.length !== 6) {
    message.error('請輸入 6 位數 OTP')
    return
  }

  submitting.value = true
  try {
    const result = await doTransfer({
      fromAccountNumber: form.value.fromAccount,
      toBankCode: form.value.toBankCode,
      toAccountNumber: form.value.toAccount,
      amount: form.value.amount,
      note: form.value.note || undefined,
      otp: otpCode.value
    })
    await loadAccounts()
    onFromChange(form.value.fromAccount)
    if (result && result.pending) {
      resultStatus.value = 'warning'
      resultTitle.value = '轉帳審核中'
      resultSub.value =
        result.pendingReason ||
        '您的轉帳交易已妥善受理。為保障您的帳戶資金安全，系統正進行例行的安全覆核程序，詳細的覆核進度已同步寄送至您的電子信箱。您無須重複送出申請，稍後亦可至「交易明細」查詢最終結果。'
    } else {
      resultStatus.value = 'success'
      resultTitle.value = '轉帳成功'
      resultSub.value = `已成功轉帳 ${formatNum(result.amount || form.value.amount)} TWD 至 ${selectedBank.value.name} ${form.value.toAccount}，預計扣款 ${formatNum(result.totalDebitAmount || totalDebitAmount.value)} TWD`
    }
    showOtpModal.value = false
    step.value = 'success'
  } catch (e) {
    message.error(e?.response?.data?.message || '系統錯誤，轉帳失敗')
  } finally {
    submitting.value = false
  }
}

function demoTransfer() {
  const checkingAccount = twdAccounts.value.find(a => a.accountType === 'CHECKING') || twdAccounts.value[0]
  if (checkingAccount) {
    form.value.fromAccount = checkingAccount.accountNumber
    onFromChange(form.value.fromAccount)
    form.value.amount = 1000
    form.value.toAccount = ''
    form.value.toBankCode = undefined
    form.value.note = ''
  } else {
    message.warning('目前沒有可用的帳號供示範')
  }
}

async function riskTestTransfer() {
  const checkingAccount = twdAccounts.value.find(a => a.accountType === 'CHECKING') || twdAccounts.value[0]
  if (!checkingAccount) {
    message.warning('目前沒有可用的帳號供示範')
    return
  }
  const fromAcc = checkingAccount.accountNumber
  const targetAcc = '090483761205' // Hardcoded target
  
  submitting.value = true
  message.loading({ content: '正在執行連續 3 次 1000 元轉帳...', key: 'riskTest' })
  try {
    for (let i = 0; i < 3; i++) {
      await doTransfer({
        fromAccountNumber: fromAcc,
        toBankCode: JAVA_BANK_CODE,
        toAccountNumber: targetAcc,
        amount: 1000,
        note: '連續轉帳測試',
        otp: '000000'
      })
    }
    message.success({ content: '連續轉帳完成！', key: 'riskTest' })
    await loadAccounts()
    onFromChange(form.value.fromAccount)
  } catch (e) {
    message.error({ content: e?.response?.data?.message || '轉帳失敗', key: 'riskTest' })
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  step.value = 'form'
  otpCode.value = ''
  demoOtp.value = ''
  form.value = {
    fromAccount: form.value.fromAccount,
    toBankCode: JAVA_BANK_CODE,
    toAccount: '',
    amount: null,
    note: '',
  }
}


function formatNum(v) {
  return Number(v || 0).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

function accountTypeLabel(type) {
  return accountTypeLabels[type] || type || '帳戶'
}

function transferAccountLabel(account) {
  return `${account.accountNumber} — ${accountTypeLabel(account.accountType)} — 餘額 ${formatNum(account.balance)} ${account.currency || 'TWD'}`
}
</script>

<style scoped>
.transfer-page {
  max-width: 640px;
  margin: 0 auto;
  padding: 24px;
}

h2 {
  margin-bottom: 22px;
  color: var(--text-primary);
  font-family: var(--font-heading);
  font-size: 40px;
  font-weight: 700;
  line-height: 1.2;
}

.transfer-page > .rounded-btn {
  margin-bottom: 22px !important;
}

.transfer-form-card {
  border-radius: 12px;
  background: rgba(255, 249, 239, 0.92);
  border: 1px solid rgba(214, 206, 195, 0.86);
  box-shadow: 0 12px 36px rgba(63, 74, 66, 0.08);
}

.transfer-form-card :deep(.ant-card-body) {
  padding: 28px;
}

.transfer-form-card :deep(.ant-form-item) {
  margin-bottom: 22px;
}

.transfer-form-card :deep(.ant-form-item-label) {
  padding-bottom: 8px;
}

.transfer-form-card :deep(.ant-form-item-label > label) {
  font-size: 16px;
  line-height: 1.45;
}

.transfer-form-card :deep(.ant-input),
.transfer-form-card :deep(.ant-input-number),
.transfer-form-card :deep(.ant-select-selector) {
  min-height: 48px;
  font-size: 16px;
}

.transfer-form-card :deep(.ant-input),
.transfer-form-card :deep(.ant-input-number-input),
.transfer-form-card :deep(.ant-select-selection-item),
.transfer-form-card :deep(.ant-select-selection-placeholder) {
  font-size: 16px;
}

.transfer-form-card :deep(.ant-input-number) {
  display: flex;
  align-items: center;
}

.transfer-form-card :deep(.ant-input-number-input-wrap) {
  width: 100%;
}

.transfer-form-card :deep(.ant-input-number-input) {
  height: 46px;
  line-height: 46px;
}

.transfer-form-card :deep(.ant-select-single .ant-select-selector) {
  align-items: center;
}

.transfer-form-card :deep(.ant-select-arrow) {
  font-size: 16px;
}

.transfer-form-card :deep(.ant-btn) {
  min-height: 44px;
  font-size: 16px;
}

.transfer-form-card :deep(.ant-btn-lg) {
  min-height: 48px;
  font-size: 16px;
}

.account-input-row {
  display: flex;
  gap: 8px;
}

.account-input-row :deep(.ant-input) {
  flex: 1;
}

.hint {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-secondary);
}

.fee-preview {
  display: grid;
  gap: 10px;
  margin: 0 0 22px;
  padding: 16px;
  border: 1px solid rgba(214, 206, 195, 0.86);
  border-radius: 8px;
  background: rgba(250, 250, 247, 0.84);
}

.fee-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--text-secondary);
  font-size: 15px;
}

.fee-row strong {
  color: var(--text-primary);
  font-weight: 600;
}

.fee-row.total {
  padding-top: 8px;
  border-top: 1px solid rgba(214, 206, 195, 0.8);
  color: var(--primary-dark);
}

.fee-row.total strong {
  color: var(--primary-dark);
  font-size: 17px;
}

.empty-favorites {
  text-align: center;
  color: #999;
  padding: 32px 20px;
}

.jb-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(43, 43, 43, 0.4);
  backdrop-filter: blur(4px);
}

.jb-modal {
  width: min(90vw, 520px);
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  text-align: center;
  background: rgba(255, 249, 239, 0.96);
  border: 1px solid rgba(214, 206, 195, 0.86);
  border-radius: 12px;
  box-shadow: 0 12px 36px rgba(63, 74, 66, 0.12);
}

.transfer-result-modal {
  min-height: 300px;
  justify-content: center;
}

.jb-modal-title {
  margin: 0;
  font-family: var(--font-heading);
  font-size: 24px;
  font-weight: 700;
  line-height: 1.35;
  color: var(--text-primary);
}

.jb-modal-content {
  margin: 0;
  white-space: pre-line;
  font-size: 16px;
  line-height: 1.65;
  color: var(--text-secondary);
}

.jb-modal-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}

.jb-modal-actions .jb-btn {
  min-width: 136px;
  font-size: 15px;
  font-weight: 600;
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.2s var(--ease);
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

@media (max-width: 640px) {
  .transfer-page {
    padding: 16px 0;
  }

  h2 {
    font-size: 34px;
    margin-bottom: 20px;
  }

  .account-input-row {
    flex-direction: column;
  }

  .transfer-form-card :deep(.ant-card-body) {
    padding: 20px 16px;
  }

  .transfer-form-card :deep(.ant-form-item-label > label),
  .transfer-form-card :deep(.ant-input),
  .transfer-form-card :deep(.ant-input-number-input),
  .transfer-form-card :deep(.ant-select-selection-item),
  .transfer-form-card :deep(.ant-select-selection-placeholder) {
    font-size: 15px;
  }

  .jb-modal {
    width: 100%;
    padding: 36px 20px;
  }

  .jb-modal-title {
    font-size: 22px;
  }

  .jb-modal-content {
    font-size: 15px;
  }

  .jb-modal-actions .jb-btn {
    width: 100%;
  }
}

.result-card {
  text-align: center;
  padding: 40px 20px;
}

.result-icon {
  margin-bottom: 24px;
}

.result-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 16px;
  font-family: var(--font-heading);
}

.result-message {
  font-size: 16px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 32px;
  max-width: 480px;
  margin-left: auto;
  margin-right: auto;
}

.result-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
}

@media (max-width: 640px) {
  .result-actions {
    flex-direction: column;
  }
  .result-actions .ant-btn {
    width: 100%;
  }
}
</style>
