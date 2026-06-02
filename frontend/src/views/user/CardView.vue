<script setup>
import { ref, onMounted } from 'vue'
import { getMyCards, activateCard } from '@/api/userCard'
import { BASE_URL } from '@/api/axios'
import { message } from 'ant-design-vue'

const cards = ref([])
const showActivateModal = ref(false)
const selectedCard = ref(null)

const fillDemoActivateForm = () => {
  activateForm.value.cardNumber = selectedCard.value.cardNumber

  activateForm.value.expiryDate = formatExpiryDate(selectedCard.value.expiryDate)

  activateForm.value.cvv = String(Math.floor(100 + Math.random() * 900))
}

const formatExpiryDate = (date) => {
  if (!date) return ''

  const [year, month] = date.split('-')

  return `${month}/${year.slice(2)}`
}


const fetchCards = async () => {
  try {
    const response = await getMyCards()
    cards.value = response
  } catch (error) {
    console.error('Failed to fetch cards:', error)
  }
}

const getImageUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http') || url.startsWith('blob:') || url.startsWith('data:')) return url

  const base = BASE_URL || ''
  const path = url.startsWith('/') ? url : `/${url}`

  if (!base) return path

  return `${base.replace(/\/+$/, '')}${path}`
}
const handleActivateCard = async (cardId) => {
  try {
    await activateCard(cardId)

    message.success('開卡成功！')

    fetchCards()
  } catch (error) {
    console.error('Failed to activate card:', error)
  }
}




const activateForm = ref({
  cardNumber: '',
  expiryDate: '',
  cvv: '',
})
const openActivateModal = (card) => {
  selectedCard.value = card

  activateForm.value = {
    cardNumber: '',
    expiryDate: '',
    cvv: '',
  }

  showActivateModal.value = true
}
const submitActivateCard = async () => {
  if (!activateForm.value.cvv || activateForm.value.cvv.length !== 3) {
    message.warning('請輸入 3 碼安全碼')
    return
  }

  try {
    await activateCard(selectedCard.value.cardId)

    message.success('開卡成功！')
    showActivateModal.value = false
    await fetchCards()
  } catch (error) {
    console.error(error)
    message.error('開卡失敗')
  }
}

onMounted(() => {
  fetchCards()
})
</script>

<template>
  <div class="page">
    <h1 class="page-title">My Cards</h1>

    <div class="cards-grid">
      <div class="card-wrapper" v-for="card in cards" :key="card.cardId">
        <!-- 卡片上方資訊列 -->
        <div class="card-header">
          <!-- 狀態 -->
          <span class="status-badge" :class="card.status?.toLowerCase()">
            {{ card.status }}
          </span>

          <!-- 開卡按鈕 -->
          <button
            v-if="card.status === 'INACTIVE'"
            class="activate-btn"
            @click="openActivateModal(card)"
          >
            開卡
          </button>
        </div>

        <!-- 信用卡 -->
        <div class="credit-card">
          <!-- 卡片背景 -->
          <img
            class="card-bg"
            :src="getImageUrl(card.cardType?.cardImageUrl)"
            alt=""
            loading="lazy"
            decoding="async"
          />

          <!-- 黑色遮罩 -->
          <div class="card-overlay"></div>

          <!-- 卡片內容 -->
          <div class="card-content">
            <div class="card-brand">
              {{ card.cardType?.brand }}
            </div>

            <div class="card-type">
              {{ card.cardType?.cardTypeName }}
            </div>

            <div class="card-number">
              {{ card.cardNumber }}
            </div>

            <div class="card-footer">
              <div>
                <span class="label">VALID THRU</span>
                <div>{{ formatExpiryDate(card.expiryDate) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div v-if="showActivateModal" class="modal-overlay">
      <div class="modal">
        <h2>信用卡開卡</h2>

        <label>卡號</label>
        <input v-model="activateForm.cardNumber" readonly />

        <label>到期日</label>
        <input v-model="activateForm.expiryDate" readonly />

        <label>安全碼 CVV</label>
        <input v-model="activateForm.cvv" maxlength="3" placeholder="請輸入卡片背面末 3 碼" />
        <button class="demo-fill-btn" @click="fillDemoActivateForm">Demo 一鍵帶入</button>

        <div class="modal-actions">
          <button @click="showActivateModal = false">取消</button>
          <button @click="submitActivateCard">確認開卡</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  padding: 24px;
}

.page-title {
  font-size: 48px;
  margin-bottom: 32px;
  color: #1f2a44;
}

.cards-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
}

.credit-card {
  position: relative;
  width: 380px;
  height: 220px;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.18);
}

.card-bg {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.28);
}

.card-content {
  position: absolute;
  inset: 0;
  padding: 24px;
  display: flex;
  flex-direction: column;
  color: white;
}

.card-brand {
  font-size: 14px;
  opacity: 0.9;
}

.card-type {
  font-size: 24px;
  font-weight: bold;
  margin-top: 4px;
}

.card-number {
  margin-top: auto;
  font-size: 28px;
  letter-spacing: 3px;
  font-weight: 600;
}

.card-footer {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.label {
  font-size: 11px;
  opacity: 0.7;
}
.activate-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 12px;
  background: #22c55e;
  color: white;
  font-weight: bold;
  cursor: pointer;
  transition: 0.2s;
}

.activate-btn:hover {
  background: #16a34a;
}

.status-badge {
  margin-top: 4px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  width: fit-content;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

/* ACTIVE */
.status-badge.active {
  background: #dcfce7;
  color: #166534;
  border: 1px solid #22c55e;
}

/* INACTIVE */
.status-badge.inactive {
  background: #fef3c7;
  color: #92400e;
  border: 1px solid #f59e0b;
}

/* BLOCKED */
.status-badge.blocked {
  background: #fee2e2;
  color: #991b1b;
  border: 1px solid #ef4444;
}

.card-wrapper {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card-header {
  width: 380px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.modal {
  width: 420px;
  background: white;
  padding: 28px;
  border-radius: 20px;
}

.modal input {
  width: 100%;
  margin: 8px 0 16px;
  padding: 10px 12px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
.modal {
  width: 420px;
  background: #fff;
  padding: 28px;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
}

.modal h2 {
  margin-bottom: 18px;
  color: #1f2a44;
}

.modal label {
  display: block;
  margin: 14px 0 6px;
  font-size: 14px;
  font-weight: 700;
  color: #374151;
}

.modal input {
  width: 100%;
  padding: 11px 12px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font-size: 15px;
  outline: none;
  background: white;
  color: #374151;
}



.demo-fill-btn {
  width: 100%;
  margin-top: 12px;
  padding: 10px 14px;
  border: none;
  border-radius: 12px;
  background: #f3f4f6;
  color: #1f2a44;
  font-weight: 700;
  cursor: pointer;
}

.demo-fill-btn:hover {
  background: #e5e7eb;
}

.modal-actions {
  margin-top: 22px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.cancel-btn,
.confirm-btn {
  padding: 10px 18px;
  border-radius: 12px;
  border: none;
  font-weight: 700;
  cursor: pointer;
}

.cancel-btn {
  background: #f3f4f6;
  color: #374151;
}

.confirm-btn {
  background: #22c55e;
  color: white;
}

.confirm-btn:hover {
  background: #16a34a;
}

@media (max-width: 480px) {
  .page {
    padding: 16px;
  }
  .page-title {
    font-size: 32px;
    margin-bottom: 24px;
  }
  .card-header {
    width: 100%;
    max-width: 380px;
  }
  .credit-card {
    width: 100%;
    max-width: 380px;
    height: auto;
    aspect-ratio: 380 / 220;
    border-radius: 16px;
  }
  .card-content {
    padding: 16px;
  }
  .card-brand {
    font-size: 12px;
  }
  .card-type {
    font-size: 20px;
  }
  .card-number {
    font-size: 20px;
    letter-spacing: 2px;
  }
  .card-footer {
    margin-top: auto;
  }
}
</style>
