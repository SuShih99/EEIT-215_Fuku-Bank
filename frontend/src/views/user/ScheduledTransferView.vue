<template>
  <div class="scheduled-transfer-page">
    <h2>預約轉帳</h2>

    <!-- 新增預約 -->
    <a-card class="form-card" title="新增預約轉帳">
      <a-form :model="form" layout="vertical" @finish="handleCreate">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="轉出帳戶" name="fromAccount" :rules="[{ required: true, message: '請選擇轉出帳戶' }]">
              <a-select v-model:value="form.fromAccount" placeholder="選擇轉出帳戶" @change="onFromChange">
                <a-select-option v-for="a in twdAccounts" :key="a.accountNumber" :value="a.accountNumber">
                  {{ a.accountNumber }} — 餘額 {{ formatNum(a.balance) }} TWD
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="轉入帳號" name="toAccount" :rules="[{ required: true, message: '請輸入轉入帳號' }]">
              <div style="display: flex; gap: 8px;">
                <a-input v-model:value="form.toAccount" placeholder="12碼帳號" style="flex: 1" />
                <a-button @click="showFavPicker = true">常用帳號</a-button>
              </div>
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="轉帳金額" name="amount" :rules="[{ required: true, message: '請輸入金額' }]">
              <a-input-number v-model:value="form.amount" :min="1" style="width: 100%"
                              :formatter="v => `${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')"
                              :parser="v => v.replace(/,/g, '')" placeholder="金額" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="預約日期" name="scheduledDate" :rules="[{ required: true, message: '請選擇日期' }]">
              <a-date-picker v-model:value="form.scheduledDate" style="width: 100%"
                             :disabled-date="disabledDate" placeholder="選擇日期" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="備註">
              <a-input v-model:value="form.note" placeholder="選填" :maxlength="100" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item>
          <a-button type="primary" html-type="submit" :loading="creating" size="large">
            建立預約
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 預約列表 -->
    <section class="list-card">
      <div class="list-card-title">我的預約轉帳</div>
      <div class="overflow-x-auto rounded-[16px] bg-white/60 p-4">
        <table class="w-full min-w-[900px] border-collapse text-left text-[14px] text-[var(--text-primary)]">
          <thead class="bg-[rgba(245,241,234,0.84)]">
            <tr>
              <th class="rounded-tl-[10px] border-b border-[rgba(214,206,195,0.72)] px-4 py-3 font-semibold">轉出帳戶</th>
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
              <td colspan="7" class="border-b border-[rgba(214,206,195,0.42)] px-4 py-14 text-center text-[var(--text-secondary)]">
                資料載入中
              </td>
            </tr>
            <tr v-else-if="schedules.length === 0">
              <td colspan="7" class="border-b border-[rgba(214,206,195,0.42)] px-4 py-14 text-center text-[var(--text-secondary)]">
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
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { getMyAccounts } from '@/api/customerAccount'
import { getScheduledTransfers, createScheduledTransfer, cancelScheduledTransfer } from '@/api/scheduledTransfer'
import { getFavoriteAccounts } from '@/api/favoriteAccount'

const form = ref({ fromAccount: undefined, toAccount: '', amount: null, scheduledDate: null, note: '' })
const accounts = ref([])
const schedules = ref([])
const favorites = ref([])
const loading = ref(false)
const creating = ref(false)
const favLoading = ref(false)
const showFavPicker = ref(false)

const twdAccounts = computed(() =>
  accounts.value.filter(a => a.currency === 'TWD' && a.status === 'ACTIVE' && a.accountType !== 'LOAN')
)

onMounted(async () => {
  await Promise.all([loadAccounts(), loadSchedules(), loadFavorites()])
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

function onFromChange() {}

function disabledDate(current) {
  return current && current < dayjs().startOf('day')
}

async function handleCreate() {
  if (form.value.fromAccount === form.value.toAccount) {
    message.error('轉出與轉入帳戶不可相同')
    return
  }
  creating.value = true
  try {
    await createScheduledTransfer({
      fromAccountNumber: form.value.fromAccount,
      toAccountNumber: form.value.toAccount,
      amount: form.value.amount,
      scheduledDate: form.value.scheduledDate?.format('YYYY-MM-DD'),
      note: form.value.note || undefined,
    })
    message.success('預約轉帳建立成功')
    form.value = { fromAccount: form.value.fromAccount, toAccount: '', amount: null, scheduledDate: null, note: '' }
    await loadSchedules()
  } catch (e) {
    message.error(e?.response?.data?.message || '建立失敗')
  } finally {
    creating.value = false
  }
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
  if (window.confirm('確定要取消此預約？')) {
    handleCancel(id)
  }
}

function pickFavorite(item) {
  form.value.toAccount = item.accountNumber
  showFavPicker.value = false
}

function favoriteDescription(item) {
  const bank = item.bankName ? `${item.bankName} ${item.bankCode || ''}`.trim() : item.bankCode
  return bank ? `${bank} ${item.accountNumber}` : item.accountNumber
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

function formatNum(v) {
  return Number(v || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
</script>

<style scoped>
.scheduled-transfer-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px;
}

h2 {
  margin-bottom: 20px;
  color: var(--text-primary);
}

.form-card {
  border-radius: 12px;
  background: rgba(255, 249, 239, 0.92);
  border: 1px solid rgba(214, 206, 195, 0.86);
  box-shadow: 0 12px 36px rgba(63, 74, 66, 0.08);
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
  padding: 16px 24px;
  border-bottom: 1px solid rgba(214, 206, 195, 0.72);
  color: var(--text-primary);
  font-weight: 700;
}

.form-card :deep(.ant-card-head) {
  color: var(--text-primary);
  background: transparent;
  border-bottom-color: rgba(214, 206, 195, 0.72);
}

.form-card :deep(.ant-card-head-title) {
  font-weight: 700;
}

.form-card :deep(.ant-form-item-label > label) {
  color: var(--text-primary);
  font-weight: 600;
}

.form-card :deep(.ant-select-selector),
.form-card :deep(.ant-input),
.form-card :deep(.ant-input-number),
.form-card :deep(.ant-picker) {
  border-color: rgba(198, 188, 174, 0.92);
  border-radius: 8px;
  background: rgba(255, 249, 239, 0.64);
}

.form-card :deep(.ant-input-number-input) {
  color: var(--text-primary);
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

@media (max-width: 640px) {
  .scheduled-transfer-page {
    padding: 16px 0;
  }

  .form-card :deep(.ant-col) {
    flex: 0 0 100%;
    max-width: 100%;
  }
}
</style>
