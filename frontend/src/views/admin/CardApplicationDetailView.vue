<script setup>
import { ref, onMounted, h } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  getApplicationItems,
  approveApplicationItem,
  rejectApplicationItem,
  getApplicationById,
} from '@/api/cardApplication'
import { useRoute } from 'vue-router'

const loading = ref(false)

const route = useRoute()
const items = ref([])
const applicationInfo = ref({})

//查詢申請明細
const fetchDetail = async () => {
  const id = route.params.id
  loading.value = true
  //主申請資料
  applicationInfo.value = await getApplicationById(id)
  console.log(applicationInfo.value)

  //明細
  const itemsData = await getApplicationItems(id)
  console.log('items', itemsData)
  items.value = itemsData.map(item => ({
  ...item,
  customLimit: item.approvedLimit
}))

  loading.value = false
}
const columns = [
  { title: '卡別', dataIndex: 'cardTypeName', key: 'cardTypeName' },
  {
    title: '額度(元)',
    dataIndex: 'approvedLimit',
    key: 'approvedLimit',
    customRender: ({ text }) => {
      return text ? Number(text).toLocaleString() : '無'
    },
  },
  {
    title: '設定核准額度',
    dataIndex: 'customLimit',
    key: 'customLimit',
  },
  {
    title: '年費(元)',
    dataIndex: 'annualFee',
    key: 'annualFee',
    customRender: ({ text }) => {
      return text ? Number(text).toLocaleString() : '0'
    },
  },
  { title: '狀態', dataIndex: 'result', key: 'result' },
  { title: '審核時間', dataIndex: 'reviewDate', key: 'reviewDate' },
  { title: '操作', key: 'action' },
]

// 核准
const approveItem = async (record) => {
  Modal.confirm({
    title: '確認核准此卡片申請？',
    content: '核准後將更新申請狀態',

    async onOk() {
      try {
        const updated = await approveApplicationItem(record.itemId, record.customLimit || record.approvedLimit)

        const index = items.value.findIndex((item) => item.itemId === record.itemId)

        if (index !== -1) {
          items.value[index] = updated
        }

        message.success('核准成功')
      } catch (err) {
        console.error(err)
        message.error('核准失敗')
      }
    },
  })
}
// 拒絕
const rejectItem = async (record) => {
  let remark = ''
  Modal.confirm({
    title: '拒絕申請',

    content: h('textarea', {
      style: 'width:100%; min-height:100px; padding:8px;',
      placeholder: '請輸入拒絕原因',

      onInput: (e) => {
        remark = e.target.value
      },
    }),

    async onOk() {
      try {
        const updated = await rejectApplicationItem(record.itemId, remark)

        const index = items.value.findIndex((item) => item.itemId === record.itemId)

        if (index !== -1) {
          items.value[index] = updated
        }

        message.success('已拒絕')
      } catch (err) {
        console.error(err)
        message.error('操作失敗')
      }
    },
  })
}

onMounted(() => {
  fetchDetail()
})
</script>
<template>
  <div>
    <h2>申請明細</h2>
    <a-card style="margin-bottom: 20px">
      <a-row :gutter="24">
        <a-col :span="6">
          <strong>申請編號：</strong>
          {{ applicationInfo.applicationId }}
        </a-col>

        <a-col :span="6">
          <strong>客戶姓名：</strong>
          {{ applicationInfo.customerName }}
        </a-col>

        <a-col :span="6">
          <strong>申請日期：</strong>
          {{ applicationInfo.applyDate }}
        </a-col>

        <a-col :span="6">
          <strong>狀態：</strong>

          <a-tag v-if="applicationInfo.status === 'APPROVED'" color="success"> APPROVED </a-tag>

          <a-tag v-else-if="applicationInfo.status === 'REJECTED'" color="error"> REJECTED </a-tag>

          <a-tag v-else color="warning"> PENDING </a-tag>
        </a-col>
      </a-row>
    </a-card>
    <a-table :columns="columns" :data-source="items" :loading="loading" row-key="itemId" bordered>
      <template #bodyCell="{ column, record }">
        <!-- 狀態 -->
        <template v-if="column.dataIndex === 'result'">
          <a-tag v-if="record.result === 'APPROVED'" color="success"> 核准 </a-tag>

          <a-tag v-else-if="record.result === 'REJECTED'" color="error"> 拒絕 </a-tag>

          <a-tag v-else color="warning"> 審核中 </a-tag>
        </template>

        <!-- 審核時間 -->
        <template v-if="column.dataIndex === 'reviewDate'">
          <span v-if="record.reviewDate">
            {{ new Date(record.reviewDate).toLocaleString() }}
          </span>

          <span v-else> - </span>
        </template>
        <!-- 核准額度 -->
        <template v-if="column.dataIndex === 'customLimit'">
          <a-input-number
            v-model:value="record.customLimit"
            :min="10000"
            :step="1000"
            style="width: 120px"
            :disabled="record.result !== 'PENDING'"
          />
        </template>

        <!-- 操作 -->
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button
              type="primary"
              :disabled="record.result !== 'PENDING'"
              @click="approveItem(record)"
            >
              核准
            </a-button>

            <a-button danger :disabled="record.result !== 'PENDING'" @click="rejectItem(record)">
              拒絕
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>
