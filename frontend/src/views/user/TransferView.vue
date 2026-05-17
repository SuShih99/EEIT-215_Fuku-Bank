<template>
  <div class="transfer-page">
    <h2>國內轉帳</h2>

    <a-card class="transfer-form-card">
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
              {{ a.accountNumber }} — 餘額 {{ formatNum(a.balance) }} TWD
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
            :formatter="(v) => `${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')"
            :parser="(v) => v.replace(/,/g, '')"
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
            確認轉帳
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <a-modal v-model:open="showResult" title="轉帳結果" :footer="null" @cancel="resetForm">
      <a-result :status="resultStatus" :title="resultTitle" :sub-title="resultSub">
        <template #extra>
          <a-button type="primary" @click="resetForm">再轉一筆</a-button>
          <a-button @click="$router.push({ name: 'user-transactions' })">查看紀錄</a-button>
        </template>
      </a-result>
    </a-modal>

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
import { getMyAccounts, doTransfer, getTransferBanks } from '@/api/customerAccount'
import { getFavoriteAccounts } from '@/api/favoriteAccount'

const JAVA_BANK_CODE = '909'
const fallbackBanks = [{ code: JAVA_BANK_CODE, name: '爪哇銀行', label: '爪哇銀行 909' }]

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
const showResult = ref(false)
const showFavorites = ref(false)
const resultStatus = ref('success')
const resultTitle = ref('')
const resultSub = ref('')
const selectedBalance = ref(null)

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
    label: `${bank.name} ${bank.code}`,
    name: bank.name,
    code: bank.code,
  })),
)

onMounted(async () => {
  await Promise.all([loadAccounts(), loadBanks()])
  if (route.query.from) {
    form.value.fromAccount = route.query.from
    onFromChange(route.query.from)
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
  const bank = item.bankName ? `${item.bankName} ${item.bankCode || ''}`.trim() : item.bankCode
  return bank ? `${bank} ${item.accountNumber}` : item.accountNumber
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
    const result = await doTransfer({
      fromAccountNumber: form.value.fromAccount,
      toBankCode: form.value.toBankCode,
      toAccountNumber: form.value.toAccount,
      amount: form.value.amount,
      note: form.value.note || undefined,
    })
    await loadAccounts()
    onFromChange(form.value.fromAccount)
    if (result && result.pending) {
      // 狀態為審核中
      resultStatus.value = 'warning' // 使用黃色警告樣式
      resultTitle.value = '轉帳審核中'
      resultSub.value =
        result.pendingReason ||
        '您的轉帳交易已妥善受理。為保障您的帳戶資金安全，系統正進行例行的安全覆核程序，詳細的覆核進度已同步寄送至您的電子信箱。您無須重複送出申請，稍後亦可至「交易明細」查詢最終結果。'
    } else {
      // 狀態為正常成功
      resultStatus.value = 'success'
      resultTitle.value = '轉帳成功'
      resultSub.value = `已成功轉帳 ${formatNum(result.amount || form.value.amount)} TWD 至 ${selectedBank.value.name} ${form.value.toAccount}，預計扣款 ${formatNum(result.totalDebitAmount || totalDebitAmount.value)} TWD`
    }
    showResult.value = true
  } catch (e) {
    resultStatus.value = 'error'
    resultTitle.value = '轉帳失敗'
    resultSub.value = e?.response?.data?.message || '系統錯誤，請稍後再試'
    showResult.value = true
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  showResult.value = false
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
</script>

<style scoped>
.transfer-page {
  max-width: 640px;
  margin: 0 auto;
  padding: 24px;
}

h2 {
  margin-bottom: 20px;
  color: var(--text-primary);
}

.transfer-form-card {
  border-radius: 12px;
  background: rgba(255, 249, 239, 0.92);
  border: 1px solid rgba(214, 206, 195, 0.86);
  box-shadow: 0 12px 36px rgba(63, 74, 66, 0.08);
}

.account-input-row {
  display: flex;
  gap: 8px;
}

.account-input-row :deep(.ant-input) {
  flex: 1;
}

.hint {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-secondary);
}

.fee-preview {
  display: grid;
  gap: 8px;
  margin: 0 0 24px;
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
  font-size: 14px;
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
  font-size: 16px;
}

.empty-favorites {
  text-align: center;
  color: #999;
  padding: 32px 20px;
}

@media (max-width: 640px) {
  .transfer-page {
    padding: 16px 0;
  }

  .account-input-row {
    flex-direction: column;
  }
}
</style>
