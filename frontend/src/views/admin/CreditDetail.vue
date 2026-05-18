<template>
  <div class="page-container">
    <a-page-header title="客戶信用評分詳情" @back="router.back()">
      <template #extra>
        <a-tag :color="riskColor(credit?.riskLevel)" class="large-risk-badge">
          {{ riskLabel(credit?.riskLevel) }}
        </a-tag>
      </template>
    </a-page-header>

    <a-alert
      message="資料敏感提醒：此頁面顯示客戶完整信用評分，查看行為已記錄。請勿截圖或外流。"
      type="error"
      show-icon
      class="large-alert"
    />

    <a-spin :spinning="loading">
      <a-descriptions
        title="基本資訊"
        bordered
        :column="2"
        size="default"
        style="margin-bottom: 24px"
      >
        <a-descriptions-item label="客戶 ID">{{ credit?.customerId }}</a-descriptions-item>
        <a-descriptions-item label="客戶姓名">{{ credit?.customerName }}</a-descriptions-item>
        <a-descriptions-item label="風險等級">{{
          riskLabel(credit?.riskLevel)
        }}</a-descriptions-item>
        <a-descriptions-item label="最後更新">{{
          formatDate(credit?.lastUpdatedAt)
        }}</a-descriptions-item>
      </a-descriptions>

      <a-descriptions title="評分詳情" bordered :column="2" size="default">
        <a-descriptions-item label="綜合評分">
          <span class="final-score">{{ credit?.finalScore }}</span> 分
        </a-descriptions-item>
        <a-descriptions-item label="聯徵分數">
          <span class="bold-text">{{ credit?.externalScore ?? '—' }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="職業">
          {{ occupationLabel(credit?.occupation) }}
        </a-descriptions-item>
        <a-descriptions-item label="年收入">
          {{
            credit?.annualIncome ? '$ ' + Number(credit.annualIncome).toLocaleString('zh-TW') : '—'
          }}
        </a-descriptions-item>
        <a-descriptions-item label="他行負債">
          {{
            credit?.otherBankDebt
              ? '$ ' + Number(credit.otherBankDebt).toLocaleString('zh-TW')
              : '—'
          }}
        </a-descriptions-item>
        <a-descriptions-item label="有無不動產">
          {{ credit?.hasRealEstate ? '是' : '否' }}
        </a-descriptions-item>
        <a-descriptions-item label="PEP 人士">
          {{ credit?.isPep ? '是' : '否' }}
        </a-descriptions-item>
      </a-descriptions>
    </a-spin>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const BASE_URL = '/api/risk/creditscore'

const credit = ref(null)
const loading = ref(false)

async function fetchDetail() {
  loading.value = true
  try {
    const res = await axios.get(`${BASE_URL}/${route.params.customerId}`, {
      withCredentials: true,
    })
    credit.value = res.data.data
  } catch (e) {
    message.error('載入失敗：' + (e.response?.data?.message || e.message))
  } finally {
    loading.value = false
  }
}

function riskLabel(r) {
  return { HIGH: '高風險', MEDIUM: '中風險', LOW: '低風險' }[r] || r
}

function riskColor(r) {
  return { HIGH: 'red', MEDIUM: 'orange', LOW: 'green' }[r] || 'default'
}

function occupationLabel(o) {
  return (
    {
      LEGISLATOR_MANAGER: '民意代表／高階主管',
      PROFESSIONAL: '專業人員',
      TECHNICIAN: '技術員及助理專業人員',
      CLERICAL: '事務支援人員',
      SERVICE_SALES: '服務及銷售工作人員',
      AGRICULTURAL: '農林漁牧業生產人員',
      CRAFT_WORKER: '技藝有關工作人員',
      MACHINE_OPERATOR: '機械設備操作及組裝人員',
      ELEMENTARY: '基層技術工及勞力工',
      MILITARY: '軍人',
      NONE: '無',
      OTHER: '其他',
    }[o] || o
  )
}

function formatDate(val) {
  if (!val) return '—'
  const cleanStr = val.replace('T', ' ').replace(/-/g, '/')
  return cleanStr.substring(0, 16)
}

onMounted(fetchDetail)
</script>

<style scoped>
/* 補回純白底色與內襯留白，維持 16px 元件間距 */
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  background-color: #ffffff; /* 補回乾淨純白底 */
  padding: 24px; /* 補回四周留白襯托空間 */
  border-radius: 8px; /* 邊緣細緻圓角 */
}

/* 放大頁頭標題 */
:deep(.ant-page-header) {
  padding: 0 0 12px 0 !important; /* 清除內建左右襯底線，維持與純白外殼貼合 */
}

:deep(.ant-page-header-heading-title) {
  font-size: 24px !important;
  font-weight: 600;
  color: #262626 !important;
}

.large-risk-badge {
  font-size: 16px !important;
  padding: 4px 14px !important;
}

.large-alert {
  font-size: 16px !important;
}

/* 放大區塊小標題 */
:deep(.ant-descriptions-title) {
  font-size: 18px !important;
  font-weight: 600;
  color: #262626 !important;
}

/* 放大表格標籤與內容，並統一字體顏色（#262626） */
:deep(.ant-descriptions-item-label) {
  font-size: 16px !important;
  color: #262626 !important;
  font-weight: 500;
  padding: 16px 24px !important;
}

:deep(.ant-descriptions-item-content) {
  font-size: 16px !important;
  color: #262626 !important;
  padding: 16px 24px !important;
}

/* 綜合評分大字：僅放大與加粗，不額外上色 */
.final-score {
  font-size: 28px;
  font-weight: 700;
  color: #262626;
}

.bold-text {
  font-weight: 600;
}
</style>
