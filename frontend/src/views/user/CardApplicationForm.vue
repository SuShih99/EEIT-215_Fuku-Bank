<script setup>
import { ref, computed, onBeforeUnmount, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { createCardApplication, getMyApplications } from '@/api/userCardApplication'
import { getUserCardTypes, uploadImage } from '@/api/cardtype'
import { BASE_URL } from '@/api/axios'
import dayjs from 'dayjs'
import { addMyApplicationDocument } from '@/api/userCardAppDoc'

const listLoading = ref(false)
const submitting = ref(false)
const applications = ref([])
const cardTypes = ref([])
const form = ref({
  cardTypeId: '',
  remark: '',
})

const uploadModalVisible = ref(false)
const currentApplication = ref(null)

const uploadForm = ref({
  fileName: '',
  fileUrl: '',
})
const proofFiles = ref([])
const previewVisible = ref(false)
const previewImage = ref('')
const previewTitle = ref('')

const applicationRows = computed(() =>
  applications.value.flatMap((app) => {
    if (!app.items?.length) {
      return [{
        ...app,
        isFirstItem: true
      }]
    }

    return app.items.map((item, index) => ({
      ...app,
      ...item,
      rowKey: `${app.applicationId}-${item.itemId}`,
      itemResult: item.result,
      remark: app.remark,
      isFirstItem: index === 0,
    }))
  }),
)

const columns = [
  {
    key: 'applicationId',
    label: '申請編號',
  },
  {
    key: 'applyDate',
    label: '申請時間',
  },
  {
    key: 'cardTypeName',
    label: '卡別',
  },
  {
    key: 'status',
    label: '狀態',
  },
  {
    key: 'document',
    label: '財力證明',
  },
  {
    key: 'remark',
    label: '備註',
  },
  {
    key: 'itemResult',
    label: '審核結果',
  },
]

const getStatusType = (status) => {
  switch (status) {
    case 'COMPLETED':
      return 'success'
    case 'RESUBMITTED':
      return 'info'
    case 'REJECTED':
      return 'danger'
    default:
      return 'warning'
  }
}

const getStatusText = (status) => {
  switch (status) {
    case 'COMPLETED':
      return '已完成'
    case 'NEED_SUPPLEMENT':
      return '需補件'
    case 'RESUBMITTED':
      return '已補件'
    case 'REJECTED':
      return '拒絕'
    default:
      return '審核中'
  }
}
const getItemResultText = (result) => {
  switch (result) {
    case 'APPROVED':
      return '核准'
    case 'REJECTED':
      return '拒絕'
    case 'PENDING':
      return '待審核'
    default:
      return '-'
  }
}

const fetchCardTypes = async () => {
  try {
    cardTypes.value = await getUserCardTypes()
  } catch (error) {
    console.error(error)
    message.error(error.response?.data?.message || '無法取得卡別資料')
  }
}

const fetchApplications = async () => {
  listLoading.value = true

  try {
    const data = await getMyApplications()
    applications.value = data.content || []
  } catch (error) {
    console.error(error)
    message.error(error.response?.data?.message || '取得申請紀錄失敗')
  } finally {
    listLoading.value = false
  }
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
    revokeProofPreviewUrls(uploadedFiles)
    message.error(error.response?.data?.message || '圖片上傳失敗')
  }
}

const submitApplication = async () => {
  if (!form.value.cardTypeId) {
    message.warning('請先選擇卡別')
    return
  }

  submitting.value = true

  try {
    await createCardApplication({
      cardTypeId: Number(form.value.cardTypeId),
      remark: form.value.remark,
    })
    message.success('信用卡申請已送出')
    form.value.cardTypeId = ''
    form.value.remark = ''
    await fetchApplications()
  } catch (error) {
    console.error(error)
    message.error(error.response?.data?.message || '信用卡申請送出失敗')
  } finally {
    submitting.value = false
  }
}

const openUploadModal = (app) => {
  currentApplication.value = app
  uploadForm.value = {
    fileName: '',
    fileUrl: '',
  }
  proofFiles.value = []
  uploadModalVisible.value = true
}

const closeUploadModal = () => {
  revokeProofPreviewUrls()
  proofFiles.value = []
  closeProofPreview()
  uploadModalVisible.value = false
  currentApplication.value = null
}

const handleUploadDocument = async () => {
  const validFiles = proofFiles.value.filter((file) => file.fileName && file.fileUrl)

  if (validFiles.length === 0) {
    message.warning('請輸入檔案路徑')
    return
  }

  if (validFiles.length > 3) {
    message.warning('最多只能上傳三個財力證明')
    return
  }

  try {
    for (const file of validFiles) {
      await addMyApplicationDocument(currentApplication.value.applicationId, {
        fileName: file.fileName,
        fileUrl: file.fileUrl,
      })
    }

    message.success('財力證明已上傳')
    closeUploadModal()
    await fetchApplications()
  } catch (error) {
    console.error(error)
    message.error(error.response?.data?.message || '上傳失敗')
  }
}



onMounted(() => {
  fetchCardTypes()
  fetchApplications()
})

onBeforeUnmount(() => {
  revokeProofPreviewUrls()
})
</script>

<template>
  <div class="card-application-page">
    <!-- <section class="application-form-section">
      <div class="page-head">
        <div>
          <h2 class="page-title">信用卡申請</h2>
          <p class="page-subtitle">送出申請後，可在下方查看審核進度。</p>
        </div>
      </div>

      <form class="application-form" @submit.prevent="submitApplication">
        <label class="field">
          <span>卡別</span>
          <select v-model="form.cardTypeId" required>
            <option value="" disabled>請選擇卡別</option>
            <option
              v-for="cardType in cardTypes"
              :key="cardType.cardTypeId"
              :value="cardType.cardTypeId"
            >
              {{ cardType.cardTypeName }}
            </option>
          </select>
        </label>

        <label class="field">
          <span>備註</span>
          <textarea
            v-model.trim="form.remark"
            rows="4"
            maxlength="200"
            placeholder="可填寫想補充給審核人員的資訊"
          ></textarea>
        </label>

        <button class="jb-btn jb-btn-primary" type="submit" :disabled="submitting">
          {{ submitting ? '送出中...' : '送出申請' }}
        </button>
      </form>
    </section> -->

    <section class="application-list-section">
      <div class="section-head">
        <h3 class="section-title">我的申請紀錄</h3>
        <button class="jb-btn jb-btn-secondary jb-btn-sm" type="button" @click="fetchApplications">
          重新整理
        </button>
      </div>

      <div v-if="listLoading" class="state-panel">載入中...</div>

      <div v-else-if="applicationRows.length === 0" class="state-panel">目前沒有信用卡申請紀錄。</div>

      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th v-for="column in columns" :key="column.key">
                {{ column.label }}
              </th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="app in applicationRows" :key="app.rowKey || app.applicationId">
              <td v-for="column in columns" :key="column.key">
                <span
                  v-if="column.key === 'status'"
                  class="status-pill"
                  :class="`status-${getStatusType(app.status)}`"
                >
                  {{ getStatusText(app.status) }}
                </span>
                <span v-else-if="column.key === 'itemResult'">
                  {{ getItemResultText(app.itemResult) }}
                </span>
                <span v-else-if="column.key === 'document'">
                  <button
                    v-if="app.status === 'NEED_SUPPLEMENT' && app.isFirstItem"  
                    class="jb-btn jb-btn-secondary jb-btn-sm"
                    type="button"
                    @click="openUploadModal(app)"
                  >
                    補件上傳
                  </button>
                  <span v-else>-</span>
                </span>

                <template v-else>
                  {{
                    column.key === 'applyDate'
                      ? dayjs(app.applyDate).format('YYYY/MM/DD HH:mm')
                      : app[column.key] || '-'
                  }}
                </template>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <div
      v-if="uploadModalVisible"
      class="modal-overlay"
      @click.self="closeUploadModal"
    >
      <div class="modal-box">
        <h3 class="modal-title">上傳財力證明</h3>

        <div class="proof-section">
          <div class="proof-head">
            <span>財力證明</span>
          </div>

          <label class="upload-box">
            <input
              type="file"
              accept="image/*"
              multiple
              hidden
              @change="handleProofImagesChange"
            />
            <span>點擊上傳財力證明（最多 3 張）</span>
          </label>

          <div v-if="proofFiles.length" class="proof-preview-list">
            <div
              v-for="(file, index) in proofFiles"
              :key="index"
              class="proof-preview"
            >
              <button
                class="proof-image-button"
                type="button"
                @click="openProofPreview(file)"
              >
                <img
                  :src="getProofImageSrc(file)"
                  :alt="file.fileName || '財力證明預覽'"
                />
              </button>

              <div class="proof-info">
                <span>{{ file.fileName }}</span>
                <button
                  type="button"
                  class="remove-btn"
                  @click="removeProofFile(index)"
                >
                  移除
                </button>
              </div>
            </div>
          </div>

          <p class="proof-tip">最多上傳 3 個財力證明</p>
        </div>

        <div class="modal-actions">
          <button
            class="modal-cancel"
            type="button"
            @click="closeUploadModal"
          >
            取消
          </button>

          <button
            class="modal-confirm"
            type="button"
            @click="handleUploadDocument"
          >
            確認上傳
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="previewVisible"
      class="image-preview-overlay"
      @click.self="closeProofPreview"
    >
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
.card-application-page {
  display: grid;
  gap: var(--space-6);
}

.page-head,
.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
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

.section-title {
  font-size: var(--text-h3);
  letter-spacing: 0;
}

.application-form {
  display: grid;
  gap: var(--space-4);
  max-width: 640px;
}

.field {
  display: grid;
  gap: var(--space-2);
  color: var(--text-primary);
  font-weight: 600;
}

.field select,
.field textarea {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: var(--space-3);
  background: var(--bg-card);
  color: var(--text-primary);
  font: inherit;
  font-weight: 400;
}

.field select {
  min-height: 44px;
}

.field textarea {
  resize: vertical;
}

.field textarea:focus {
  outline: 2px solid var(--primary-light);
  border-color: var(--primary);
}

.table-wrap {
  overflow-x: auto;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-card);
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  padding: var(--space-3);
  border-bottom: 1px solid var(--border);
  text-align: left;
  white-space: nowrap;
}

th {
  color: var(--text-secondary);
  font-size: var(--text-xs);
  font-weight: 600;
  background: var(--bg-secondary);
}

tbody tr:last-child td {
  border-bottom: none;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 var(--space-2);
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  font-weight: 600;
}

.status-success {
  color: #237804;
  background: #f6ffed;
}

.status-danger {
  color: #a8071a;
  background: #fff1f0;
}

.status-warning {
  color: #ad6800;
  background: #fffbe6;
}

.status-info {
  color: #096dd9;
  background: #e6f7ff;
}

.state-panel {
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
}

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(0, 0, 0, 0.45);
}

.modal-box {
  width: 520px;
  max-width: 90vw;
  max-height: 90vh;
  overflow-y: auto;
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
}

.modal-title {
  margin-bottom: 12px;
  font-size: 20px;
  font-weight: 700;
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
  color: white;
  background: #1d4ed8;
}

.proof-section {
  margin-top: 16px;
}

.proof-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-weight: 700;
}

@media (max-width: 700px) {
  .page-head,
  .section-head {
    flex-direction: column;
  }
}
.field select,
.field textarea,
.field input {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: var(--space-3);
  background: var(--bg-card);
  color: var(--text-primary);
  font: inherit;
  font-weight: 400;
}

.upload-box {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 96px;
  border: 2px dashed #d1d5db;
  border-radius: 12px;
  padding: 16px;
  color: #6b7280;
  background: #f9fafb;
  cursor: pointer;
  font-weight: 600;
}

.upload-box:hover {
  border-color: #2563eb;
  background: #eff6ff;
}

.proof-preview-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.proof-preview {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.proof-image-button {
  width: 100%;
  border: 0;
  padding: 0;
  background: transparent;
  border-radius: 10px;
  cursor: zoom-in;
}

.proof-image-button:focus-visible {
  outline: 3px solid #93c5fd;
  outline-offset: 2px;
}

.proof-preview img {
  display: block;
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  border: 1px solid #d1d5db;
  border-radius: 10px;
}

.proof-info {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 6px;
}

.proof-info span {
  overflow: hidden;
  color: #374151;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.remove-btn {
  border: none;
  border-radius: 10px;
  padding: 6px 10px;
  color: #b91c1c;
  background: #fee2e2;
  cursor: pointer;
  font-size: 13px;
  white-space: nowrap;
}

.proof-tip {
  margin: 10px 0 0;
  color: #6b7280;
  font-size: 13px;
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
</style>
