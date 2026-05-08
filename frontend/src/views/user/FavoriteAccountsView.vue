<template>
  <div class="favorite-accounts-page">
    <h2>常用帳號管理</h2>

    <!-- 新增常用帳號 -->
    <a-card class="form-card">
      <a-form :model="form" layout="inline" @finish="handleAdd">
        <a-form-item label="帳號" name="accountNumber" :rules="[{ required: true, message: '請輸入帳號' }]">
          <a-input v-model:value="form.accountNumber" placeholder="12碼帳號" style="width: 200px" />
        </a-form-item>
        <a-form-item label="備註名稱" name="alias" :rules="[{ required: true, message: '請輸入備註名稱' }]">
          <a-input v-model:value="form.alias" placeholder="例如：房東、公司帳戶" style="width: 200px" :maxlength="50" />
        </a-form-item>
        <a-form-item label="銀行名稱">
          <a-input v-model:value="form.bankName" placeholder="選填" style="width: 160px" :maxlength="50" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" :loading="adding">新增常用帳號</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 常用帳號列表 -->
    <a-card class="list-card" style="margin-top: 20px">
      <a-table :dataSource="favorites" :columns="columns" :loading="loading"
               rowKey="id" :pagination="{ pageSize: 10 }">
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
        <a-form-item label="備註名稱">
          <a-input v-model:value="editForm.alias" placeholder="備註名稱" :maxlength="50" />
        </a-form-item>
        <a-form-item label="銀行名稱">
          <a-input v-model:value="editForm.bankName" placeholder="選填" :maxlength="50" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getFavoriteAccounts, addFavoriteAccount, updateFavoriteAccount, deleteFavoriteAccount } from '@/api/favoriteAccount'

const form = ref({ accountNumber: '', alias: '', bankName: '' })
const editForm = ref({ id: null, accountNumber: '', alias: '', bankName: '' })
const favorites = ref([])
const loading = ref(false)
const adding = ref(false)
const updating = ref(false)
const showEdit = ref(false)

const columns = [
  { title: '帳號', dataIndex: 'accountNumber', key: 'accountNumber', width: 180 },
  { title: '備註名稱', dataIndex: 'alias', key: 'alias' },
  { title: '銀行名稱', dataIndex: 'bankName', key: 'bankName' },
  { title: '建立時間', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 160 },
]

onMounted(() => loadFavorites())

async function loadFavorites() {
  loading.value = true
  try { favorites.value = await getFavoriteAccounts() } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function handleAdd() {
  if (favorites.value.some(f => f.accountNumber === form.value.accountNumber)) {
    message.warning('此帳號已在常用名單中')
    return
  }
  adding.value = true
  try {
    await addFavoriteAccount({
      accountNumber: form.value.accountNumber,
      alias: form.value.alias,
      bankName: form.value.bankName || undefined,
    })
    message.success('已新增常用帳號')
    form.value = { accountNumber: '', alias: '', bankName: '' }
    await loadFavorites()
  } catch (e) {
    message.error(e?.response?.data?.message || '新增失敗')
  } finally {
    adding.value = false
  }
}

function startEdit(record) {
  editForm.value = { id: record.id, accountNumber: record.accountNumber, alias: record.alias, bankName: record.bankName || '' }
  showEdit.value = true
}

async function handleUpdate() {
  updating.value = true
  try {
    await updateFavoriteAccount(editForm.value.id, {
      alias: editForm.value.alias,
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
.favorite-accounts-page { max-width: 960px; margin: 0 auto; padding: 24px; }
h2 { margin-bottom: 20px; color: #1a1a2e; }
.form-card { border-radius: 12px; }
.list-card { border-radius: 12px; }
</style>
