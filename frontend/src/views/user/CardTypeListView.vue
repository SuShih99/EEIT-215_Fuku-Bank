<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { getUserCardTypes } from '@/api/cardtype'
import { BASE_URL } from '@/api/axios'
import { createCardApplication, getMyApplications } from '@/api/userCardApplication'

const cardTypes = ref([])
const loading = ref(false)

const appliedCardTypeIds = ref([]) // 已申辦的信用卡別 ID 列表

const showApplyModal = ref(false)
const selectedCardType = ref(null)

const hasCardTypes = computed(() => cardTypes.value.length > 0)

function formatMoney(value) {
  if (value === null || value === undefined || value === '') return '-'

  return new Intl.NumberFormat('zh-TW', {
    style: 'currency',
    currency: 'TWD',
    maximumFractionDigits: 0,
  }).format(Number(value))
}

function formatRate(value) {
  if (value === null || value === undefined || value === '') return '-'
  return `${Number(value).toFixed(2)}%`
}

function getImageUrl(path) {
  if (!path) return ''
  return path.startsWith('http') ? path : `${BASE_URL}/${path}`
}

async function fetchCardTypes() {
  loading.value = true

  try {
    cardTypes.value = await getUserCardTypes()
  } catch (error) {
    message.error(error.response?.data?.message || '讀取信用卡別失敗')
  } finally {
    loading.value = false
  }
}
//開啟申辦模態框
function openApplyModal(cardType) {
  selectedCardType.value = cardType
  showApplyModal.value = true
}
//關閉申辦模態框
function closeApplyModal() {
  showApplyModal.value = false
  selectedCardType.value = null
}

async function fetchMyApplications() {
  try {
    const data = await getMyApplications()

    appliedCardTypeIds.value = data.content.map((app) => app.cardTypeId)
  } catch (error) {
    console.error(error)
  }
}

async function applyCardType() {
  if (!selectedCardType.value) return

  try {
    await createCardApplication({ cardTypeId: selectedCardType.value.cardTypeId })

    appliedCardTypeIds.value.push(selectedCardType.value.cardTypeId)

    message.success(`成功申辦 ${selectedCardType.value.cardTypeName}！`)
    closeApplyModal()
  } catch (error) {
    message.error(error.response?.data?.message || '申辦失敗')
  }
}

onMounted(() => {
  fetchCardTypes()
  fetchMyApplications()
})


</script>

<template>
  <div class="customer-page card-type-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">信用卡別</h2>
        <p class="page-subtitle">選擇適合你的卡片，查看年費與現金回饋。</p>
      </div>
      <button class="jb-btn jb-btn-secondary jb-btn-sm" type="button" @click="fetchCardTypes">
        重新整理
      </button>
    </div>

    <div v-if="loading" class="state-panel">
      <span class="jb-spinner" aria-hidden="true"></span>
      <span>讀取中...</span>
    </div>

    <div v-else-if="hasCardTypes" class="card-type-grid">
      <article v-for="card in cardTypes" :key="card.cardTypeId" class="card-type-card">
        <div class="card-image-wrap">
          <img
            v-if="card.cardImageUrl"
            class="card-image"
            :src="getImageUrl(card.cardImageUrl)"
            :alt="card.cardTypeName"
          />
          <div v-else class="card-image-placeholder" aria-hidden="true">
            <span>{{ card.cardTypeName?.slice(0, 2) || '卡片' }}</span>
          </div>
        </div>

        <div class="card-info">
          <p v-if="card.brand" class="card-brand">{{ card.brand }}</p>
          <h3 class="card-name">{{ card.cardTypeName }}</h3>

          <dl class="card-meta">
            <div>
              <dt>年費</dt>
              <dd>{{ formatMoney(card.annualFee) }}</dd>
            </div>
            <div>
              <dt>現金回饋</dt>
              <dd>{{ formatRate(card.cashbackRate) }}</dd>
            </div>
          </dl>
          <button
            class="jb-btn jb-btn-primary jb-btn-sm"
            type="button"
            :disabled="appliedCardTypeIds.includes(card.cardTypeId)"
            @click="openApplyModal(card)"
          >
            {{ appliedCardTypeIds.includes(card.cardTypeId) ? '已申辦' : '立即申辦' }}
          </button>
        </div>
      </article>
    </div>

    <div v-else class="state-panel">
      <span>目前沒有可申辦的信用卡別。</span>
    </div>

    <!-- 申辦模態框 -->
    <div v-if="showApplyModal" class="modal-overlay">
      <div class="modal-box">
        <h3 class="modal-title">確認申辦</h3>

        <p class="modal-text">
          是否確認申辦
          <span class="font-semibold">
            {{ selectedCardType?.cardTypeName }}
          </span>
          ？
        </p>

        <div class="modal-actions">
          <button class="modal-cancel" @click="closeApplyModal">取消</button>

          <button class="modal-confirm" @click="applyCardType">確認申辦</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card-type-page {
  max-width: 960px;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}

.page-title {
  font-family: var(--font-heading);
  font-size: var(--text-h2);
  margin-bottom: var(--space-1);
  letter-spacing: 0;
}

.page-subtitle {
  color: var(--text-secondary);
  font-size: var(--text-sm);
}

.card-type-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
}

.card-type-card {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: var(--space-4);
  align-items: center;
  background: var(--bg-card);
  border: 1px solid rgba(214, 206, 195, 0.55);
  border-radius: var(--radius-md);
  padding: var(--space-4);
  box-shadow: var(--shadow-sm);
}

.card-image-wrap {
  width: 100%;
  aspect-ratio: 1.58 / 1;
  border-radius: var(--radius-sm);
  overflow: hidden;
  background: var(--bg-secondary);
}

.card-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.card-image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary);
  font-weight: 600;
  background: var(--primary-light);
}

.card-brand {
  color: var(--text-secondary);
  font-size: var(--text-xs);
  margin-bottom: var(--space-1);
}

.card-name {
  font-size: var(--text-h3);
  line-height: 1.25;
  margin-bottom: var(--space-3);
  letter-spacing: 0;
}

.card-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
  margin: 0;
}

.card-meta div {
  min-width: 0;
}

.card-meta dt {
  color: var(--text-secondary);
  font-size: var(--text-xs);
  margin-bottom: 4px;
}

.card-meta dd {
  color: var(--text-primary);
  font-size: var(--text-body);
  font-weight: 600;
  margin: 0;
}

.state-panel {
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  color: var(--text-secondary);
  background: var(--bg-secondary);
  border: 1px solid rgba(214, 206, 195, 0.55);
  border-radius: var(--radius-md);
}

@media (max-width: 900px) {
  .card-type-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .page-head {
    flex-direction: column;
  }

  .card-type-card {
    grid-template-columns: 1fr;
  }

  .card-image-wrap {
    max-width: 320px;
  }
}
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.modal-box {
  width: 360px;
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
}

.modal-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 12px;
}

.modal-text {
  color: #4b5563;
  line-height: 1.6;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.modal-cancel,
.modal-confirm {
  border: none;
  padding: 10px 16px;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 600;
}

.modal-cancel {
  background: #e5e7eb;
}

.modal-confirm {
  background: #1d4ed8;
  color: white;
}
</style>
