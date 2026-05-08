<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">風險管理 - 黑名單系統</h2>
    </div>

    <!-- 功能列 -->
    <div class="action-bar" style="justify-content: flex-end;">
      <div class="global-actions">
        <a-button type="primary" class="rounded-btn" @click="openModal('create')">
          <template #icon><PlusOutlined /></template>
          新增黑名單
        </a-button>
      </div>
    </div>

    <!-- 數據表格 -->
    <a-table
      :columns="columns"
      :data-source="dataSource"
      :pagination="pagination"
      :loading="loading"
      @change="handleTableChange"
      row-key="id"
      class="custom-table"
    >
      <!-- 狀態欄位插槽 -->
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'listType'">
          <a-tag color="blue">{{ typeMap[record.listType] || record.listType }}</a-tag>
        </template>
        
        <template v-else-if="column.key === 'status'">
          <a-switch
            v-model:checked="record.status"
            @change="(checked) => handleStatusChange(record, checked)"
          />
        </template>

        <!-- 操作欄位 -->
        <template v-else-if="column.key === 'action'">
          <div class="action-cell">
            <a-button type="link" class="action-btn edit-btn" @click="openModal('edit', record)">編輯</a-button>
          </div>
        </template>
      </template>
    </a-table>

    <!-- 新增/編輯 彈窗 -->
    <a-modal
      v-model:visible="modalVisible"
      :title="modalType === 'create' ? '新增黑名單' : '編輯黑名單'"
      @ok="handleModalOk"
    >
      <a-form :model="formData" layout="vertical">
        <a-form-item label="類型">
          <a-select v-model:value="formData.type" :disabled="modalType === 'edit'">
            <a-select-option value="ID_CARD">身份證字號</a-select-option>
            <a-select-option value="ACCOUNT_NO">帳戶</a-select-option>
            <a-select-option value="EMAIL">電子郵件</a-select-option>
            <a-select-option value="PHONE">電話</a-select-option>
            <!--<a-select-option value="IP">IP 位址</a-select-option>-->
          </a-select>
        </a-form-item>
        <a-form-item label="數值">
          <a-input v-model:value="formData.listValue" :disabled="modalType === 'edit'" />
        </a-form-item>
        <a-form-item label="備註原因">
          <a-textarea v-model:value="formData.reason" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import api from '@/api/axios'

// --- 資料定義 ---
const loading = ref(false)
const dataSource = ref([])
const modalVisible = ref(false)
const modalType = ref('create') // 'create' or 'edit'

const formData = reactive({
  listType: 'ID_CARD',
  listValue: '',
  reason: '',
  status: true,
})
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const typeMap = {
  ID_CARD: '身份證字號',
  ACCOUNT_NO: '帳戶',
  EMAIL: '電子郵件',
  PHONE: '電話',
}

const columns = [
  { title: '類型', dataIndex: 'listType', key: 'listType', width: 120 },
  { title: '數值', dataIndex: 'listValue', key: 'listValue', width: 180 },
  { title: '原因', dataIndex: 'reason', key: 'reason' },
  { title: '狀態', dataIndex: 'status', key: 'status', width: 100 },
  { title: '建立時間', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 100, align: 'right', fixed: 'right' },
]

// --- 邏輯處理 ---

// 獲取列表資料
const fetchList = async () => {
  loading.value = true
  try {
    // 對應後端 @GetMapping
    const res = await api.get('/api/blacklist', {
      params: {
        page: pagination.current - 1, // Spring Pageable 從 0 開始
        size: pagination.pageSize,
        sort: 'createdAt,desc',
      },
    })
    dataSource.value = res.data.data.content
    pagination.total = res.data.data.totalElements
  } catch {
    message.error('讀取失敗')
  } finally {
    loading.value = false
  }
}

// 切換狀態
const handleStatusChange = async (record, checked) => {
  try {
    // 對應後端 @PutMapping("/{type}/{value}/status")
    await api.put(`/api/blacklist/${record.listType}/${record.listValue}/status`, null, {
      params: { enabled: checked },
    })
    message.success('狀態更新成功')
  } catch {
    record.status = !checked // 失敗時還原開關狀態
    message.error('狀態更新失敗')
  }
}

// 打開彈窗
const openModal = (type, record = null) => {
  modalType.value = type
  if (type === 'edit' && record) {
    Object.assign(formData, record)
  } else {
    Object.assign(formData, { listType: 'ID_CARD', listValue: '', reason: '', status: true })
  }
  modalVisible.value = true
}

// 提交彈窗表單
const handleModalOk = async () => {
  try {
    if (modalType.value === 'create') {
      // 對應後端 @PostMapping("/create")
      await api.post('/api/blacklist/create', formData)
      message.success('新增成功')
    } else {
      // 對應後端 @PostMapping("/{type}/{value}/update")
      await api.post(`/api/blacklist/${formData.listType}/${formData.listValue}/update`, formData)
      message.success('更新成功')
    }
    modalVisible.value = false
    fetchList()
  } catch {
    message.error('操作失敗')
  }
}

// 處理表格換頁
const handleTableChange = (pag) => {
  pagination.current = pag.current
  fetchList()
}

onMounted(() => {
  fetchList()
})
</script>
