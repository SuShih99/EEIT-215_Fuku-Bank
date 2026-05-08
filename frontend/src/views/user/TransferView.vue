<template>
  <div class="transfer-page">
    <h2>國內轉帳</h2>

    <a-card class="transfer-form-card">
      <a-form :model="form" layout="vertical" @finish="handleTransfer">
        <a-form-item label="轉出帳戶" name="fromAccount" :rules="[{ required: true, message: '請選擇轉出帳戶' }]">
          <a-select v-model:value="form.fromAccount" placeholder="選擇轉出帳戶" @change="onFromChange">
            <a-select-option v-for="a in twdAccounts" :key="a.accountNumber" :value="a.accountNumber">
              {{ a.accountNumber }} — 餘額 {{ formatNum(a.balance) }} TWD
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="轉入帳號" name="toAccount" :rules="[{ required: true, message: '請輸入轉入帳號' }]">
          <a-input-group compact>
            <a-input v-model:value="form.toAccount" placeholder="請輸入12碼帳號" style="width: calc(100% - 120px)" />
            <a-button @click="showFavorites = true">常用帳號</a-button>
          </a-input-group>
        </a-form-item>

        <a-form-item label="轉帳金額" name="amount" :rules="[{ required: true, message: '請輸入金額' }]">
          <a-input-number v-model:value="form.amount" :min="1" :max="maxAmount" style="width: 100%"
                          :formatter="v => `${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')"
                          :parser="v => v.replace(/,/g, '')" placeholder="請輸入金額" />
          <div class="hint" v-if="selectedBalance !== null">可用餘額：{{ formatNum(selectedBalance) }} TWD</div>
        </a-form-item>

        <a-form-item label="備註">
          <a-input v-model:value="form.note" placeholder="選填，例如：房租、貨款" :maxlength="100" />
        </a-form-item>

        <a-form-item>
          <a-button type="primary" html-type="submit" :loading="submitting" block size="large">
            確認轉帳
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 轉帳結果 -->
    <a-modal v-model:open="showResult" title="轉帳結果" :footer="null" @cancel="resetForm">
      <a-result :status="resultStatus" :title="resultTitle" :sub-title="resultSub">
        <template #extra>
          <a-button type="primary" @click="resetForm">再轉一筆</a-button>
          <a-button @click="$router.push({ name: 'user-transactions' })">查看紀錄</a-button>
        </template>
      </a-result>
    </a-modal>

    <!-- 常用帳號選擇 -->
    <a-modal v-model:open="showFavorites" title="常用帳號" :footer="null" width="500px">
      <a-list :data-source="favorites" :loading="favLoading">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta :title="item.alias" :description="item.accountNumber" />
            <template #actions>
              <a-button size="small" type="link" @click="selectFavorite(item)">選用</a-button>
            </template>
          </a-list-item>
        </template>
        <template #header>
          <div v-if="favorites.length === 0" style="text-align:center;color:#999;padding:20px">
            尚無常用帳號，請至「常用帳號管理」新增
          </div>
        </template>
      </a-list>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { getMyAccounts, doTransfer } from '@/api/customerAccount'
import { getFavoriteAccounts } from '@/api/favoriteAccount'

const route = useRoute()
const form = ref({ fromAccount: undefined, toAccount: '', amount: null, note: '' })
const accounts = ref([])
const favorites = ref([])
const favLoading = ref(false)
const submitting = ref(false)
const showResult = ref(false)
const showFavorites = ref(false)
const resultStatus = ref('success')
const resultTitle = ref('')
const resultSub = ref('')
const selectedBalance = ref(null)

const twdAccounts = computed(() => accounts.value.filter(a => a.currency === 'TWD' && a.status === 'ACTIVE' && a.accountType !== 'LOAN'))
const maxAmount = computed(() => selectedBalance.value || 999999999)

onMounted(async () => {
  try {
    const res = await getMyAccounts()
    accounts.value = res
    if (route.query.from) {
      form.value.fromAccount = route.query.from
      onFromChange(route.query.from)
    }
    loadFavorites()
  } catch (e) { console.error(e) }
})

async function loadFavorites() {
  favLoading.value = true
  try { favorites.value = await getFavoriteAccounts() } catch (e) { console.error(e) }
  finally { favLoading.value = false }
}

function onFromChange(val) {
  const acct = accounts.value.find(a => a.accountNumber === val)
  selectedBalance.value = acct ? Number(acct.balance) : null
}

function selectFavorite(item) {
  form.value.toAccount = item.accountNumber
  showFavorites.value = false
}

async function handleTransfer() {
  if (form.value.fromAccount === form.value.toAccount) {
    message.error('轉出與轉入帳戶不可相同')
    return
  }
  submitting.value = true
  try {
    await doTransfer({
      fromAccountNumber: form.value.fromAccount,
      toAccountNumber: form.value.toAccount,
      amount: form.value.amount,
      note: form.value.note || undefined,
    })
    resultStatus.value = 'success'
    resultTitle.value = '轉帳成功'
    resultSub.value = `已成功轉帳 ${formatNum(form.value.amount)} TWD 至 ${form.value.toAccount}`
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
  form.value = { fromAccount: form.value.fromAccount, toAccount: '', amount: null, note: '' }
  // Refresh balance
  onMounted
}

function formatNum(v) {
  return Number(v || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
</script>

<style scoped>
.transfer-page { max-width: 600px; margin: 0 auto; padding: 24px; }
h2 { margin-bottom: 20px; color: #1a1a2e; }
.transfer-form-card { border-radius: 12px; }
.hint { margin-top: 4px; font-size: 12px; color: #888; }
</style>
