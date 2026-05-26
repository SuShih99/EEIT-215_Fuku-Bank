<script setup>
import { ref, onMounted,watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { SearchOutlined, SyncOutlined } from '@ant-design/icons-vue'
import { getTransactions, refundTransaction } from '@/api/cardTxn'
import dayjs from 'dayjs'

const transactions = ref([])
const loading = ref(true)
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
})

const keyword = ref('')
const txnType = ref(null)
const dateRange = ref([])

const txnTypeOptions = [
  { label: '全部', value: null },
  { label: '消費', value: 'PURCHASE' },
  { label: '刷退', value: 'REFUND' },
  { label: '年費', value: 'ANNUAL_FEE' },
]

const txnTypeLabelMap = {
  PURCHASE: '消費',
  REFUND: '刷退',
  ANNUAL_FEE: '年費',
}

const getTxnTypeLabel = (typeValue) => txnTypeLabelMap[typeValue] || typeValue

const getTxnTypeColor = (typeValue) => {
  if (typeValue === 'REFUND') return 'red'
  if (typeValue === 'ANNUAL_FEE') return 'gold'
  return 'blue'
}

const columns = [
  {
    title: '交易日期',
    dataIndex: 'txnDate',
    key: 'txnDate',
    width: 220,
    align: 'center',
  },
  {
    title: '客戶姓名',
    dataIndex: 'customerName',
    key: 'customerName',
    align: 'center',
  },
  {
    title: '卡號',
    dataIndex: 'cardNumber',
    key: 'cardNumber',
    align: 'center',
  },
  {
    title: '商家名稱',
    dataIndex: 'merchantName',
    key: 'merchantName',
    align: 'center',
  },
  {
    title: '交易金額',
    dataIndex: 'txnAmount',
    key: 'txnAmount',
    width: 150,
    align: 'center',
  },
  {
    title: '回饋率',
    dataIndex: 'cashbackRate',
    key: 'cashbackRate',
    width: 150,
    align: 'center',
  },
  {
    title: '回饋金額',
    dataIndex: 'cashbackAmount',
    key: 'cashbackAmount',
    width: 150,
    align: 'center',
  },
  {
    title: '交易類型',
    dataIndex: 'txnType',
    key: 'txnType',
    width: 150,
    align: 'center',
  },
  {
    title: '描述',
    dataIndex: 'description',
    key: 'description',
    align: 'center',
  },
  {
    title: '操作刷退',
    key: 'action',
    align: 'center',
    width: 150,
  },
]

// 讀取資料
const fetchCardTxns = async () => {
  loading.value = true

  try {
    const res = await getTransactions({
      page: pagination.value.current - 1,
      size: pagination.value.pageSize,
      keyword:keyword.value,
      txnType: txnType.value,
      startDate: dateRange.value?.[0]
        ? dayjs(dateRange.value[0]).format('YYYY-MM-DD')
        : null,
      endDate: dateRange.value?.[1]
        ? dayjs(dateRange.value[1]).format('YYYY-MM-DD')
        : null,
    })
    transactions.value = res.content

    pagination.value.total = res.totalElements
  } catch (error) {
    message.error(error.response?.data?.message || '讀取資料失敗')
  } finally {
    loading.value = false
  }
}
// 搜尋
const handleSearch = () => {
  pagination.value.current = 1
  fetchCardTxns()
}
// 重設
const handleReset = () => {
  keyword.value = ''
  dateRange.value = []
  txnType.value = null
  pagination.value.current = 1
  fetchCardTxns()
}

//刷退
const handleRefund = (record) => {
  Modal.confirm({
    title: '確認刷退',
    content: `確定要刷退 ${record.txnAmount} 元嗎？`,
    okText: '確認',
    cancelText: '取消',
    async onOk() {
      try {
        await refundTransaction(record.txnId)
        message.success('刷退成功')
        fetchCardTxns()
      } catch (error) {
        message.error(error.response?.data?.message || '刷退失敗')
      }
    },
  })
}
// table page change
const handleTableChange = (pager) => {
  pagination.value.current = pager.current
  pagination.value.pageSize = pager.pageSize
  fetchCardTxns()
}
//已刷退
const isRefunded = (record) => {
  return record.refunded === true
}

watch([keyword, txnType, dateRange], () => {
  pagination.value.current = 1
  fetchCardTxns()
})




onMounted(() => {
  fetchCardTxns()
})
</script>
<template>
  <div class="p-6 admin-card-txn-page">
    <!-- Title -->
    <div class="mb-6">
      <h2 class="text-2xl font-semibold">信用卡交易管理</h2>

      <p class="text-gray-400 mt-1">Credit Card Transactions</p>
    </div>

    <!-- Search -->

    <div class="action-bar">
      <a-input
        v-model:value="keyword"
        placeholder="搜尋客戶姓名 / 商家 / 描述"
        allow-clear
        style="width: 260px"
        @pressEnter="handleSearch"
      >
        <template #prefix><SearchOutlined /></template>
      </a-input>

      <a-range-picker v-model:value="dateRange" format="YYYY-MM-DD" />

      <a-select
        v-model:value="txnType"
        :options="txnTypeOptions"
        placeholder="交易類型"
        allow-clear
        style="width: 140px"
      />

      <a-button type="primary" @click="handleSearch">
        <template #icon><SearchOutlined /></template>
        搜尋
      </a-button>

      <a-button @click="handleReset">
        <template #icon><SyncOutlined /></template>
        重新整理
      </a-button>
    </div>
    <!-- Table -->
    <a-card class="txn-table-card">
      <a-table
        :columns="columns"
        :data-source="transactions"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1280 }"
        row-key="txnId"
        @change="handleTableChange"
      >
        <!-- 客製欄位 -->
        <template #bodyCell="{ column, record }">
          <!-- 日期 -->
          <template v-if="column.dataIndex === 'txnDate'">
            {{ dayjs(record.txnDate).format('YYYY/MM/DD HH:mm') }}
          </template>

          <!-- 金額 -->
          <template v-else-if="column.dataIndex === 'txnAmount'">
            NT$
            {{ Number(record.txnAmount).toLocaleString() }}
          </template>
          <!-- 回饋率 -->
          <template v-else-if="column.dataIndex === 'cashbackRate'">
            {{ record.cashbackRate }}%
          </template>

          <!-- 回饋金 -->
          <template v-else-if="column.dataIndex === 'cashbackAmount'">
            NT$
            {{ Number(record.cashbackAmount).toLocaleString() }}
          </template>

          <!-- 類型 -->
          <template v-else-if="column.dataIndex === 'txnType'">
            <a-tag :color="getTxnTypeColor(record.txnType)">
              {{ getTxnTypeLabel(record.txnType) }}
            </a-tag>
          </template>

          <!-- 刷退按鈕 -->
          <template v-else-if="column.key === 'action'">
            <template v-if="record.txnType === 'REFUND'">
              <a-tag color="red"> 刷退交易 </a-tag>
            </template>

            <template v-else-if="record.txnType === 'ANNUAL_FEE'">
              <a-tag color="gold">年費</a-tag>
            </template>

            <template v-else-if="isRefunded(record)">
              <a-tag color="red"> 已刷退 </a-tag>
            </template>

            <template v-else>
              <a-button danger size="small" @click="handleRefund(record)"> 刷退 </a-button>
            </template>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>
<style scoped>
.action-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.admin-card-txn-page {
  min-width: 0;
  overflow-x: hidden;
}

.txn-table-card {
  max-width: 100%;
  overflow: hidden;
}

.txn-table-card :deep(.ant-card-body) {
  overflow-x: auto;
}

.txn-table-card :deep(.ant-table) {
  min-width: 0;
}

@media (max-width: 768px) {
  .admin-card-txn-page {
    padding: 20px 16px;
  }

  .action-bar {
    display: grid;
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .action-bar :deep(.ant-input-affix-wrapper),
  .action-bar :deep(.ant-picker),
  .action-bar :deep(.ant-select),
  .action-bar :deep(.ant-btn) {
    width: 100% !important;
  }

  .txn-table-card :deep(.ant-card-body) {
    padding: 14px;
  }

  .txn-table-card :deep(.ant-table-wrapper) {
    max-width: 100%;
    overflow-x: auto;
  }
}
</style>
