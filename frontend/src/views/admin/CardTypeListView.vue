<script setup>
import { ref, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { BASE_URL } from '@/api/axios'
import {
  getCardTypes,
  createCardType,
  uploadImage,
  deleteCardType,
  updateCardType,
} from '@/api/cardtype'
import { PlusOutlined, SyncOutlined } from '@ant-design/icons-vue'

const cardTypes = ref([])
const loading = ref(true)
let cardTypeRequestId = 0

const file = ref(null)
const form = ref({
  cardTypeName: '',
  cashbackRate: 0,
  brand: 'VISA',
  annualFee: 0,
  cardImageUrl: '',
})
const editingId = ref(null)
const open = ref(false)
const getImageUrl = (path) => {
  if (!path) return ''
  if (path.startsWith('http') || path.startsWith('blob:') || path.startsWith('data:')) return path

  const base = BASE_URL || ''
  const normalizedPath = path.startsWith('/') ? path : `/${path}`

  if (!base) return normalizedPath

  return `${base.replace(/\/+$/, '')}${normalizedPath}`
}
const handleFileChange = (e) => {
  file.value = e.target.files[0]
}

const columns = [
  { title: 'ID', dataIndex: 'cardTypeId', key: 'cardTypeId', width: 80, align: 'center' },
  { title: '卡片名稱', dataIndex: 'cardTypeName', key: 'cardTypeName' },
  {
    title: '卡片圖片',
    dataIndex: 'cardImageUrl',
    key: 'cardImageUrl',
    width: 150,
    align: 'center',
  },
  { title: '回饋率(%)', dataIndex: 'cashbackRate', key: 'cashbackRate' },
  { title: '操作', key: 'action', width: 140, align: 'right', fixed: 'right' },
]

const fetchCardTypes = async () => {
  const requestId = ++cardTypeRequestId
  loading.value = true

  try {
    const data = await getCardTypes()

    if (requestId !== cardTypeRequestId) return

    cardTypes.value = data
  } catch (error) {
    if (requestId !== cardTypeRequestId) return

    message.error(error.response?.data?.message || '讀取資料失敗')
  } finally {
    if (requestId === cardTypeRequestId) {
      loading.value = false
    }
  }
}

const handleCreate = async () => {
  try {
    if (!form.value.cardTypeName) {
      message.error('請輸入卡片名稱')
      return
    }

    let imageUrl = null

    if (file.value) {
      const formData = new FormData()
      formData.append('file', file.value)

      const imageRes = await uploadImage(formData)
      imageUrl = imageRes.url
    }

    if (editingId.value) {
      await updateCardType(editingId.value, {
        cardTypeName: form.value.cardTypeName,
        cashbackRate: form.value.cashbackRate,
        cardImageUrl: imageUrl || form.value.cardImageUrl,
        brand: form.value.brand,
        annualFee: form.value.annualFee,
      })
      message.success('更新成功')
    } else {
      if (!imageUrl) {
        message.error('請選擇圖片')
        return
      }

      await createCardType({
        cardTypeName: form.value.cardTypeName,
        cashbackRate: form.value.cashbackRate,
        cardImageUrl: imageUrl,
        brand: form.value.brand,
        annualFee: form.value.annualFee,
      })
      message.success('新增成功')
    }

    open.value = false
    editingId.value = null
    form.value = {
      cardTypeName: '',
      cashbackRate: 0,
      brand: 'VISA',
      annualFee: 0,
    }
    file.value = null
    await fetchCardTypes()
  } catch (error) {
    message.error(error.response?.data?.message || '新增失敗')
  }
}
const handleEdit = (record) => {
  editingId.value = record.cardTypeId
  form.value.cardTypeName = record.cardTypeName
  form.value.cashbackRate = record.cashbackRate
  form.value.brand = record.brand
  form.value.annualFee = record.annualFee
  form.value.cardImageUrl = record.cardImageUrl
  open.value = true
  file.value = null
}

const handleDelete = (id) => {
  Modal.confirm({
    title: '確定要刪除此卡別嗎？',
    content: '此操作不可復原，若該卡別已被信用卡使用則無法刪除。',
    okText: '確定刪除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await deleteCardType(id)
        message.success('刪除成功')
        fetchCardTypes()
      } catch (error) {
        message.error(error.response?.data?.message || '刪除失敗')
        throw error
      }
    },
  })
}

onMounted(() => {
  fetchCardTypes()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">卡別管理</h2>
    </div>

    <div class="action-bar" style="justify-content: flex-end">
      <div class="global-actions">
        <a-button class="rounded-btn btn-ghost" @click="fetchCardTypes">
          <template #icon><SyncOutlined /></template>
          重新整理
        </a-button>
        <a-button type="primary" class="rounded-btn" @click="open = true">
          <template #icon><PlusOutlined /></template>
          新增卡別
        </a-button>
      </div>
    </div>

    <a-table
      :columns="columns"
      :data-source="cardTypes"
      :loading="loading"
      row-key="cardTypeId"
      class="custom-table"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'cardImageUrl'">
          <a-image
            v-if="record.cardImageUrl"
            :width="100"
            :src="getImageUrl(record.cardImageUrl)"
            :preview="true"
            loading="lazy"
            decoding="async"
          />
          <span v-else>無圖片</span>
        </template>

        <template v-else-if="column.key === 'action'">
          <div class="action-cell">
            <a-button type="link" class="action-btn edit-btn" @click="handleEdit(record)"
              >編輯</a-button
            >
            <a-divider type="vertical" />
            <a-button
              type="link"
              class="action-btn suspend-btn"
              @click="handleDelete(record.cardTypeId)"
              >刪除</a-button
            >
          </div>
        </template>
      </template>
    </a-table>
    <a-modal v-model:open="open" :title="editingId ? '編輯卡別' : '新增卡別'" @ok="handleCreate">
      <a-form layout="vertical">
        <a-form-item label="卡片名稱">
          <a-input v-model:value="form.cardTypeName" />
        </a-form-item>

        <a-form-item label="圖片">
          <input type="file" @change="handleFileChange" />
        </a-form-item>

        <a-form-item label="品牌">
          <a-select v-model:value="form.brand">
            <a-select-option value="VISA">VISA</a-select-option>
            <a-select-option value="MasterCard">MasterCard</a-select-option>
            <a-select-option value="JCB">JCB</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="年費">
          <a-input-number v-model:value="form.annualFee" :min="0" style="width: 100%" />
        </a-form-item>

        <a-form-item label="回饋率(%)">
          <a-input-number v-model:value="form.cashbackRate" min="0" max="100" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.suspend-btn {
  color: #ff4d4f;
}
.suspend-btn:hover {
  color: #d9363e;
  background-color: rgba(255, 77, 79, 0.05);
}
</style>
