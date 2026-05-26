<template>
  <div class="scheduled-transfer-page">
    <h2>預約轉帳</h2>
    <a-button type="primary" class="rounded-btn" style="margin-bottom: 20px;" @click="demoScheduledTransfer">示範預約1000</a-button>

    <!-- 新增預約 -->
    <a-card class="form-card" title="新增預約轉帳" v-show="step === 'form'">
      <a-form :model="form" layout="vertical" @finish="handleCreate">
        <div class="scheduled-form-grid">
          <div class="form-field from-account-field">
            <a-form-item label="轉出帳戶" name="fromAccount" :rules="[{ required: true, message: '請選擇轉出帳戶' }]">
              <a-select
                v-model:value="form.fromAccount"
                placeholder="選擇轉出帳戶"
                :dropdown-match-select-width="false"
                popup-class-name="scheduled-account-dropdown"
                @change="onFromChange"
              >
                <a-select-option v-for="a in twdAccounts" :key="a.accountNumber" :value="a.accountNumber">
                  {{ scheduledAccountLabel(a) }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </div>
          <div class="form-field">
            <a-form-item label="轉入銀行" name="toBankCode" :rules="[{ required: true, message: '請選擇轉入銀行' }]">
              <a-select
                v-model:value="form.toBankCode"
                show-search
                placeholder="搜尋或選擇銀行"
                :loading="bankLoading"
                :options="bankSelectOptions"
                option-filter-prop="label"
              />
            </a-form-item>
          </div>
          <div class="form-field">
            <a-form-item label="轉入帳號" name="toAccount" :rules="[{ required: true, message: '請輸入轉入帳號' }]">
              <div class="account-input-row">
                <a-input v-model:value="form.toAccount" placeholder="12碼帳號" />
                <a-button @click="showFavPicker = true">常用帳號</a-button>
              </div>
            </a-form-item>
          </div>
          <div class="form-field">
            <a-form-item label="轉帳金額" name="amount" :rules="[{ required: true, message: '請輸入金額' }]">
              <a-input-number v-model:value="form.amount" :min="1" style="width: 100%"
                              :formatter="v => `${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')"
                              :parser="v => v.replace(/,/g, '')" placeholder="金額" />
            </a-form-item>
          </div>
          <div class="form-field">
            <a-form-item label="預約日期" name="scheduledDate" :rules="[{ required: true, message: '請選擇日期' }]">
              <a-date-picker v-model:value="form.scheduledDate" style="width: 100%"
                             :disabled-date="disabledDate" placeholder="選擇日期" />
            </a-form-item>
          </div>
          <div class="form-field note-field">
            <a-form-item label="備註">
              <a-input v-model:value="form.note" placeholder="選填" :maxlength="100" />
            </a-form-item>
          </div>
        </div>

        <a-form-item>
          <a-button type="primary" html-type="submit" :loading="creating" block size="large">
            下一步 (發送驗證碼)
          </a-button>
          <a-button @click="handleCreateBypassOtp" html-type="button" :loading="creating" block size="large" style="margin-top: 10px;">
            直接預約（免驗證碼，For Demo）
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <a-modal v-model:open="showOtpModal" title="OTP 驗證碼" :footer="null" :closable="false" :maskClosable="false">
      <p>系統已發送一封包含 6 位數 OTP 驗證碼的信件至您的電子信箱，請輸入該驗證碼以完成預約轉帳設定。</p>
      <div style="display: flex; gap: 10px; margin-bottom: 20px;">
        <a-input v-model:value="otpCode" placeholder="請輸入 6 位數 OTP" :maxlength="6" />
        <a-button @click="fillDemoOtp" type="dashed">一鍵帶入剛寄出的 OTP</a-button>
      </div>
      <a-button type="primary" :loading="creating" @click="submitOtpAndCreate" block size="large">
        驗證並送出預約
      </a-button>
      <a-button type="link" @click="cancelOtp" block style="margin-top: 10px;">
        取消並回上一步修改
      </a-button>
    </a-modal>

    <a-card v-if="step === 'success'" class="form-card result-card" style="margin-top: 20px;">
      <div class="result-icon">
        <check-circle-filled style="color: #5b6b55; font-size: 64px;" />
      </div>
      <h3 class="result-title">預約轉帳建立成功</h3>
      <p class="result-message">您的預約轉帳已成功設定，將於 {{ form.scheduledDate ? form.scheduledDate.format('YYYY-MM-DD') : '' }} 執行轉帳。</p>
      
      <div class="result-actions">
        <a-button type="primary" size="large" @click="resetForm" style="min-width: 140px;">
          再建立一筆
        </a-button>
      </div>
    </a-card>

    <!-- 預約列表 -->
    <section class="list-card">
      <div class="list-card-title">我的預約轉帳</div>
      <div class="overflow-x-auto rounded-[16px] bg-white/60 p-4">
        <table class="w-full min-w-[1000px] border-collapse text-left text-[14px] text-[var(--text-primary)]">
          <thead class="bg-[rgba(245,241,234,0.84)]">
            <tr>
              <th class="rounded-tl-[10px] border-b border-[rgba(214,206,195,0.72)] px-4 py-3 font-semibold">轉出帳戶</th>
              <th class="border-b border-[rgba(214,206,195,0.72)] px-4 py-3 font-semibold">轉入銀行</th>
              <th class="border-b border-[rgba(214,206,195,0.72)] px-4 py-3 font-semibold">轉入帳戶</th>
              <th class="border-b border-[rgba(214,206,195,0.72)] px-4 py-3 font-semibold">金額</th>
              <th class="border-b border-[rgba(214,206,195,0.72)] px-4 py-3 font-semibold">預約日期</th>
              <th class="border-b border-[rgba(214,206,195,0.72)] px-4 py-3 font-semibold">備註</th>
              <th class="border-b border-[rgba(214,206,195,0.72)] px-4 py-3 font-semibold">狀態</th>
              <th class="rounded-tr-[10px] border-b border-[rgba(214,206,195,0.72)] px-4 py-3 font-semibold">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="8" class="border-b border-[rgba(214,206,195,0.42)] px-4 py-14 text-center text-[var(--text-secondary)]">
                資料載入中
              </td>
            </tr>
            <tr v-else-if="schedules.length === 0">
              <td colspan="8" class="border-b border-[rgba(214,206,195,0.42)] px-4 py-14 text-center text-[var(--text-secondary)]">
                尚無預約轉帳
              </td>
            </tr>
            <template v-else>
              <tr
                v-for="record in schedules"
                :key="record.id"
                class="transition-colors hover:bg-[rgba(92,107,95,0.045)]"
              >
                <td class="border-b border-[rgba(214,206,195,0.42)] px-4 py-3 font-medium">{{ record.fromAccountNumber }}</td>
                <td class="border-b border-[rgba(214,206,195,0.42)] px-4 py-3">{{ scheduleBankLabel(record) }}</td>
                <td class="border-b border-[rgba(214,206,195,0.42)] px-4 py-3">{{ record.toAccountNumber }}</td>
                <td class="border-b border-[rgba(214,206,195,0.42)] px-4 py-3">{{ formatNum(record.amount) }} TWD</td>
                <td class="border-b border-[rgba(214,206,195,0.42)] px-4 py-3">{{ record.scheduledDate }}</td>
                <td class="border-b border-[rgba(214,206,195,0.42)] px-4 py-3">{{ record.note || '-' }}</td>
                <td class="border-b border-[rgba(214,206,195,0.42)] px-4 py-3">
                  <span :class="statusBadgeClass(record.status)">
                    {{ statusLabel(record.status) }}
                  </span>
                </td>
                <td class="border-b border-[rgba(214,206,195,0.42)] px-4 py-3">
                  <button
                    v-if="record.status === 'PENDING'"
                    type="button"
                    class="rounded-[8px] border border-[rgba(166,90,77,0.32)] bg-[rgba(166,90,77,0.08)] px-3 py-1.5 text-[13px] font-medium text-[var(--accent)] transition hover:bg-[rgba(166,90,77,0.14)]"
                    @click="confirmCancel(record.id)"
                  >
                    取消
                  </button>
                  <span v-else class="text-[var(--text-secondary)]">-</span>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>
    </section>

    <!-- 常用帳號選擇 Modal -->
    <a-modal v-model:open="showFavPicker" title="常用帳號" :footer="null" width="480px">
      <div v-if="favorites.length === 0 && !favLoading" style="text-align:center;color:#999;padding:32px 16px">
        尚無常用帳號
      </div>
      <a-list v-else :data-source="favorites" :loading="favLoading" size="small">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta :title="item.alias" :description="favoriteDescription(item)" />
            <template #actions>
              <a-button size="small" type="link" @click="pickFavorite(item)">選用</a-button>
            </template>
          </a-list-item>
        </template>
      </a-list>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { CheckCircleFilled } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { getMyAccounts, getTransferBanks } from '@/api/customerAccount'
import { getScheduledTransfers, createScheduledTransfer, cancelScheduledTransfer, requestScheduledTransferOtp } from '@/api/scheduledTransfer'
import { getFavoriteAccounts } from '@/api/favoriteAccount'

const JAVA_BANK_CODE = '909'
const fallbackBanks = [{ code: JAVA_BANK_CODE, name: '福庫銀行', label: '福庫銀行 909' }]

const form = ref({ fromAccount: undefined, toBankCode: JAVA_BANK_CODE, toAccount: '', amount: null, scheduledDate: null, note: '' })
const accounts = ref([])
const schedules = ref([])
const favorites = ref([])
const banks = ref(fallbackBanks)
const loading = ref(false)
const creating = ref(false)
const favLoading = ref(false)
const bankLoading = ref(false)
const showFavPicker = ref(false)
const showOtpModal = ref(false)

const step = ref('form') // 'form', 'success'
const otpCode = ref('')
const demoOtp = ref('')

const twdAccounts = computed(() =>
  accounts.value.filter(a => a.currency === 'TWD' && a.status === 'ACTIVE' && a.accountType !== 'LOAN' && a.accountType !== 'SUB_ACCOUNT')
)
const bankSelectOptions = computed(() =>
  banks.value.map((bank) => ({
    value: bank.code,
    label: bankOptionLabel(bank),
  }))
)

onMounted(async () => {
  await Promise.all([loadAccounts(), loadSchedules(), loadFavorites(), loadBanks()])
})

async function loadAccounts() {
  try { accounts.value = await getMyAccounts() } catch (e) { console.error(e) }
}

async function loadSchedules() {
  loading.value = true
  try { schedules.value = await getScheduledTransfers() } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function loadFavorites() {
  favLoading.value = true
  try { favorites.value = await getFavoriteAccounts() } catch (e) { console.error(e) }
  finally { favLoading.value = false }
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

function onFromChange() {}

function disabledDate(current) {
  return current && current < dayjs().startOf('day')
}

async function handleCreate() {
  if (form.value.toBankCode === JAVA_BANK_CODE && form.value.fromAccount === form.value.toAccount) {
    message.error('轉出與轉入帳戶不可相同')
    return
  }

  creating.value = true
  try {
    const res = await requestScheduledTransferOtp()
    demoOtp.value = res.demoOtp
    otpCode.value = ''
    showOtpModal.value = true
    message.success('已發送 OTP 驗證碼至您的信箱')
  } catch (e) {
    message.error(e?.response?.data?.message || '發送 OTP 失敗')
  } finally {
    creating.value = false
  }
}

async function handleCreateBypassOtp() {
  if (!form.value.fromAccount || !form.value.toAccount || !form.value.amount || !form.value.scheduledDate) {
    message.warning('請先填寫完整預約資訊')
    return
  }
  if (form.value.toBankCode === JAVA_BANK_CODE && form.value.fromAccount === form.value.toAccount) {
    message.error('轉出與轉入帳戶不可相同')
    return
  }
  otpCode.value = '000000'
  await submitOtpAndCreate()
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

async function submitOtpAndCreate() {
  if (!otpCode.value || otpCode.value.length !== 6) {
    message.error('請輸入 6 位數 OTP')
    return
  }

  creating.value = true
  try {
    await createScheduledTransfer({
      fromAccountNumber: form.value.fromAccount,
      toBankCode: form.value.toBankCode,
      toAccountNumber: form.value.toAccount,
      amount: form.value.amount,
      scheduledDate: form.value.scheduledDate?.format('YYYY-MM-DD'),
      note: form.value.note || undefined,
      otp: otpCode.value
    })
    message.success('預約轉帳建立成功')
    showOtpModal.value = false
    step.value = 'success'
    await loadSchedules()
  } catch (e) {
    message.error(e?.response?.data?.message || '建立失敗')
  } finally {
    creating.value = false
  }
}

function resetForm() {
  step.value = 'form'
  otpCode.value = ''
  demoOtp.value = ''
  form.value = { fromAccount: form.value.fromAccount, toBankCode: JAVA_BANK_CODE, toAccount: '', amount: null, scheduledDate: null, note: '' }
}

async function handleCancel(id) {
  try {
    await cancelScheduledTransfer(id)
    message.success('已取消預約')
    await loadSchedules()
  } catch (e) {
    message.error(e?.response?.data?.message || '取消失敗')
  }
}

function confirmCancel(id) {
  Modal.confirm({
    title: '取消預約',
    content: '確定要取消此預約轉帳嗎？',
    okText: '確認',
    cancelText: '保留',
    okType: 'danger',
    onOk() {
      handleCancel(id)
    }
  })
}

function pickFavorite(item) {
  form.value.toAccount = item.accountNumber
  if (item.bankCode) form.value.toBankCode = item.bankCode
  showFavPicker.value = false
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

function scheduleBankLabel(record) {
  return record.toBankName ? `${record.toBankName} ${record.toBankCode || ''}`.trim() : record.toBankCode || '-'
}

function statusLabel(s) {
  const map = { PENDING: '待執行', EXECUTED: '已執行', CANCELLED: '已取消', FAILED: '執行失敗' }
  return map[s] || s
}

function statusBadgeClass(s) {
  const base = 'inline-flex rounded-full px-3 py-1 text-[12px] font-semibold'
  const map = {
    PENDING: 'bg-[rgba(92,107,95,0.1)] text-[var(--primary-dark)]',
    EXECUTED: 'bg-[rgba(92,107,95,0.14)] text-[var(--primary-dark)]',
    CANCELLED: 'bg-[rgba(166,90,77,0.1)] text-[var(--accent)]',
    FAILED: 'bg-[rgba(196,164,124,0.18)] text-[#7b5a2f]',
  }
  return `${base} ${map[s] || 'bg-[rgba(214,206,195,0.45)] text-[var(--text-secondary)]'}`
}

function scheduledAccountLabel(account) {
  return `${account.accountNumber} — ${formatTwd(account.balance)} TWD`
}

function formatTwd(v) {
  return Number(v || 0).toLocaleString('en-US', { maximumFractionDigits: 0 })
}

function formatNum(v) {
  return Number(v || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function demoScheduledTransfer() {
  const checkingAccount = twdAccounts.value.find(a => a.accountType === 'CHECKING') || twdAccounts.value[0]
  if (checkingAccount) {
    form.value.fromAccount = checkingAccount.accountNumber
    form.value.toBankCode = JAVA_BANK_CODE
    form.value.amount = 1000
    form.value.toAccount = ''
    form.value.scheduledDate = dayjs().add(1, 'day')
    form.value.note = ''
  } else {
    message.warning('目前沒有可用的帳號供示範')
  }
}
</script>

<style scoped>
.scheduled-transfer-page {
  max-width: 1180px;
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

.form-card {
  border-radius: 12px;
  background: rgba(255, 249, 239, 0.92);
  border: 1px solid rgba(214, 206, 195, 0.86);
  box-shadow: 0 12px 36px rgba(63, 74, 66, 0.08);
}

.form-card :deep(.ant-card-head) {
  min-height: 64px;
  padding: 0 28px;
}

.form-card :deep(.ant-card-body) {
  padding: 28px 32px;
}

.form-card :deep(.ant-form-item) {
  margin-bottom: 22px;
}

.form-card :deep(.ant-form-item-label) {
  padding-bottom: 8px;
}

.list-card {
  margin-top: 20px;
  padding: 0 0 24px;
  border-radius: 12px;
  background: rgba(255, 249, 239, 0.92);
  border: 1px solid rgba(214, 206, 195, 0.86);
  box-shadow: 0 12px 36px rgba(63, 74, 66, 0.08);
}

.list-card-title {
  padding: 14px 22px;
  border-bottom: 1px solid rgba(214, 206, 195, 0.72);
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 700;
}

.form-card :deep(.ant-card-head-title) {
  color: var(--text-primary);
  font-family: var(--font-heading);
  font-size: 20px;
  line-height: 1.35;
  font-weight: 700;
}

.form-card :deep(.ant-form-item-label > label) {
  color: var(--text-primary);
  font-size: 16px;
  line-height: 1.45;
  font-weight: 600;
}

.scheduled-form-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0 18px;
  min-width: 0;
}

.form-field {
  min-width: 0;
}

.account-input-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  min-width: 0;
}

.account-input-row :deep(.ant-input) {
  min-width: 0;
}

.account-input-row :deep(.ant-btn) {
  flex: 0 0 auto;
}

.form-card :deep(.ant-select-selector),
.form-card :deep(.ant-input),
.form-card :deep(.ant-input-number),
.form-card :deep(.ant-picker) {
  min-height: 48px;
  border-color: rgba(198, 188, 174, 0.92);
  border-radius: 8px;
  background: rgba(255, 249, 239, 0.64);
  font-size: 16px;
}

.form-card :deep(.ant-input),
.form-card :deep(.ant-input-number-input),
.form-card :deep(.ant-picker-input > input),
.form-card :deep(.ant-select-selection-item),
.form-card :deep(.ant-select-selection-placeholder) {
  color: var(--text-primary);
  font-size: 16px;
}

.form-card :deep(.ant-input-number) {
  display: flex;
  align-items: center;
}

.form-card :deep(.ant-input-number-input-wrap) {
  width: 100%;
}

.form-card :deep(.ant-input-number-input) {
  height: 46px;
  line-height: 46px;
}

.form-card :deep(.ant-select-single .ant-select-selector),
.form-card :deep(.ant-picker) {
  align-items: center;
}

.form-card :deep(.ant-select-arrow),
.form-card :deep(.ant-picker-suffix) {
  font-size: 16px;
}

.form-card :deep(.ant-btn) {
  min-height: 44px;
  font-size: 16px;
}

.form-card :deep(.ant-btn-lg) {
  min-height: 48px;
  font-size: 16px;
}

.form-card :deep(.ant-btn-primary) {
  border-color: var(--primary);
  background: var(--primary);
  box-shadow: 0 6px 14px rgba(63, 74, 66, 0.14);
}

.form-card :deep(.ant-btn-primary:hover) {
  border-color: var(--primary-dark);
  background: var(--primary-dark);
}

:global(.scheduled-account-dropdown) {
  min-width: 360px;
}

:global(.scheduled-account-dropdown .ant-select-item-option-content) {
  overflow: visible;
  color: var(--text-primary);
  font-size: 14px;
  white-space: nowrap;
  text-overflow: clip;
}

.scheduled-transfer-page table {
  font-size: 13px;
}

.scheduled-transfer-page th {
  font-size: 13px;
}

.scheduled-transfer-page td {
  font-size: 13px;
}

.scheduled-transfer-page td button {
  font-size: 12px;
}

.scheduled-transfer-page :deep(.ant-modal-title) {
  font-size: 18px;
}

.scheduled-transfer-page :deep(.ant-list-item-meta-title) {
  font-size: 15px;
}

.scheduled-transfer-page :deep(.ant-list-item-meta-description),
.scheduled-transfer-page :deep(.ant-list-item-action .ant-btn-link) {
  font-size: 13px;
}

@media (min-width: 760px) {
  .scheduled-form-grid {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
    column-gap: 20px;
  }

  .from-account-field,
  .note-field {
    grid-column: 1 / -1;
  }
}

@media (min-width: 1280px) {
  .scheduled-form-grid {
    grid-template-columns: minmax(360px, 1.35fr) minmax(260px, 1fr) minmax(360px, 1.2fr);
    column-gap: 24px;
  }

  .from-account-field,
  .note-field {
    grid-column: auto;
  }
}

@media (max-width: 1199px) {
  .scheduled-transfer-page {
    width: min(920px, calc(100vw - 48px));
    max-width: none;
    padding: 16px;
  }

  h2 {
    font-size: 34px;
    margin-bottom: 20px;
  }

  .form-card :deep(.ant-card-head) {
    min-height: 60px;
    padding: 0 22px;
  }

  .form-card :deep(.ant-card-head-title) {
    font-size: 18px;
  }

  .form-card :deep(.ant-card-body) {
    padding: 20px 16px;
  }

  .form-card :deep(.ant-form-item-label > label),
  .form-card :deep(.ant-input),
  .form-card :deep(.ant-input-number-input),
  .form-card :deep(.ant-picker-input > input),
  .form-card :deep(.ant-select-selection-item),
  .form-card :deep(.ant-select-selection-placeholder) {
    font-size: 15px;
  }
}

@media (max-width: 520px) {
  .scheduled-transfer-page {
    width: auto;
    padding: 14px 10px;
  }

  .account-input-row {
    grid-template-columns: 1fr;
  }

  .account-input-row :deep(.ant-btn) {
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
