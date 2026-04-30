<template>
  <div style="padding: 20px">
    <h2>風控事件查詢</h2>
    <div style="margin-bottom: 16px; display: flex; gap: 8px; align-items: center">
      <a-select
        v-model:value="filterParams.eventType"
        placeholder="事件類型"
        style="width: 150px"
        allow-clear
      >
        <a-select-option value="LOAN">貸款</a-select-option>
        <a-select-option value="TRANSFER">轉帳</a-select-option>
        <a-select-option value="USER_LOGIN">登入</a-select-option>
        <a-select-option value="CREDIT_CARD">信用卡</a-select-option>
      </a-select>
      <a-select
        v-model:value="filterParams.riskLevel"
        placeholder="風險等級"
        style="width: 150px"
        allow-clear
      >
        <a-select-option value="LOW">低風險</a-select-option>
        <a-select-option value="MEDIUM">中風險</a-select-option>
        <a-select-option value="HIGH">高風險</a-select-option>
        <a-select-option value="SUSPENDED">拒絕往來/凍結</a-select-option>
      </a-select>
      <a-select
        v-model:value="filterParams.actionTaken"
        placeholder="採取行動"
        style="width: 150px"
        allow-clear
      >
        <a-select-option value="PASSED">通過</a-select-option>
        <a-select-option value="REJECTED">拒絕</a-select-option>
        <a-select-option value="MANUAL_REVIEW">人工審核</a-select-option>
      </a-select>
      <a-button type="primary" @click="loadData">查詢</a-button>
    </div>
    <a-table
      :columns="columns"
      :data-source="dataSource"
      :pagination="pagination"
      :loading="loading"
      @change="handleTableChange"
      row-key="id"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, h } from 'vue'
import axios from 'axios'

const dataSource = ref([])
const loading = ref(false)

const filterParams = reactive({
  eventType: null,
  actionTaken: null,
  riskLevel: null,
})

const columns = [
  { title: 'ID', dataIndex: 'logId' },
  { title: '事件類型', dataIndex: 'eventType' },
  { title: '目標識別碼', dataIndex: 'targetIdentifier' },
  {
    title: '風險等級',
    dataIndex: 'riskLevel',
    customRender: ({ text }) => {
      const colorMap = {
        LOW: 'green',
        MEDIUM: 'orange',
        HIGH: 'red',
        SUSPENDED: 'purple',
      }

      const color = colorMap[text] || 'gray'

      return h('span', { style: { color, fontWeight: 'bold' } }, text)
    },
  },
  { title: '採取行動', dataIndex: 'actionTaken' },
  { title: '時間', dataIndex: 'createdAt' },
]

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/riskevent/search', {
      params: {
        page: pagination.current - 1, // 後端從 0 開始
        size: pagination.pageSize,
        eventType: filterParams.eventType || null,
        actionTaken: filterParams.actionTaken || null,
        riskLevel: filterParams.riskLevel || null,
      },
    })
    // 對接 Spring Page 物件
    dataSource.value = res.data.content
    pagination.total = res.data.totalElements
  } catch (error) {
    console.error('載入失敗', error)
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

onMounted(() => loadData())
</script>
