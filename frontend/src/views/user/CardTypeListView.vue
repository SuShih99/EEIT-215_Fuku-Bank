<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { getUserCardTypes, uploadImage } from '@/api/cardtype'
import { BASE_URL } from '@/api/axios'
import { createCardApplication, getMyApplications } from '@/api/userCardApplication'
import { addMyApplicationDocument } from '@/api/userCardAppDoc'
const cardTypes = ref([])
const loading = ref(false)
const loadError = ref('')
let cardTypeRequestId = 0

const appliedCardTypeIds = ref([])

const showApplyModal = ref(false)
const selectedCards = ref([])

const hasCardTypes = computed(() => Array.isArray(cardTypes.value) && cardTypes.value.length > 0)

const proofFiles = ref([])
const previewVisible = ref(false)
const previewImage = ref('')
const previewTitle = ref('')

const cartCardTypeIds = computed(() => selectedCards.value.map((card) => card.cardTypeId))

function addToCart(card) {
  if (appliedCardTypeIds.value.includes(card.cardTypeId)) {
    message.warning('此卡已申辦')
    return
  }

  if (cartCardTypeIds.value.includes(card.cardTypeId)) {
    message.warning('此卡已加入申辦清單')
    return
  }

  selectedCards.value.push(card)
}

function removeFromCart(cardTypeId) {
  selectedCards.value = selectedCards.value.filter((card) => card.cardTypeId !== cardTypeId)
}

function openApplyModal() {
  if (selectedCards.value.length === 0) {
    message.warning('請先加入至少一張卡片')
    return
  }

  proofFiles.value = []
  showApplyModal.value = true
}

function formatMoney(value) {
  if (value === null || value === undefined || value === '') return '-'

  return new Intl.NumberFormat('zh-TW', {
    style: 'currency',
    currency: 'TWD',
    maximumFractionDigits: 0,
  }).format(Number(value))
}
function addProofFile() {
  if (proofFiles.value.length >= 3) {
    message.warning('最多只能上傳三個財力證明')
    return
  }

  proofFiles.value.push({ fileName: '', fileUrl: '' })
}

function revokeProofPreviewUrls(files = proofFiles.value) {
  files.forEach((file) => {
    if (file.previewUrl?.startsWith('blob:')) {
      URL.revokeObjectURL(file.previewUrl)
    }
  })
}

function removeProofFile(index) {
  const removedFiles = proofFiles.value.splice(index, 1)
  revokeProofPreviewUrls(removedFiles)
}

function formatRate(value) {
  if (value === null || value === undefined || value === '') return '-'
  return `${Number(value).toFixed(2)}%`
}

function normalizeImagePath(value) {
  if (!value) return ''
  if (typeof value === 'string') return value
  return value.url || value.fileUrl || value.data?.url || ''
}

function getImageUrl(path) {
  const imagePath = normalizeImagePath(path)

  if (!imagePath) return ''
  if (
    imagePath.startsWith('http') ||
    imagePath.startsWith('blob:') ||
    imagePath.startsWith('data:')
  ) {
    return imagePath
  }

  const base = BASE_URL || ''
  const normalizedPath = imagePath.startsWith('/') ? imagePath : `/${imagePath}`

  if (!base) return normalizedPath

  return `${base.replace(/\/+$/, '')}${normalizedPath}`
}

function getProofImageSrc(file) {
  return file?.previewUrl || getImageUrl(file?.fileUrl)
}

function openProofPreview(file) {
  const imageSrc = getProofImageSrc(file)

  if (!imageSrc) return

  previewImage.value = imageSrc
  previewTitle.value = file.fileName || '財力證明預覽'
  previewVisible.value = true
}

function closeProofPreview() {
  previewVisible.value = false
  previewImage.value = ''
  previewTitle.value = ''
}

async function handleProofImagesChange(event) {
  const files = Array.from(event.target.files || [])
  const uploadedFiles = []

  if (files.length === 0) return

  const availableSlots = 3 - proofFiles.value.length

  if (availableSlots <= 0) {
    message.warning('最多只能上傳三個財力證明')
    event.target.value = ''
    return
  }

  if (files.length > availableSlots) {
    message.warning(`最多只能再上傳 ${availableSlots} 張財力證明`)
    event.target.value = ''
    return
  }

  try {
    for (const file of files) {
      const formData = new FormData()
      formData.append('file', file)

      const uploadResult = await uploadImage(formData)

      uploadedFiles.push({
        fileName: file.name,
        fileUrl: normalizeImagePath(uploadResult),
        previewUrl: URL.createObjectURL(file),
      })
    }

    proofFiles.value = [...proofFiles.value, ...uploadedFiles]
    event.target.value = ''

    message.success('圖片上傳成功')
  } catch (error) {
    uploadedFiles.forEach((file) => {
      if (file.previewUrl?.startsWith('blob:')) {
        URL.revokeObjectURL(file.previewUrl)
      }
    })
    message.error(error.response?.data?.message || '圖片上傳失敗')
  }
}

async function fetchCardTypes() {
  const requestId = ++cardTypeRequestId
  loading.value = true
  loadError.value = ''

  try {
    const data = await getUserCardTypes()

    if (requestId !== cardTypeRequestId) return

    cardTypes.value = data
  } catch (error) {
    if (requestId !== cardTypeRequestId) return

    loadError.value = error.response?.data?.message || error.message || '讀取信用卡別失敗'
    message.error(loadError.value)
  } finally {
    if (requestId === cardTypeRequestId) {
      loading.value = false
    }
  }
}

function closeApplyModal() {
  revokeProofPreviewUrls()
  proofFiles.value = []
  closeProofPreview()
  showApplyModal.value = false
}

async function fetchMyApplications() {
  try {
    const data = await getMyApplications()
    const applications = Array.isArray(data) ? data : data?.content || []
    const ids = applications.flatMap((app) => [
      app.cardTypeId,
      ...(Array.isArray(app.items) ? app.items.map((item) => item.cardTypeId) : []),
    ])

    appliedCardTypeIds.value = [...new Set(ids.filter((id) => id !== null && id !== undefined))]
  } catch (error) {
    console.error(error)
  }
}

async function applyCardType() {
  if (selectedCards.value.length === 0) {
    message.warning('請先選擇卡片')
    return
  }

  const duplicateCards = selectedCards.value.filter((card) =>
    appliedCardTypeIds.value.includes(card.cardTypeId),
  )

  if (duplicateCards.length > 0) {
    message.warning(`${duplicateCards.map((card) => card.cardTypeName).join('、')} 已申辦`)
    selectedCards.value = selectedCards.value.filter(
      (card) => !appliedCardTypeIds.value.includes(card.cardTypeId),
    )
    return
  }

  const validFiles = proofFiles.value.filter((file) => file.fileName && file.fileUrl)

  if (validFiles.length === 0) {
    message.warning('請至少上傳一個財力證明')
    return
  }

  if (validFiles.length > 3) {
    message.warning('最多只能上傳三個財力證明')
    return
  }

  try {
    const application = await createCardApplication({
      cardTypeIds: selectedCards.value.map((card) => card.cardTypeId),
    })

    for (const file of validFiles) {
      await addMyApplicationDocument(application.applicationId, file)
    }

    appliedCardTypeIds.value.push(...selectedCards.value.map((card) => card.cardTypeId))

    message.success('信用卡申請已送出')
    selectedCards.value = []
    closeApplyModal()
  } catch (error) {
    message.error(error.response?.data?.message || '申辦失敗')
  }
}

const isScrolled = ref(false)
const cartExpanded = ref(false)

function handleScroll(e) {
  const scrollTop = e.target.scrollTop || window.scrollY || document.documentElement.scrollTop || 0
  isScrolled.value = scrollTop > 150
  if (!isScrolled.value) {
    cartExpanded.value = false
  }
}

onMounted(() => {
  fetchCardTypes()
  fetchMyApplications()
  window.addEventListener('scroll', handleScroll, { capture: true, passive: true })
})

onBeforeUnmount(() => {
  revokeProofPreviewUrls()
  window.removeEventListener('scroll', handleScroll, { capture: true })
})
</script>

<template>
  <div class="customer-page card-type-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">信用卡別</h2>
        <p class="page-subtitle">選擇適合你的卡片，查看年費與現金回饋。</p>
      </div>

      <div class="page-head-actions">
        <button 
          class="desktop-cart-btn jb-btn jb-btn-primary jb-btn-sm" 
          type="button" 
          @click="cartExpanded = true"
        >
          申辦清單 ({{ selectedCards.length }})
        </button>
        <button class="jb-btn jb-btn-secondary jb-btn-sm" type="button" @click="fetchCardTypes">
          重新整理
        </button>
      </div>
    </div>

    <div v-if="loading" class="state-panel">
      <span class="jb-spinner" aria-hidden="true"></span>
      <span>讀取中...</span>
    </div>

    <template v-else-if="hasCardTypes">
      <div class="card-layout">
        <div class="card-type-grid">
          <article v-for="card in cardTypes" :key="card.cardTypeId" class="card-type-card">
            <div class="card-image-wrap">
              <img
                v-if="card.cardImageUrl"
                class="card-image"
                :src="getImageUrl(card.cardImageUrl)"
                :alt="card.cardTypeName"
                loading="lazy"
                decoding="async"
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
                :disabled="
                  appliedCardTypeIds.includes(card.cardTypeId) ||
                  cartCardTypeIds.includes(card.cardTypeId)
                "
                @click="addToCart(card)"
              >
                {{
                  appliedCardTypeIds.includes(card.cardTypeId)
                    ? '已申辦'
                    : cartCardTypeIds.includes(card.cardTypeId)
                      ? '已加入'
                      : '加入申辦清單'
                }}
              </button>
            </div>
          </article>
        </div>

        <a-drawer
          v-model:open="cartExpanded"
          title="申辦清單"
          placement="right"
          :width="360"
        >
          <div v-if="selectedCards.length === 0" class="cart-empty">
            尚未加入卡片
          </div>

          <div v-else class="cart-list">
            <div v-for="card in selectedCards" :key="card.cardTypeId" class="cart-item">
              <span>{{ card.cardTypeName }}</span>

              <button type="button" class="remove-btn" @click="removeFromCart(card.cardTypeId)">
                移除
              </button>
            </div>

            <button class="modal-confirm cart-submit" type="button" @click="cartExpanded = false; openApplyModal()">
              立即申辦
            </button>
          </div>
        </a-drawer>

        <!-- 手機版 Sticky Bottom Bar -->
        <div class="application-summary-bar mobile-cart-bar">
          <span class="summary-text">已選 {{ selectedCards.length }} 張卡</span>
          <button 
            class="jb-btn jb-btn-sm" 
            :class="selectedCards.length > 0 ? 'jb-btn-primary' : 'jb-btn-secondary'"
            :disabled="selectedCards.length === 0"
            @click="selectedCards.length > 0 ? (cartExpanded = true) : null"
          >
            查看清單
          </button>
        </div>
      </div>
    </template>

    <div v-else-if="loadError" class="state-panel">
      <span>{{ loadError }}</span>
      <button class="jb-btn jb-btn-secondary jb-btn-sm" type="button" @click="fetchCardTypes">
        重新讀取
      </button>
    </div>

    <div v-else class="state-panel">
      <span>目前沒有可申辦的信用卡別。</span>
    </div>

    <div v-if="showApplyModal" class="modal-overlay">
      <div class="modal-box">
        <h3 class="modal-title">確認申辦</h3>

        <p class="modal-text">
          是否確認申辦
          <span class="font-semibold">
            {{ selectedCards.map((card) => card.cardTypeName).join('、') }}
          </span>
          ？
        </p>

        <div class="proof-section">
          <div class="proof-head">
            <span>財力證明</span>
          </div>

          <label class="upload-box">
            <input type="file" accept="image/*" multiple hidden @change="handleProofImagesChange" />
            <div class="upload-placeholder">點擊上傳財力證明（最多 3 張）</div>
          </label>

          <div v-if="proofFiles.length" class="proof-preview-list">
            <div v-for="(file, index) in proofFiles" :key="index" class="proof-preview">
              <button class="proof-image-button" type="button" @click="openProofPreview(file)">
                <img :src="getProofImageSrc(file)" :alt="file.fileName || '財力證明預覽'" />
              </button>

              <div class="proof-info">
                <span>{{ file.fileName }}</span>

                <button type="button" class="remove-btn" @click="removeProofFile(index)">
                  移除
                </button>
              </div>
            </div>
          </div>

          <p class="proof-tip">最多上傳 3 個財力證明</p>
        </div>

        <div class="modal-actions">
          <button class="modal-cancel" @click="closeApplyModal">取消</button>
          <button class="modal-confirm" @click="applyCardType">確認申辦</button>
        </div>
      </div>
    </div>

    <div v-if="previewVisible" class="image-preview-overlay" @click.self="closeProofPreview">
      <div class="image-preview-dialog" role="dialog" aria-modal="true">
        <div class="image-preview-head">
          <h3>{{ previewTitle }}</h3>

          <button
            class="image-preview-close"
            type="button"
            aria-label="關閉預覽"
            @click="closeProofPreview"
          >
            x
          </button>
        </div>

        <img :src="previewImage" :alt="previewTitle" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.card-type-page {
  max-width: 1080px;
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
  grid-template-columns: repeat(2, minmax(280px, 1fr));
  gap: 20px;
}

.card-type-card {
  display: flex;
  flex-direction: column;
  gap: 18px;
  background: var(--bg-card);
  border: 1px solid rgba(214, 206, 195, 0.55);
  border-radius: 16px;
  padding: 22px;
  box-shadow: var(--shadow-sm);
  min-width: 0;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.card-type-card:hover {
  transform: translateY(-2px);
  border-color: rgba(92, 107, 95, 0.28);
  box-shadow: 0 16px 34px rgba(43, 38, 31, 0.1);
}

.card-image-wrap {
  width: 100%;
  max-width: 260px;
  aspect-ratio: 1.58 / 1;
  border-radius: var(--radius-sm);
  overflow: hidden;
  background: var(--bg-secondary);
  align-self: center;
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
  line-height: 1.4;
}

.card-name {
  font-size: 22px;
  line-height: 1.28;
  margin-bottom: 16px;
  letter-spacing: 0;
  word-break: keep-all;
}

.card-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 0 0 18px;
}

.card-meta div {
  min-width: 0;
}

.card-meta dt {
  color: var(--text-secondary);
  font-size: var(--text-xs);
  margin-bottom: 4px;
  white-space: nowrap;
}

.card-meta dd {
  color: var(--text-primary);
  font-size: var(--text-body);
  font-weight: 600;
  margin: 0;
  white-space: nowrap;
}

.card-info {
  min-width: 0;
  width: 100%;
}

.card-info .jb-btn {
  width: 100%;
  justify-content: center;
  white-space: nowrap;
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


.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
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
  background: var(--primary);
  color: white;
}

.modal-confirm:hover {
  background: var(--primary-dark);
}
.proof-section {
  margin-top: 16px;
}

.proof-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-weight: 700;
}

.proof-row {
  display: grid;
  gap: 8px;
  margin-bottom: 10px;
}

.proof-row input {
  width: 100%;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  padding: 10px;
}

.proof-tip {
  color: #6b7280;
  font-size: 13px;
}

.remove-btn {
  border: none;
  background: #fee2e2;
  color: #b91c1c;
  padding: 6px 10px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 13px;
  white-space: nowrap;
}

.upload-box:hover {
  border-color: var(--primary);
  background: var(--bg-secondary);
}
.modal-box {
  width: 520px;
  max-width: 90vw;
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
}

.upload-box {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 96px;
  border: 2px dashed #d1d5db;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  background: #f9fafb;
}

.upload-placeholder {
  width: 100%;
  text-align: center;
  color: #6b7280;
  font-weight: 600;
}

.proof-preview {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.proof-preview-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.proof-image-button {
  width: 100%;
  border: 0;
  padding: 0;
  background: transparent;
  cursor: zoom-in;
  border-radius: 10px;
}

.proof-image-button:focus-visible {
  outline: 3px solid rgba(92, 107, 95, 0.35);
  outline-offset: 2px;
}

.proof-preview img {
  width: 100%;
  aspect-ratio: 1 / 1;
  height: auto;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  display: block;
}

.proof-info {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 6px;
}

.proof-preview span {
  color: #374151;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.image-preview-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(0, 0, 0, 0.68);
}

.image-preview-dialog {
  width: min(880px, 92vw);
  max-height: 90vh;
  padding: 16px;
  background: #ffffff;
  border-radius: 14px;
  box-shadow: 0 18px 44px rgba(0, 0, 0, 0.25);
}

.image-preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.image-preview-head h3 {
  margin: 0;
  overflow: hidden;
  color: #111827;
  font-size: 16px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.image-preview-close {
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 50%;
  color: #374151;
  background: #f3f4f6;
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
}

.image-preview-dialog > img {
  display: block;
  width: 100%;
  max-height: calc(90vh - 84px);
  object-fit: contain;
  border-radius: 10px;
  background: #f9fafb;
}

@media (max-width: 520px) {
  .proof-preview-list {
    grid-template-columns: 1fr;
  }
}


.cart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.cart-head h3 {
  margin: 0;
  font-size: 18px;
  letter-spacing: 0;
}

.cart-head span {
  flex-shrink: 0;
  color: var(--text-primary);
  font-weight: 700;
}

.cart-empty {
  color: #6b7280;
  font-size: 14px;
  padding: 12px 0 2px;
}

.cart-list {
  display: grid;
  gap: 12px;
}

.cart-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  background: #f9fafb;
  border: 1px solid #eef0ee;
  border-radius: 12px;
}

.cart-item span {
  min-width: 0;
  overflow: hidden;
  color: var(--text-primary);
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cart-submit {
  margin-top: 8px;
  width: 100%;
}
.card-layout {
  display: block;
}

.page-head-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.application-summary-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 40;
  padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
  background: rgba(250, 247, 240, 0.92);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-top: 1px solid rgba(80, 72, 60, 0.12);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.summary-text {
  font-weight: 700;
  color: #1a1a2e;
  font-size: 15px;
}

@media (min-width: 901px) {
  .mobile-cart-bar {
    display: none !important;
  }
}

@media (max-width: 900px) {
  .desktop-cart-btn {
    display: none !important;
  }

  .card-layout {
    padding-bottom: calc(72px + env(safe-area-inset-bottom));
  }

  .card-type-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .page-head {
    flex-direction: column;
  }

  .card-image-wrap {
    max-width: 320px;
  }
}
</style>
