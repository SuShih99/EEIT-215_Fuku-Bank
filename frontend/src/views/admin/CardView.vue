<script setup>
import { ref, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { getCards, createCard, updateCard, deleteCard } from '@/api/card'
import { PlusOutlined } from '@ant-design/icons-vue'
import api from '@/api/axios'

const cards = ref([])
const loading = ref(false)

const modalVisible = ref(false)
const form = ref({
  annualFee: 0,
})
const isEdit = ref(false)

const currentId = ref(null)

//取得資料
const fetchData = async () => {
  loading.value = true
  try {
    const response = await getCards()
    cards.value = response.data.data
    console.log(response.data)
  } catch (error) {
    console.log(error)
  } finally {
    loading.value = false
  }
}
//編輯資料
const handleEdit = (record) => {
  isEdit.value = true
  currentId.value = record.cardId
  form.value.annualFee = record.annualFee
  modalVisible.value = true
}
//新增資料
const handleCreate = () => {
  isEdit.value = false
  form.value.annualFee = 0
  modalVisible.value = true
}
//刪除資料
const handleDelete = async (record) => {
  Modal.confirm({
    title: '確定刪除嗎?',
    onOk: async () => {
      try {
        await deleteCard(record.cardId)
        await fetchData()
        message.success('刪除成功')
      } catch (error) {
        console.log(error)
      }
    },
  })
}

const columns = [
  { title: 'ID', dataIndex: 'cardId', width: 80 },
  { title: '卡號', dataIndex: 'cardNumber', width: 180 },
  { title: '卡片名稱', key: 'cardTypeName', width: 150 },
  { title: '卡片圖片', key: 'image', width: 120, align: 'center' },
  { title: '額度', dataIndex: 'creditLimit', width: 100 },
  { title: '已用額度', dataIndex: 'currentBalance', width: 100 },
  { title: '狀態', dataIndex: 'status', width: 100 },
  { title: '操作', key: 'action', width: 140, align: 'right', fixed: 'right' },
]
onMounted(() => {
  fetchData()
})
</script>
<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">信用卡卡片管理</h2>
    </div>

    <!-- 頂部 F 橫劃：主操作 -->
    <div class="action-bar" style="justify-content: flex-end;">
      <div class="global-actions">
        <a-button type="primary" class="rounded-btn" @click="handleCreate">
          <template #icon><PlusOutlined /></template>
          新增卡片
        </a-button>
      </div>
    </div>

    <a-table 
      :columns="columns" 
      :data-source="cards" 
      :loading="loading" 
      row-key="cardId"
      class="custom-table"
      :scroll="{ x: 1000 }"
    >
      <template #bodyCell="{ column, record }">
        <!-- 卡片名稱 -->
        <template v-if="column.key === 'cardTypeName'">
          <span style="font-weight: 600">{{ record.cardType?.cardTypeName }}</span>
        </template>

        <!-- 卡片圖片 -->
        <template v-else-if="column.key === 'image'">
          <img :src="`${api.defaults.baseURL}/${record.cardType?.cardImageUrl}`" style="height: 40px; border-radius: 4px;" />
        </template>
        
        <template v-else-if="column.key === 'status'">
          <div :class="['status-tag', `status-${record.status.toLowerCase()}`]">
            <span class="status-dot"></span>
            {{ record.status }}
          </div>
        </template>

        <!-- 操作按鈕處理 -->
        <template v-else-if="column.key === 'action'">
          <div class="action-cell">
            <a-button type="link" class="action-btn edit-btn" @click="handleEdit(record)">編輯</a-button>
            <a-divider type="vertical" />
            <a-button type="link" class="action-btn suspend-btn" @click="handleDelete(record)">刪除</a-button>
          </div>
        </template>
      </template>
    </a-table>
  <a-modal v-model:open="modalVisible" title="卡片">
    <a-form layout="vertical">
      <a-form-item label="年費">
        <a-input-number v-model:value="form.annualFee" style="width: 100%" />
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

/* 狀態標籤 */
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.status-active { background-color: rgba(82, 196, 26, 0.1); color: #389e0d; }
.status-active .status-dot { background-color: #52c41a; }

.status-inactive { background-color: rgba(255, 77, 79, 0.1); color: #d9363e; }
.status-inactive .status-dot { background-color: #ff4d4f; }
</style>
