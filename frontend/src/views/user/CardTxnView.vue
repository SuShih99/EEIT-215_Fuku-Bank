<template>
  <div class="card-txns">
    <header class="page-header">
      <h2>刷卡交易管理</h2>
      <p>查詢信用卡消費紀錄，或建立測試消費與 LINE Pay 付款。</p>
    </header>

    <section class="txn-form-panel" aria-label="新增刷卡交易">
      <div class="panel-heading">
        <h3>新增刷卡交易</h3>
        <span>建立一筆信用卡消費紀錄</span>
      </div>

      <div class="form-grid">
        <!-- 卡號 -->
        <div class="card-number-block">
          <label class="field-shell card-number-field">
            <span class="field-label">卡號</span>
            <input
              v-model="form.cardNumber"
              class="field-control card-number-input"
              inputmode="numeric"
              maxlength="19"
              placeholder="輸入卡號開頭"
              autocomplete="off"
              @input="formatCardNumberInput"
            />
          </label>

          <div v-if="cardPickerCards.length" class="card-picker" aria-label="可選擇卡片">
            <button
              v-for="card in cardPickerCards"
              :key="card.cardId"
              type="button"
              class="card-option"
              :class="{ selected: form.cardId === card.cardId }"
              :disabled="card.status !== 'ACTIVE'"
              @click="selectCard(card)"
            >
              <span class="card-option-name">{{ getCardName(card) }}</span>
              <span class="card-option-number">{{
                formatCardNumberDisplay(getDisplayCardNumber(card))
              }}</span>
              <span class="card-option-status">{{ getCardStatusText(card.status) }}</span>
            </button>
          </div>

          <p v-else-if="cardSearchDigits" class="card-picker-empty">沒有符合開頭的卡片</p>
        </div>

        <!-- 有效日期 -->
        <label class="field-shell">
          <span class="field-label">有效日期</span>
          <input
            v-model="form.expiryDate"
            class="field-control"
            maxlength="5"
            placeholder="MM/YY"
            @input="formatExpiryInput"
          />
        </label>

        <!-- 持卡人 -->
        <label class="field-shell">
          <span class="field-label">持卡人</span>
          <input v-model="form.cardHolderName" class="field-control" placeholder="請輸入姓名" />
        </label>

        <!-- CVV -->
        <label class="field-shell">
          <span class="field-label">CVV</span>
          <input
            v-model="form.cvv"
            class="field-control"
            inputmode="numeric"
            maxlength="3"
            placeholder="自動產生"
            @input="formatCvvInput"
          />
        </label>

        <!-- 商家 -->
        <label class="field-shell select-shell">
          <span class="field-label">商家</span>
          <select v-model="form.merchantId" class="field-control">
            <option disabled value="">請選擇商家</option>

            <option
              v-for="merchant in merchants"
              :key="merchant.merchantId"
              :value="merchant.merchantId"
            >
              {{ merchant.merchantName }}
            </option>
          </select>
        </label>

        <!-- 金額 -->
        <label class="field-shell">
          <span class="field-label">金額</span>
          <input
            v-model="form.txnAmount"
            class="field-control"
            type="number"
            min="1"
            placeholder="請輸入交易金額"
          />
        </label>

        <!-- 類型 -->
        <label class="field-shell select-shell">
          <span class="field-label">交易類型</span>

          <select v-model="form.txnType" class="field-control">
            <option value="PURCHASE">一般消費</option>
          </select>
        </label>

        <!-- 備註 -->
        <label class="field-shell textarea-shell">
          <span class="field-label">商品</span>

          <textarea
            v-model="form.description"
            class="field-control"
            rows="3"
            placeholder="LINE Pay 付款時請填寫付款說明"
          />
        </label>
      </div>

      <div class="form-actions">
        <button type="button" class="primary-action" @click="handleCreateTransaction">
          新增刷卡交易
        </button>

        <button type="button" class="linepay-action" @click="handleLinePay">
          使用 LINE Pay 付款
        </button>
      </div>
    </section>

    <!-- 交易紀錄 -->
    <section class="transactions-panel" aria-label="刷卡交易紀錄">
      <div class="transactions-header">
        <h3>刷卡交易紀錄</h3>
        <span>共 {{ totalElements }} 筆</span>
      </div>

      <div class="transactions-table-wrap">
        <table class="transactions-table">
          <thead>
            <tr>
              <th>交易日期</th>
              <th>商家</th>
              <th>商品 / 備註</th>
              <th>卡號</th>
              <th class="align-right">交易金額</th>
              <th class="align-right">回饋金額</th>
              <th>類型</th>
              <th>狀態</th>
            </tr>
          </thead>

          <tbody v-if="transactions.length">
            <tr v-for="txn in transactions" :key="txn.txnId">
              <td class="mono-cell">
                {{ formatDate(txn.txnDate) }}
              </td>

              <td>{{ txn.merchantName || '-' }}</td>
              <td class="description-cell">
                {{ txn.description || '-' }}
              </td>

              <td class="mono-cell">
                {{ txn.cardNumber || '-' }}
              </td>

              <td class="amount-cell debit">
                {{ formatMoney(txn.txnAmount) }}
              </td>

              <td class="amount-cell credit">+{{ formatMoney(txn.cashbackAmount) }}</td>

              <td>
                <span class="type-pill" :class="txnTypeClass(txn.txnType)">
                  {{ txnTypeLabel(txn.txnType) }}
                </span>
              </td>

              <td>
                <span class="status-pill" :class="{ refunded: isRefunded(txn) }">
                  {{ statusLabel(txn) }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-if="loading" class="transactions-loading-state">
          <span class="loading-brush"></span>
          <strong>刷卡交易整理中</strong>
        </div>

        <div v-else-if="transactions.length === 0" class="transactions-empty-state">
          <strong>目前沒有刷卡交易</strong>
          <span>新增交易或完成付款後，紀錄會整理在這裡。</span>
        </div>
      </div>

      <div v-if="totalElements > 0" class="transactions-pagination">
        <span> 第 {{ currentPage + 1 }} / {{ displayTotalPages }} 頁 </span>

        <button type="button" :disabled="currentPage === 0" @click="prevPage">上一頁</button>

        <button type="button" :disabled="currentPage + 1 >= totalPages" @click="nextPage">
          下一頁
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  createTransaction,
  getMerchantNames,
  getTransactions,
  requestLinePay,
} from '@/api/userCardTxn'
import { getMyCards } from '@/api/userCard'

const transactions = ref([])
const loading = ref(false)
const cards = ref([])
const merchants = ref([])

const form = ref({
  cardId: '',
  cardNumber: '',
  expiryDate: '',
  cardHolderName: '',
  cvv: '',
  merchantId: '',
  txnAmount: '',
  txnType: 'PURCHASE',
  description: '',
})

const cardSearchDigits = computed(() => normalizeCardNumber(form.value.cardNumber))

const cardPickerCards = computed(() => {
  const keyword = cardSearchDigits.value

  return cards.value
    .filter((card) => {
      return !keyword || cardMatchesSearch(card, keyword)
    })
    .slice(0, 5)
})

function normalizeCardNumber(value) {
  return String(value || '').replace(/\D/g, '')
}

function formatCardNumberInput() {
  const digits = normalizeCardNumber(form.value.cardNumber).slice(0, 16)
  form.value.cardNumber = digits.replace(/(.{4})/g, '$1 ').trim()
}

function formatCvvInput() {
  form.value.cvv = String(form.value.cvv || '')
    .replace(/\D/g, '')
    .slice(0, 3)
}

function formatExpiryInput() {
  const digits = String(form.value.expiryDate || '')
    .replace(/\D/g, '')
    .slice(0, 4)
  form.value.expiryDate = digits.length > 2 ? `${digits.slice(0, 2)}/${digits.slice(2)}` : digits
}

function randomCvv() {
  return String(Math.floor(Math.random() * 1000)).padStart(3, '0')
}

function seededDigits(seed, length) {
  let hash = 0
  const source = String(seed || '')

  for (let i = 0; i < source.length; i++) {
    hash = (hash * 31 + source.charCodeAt(i)) % 1000000007
  }

  let value = hash || 24681357
  let result = ''

  for (let i = 0; i < length; i++) {
    value = (value * 1103515245 + 12345) % 2147483647
    result += String(value % 10)
  }

  return result
}

function getDisplayCardNumber(card) {
  const digits = normalizeCardNumber(card.cardNumber)

  if (digits.length >= 16) {
    return digits.slice(0, 16)
  }

  if (digits.length >= 8) {
    const prefix = digits.slice(0, 4)
    const suffix = digits.slice(-4)
    return `${prefix}${seededDigits(`${card.cardId}-${digits}`, 8)}${suffix}`
  }

  return digits.padEnd(16, '0').slice(0, 16)
}

function formatCardNumberDisplay(cardNumber) {
  return normalizeCardNumber(cardNumber)
    .slice(0, 16)
    .replace(/(.{4})/g, '$1 ')
    .trim()
}

function cardMatchesSearch(card, keyword) {
  const sourceCardNumber = normalizeCardNumber(card.cardNumber)
  const displayCardNumber = getDisplayCardNumber(card)

  return sourceCardNumber.startsWith(keyword) || displayCardNumber.startsWith(keyword)
}

function formatCardExpiry(card) {
  if (!card.expiryDate) return ''
  return dayjs(card.expiryDate).isValid() ? dayjs(card.expiryDate).format('MM/YY') : ''
}

function fillCardFields(card) {
  form.value.cardId = card.cardId
  form.value.cardNumber = formatCardNumberDisplay(getDisplayCardNumber(card))
  form.value.expiryDate = formatCardExpiry(card)
  form.value.cardHolderName = card.customerName || form.value.cardHolderName
  form.value.cvv = randomCvv()
}

function selectCard(card) {
  if (card.status !== 'ACTIVE') {
    message.warning('這張卡片目前無法交易')
    return
  }

  fillCardFields(card)
}

function getSelectedCard() {
  return cards.value.find((card) => card.cardId === form.value.cardId)
}

watch(
  () => form.value.cardNumber,
  (newValue) => {
    const inputCardNumber = normalizeCardNumber(newValue)

    const matchedCard = cards.value.find((card) => {
      return (
        normalizeCardNumber(card.cardNumber) === inputCardNumber ||
        getDisplayCardNumber(card) === inputCardNumber
      )
    })

    if (matchedCard) {
      fillCardFields(matchedCard)
      return
    }

    form.value.cardId = ''
  },
)

function validateCardInput() {
  if (!form.value.cardId) {
    message.warning('找不到符合的卡號')
    return false
  }

  if (getSelectedCard()?.status !== 'ACTIVE') {
    message.warning('這張卡片目前無法交易')
    return false
  }

  if (!/^\d{2}\/\d{2}$/.test(form.value.expiryDate)) {
    message.warning('有效日期格式請輸入 MM/YY')
    return false
  }

  if (!form.value.cardHolderName.trim()) {
    message.warning('請輸入持卡人姓名')
    return false
  }

  if (!/^\d{3}$/.test(form.value.cvv)) {
    form.value.cvv = randomCvv()
  }

  return true
}
const currentPage = ref(0)
const pageSize = ref(10)
const totalPages = ref(0)
const totalElements = ref(0)

const displayTotalPages = computed(() => Math.max(totalPages.value || 1, 1))

const fetchCards = async () => {
  try {
    const response = await getMyCards()

    cards.value = Array.isArray(response) ? response : []
  } catch (error) {
    console.error(error)
    message.error('讀取信用卡資料失敗')
  }
}

const fetchTransactions = async () => {
  try {
    loading.value = true
    const response = await getTransactions(currentPage.value, pageSize.value)
    transactions.value = Array.isArray(response?.content) ? response.content : []
    totalPages.value = response?.totalPages ?? 0
    totalElements.value = response?.totalElements ?? 0
  } catch (error) {
    console.error(error)
    message.error('讀取刷卡交易失敗')
  } finally {
    loading.value = false
  }
}

const fetchMerchantNames = async () => {
  try {
    const response = await getMerchantNames()
    merchants.value = Array.isArray(response) ? response : []
  } catch (error) {
    console.error(error)
    message.error('讀取商家資料失敗')
  }
}

const handleCreateTransaction = async () => {
  try {
    if (!validateCardInput() || !form.value.merchantId || !form.value.txnAmount) {
      message.warning('請輸入卡片資料、選擇商家並輸入交易金額')
      return
    }

    await createTransaction(form.value)
    message.success('刷卡交易建立成功')
    resetForm()
    currentPage.value = 0
    await fetchTransactions()
  } catch (err) {
    console.error(err)
    message.error(err.response?.data?.message || '建立刷卡交易失敗')
  }
}

const handleLinePay = async () => {
  try {
    if (
      !validateCardInput() ||
      !form.value.merchantId ||
      !form.value.txnAmount ||
      !form.value.description.trim()
    ) {
      message.warning('LINE Pay 付款需輸入卡片資料、商家、金額並填寫備註')
      return
    }

    const response = await requestLinePay({
      cardId: form.value.cardId,
      merchantId: form.value.merchantId,
      amount: form.value.txnAmount,
      description: form.value.description,
    })

    const paymentUrl = response.info?.paymentUrl?.web || response.paymentUrl
    if (response.orderId) {
      sessionStorage.setItem('linepay_order_id', response.orderId)
    }
    if (!paymentUrl) {
      throw new Error('找不到 LINE Pay 付款網址')
    }

    window.location.href = paymentUrl
  } catch (err) {
    console.error(err)
    message.error(err.response?.data?.message || 'LINE Pay 付款建立失敗')
  }
}

const nextPage = async () => {
  if (currentPage.value + 1 >= totalPages.value) return
  currentPage.value++
  await fetchTransactions()
}

const prevPage = async () => {
  if (currentPage.value === 0) return
  currentPage.value--
  await fetchTransactions()
}

function resetForm() {
  form.value = {
    cardId: '',
    cardNumber: '',
    expiryDate: '',
    cardHolderName: '',
    cvv: '',
    merchantId: '',
    txnAmount: '',
    txnType: 'PURCHASE',
    description: '',
  }
}

function formatDate(value) {
  if (!value) return '-'
  return dayjs(value).format('YYYY/MM/DD HH:mm')
}

function formatMoney(value) {
  return `NT$ ${Number(value || 0).toLocaleString('en-US', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  })}`
}

function txnTypeLabel(type) {
  const map = {
    PURCHASE: '一般消費',
    REFUND: '刷退',
    ANNUAL_FEE: '年費',
  }
  return map[type] || type || '-'
}

function txnTypeClass(type) {
  if (type === 'REFUND') return 'refund'
  if (type === 'ANNUAL_FEE') return 'fee'
  return 'purchase'
}

function isRefunded(txn) {
  return txn.refunded === true || txn.txnType === 'REFUND'
}

function statusLabel(txn) {
  if (txn.txnType === 'REFUND') return '刷退交易'
  if (txn.refunded) return '已刷退'
  return '已完成'
}

function getCardName(card) {
  return card.cardTypeName || card.cardType?.cardTypeName || '信用卡'
}

function getCardStatusText(status) {
  const map = {
    ACTIVE: '可使用',
    BLOCKED: '已停卡',
    INACTIVE: '未開卡',
  }

  return map[status] || status || '-'
}

onMounted(async () => {
  await fetchCards()
  await fetchTransactions()
  await fetchMerchantNames()
})
</script>

<style scoped>
.card-txns {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  color: var(--text-primary);
  font-family: var(--font-heading);
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 0;
}

.page-header p {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 14px;
  letter-spacing: 0;
}

.txn-form-panel,
.transactions-panel {
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(255, 249, 239, 0.86), rgba(249, 244, 235, 0.72)),
    url('/washi-texture.webp');
  background-size:
    auto,
    300px 300px;
  border: 1px solid rgba(214, 206, 195, 0.84);
  border-radius: 18px;
  box-shadow: 0 14px 34px rgba(63, 74, 66, 0.08);
}

.txn-form-panel {
  padding: 18px;
  margin-bottom: 18px;
}

.panel-heading,
.transactions-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-heading h3,
.transactions-header h3 {
  margin: 0;
  color: var(--text-primary);
  font-family: var(--font-heading);
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0;
}

.panel-heading span,
.transactions-header span {
  color: var(--text-secondary);
  font-size: 13px;
  letter-spacing: 0;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.field-shell {
  display: flex;
  align-items: stretch;
  min-height: 42px;
  color: var(--text-secondary);
  background: rgba(255, 249, 239, 0.62);
  border: 1px solid rgba(198, 188, 174, 0.92);
  border-radius: 8px;
}

.card-number-block {
  display: grid;
  gap: 8px;
  align-content: start;
}

.card-number-field {
  max-width: 480px;
}

.card-number-input {
  font-family: var(--font-mono);
}

.card-picker {
  display: grid;
  gap: 6px;
  max-width: 480px;
}

.card-option {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 10px;
  min-height: 36px;
  padding: 7px 10px;
  color: var(--text-primary);
  background: rgba(255, 249, 239, 0.72);
  border: 1px solid rgba(198, 188, 174, 0.72);
  border-radius: 8px;
  cursor: pointer;
  text-align: left;
}

.card-option:hover,
.card-option.selected {
  border-color: rgba(92, 107, 95, 0.52);
  box-shadow: inset 0 0 0 1px rgba(92, 107, 95, 0.16);
}

.card-option:disabled {
  color: var(--text-disabled);
  cursor: not-allowed;
  opacity: 0.62;
}

.card-option-name,
.card-option-number,
.card-option-status {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-option-name {
  font-weight: 700;
}

.card-option-number {
  color: var(--text-secondary);
  font-family: var(--font-mono);
  font-size: 12px;
}

.card-option-status {
  color: var(--primary-dark);
  font-size: 12px;
  font-weight: 700;
}

.card-picker-empty {
  max-width: 480px;
  margin: 0;
  color: var(--text-secondary);
  font-size: 12px;
}

.textarea-shell {
  grid-column: 1 / -1;
}

.field-label {
  display: flex;
  align-items: center;
  min-width: 82px;
  padding: 0 12px;
  color: var(--text-secondary);
  font-size: 12px;
  white-space: nowrap;
}

.field-control {
  width: 100%;
  min-width: 0;
  min-height: 40px;
  appearance: none;
  color: var(--text-primary);
  background: transparent;
  border: 0;
  border-left: 1px solid rgba(214, 206, 195, 0.72);
  border-radius: 0;
  padding: 0 12px;
  font-family: var(--font-body);
  font-size: 14px;
  letter-spacing: 0;
  outline: none;
}

select.field-control {
  padding-right: 34px;
}

.select-shell {
  position: relative;
}

.select-shell::after {
  content: '';
  position: absolute;
  top: 50%;
  right: 14px;
  width: 8px;
  height: 8px;
  border-right: 1.5px solid var(--text-secondary);
  border-bottom: 1.5px solid var(--text-secondary);
  transform: translateY(-66%) rotate(45deg);
  pointer-events: none;
}

textarea.field-control {
  min-height: 84px;
  padding-top: 10px;
  resize: vertical;
}

.field-control:focus {
  box-shadow: inset 0 0 0 2px rgba(92, 107, 95, 0.18);
}

.form-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.primary-action,
.linepay-action {
  min-height: 38px;
  padding: 0 18px;
  border: 0;
  border-radius: 8px;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.primary-action {
  background: var(--primary);
  box-shadow: 0 6px 14px rgba(63, 74, 66, 0.14);
}

.primary-action:hover {
  background: var(--primary-dark);
}

.linepay-action {
  background: #06c755;
}

.linepay-action:hover {
  background: #05b54d;
}

.transactions-panel {
  padding: 0;
}

.transactions-header {
  padding: 18px 18px 0;
}

.transactions-table-wrap {
  position: relative;
  overflow-x: auto;
}

.transactions-table {
  width: 100%;
  min-width: 980px;
  border-collapse: collapse;
  color: var(--text-primary);
  font-size: 14px;
}

.transactions-table th {
  padding: 14px 12px;
  text-align: left;
  color: var(--text-primary);
  font-weight: 700;
  background: rgba(245, 241, 234, 0.74);
  border-bottom: 1px solid rgba(214, 206, 195, 0.72);
  white-space: nowrap;
}

.transactions-table td {
  padding: 14px 12px;
  border-bottom: 1px solid rgba(214, 206, 195, 0.42);
  vertical-align: middle;
}

.transactions-table tbody tr:hover {
  background: rgba(92, 107, 95, 0.045);
}

.align-right,
.amount-cell {
  text-align: right;
}

.mono-cell {
  font-family: var(--font-mono);
  color: var(--text-secondary);
}

.amount-cell {
  font-weight: 700;
  white-space: nowrap;
}

.amount-cell.credit {
  color: var(--primary-dark);
}

.amount-cell.debit {
  color: var(--accent);
}

.type-pill,
.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 24px;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.type-pill.purchase,
.status-pill {
  color: var(--primary-dark);
  background: rgba(92, 107, 95, 0.12);
  border: 1px solid rgba(92, 107, 95, 0.28);
}

.type-pill.refund,
.status-pill.refunded {
  color: var(--accent);
  background: rgba(166, 90, 77, 0.08);
  border: 1px solid rgba(166, 90, 77, 0.24);
}

.type-pill.fee {
  color: #7b5a2f;
  background: rgba(196, 164, 124, 0.16);
  border: 1px solid rgba(196, 164, 124, 0.34);
}

.transactions-loading-state,
.transactions-empty-state {
  display: grid;
  justify-items: center;
  gap: 8px;
  min-height: 210px;
  padding: 34px 16px 38px;
  color: var(--text-secondary);
  text-align: center;
  background: linear-gradient(180deg, rgba(255, 249, 239, 0.68), rgba(249, 244, 235, 0.5));
}

.transactions-empty-mark {
  width: 76px;
  height: 76px;
}

.transactions-empty-mark svg {
  display: block;
  width: 100%;
  height: 100%;
}

.transactions-empty-state strong,
.transactions-loading-state strong {
  color: var(--text-primary);
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0;
}

.transactions-empty-state span {
  color: var(--text-secondary);
  font-size: 14px;
  letter-spacing: 0;
}

.loading-brush {
  width: 72px;
  height: 18px;
  border-radius: 999px;
  background: linear-gradient(90deg, transparent, rgba(92, 107, 95, 0.34), transparent);
  animation: brushLoading 1.2s ease-in-out infinite;
}

@keyframes brushLoading {
  0%,
  100% {
    opacity: 0.38;
    transform: scaleX(0.72);
  }

  50% {
    opacity: 1;
    transform: scaleX(1);
  }
}

.transactions-pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 16px;
  color: var(--text-secondary);
  border-top: 1px solid rgba(214, 206, 195, 0.58);
}

.transactions-pagination button {
  min-height: 32px;
  padding: 4px 10px;
  color: var(--primary-dark);
  background: rgba(255, 249, 239, 0.66);
  border: 1px solid rgba(198, 188, 174, 0.88);
  border-radius: 8px;
  cursor: pointer;
}

.transactions-pagination button:disabled {
  color: var(--text-disabled);
  cursor: not-allowed;
}

@media (max-width: 760px) {
  .card-txns {
    padding: 20px 12px;
  }

  .panel-heading,
  .transactions-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .field-shell {
    flex-direction: column;
  }

  .card-number-field,
  .card-picker,
  .card-picker-empty {
    max-width: none;
  }

  .field-label {
    min-height: 34px;
  }

  .field-control {
    border-left: 0;
    border-top: 1px solid rgba(214, 206, 195, 0.72);
  }

  .card-option {
    grid-template-columns: 1fr;
    gap: 2px;
  }

  .transactions-pagination {
    justify-content: center;
    flex-wrap: wrap;
  }
}
</style>
