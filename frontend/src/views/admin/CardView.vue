<script setup>
import { ref, onMounted, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { getCards, createCard, updateCard, deleteCard, blockCard, unblockCard } from '@/api/card'
import { PlusOutlined, SearchOutlined, SyncOutlined } from '@ant-design/icons-vue'
import api from '@/api/axios'

const cards = ref([])
const loading = ref(false)

const keyword = ref('')
const status = ref(null)

const statusOptions = [
  { label: '啟用中', value: 'ACTIVE' },
  { label: '未開卡', value: 'INACTIVE' },
  { label: '已停卡', value: 'BLOCKED' },
]

const cardStatusLabelMap = {
  ACTIVE: '啟用中',
  INACTIVE: '未開卡',
  BLOCKED: '已停卡',
}

const getCardStatusLabel = (statusValue) => cardStatusLabelMap[statusValue] || statusValue
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
})

const modalVisible = ref(false)
const form = ref({
  creditLimit: 0,
})
const isEdit = ref(false)

const currentId = ref(null)

//取得資料
const fetchData = async () => {
  loading.value = true
  try {
    const response = await getCards({
      page: pagination.value.current - 1,
      size: pagination.value.pageSize,
      keyword: keyword.value,
      status: status.value,
    })
    cards.value = response.data.data.content
    pagination.value.total = response.data.data.totalElements
    console.log(response.data.data.content)
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
  form.value.creditLimit = record.creditLimit
  modalVisible.value = true
}
//新增資料
const handleCreate = () => {
  isEdit.value = false
  form.value.creditLimit = 0
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

//停用卡片
const handleBlock = async (record) => {
  try {
    await blockCard(record.cardId)
    await fetchData()
    message.success('停用成功')
  } catch (error) {
    console.log(error)
  }
}

//啟用卡片
const handleUnblock = async (record) => {
  try {
    await unblockCard(record.cardId)
    await fetchData()
    message.success('啟用成功')
  } catch (error) {
    console.log(error)
  }
}

const columns = [
  { title: 'ID', dataIndex: 'cardId', width: 80 },
  { title: '客戶姓名', dataIndex: 'customerName', width: 150 },
  { title: '卡號', dataIndex: 'cardNumber', width: 180 },
  { title: '卡片名稱', key: 'cardTypeName', width: 150 },
  { title: '卡片圖片', key: 'image', width: 120, align: 'center' },
  { title: '額度', dataIndex: 'creditLimit', width: 100 },
  { title: '已用額度', dataIndex: 'currentDebt', width: 100 },
  { title: '狀態', dataIndex: 'status', width: 100 },
  { title: '編輯額度', key: 'edit', width: 80 },
  { title: '卡片狀態操作', key: 'statusAction', width: 120 },
]

const handleSubmit = async () => {
  try {
    await updateCard(currentId.value, form.value)
    message.success('更新成功')
    modalVisible.value = false
    fetchData()
  } catch (error) {
    console.log(error)
    message.error(error.response?.data?.message || (isEdit.value ? '更新卡片失敗' : '新增卡片失敗'))
  }
}

const handleSearch = () => {
  pagination.value.current = 1
  fetchData()
}

const handleRefresh = () => {
  pagination.value.current = 1
  keyword.value = ''
  status.value = null
  fetchData()
}

const handlePageChange = (page) => {
  pagination.value.current = page.current
  pagination.value.pageSize = page.pageSize
  fetchData()
}
watch([keyword, status], () => {
  pagination.value.current = 1
  fetchData()
})

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
    <div class="action-bar">
      <!-- 左側搜尋 -->
      <div class="search-group">
        <a-input
          v-model:value="keyword"
          placeholder="搜尋客戶姓名 / 卡別名稱"
          class="rounded-input search-input"
          allow-clear
          @pressEnter="handleSearch"
        >
          <template #prefix>
            <SearchOutlined style="color: #bfbfbf" />
          </template>
        </a-input>

        <a-select
          v-model:value="status"
          :options="statusOptions"
          placeholder="狀態"
          style="width: 140px"
          allow-clear
        />

        <a-button type="primary" class="rounded-btn" @click="handleSearch">
          <template #icon><SearchOutlined /></template>
          搜尋
        </a-button>
      </div>

      <!-- 右側新增 -->
      <div class="global-actions">
        <a-button class="rounded-btn btn-ghost" @click="handleRefresh">
          <template #icon><SyncOutlined /></template>
          重新整理
        </a-button>
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
      @change="handlePageChange"
      :pagination="pagination"
    >
      <template #bodyCell="{ column, record }">
        <!-- 卡片名稱 -->
        <template v-if="column.key === 'cardTypeName'">
          <span style="font-weight: 600">{{ record.cardType?.cardTypeName }}</span>
        </template>

        <!-- 卡片圖片 -->
        <template v-else-if="column.key === 'image'">
          <img
            :src="`${api.defaults.baseURL}/${record.cardType?.cardImageUrl}`"
            style="height: 40px; border-radius: 4px"
          />
        </template>
        <!-- 信用額度 -->
        <template v-else-if="column.dataIndex === 'creditLimit'">
          NT$
          {{ Number(record.creditLimit).toLocaleString() }}
        </template>

        <!-- 已用額度 -->
        <template v-else-if="column.dataIndex === 'currentDebt'">
          NT$
          {{ Number(record.currentDebt).toLocaleString() }}
        </template>

        <template v-else-if="column.dataIndex === 'status'">
          <div :class="['status-tag', `status-${record.status.toLowerCase()}`]">
            <span class="status-dot"></span>
            {{ getCardStatusLabel(record.status) }}
          </div>
        </template>

        <!-- 操作按鈕處理 -->
        <template v-else-if="column.key === 'edit'">
          <div class="action-cell">
            <a-button type="link" class="action-btn edit-btn" @click="handleEdit(record)">
              編輯
            </a-button>
          </div>
        </template>

        <template v-else-if="column.key === 'statusAction'">
          <div class="action-cell">
            <!-- ACTIVE 顯示停卡 -->
            <a-button
              v-if="record.status === 'ACTIVE'"
              type="link"
              class="action-btn suspend-btn"
              @click="handleBlock(record)"
            >
              停卡
            </a-button>

            <!-- BLOCKED 顯示解除停卡 -->
            <a-button
              v-else-if="record.status === 'BLOCKED'"
              type="link"
              class="action-btn active-btn"
              @click="handleUnblock(record)"
            >
              解除停卡
            </a-button>

            <!-- INACTIVE 顯示開卡 -->
            <a-button
              v-else-if="record.status === 'INACTIVE'"
              type="link"
              class="action-btn active-btn"
            >
              未開卡
            </a-button>
          </div>
        </template>
      </template>
    </a-table>
    <a-modal v-model:open="modalVisible" title="卡片" @ok="handleSubmit">
      <a-form layout="vertical">
        <a-form-item label="信用額度">
          <a-input-number v-model:value="form.creditLimit" style="width: 100%" />
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

.status-active {
  background-color: rgba(82, 196, 26, 0.1);
  color: #389e0d;
}
.status-active .status-dot {
  background-color: #52c41a;
}

.status-inactive {
  background-color: rgba(140, 140, 140, 0.1);
}
.status-inactive .status-dot {
  background-color: #8c8c8c;
}

.active-btn {
  color: #52c41a;
}

.active-btn:hover {
  color: #389e0d;
  background-color: rgba(82, 196, 26, 0.05);
}
.status-blocked {
  background-color: rgba(255, 77, 79, 0.1);
  color: #d9363e;
}

.status-blocked .status-dot {
  background-color: #ff4d4f;
}
.action-btn {
  padding: 0;
  height: auto;
}
.action-cell {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
}
</style>
