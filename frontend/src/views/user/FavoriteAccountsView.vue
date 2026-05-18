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
    <section class="list-card">
      <div class="overflow-x-auto rounded-[16px] bg-white/60 p-4">
        <table class="w-full min-w-[820px] border-collapse text-left text-[14px] text-[var(--text-primary)]">
          <thead class="bg-[rgba(245,241,234,0.84)]">
            <tr>
              <th class="rounded-tl-[10px] border-b border-[rgba(214,206,195,0.72)] px-4 py-3 font-semibold">帳號</th>
              <th class="border-b border-[rgba(214,206,195,0.72)] px-4 py-3 font-semibold">銀行代號</th>
              <th class="border-b border-[rgba(214,206,195,0.72)] px-4 py-3 font-semibold">備註名稱</th>
              <th class="border-b border-[rgba(214,206,195,0.72)] px-4 py-3 font-semibold">銀行名稱</th>
              <th class="border-b border-[rgba(214,206,195,0.72)] px-4 py-3 font-semibold">建立時間</th>
              <th class="rounded-tr-[10px] border-b border-[rgba(214,206,195,0.72)] px-4 py-3 font-semibold">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="6" class="border-b border-[rgba(214,206,195,0.42)] px-4 py-14 text-center text-[var(--text-secondary)]">
                資料載入中
              </td>
            </tr>
            <tr v-else-if="favorites.length === 0">
              <td colspan="6" class="border-b border-[rgba(214,206,195,0.42)] px-4 py-14 text-center text-[var(--text-secondary)]">
                尚無常用帳號
              </td>
            </tr>
            <template v-else>
              <tr
                v-for="record in favorites"
                :key="record.id"
                class="transition-colors hover:bg-[rgba(92,107,95,0.045)]"
              >
                <td class="border-b border-[rgba(214,206,195,0.42)] px-4 py-3 font-medium">{{ record.accountNumber }}</td>
                <td class="border-b border-[rgba(214,206,195,0.42)] px-4 py-3">{{ record.bankCode || '-' }}</td>
                <td class="border-b border-[rgba(214,206,195,0.42)] px-4 py-3">{{ record.alias || '-' }}</td>
                <td class="border-b border-[rgba(214,206,195,0.42)] px-4 py-3">{{ record.bankName || '-' }}</td>
                <td class="border-b border-[rgba(214,206,195,0.42)] px-4 py-3">{{ record.createdAt || '-' }}</td>
                <td class="border-b border-[rgba(214,206,195,0.42)] px-4 py-3">
                  <div class="flex items-center gap-2">
                    <button
                      type="button"
                      class="rounded-[8px] border border-[rgba(214,206,195,0.86)] bg-white/50 px-3 py-1.5 text-[13px] font-medium text-[var(--primary-dark)] transition hover:border-[var(--primary)] hover:bg-[var(--primary-light)]"
                      @click="startEdit(record)"
                    >
                      編輯
                    </button>
                    <button
                      type="button"
                      class="rounded-[8px] border border-[rgba(166,90,77,0.32)] bg-[rgba(166,90,77,0.08)] px-3 py-1.5 text-[13px] font-medium text-[var(--accent)] transition hover:bg-[rgba(166,90,77,0.14)]"
                      @click="confirmDelete(record.id)"
                    >
                      刪除
                    </button>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>
    </section>

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

function confirmDelete(id) {
  if (window.confirm('確定要刪除此常用帳號？')) {
    handleDelete(id)
  }
}
</script>

<style scoped>
.favorite-accounts-page {
  max-width: 1040px;
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

.form-card :deep(.ant-card-body) {
  padding: 28px;
}

.favorite-form {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  column-gap: 24px;
  row-gap: 18px;
}

.favorite-form :deep(.ant-form-item) {
  margin-inline-end: 0;
  margin-bottom: 0;
}

.favorite-form :deep(.ant-form-item-label) {
  padding-right: 8px;
}

.favorite-form :deep(.ant-form-item-label > label) {
  color: var(--text-primary);
  font-weight: 600;
}

.favorite-form :deep(.ant-input),
.favorite-form :deep(.ant-select-selector) {
  border-color: rgba(198, 188, 174, 0.92);
  border-radius: 8px;
  background: rgba(250, 250, 247, 0.84);
}

.favorite-form :deep(.ant-input:focus),
.favorite-form :deep(.ant-input-focused),
.favorite-form :deep(.ant-select-focused .ant-select-selector) {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}

.favorite-form :deep(.ant-btn-primary),
.favorite-accounts-page :deep(.ant-modal .ant-btn-primary) {
  border-color: var(--primary);
  background: var(--primary);
  box-shadow: 0 6px 14px rgba(63, 74, 66, 0.14);
}

.favorite-form :deep(.ant-btn-primary:hover),
.favorite-accounts-page :deep(.ant-modal .ant-btn-primary:hover) {
  border-color: var(--primary-dark);
  background: var(--primary-dark);
}

.submit-item {
  margin-left: 0;
}

.list-card {
  margin-top: 24px;
  padding: 24px;
  border-radius: 12px;
  background: rgba(255, 249, 239, 0.92);
  border: 1px solid rgba(214, 206, 195, 0.86);
  box-shadow: 0 12px 36px rgba(63, 74, 66, 0.08);
}

@media (max-width: 640px) {
  .favorite-accounts-page {
    padding: 16px 0;
  }

  .form-card :deep(.ant-card-body) {
    padding: 20px;
  }

  .favorite-form,
  .favorite-form :deep(.ant-form-item),
  .favorite-form :deep(.ant-input),
  .favorite-form :deep(.ant-select) {
    width: 100% !important;
  }
}
</style>
