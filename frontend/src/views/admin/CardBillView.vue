<script setup>
import { ref, onMounted } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { getBills, generateBills } from '@/api/cardBill'
import { useRouter } from 'vue-router'

const router = useRouter()

const goDetail = (billId) => {
  router.push(`/admin/card-bills/${billId}`)
}

const loading = ref(false)
const generating = ref(false)

const bills = ref([])

const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
})

const searchForm = ref({
  customerName: '',
  billingMonth: '',
  billStatus: ''
})

const columns = [
  {
    title: '帳單編號',
    dataIndex: 'billId',
    key: 'billId',
  },
  {
    title: '客戶姓名',
    dataIndex: 'customerName',
    key: 'customerName',
  },
  {
    title: '帳單月份',
    dataIndex: 'billingMonth',
    key: 'billingMonth',
  },
  {
    title: '帳單金額',
    dataIndex: 'totalAmount',
    key: 'totalAmount',
  },
  {
    title: '最低應繳金額',
    dataIndex: 'minimumPayment',
    key: 'minimumPayment',
  },
  {
    title: '已繳金額',
    dataIndex: 'paidAmount',
    key: 'paidAmount',
  },
  {
    title: '本期回饋金',
    dataIndex: 'cashbackAmount',
    key: 'cashbackAmount',
  },
  {
    title: '回饋狀態',
    dataIndex: 'rewardPosted',
    key: 'rewardPosted',
  },
  {
    title: '剩餘應繳',
    dataIndex: 'remainingAmount',
    key: 'remainingAmount',
  },
  {
    title: '繳費截止日',
    dataIndex: 'dueDate',
    key: 'dueDate',
  },
  {
    title: '狀態',
    dataIndex: 'billStatus',
    key: 'billStatus',
  },
  
]

const fetchBills = async (page = 1) => {
  loading.value = true

  try {
    const response = await getBills({
      page: page - 1,
      size: pagination.value.pageSize,

      customerName: searchForm.value.customerName || undefined,

      billingMonth: searchForm.value.billingMonth || undefined,

      billStatus: searchForm.value.billStatus || undefined,
    })

    console.log(response)

    bills.value = response.content.map((item) => ({
      ...item,
      dueDate: dayjs(item.dueDate).format('YYYY-MM-DD'),
      remainingAmount: item.totalAmount - item.paidAmount,
    }))

    pagination.value.total = response.totalElements
    pagination.value.current = page
  } catch (error) {
    console.error(error)
    message.error('獲取帳單資料失敗')
  } finally {
    loading.value = false
  }
}

const handleGenerateBills = async () => {
  try {
    generating.value = true

    const count = await generateBills()
    message.success(`成功產生 ${count} 筆帳單`)
    await fetchBills()
  } catch (error) {
    console.log(error)
    console.log(error.response?.data?.message)

    message.error(error.response?.data?.message || '生成帳單失敗')
  }finally{
    generating.value = false
  }
}

const handleTableChange = (pager) => {
  fetchBills(pager.current)
}

onMounted(() => {
  fetchBills()
})
</script>

<template>
  <div>
    <!-- Header -->
    <div
      style="
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
      "
    >
      <a-typography-title :level="2" style="margin: 0"> 帳單管理 </a-typography-title>

      <a-button type="primary" @click="handleGenerateBills"> 產生帳單 </a-button>
    </div>
    <!-- Search Form -->
    <a-form layout="inline" :model="searchForm" style="margin-bottom: 16px">
  <a-form-item>
    <a-input
      v-model:value="searchForm.customerName"
      placeholder="客戶姓名"
      allow-clear
    />
  </a-form-item>

  <a-form-item>
    <a-input
      v-model:value="searchForm.billingMonth"
      placeholder="帳單月份"
      allow-clear
    />
  </a-form-item>

  <a-form-item>
    <a-select
      v-model:value="searchForm.billStatus"
      placeholder="帳單狀態"
      allow-clear
      style="width: 140px"
    >
      <a-select-option value="PAID">已繳費</a-select-option>
      <a-select-option value="UNPAID">未繳費</a-select-option>
      <a-select-option value="PARTIAL">部分繳款</a-select-option>
      <a-select-option value="OVERDUE">逾期</a-select-option>
    </a-select>
  </a-form-item>

  <a-form-item>
    <a-button type="primary" @click="fetchBills()">
      搜尋
    </a-button>
  </a-form-item>
</a-form>



    <!-- Table -->
    <a-table
      :columns="columns"
      :data-source="bills"
      :loading="loading"
      :pagination="pagination"
      row-key="billId"
      bordered
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <!-- 帳單金額 -->
        <template v-if="column.dataIndex === 'totalAmount'">
          NT$
          {{ Number(record.totalAmount).toLocaleString() }}
        </template>

        <!-- 最低應繳 -->
        <template v-else-if="column.dataIndex === 'minimumPayment'">
          NT$
          {{ Number(record.minimumPayment).toLocaleString() }}
        </template>

        <!-- 已繳金額 -->
        <template v-else-if="column.dataIndex === 'paidAmount'">
          NT$
          {{ Number(record.paidAmount).toLocaleString() }}
        </template>
        <!-- 本期回饋金 -->
        <template v-else-if="column.dataIndex === 'cashbackAmount'">
          NT$
          {{ Number(record.cashbackAmount || 0).toLocaleString() }}
        </template>

        <!-- 回饋狀態 -->
        <template v-else-if="column.dataIndex === 'rewardPosted'">
          <a-tag color="green" v-if="record.rewardPosted"> 已入帳 </a-tag>
          <a-tag color="default" v-else> 未入帳 </a-tag>
        </template>

        <!-- 剩餘應繳 -->
        <template v-else-if="column.dataIndex === 'remainingAmount'">
          NT$
          {{ Number(record.remainingAmount).toLocaleString() }}
        </template>

        <!-- 帳單狀態 -->
        <template v-else-if="column.key === 'billStatus'">
          <a-tag color="green" v-if="record.billStatus === 'PAID'"> 已繳費 </a-tag>

          <a-tag color="orange" v-else-if="record.billStatus === 'UNPAID'"> 未繳費 </a-tag>
          <a-tag color="blue" v-else-if="record.billStatus === 'PARTIAL'"> 部分繳款 </a-tag>

          <a-tag color="red" v-else> 逾期 </a-tag>
        </template>

        <!-- 操作 -->
        <template v-else-if="column.key === 'action'">
          <a-button type="primary" size="small" @click="goDetail(record.billId)">
            查看明細
          </a-button>
        </template>
      </template>
    </a-table>
  </div>
</template>
