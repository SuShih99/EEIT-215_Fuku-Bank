<template>
  <div class="favorite-accounts-page">
    <h2>常用帳號管理</h2>

    <!-- 新增常用帳號 -->
    <a-card class="form-card">
      <a-form class="favorite-form" :model="form" layout="inline" @finish="handleAdd">
        <a-form-item label="帳號" name="accountNumber" :rules="[{ required: true, message: '請輸入帳號' }]">
          <a-input v-model:value="form.accountNumber" placeholder="12碼帳號" style="width: 200px" />
        </a-form-item>
        <a-form-item label="銀行代號" name="bankCode" :rules="[{ required: true, message: '請選擇銀行' }]">
          <a-select
            v-model:value="form.bankCode"
            show-search
            placeholder="選擇銀行"
            :loading="bankLoading"
            :options="bankSelectOptions"
            option-filter-prop="label"
            style="width: 220px"
            @change="syncFormBankName"
          />
        </a-form-item>
        <a-form-item label="備註名稱" name="alias" :rules="[{ required: true, message: '請輸入備註名稱' }]">
          <a-input v-model:value="form.alias" placeholder="例如：房東、公司帳戶" style="width: 200px" :maxlength="50" />
        </a-form-item>
        <a-form-item class="submit-item">
          <a-button type="primary" html-type="submit" :loading="adding">新增常用帳號</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 常用帳號列表 -->
    <a-card class="list-card" style="margin-top: 20px">
      <a-table :dataSource="favorites" :columns="columns" :loading="loading"
               rowKey="id" :pagination="{ pageSize: 10 }">
        <template #emptyText>
          <div style="padding: 32px 0; color: #999;">尚無常用帳號</div>
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button size="small" @click="startEdit(record)">編輯</a-button>
              <a-popconfirm title="確定要刪除此常用帳號？" @confirm="handleDelete(record.id)">
                <a-button size="small" danger>刪除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 編輯 Modal -->
    <a-modal v-model:open="showEdit" title="編輯常用帳號" @ok="handleUpdate" :confirmLoading="updating">
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="帳號">
          <a-input :value="editForm.accountNumber" disabled />
        </a-form-item>
        <a-form-item label="銀行代號">
          <a-select
            v-model:value="editForm.bankCode"
            show-search
            placeholder="選擇銀行"
            :loading="bankLoading"
            :options="bankSelectOptions"
            option-filter-prop="label"
            @change="syncEditBankName"
          />
        </a-form-item>
        <a-form-item label="備註名稱">
          <a-input v-model:value="editForm.alias" placeholder="備註名稱" :maxlength="50" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getFavoriteAccounts, addFavoriteAccount, updateFavoriteAccount, deleteFavoriteAccount } from '@/api/favoriteAccount'
import { getTransferBanks } from '@/api/customerAccount'

const JAVA_BANK_CODE = '909'
const fallbackBanks = [{ code: JAVA_BANK_CODE, name: '爪哇銀行', label: '爪哇銀行 909' }]

const form = ref({ accountNumber: '', bankCode: JAVA_BANK_CODE, alias: '', bankName: '爪哇銀行' })
const editForm = ref({ id: null, accountNumber: '', bankCode: JAVA_BANK_CODE, alias: '', bankName: '爪哇銀行' })
const favorites = ref([])
const banks = ref(fallbackBanks)
const loading = ref(false)
const bankLoading = ref(false)
const adding = ref(false)
const updating = ref(false)
const showEdit = ref(false)

const columns = [
  { title: '帳號', dataIndex: 'accountNumber', key: 'accountNumber', width: 180 },
  { title: '銀行代號', dataIndex: 'bankCode', key: 'bankCode', width: 110 },
  { title: '備註名稱', dataIndex: 'alias', key: 'alias' },
  { title: '銀行名稱', dataIndex: 'bankName', key: 'bankName' },
  { title: '建立時間', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 160 },
]

const bankSelectOptions = computed(() =>
  banks.value.map((bank) => ({
    value: bank.code,
    label: `${bank.name} ${bank.code}`,
  })),
)

onMounted(async () => {
  await Promise.all([loadFavorites(), loadBanks()])
})

async function loadFavorites() {
  loading.value = true
  try { favorites.value = await getFavoriteAccounts() } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function loadBanks() {
  bankLoading.value = true
  try {
    const res = await getTransferBanks()
    banks.value = Array.isArray(res) && res.length ? res : fallbackBanks
    syncFormBankName(form.value.bankCode)
  } catch (e) {
    console.error(e)
    banks.value = fallbackBanks
  } finally {
    bankLoading.value = false
  }
}

function findBankName(code) {
  return banks.value.find((bank) => bank.code === code)?.name || ''
}

function syncFormBankName(code) {
  form.value.bankName = findBankName(code)
}

function syncEditBankName(code) {
  editForm.value.bankName = findBankName(code)
}

async function handleAdd() {
  if (favorites.value.some(f => f.bankCode === form.value.bankCode && f.accountNumber === form.value.accountNumber)) {
    message.warning('此帳號已在常用名單中')
    return
  }
  adding.value = true
  try {
    await addFavoriteAccount({
      accountNumber: form.value.accountNumber,
      bankCode: form.value.bankCode,
      alias: form.value.alias,
      bankName: form.value.bankName || undefined,
    })
    message.success('已新增常用帳號')
    form.value = { accountNumber: '', bankCode: JAVA_BANK_CODE, alias: '', bankName: findBankName(JAVA_BANK_CODE) }
    await loadFavorites()
  } catch (e) {
    message.error(e?.response?.data?.message || '新增失敗')
  } finally {
    adding.value = false
  }
}

function startEdit(record) {
  editForm.value = {
    id: record.id,
    accountNumber: record.accountNumber,
    bankCode: record.bankCode || JAVA_BANK_CODE,
    alias: record.alias,
    bankName: record.bankName || findBankName(record.bankCode || JAVA_BANK_CODE),
  }
  showEdit.value = true
}

async function handleUpdate() {
  updating.value = true
  try {
    await updateFavoriteAccount(editForm.value.id, {
      alias: editForm.value.alias,
      bankCode: editForm.value.bankCode,
      bankName: editForm.value.bankName || undefined,
    })
    message.success('已更新常用帳號')
    showEdit.value = false
    await loadFavorites()
  } catch (e) {
    message.error(e?.response?.data?.message || '更新失敗')
  } finally {
    updating.value = false
  }
}

async function handleDelete(id) {
  try {
    await deleteFavoriteAccount(id)
    message.success('已刪除常用帳號')
    await loadFavorites()
  } catch (e) {
    message.error(e?.response?.data?.message || '刪除失敗')
  }
}
</script>

<style scoped>
.favorite-accounts-page { max-width: 1040px; margin: 0 auto; padding: 32px 28px; }
h2 { margin-bottom: 28px; color: #1a1a2e; }
.form-card { border-radius: 12px; }
.form-card :deep(.ant-card-body) { padding: 44px 48px; }
.favorite-form {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  column-gap: 42px;
  row-gap: 28px;
}
.favorite-form :deep(.ant-form-item) {
  margin-inline-end: 0;
  margin-bottom: 0;
}
.favorite-form :deep(.ant-form-item-label) {
  padding-right: 10px;
}
.submit-item {
  margin-left: 12px;
}
.list-card { border-radius: 12px; }
</style>
