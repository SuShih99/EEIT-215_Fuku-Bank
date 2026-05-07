<script setup>
import { ref, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { getTransactions, refundTransaction } from '@/api/cardTxn'
import dayjs from 'dayjs'

const transactions = ref([])
const loading = ref(true)
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
})

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
    const res = await getTransactions(pagination.value.current - 1, pagination.value.pageSize)

    transactions.value = res.content
    console.log(res.content)

    pagination.value.total = res.totalElements
  } catch (error) {
    message.error(error.response?.data?.message || '讀取資料失敗')
  } finally {
    loading.value = false
  }
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

onMounted(() => {
  fetchCardTxns()
})
</script>
<template>
  <div class="p-6">
    <!-- Title -->
    <div class="mb-6">
      <h2 class="text-2xl font-semibold">信用卡交易管理</h2>

      <p class="text-gray-400 mt-1">Credit Card Transactions</p>
    </div>

    <!-- Table -->
    <a-card>
      <a-table
        :columns="columns"
        :data-source="transactions"
        :loading="loading"
        :pagination="pagination"
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

          <!-- 類型 -->
          <template v-else-if="column.dataIndex === 'txnType'">
            <a-tag :color="record.txnType === 'REFUND' ? 'red' : 'blue'">
              {{ record.txnType }}
            </a-tag>
          </template>

          <!-- 刷退按鈕 -->
          <template v-else-if="column.key === 'action'">

  <template v-if="record.txnType === 'REFUND'">

    <a-tag color="red">
      刷退交易
    </a-tag>

  </template>

  <template v-else-if="isRefunded(record)">

    <a-tag color="red">
      已刷退
    </a-tag>

  </template>

  <template v-else>

    <a-button
      danger
      size="small"
      @click="handleRefund(record)"
    >
      刷退
    </a-button>

  </template>

</template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>
