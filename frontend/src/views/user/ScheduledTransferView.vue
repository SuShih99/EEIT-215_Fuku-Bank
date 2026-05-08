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
              <a-input-group compact>
                <a-input v-model:value="form.toAccount" placeholder="12碼帳號" style="width: calc(100% - 100px)" />
                <a-button @click="showFavPicker = true">常用帳號</a-button>
              </a-input-group>
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
    <a-card class="list-card" title="我的預約轉帳" style="margin-top: 20px">
      <a-table :dataSource="schedules" :columns="columns" :loading="loading"
               rowKey="id" :pagination="{ pageSize: 10 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            {{ formatNum(record.amount) }} TWD
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-popconfirm title="確定要取消此預約？" @confirm="handleCancel(record.id)"
                          v-if="record.status === 'PENDING'">
              <a-button size="small" danger>取消</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 常用帳號選擇 Modal -->
    <a-modal v-model:open="showFavPicker" title="常用帳號" :footer="null" width="480px">
      <a-list :data-source="favorites" :loading="favLoading" size="small">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta :title="item.alias" :description="item.accountNumber" />
            <template #actions>
              <a-button size="small" type="link" @click="pickFavorite(item)">選用</a-button>
            </template>
          </a-list-item>
        </template>
        <template #header>
          <div v-if="favorites.length === 0" style="text-align:center;color:#999;padding:16px">
            尚無常用帳號
          </div>
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

const columns = [
  { title: '轉出帳戶', dataIndex: 'fromAccountNumber', key: 'fromAccountNumber', ellipsis: true },
  { title: '轉入帳戶', dataIndex: 'toAccountNumber', key: 'toAccountNumber', ellipsis: true },
  { title: '金額', key: 'amount', width: 140 },
  { title: '預約日期', dataIndex: 'scheduledDate', key: 'scheduledDate', width: 120 },
  { title: '備註', dataIndex: 'note', key: 'note', ellipsis: true },
  { title: '狀態', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 90 },
]

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

function pickFavorite(item) {
  form.value.toAccount = item.accountNumber
  showFavPicker.value = false
}

function statusColor(s) {
  const map = { PENDING: 'blue', EXECUTED: 'green', CANCELLED: 'red', FAILED: 'orange' }
  return map[s] || 'default'
}

function statusLabel(s) {
  const map = { PENDING: '待執行', EXECUTED: '已執行', CANCELLED: '已取消', FAILED: '執行失敗' }
  return map[s] || s
}

function formatNum(v) {
  return Number(v || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
</script>

<style scoped>
.scheduled-transfer-page { max-width: 960px; margin: 0 auto; padding: 24px; }
h2 { margin-bottom: 20px; color: #1a1a2e; }
.form-card { border-radius: 12px; }
.list-card { border-radius: 12px; }
</style>
