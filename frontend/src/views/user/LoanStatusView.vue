<template>
  <div class="loan-status-page">

    <!-- ── Page Header ── -->
    <div class="page-header">
      <div class="page-header-inner">
        <div class="page-title-block">
          <span class="page-icon"><i class="fa-solid fa-file-lines"></i></span>
          <div>
            <h1 class="page-title">我的貸款申請</h1>
            <p class="page-subtitle">查看所有已提交的貸款申請狀態</p>
          </div>
        </div>
        <button class="btn-apply" @click="$router.push({ name: 'user-loan-apply' })">
          + 新增申請
        </button>
      </div>
    </div>

    <div class="page-body">

      <!-- ── Loading ── -->
      <div v-if="loading" class="state-block">
        <div class="skeleton-list">
          <div v-for="i in 3" :key="i" class="skel-card">
            <div class="sk sk-tag"></div>
            <div class="sk sk-line"></div>
            <div class="sk sk-short"></div>
          </div>
        </div>
      </div>

      <!-- ── Error ── -->
      <div v-else-if="error" class="state-block state-error">
        <span class="state-icon"><i class="fa-solid fa-triangle-exclamation"></i></span>
        <div class="state-text">{{ error }}</div>
        <button class="btn-retry" @click="load">重新載入</button>
      </div>

      <!-- ── Empty ── -->
      <div v-else-if="applications.length === 0" class="state-block state-empty">
        <span class="state-icon"><i class="fa-solid fa-inbox"></i></span>
        <div class="state-text">您目前尚未有任何貸款申請紀錄</div>
        <button class="btn-apply-sm" @click="$router.push({ name: 'user-loan-apply' })">
          立即申請貸款
        </button>
      </div>

      <!-- ── List ── -->
      <div v-else class="app-list">
        <div
          v-for="app in applications"
          :key="app.applicationId"
          class="app-card"
        >
          <!-- 卡片頂部：ID + 狀態標籤 -->
          <div class="card-top">
            <span class="app-id">{{ app.applicationId }}</span>
            <span class="status-badge" :class="statusClass(app.applicationStatus)">
              {{ statusLabel(app.applicationStatus) }}
            </span>
          </div>

          <!-- 核心資訊列 -->
          <div class="card-info-grid">
            <div class="info-item">
              <span class="info-label">貸款類型</span>
              <span class="info-val">{{ loanTypeLabel(app.applyType) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">申請金額</span>
              <span class="info-val accent">$ {{ formatAmount(app.applyAmount) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">申請期數</span>
              <span class="info-val">{{ app.applyPeriod }} 個月</span>
            </div>
            <div class="info-item">
              <span class="info-label">年利率</span>
              <span class="info-val">{{ app.rate ? (app.rate * 100).toFixed(2) + ' %' : '—' }}</span>
            </div>
          </div>

          <!-- ── 聯繫狀態區塊 ── -->
          <div
            class="contact-banner"
            :class="contactBannerClass(app.latestContactStatus)"
            v-if="app.latestContactStatus"
          >
            <span class="contact-banner-icon"><i :class="contactIcon(app.latestContactStatus)"></i></span>
            <div class="contact-banner-body">
              <span class="contact-banner-label">{{ contactLabel(app.latestContactStatus) }}</span>
              <span class="contact-banner-desc">{{ contactDesc(app.latestContactStatus) }}</span>
            </div>
            <span class="contact-banner-time" v-if="app.latestContactTime">
              {{ formatTime(app.latestContactTime) }}
            </span>
          </div>

          <!-- 卡片底部：申請時間 + 補交文件 / 查看貸款帳戶 -->
          <div class="card-footer">
            <span class="footer-item">
              <i class="fa-solid fa-clock"></i> 申請時間：{{ formatTime(app.createTime) }}
            </span>
            <!-- 已撥款：跳轉貸款帳戶頁面 -->
            <button
              v-if="app.applicationStatus === 'DISBURSED'"
              class="btn-loan-account"
              @click="$router.push({ name: 'user-loan-accounts' })"
            >
              查看貸款帳戶
            </button>
            <!-- 其他狀態：補交文件 -->
            <button
              v-else
              class="btn-resubmit"
              :class="{ active: docPanelId === app.applicationId }"
              @click="toggleDocPanel(app)"
            >
              <i class="fa-solid fa-folder"></i> 補交文件
              <span v-if="docCount(app.applicationId) > 0" class="doc-badge">
                {{ docCount(app.applicationId) }}
              </span>
            </button>
          </div>

          <!-- ── 補件面板（展開） ── -->
          <transition name="slide-down">
            <div v-if="docPanelId === app.applicationId" class="doc-panel">

              <!-- 已上傳清單 -->
              <div class="doc-panel-section">
                <div class="doc-section-title">已上傳文件</div>
                <div v-if="docLoading" class="doc-loading">載入中…</div>
                <div v-else-if="docs[app.applicationId] && docs[app.applicationId].length > 0"
                     class="doc-list">
                  <div
                    v-for="d in docs[app.applicationId]"
                    :key="d.documentId"
                    class="doc-item"
                  >
                    <span class="doc-icon" v-html="docIcon(d.originalName)"></span>
                    <div class="doc-info">
                      <a :href="d.fileUrl" target="_blank" class="doc-name">
                        {{ d.originalName || '（未命名）' }}
                      </a>
                      <span class="doc-meta">
                        {{ DOC_TYPE_MAP[d.documentType] || d.documentType }}
                        · {{ formatTime(d.uploadTime) }}
                      </span>
                    </div>
                    <button
                      class="doc-del-btn"
                      :disabled="deletingId === d.documentId"
                      @click="deleteDoc(d.documentId, app.applicationId)"
                      title="刪除此文件"
                    >
                      <i v-if="deletingId === d.documentId" class="fa-solid fa-spinner fa-spin"></i>
                      <i v-else class="fa-solid fa-trash"></i>
                    </button>
                  </div>
                </div>
                <div v-else class="doc-empty">尚未上傳任何文件</div>
              </div>

              <!-- 上傳新文件 -->
              <div class="doc-panel-section" v-if="!app.documentsSubmittedAt">
                <div class="doc-section-title">
                  上傳新文件
                  <span class="doc-quota">
                    {{ docCount(app.applicationId) }} / 5
                  </span>
                </div>

                <!-- 已達上限 -->
                <div v-if="docCount(app.applicationId) >= 5" class="doc-quota-full">
                  已達每筆申請最多 5 份文件上限，如需更換請聯繫行員。
                </div>

                <!-- 上傳表單 -->
                <template v-else>
                  <div class="doc-upload-form">
                    <select v-model="uploadForm.documentType" class="doc-select">
                      <option value="" disabled>選擇文件類型</option>
                      <option v-for="(label, key) in DOC_TYPE_MAP" :key="key" :value="key">
                        {{ label }}
                      </option>
                    </select>
                    <label class="doc-file-btn">
                      {{ uploadForm.file ? uploadForm.file.name : '選擇檔案（JPG / PNG / PDF，最大 10 MB）' }}
                      <input
                        type="file"
                        accept=".jpg,.jpeg,.png,.pdf"
                        style="display:none"
                        @change="onFileChange"
                      />
                    </label>
                    <button
                      class="doc-submit-btn"
                      :disabled="!uploadForm.documentType || !uploadForm.file || uploading"
                      @click="submitUpload(app.applicationId)"
                    >
                      {{ uploading ? '上傳中…' : '確認上傳' }}
                    </button>
                  </div>
                  <p v-if="uploadError" class="doc-upload-error">{{ uploadError }}</p>
                  <p v-if="uploadSuccess" class="doc-upload-success"><i class="fa-solid fa-circle-check"></i> 上傳成功！</p>
                </template>
              </div>

              <!-- 送出補件（右下角） -->
              <div class="doc-panel-footer" v-if="docs[app.applicationId]?.length > 0">
                <span v-if="app.documentsSubmittedAt" class="doc-submitted-hint">
                  <i class="fa-solid fa-circle-check"></i>
                  已於 {{ formatTime(app.documentsSubmittedAt) }} 送出
                </span>
                <button
                  v-else
                  class="doc-submit-final-btn"
                  :disabled="submittingAppId === app.applicationId"
                  @click="submitDocuments(app.applicationId)"
                >
                  <i v-if="submittingAppId === app.applicationId" class="fa-solid fa-spinner fa-spin"></i>
                  <i v-else class="fa-solid fa-paper-plane"></i>
                  {{ submittingAppId === app.applicationId ? '送出中…' : '送出補件' }}
                </button>
              </div>

            </div>
          </transition>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios'

const applications = ref([])
const loading = ref(false)
const error = ref('')

// ── 補件面板狀態 ──
const docPanelId   = ref(null)   // 目前展開的申請 ID
const docs         = ref({})     // { [applicationId]: LoanDocumentResponseDTO[] }
const docLoading   = ref(false)

const uploadForm   = ref({ documentType: '', file: null })
const uploading    = ref(false)
const uploadError  = ref('')
const uploadSuccess = ref(false)
const deletingId      = ref(null)
const submittingAppId = ref(null)

// ── 文件類型對照 ──
const DOC_TYPE_MAP = {
  ID_CARD:         '身分證',
  INCOME_CERT:     '收入證明',
  EMPLOYMENT_CERT: '在職證明',
  BANK_STATEMENT:  '銀行存摺',
  PROPERTY_CERT:   '不動產謄本',
  TITLE_DEED:      '所有權狀',
  OTHER:           '其他',
}

// ── 貸款類型 ──
const LOAN_TYPE_MAP = {
  PERSONAL: '個人信貸',
  CAR:      '汽車貸款',
  MOTOR:    '機車貸款',
  STUDENT:  '學貸',
  BUSINESS: '創業貸款',
  HOUSE:    '房屋貸款',
  LAND:     '土地貸款',
}

// ── 申請狀態 ──
const STATUS_MAP = {
  PENDING_CONTACT: { label: '新申請（待聯繫）', cls: 'st-pending'   },
  IN_CONTACT:      { label: '聯繫中',           cls: 'st-contact'   },
  PENDING_REVIEW:  { label: '審核中',            cls: 'st-review'    },
  APPROVED:        { label: '核准撥款',          cls: 'st-approved'  },
  REJECTED:        { label: '申請拒絕',          cls: 'st-rejected'  },
  CANCELLED:       { label: '已取消',            cls: 'st-cancelled' },
  DISBURSED:       { label: '已撥款',            cls: 'st-disbursed' },
  CLOSED:          { label: '已結案',            cls: 'st-closed'    },
}

// ── 聯繫狀態：標籤 / 圖示 / 說明文字 / 顏色 ──
const CONTACT_MAP = {
  NOT_CONTACTED: {
    label:  '尚未聯繫',
    icon:   'fa-solid fa-clock',
    desc:   '行員將盡快與您聯繫，請保持電話暢通。',
    banner: 'banner-gray',
  },
  ATTEMPTED: {
    label:  '嘗試聯繫中',
    icon:   'fa-solid fa-phone-slash',
    desc:   '行員已嘗試聯繫您，請確認是否有未接來電或訊息。',
    banner: 'banner-blue',
  },
  REACHED: {
    label:  '已成功聯繫',
    icon:   'fa-solid fa-phone-flip',
    desc:   '行員已與您取得聯繫，申請正在進行中。',
    banner: 'banner-green',
  },
  CONFIRMED: {
    label:  '資料已確認',
    icon:   'fa-solid fa-clipboard-check',
    desc:   '行員已確認您的申請資料，即將進入審核流程。',
    banner: 'banner-gold',
  },
  DECLINED: {
    label:  '已放棄申請',
    icon:   'fa-solid fa-ban',
    desc:   '您已放棄此次申請，如有需要請重新提交。',
    banner: 'banner-red',
  },
}

function loanTypeLabel(type)   { return LOAN_TYPE_MAP[type] || type }
function statusLabel(st)       { return STATUS_MAP[st]?.label || st }
function statusClass(st)       { return STATUS_MAP[st]?.cls   || '' }
function contactLabel(ct)      { return CONTACT_MAP[ct]?.label  || ct }
function contactIcon(ct)       { return CONTACT_MAP[ct]?.icon   || 'fa-solid fa-comment-dots' }
function contactDesc(ct)       { return CONTACT_MAP[ct]?.desc   || '' }
function contactBannerClass(ct){ return CONTACT_MAP[ct]?.banner || 'banner-gray' }

function formatAmount(n) {
  return n != null ? Number(n).toLocaleString('zh-TW') : '—'
}

function formatTime(t) {
  if (!t) return '—'
  return new Date(t).toLocaleString('zh-TW', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  })
}

// ── API ──
async function load() {
  loading.value = true
  error.value = ''
  try {
    const token = localStorage.getItem('customer_token')
    const res = await api.get('/api/loan-applications/my', {
      headers: { Authorization: `Bearer ${token}` },
    })
    applications.value = res.data.data || []
  } catch (e) {
    error.value = e.response?.data?.message || '載入失敗，請稍後再試'
  } finally {
    loading.value = false
  }
}

// ── 補件面板邏輯 ──
function docCount(appId) {
  return docs.value[appId]?.length || 0
}

function docIcon(name) {
  if (!name) return '<i class="fa-solid fa-file doc-type-icon"></i>'
  const ext = name.split('.').pop()?.toLowerCase()
  if (ext === 'pdf') return '<i class="fa-solid fa-file-pdf doc-type-icon"></i>'
  if (['jpg', 'jpeg', 'png'].includes(ext)) return '<i class="fa-solid fa-file-image doc-type-icon"></i>'
  return '<i class="fa-solid fa-file doc-type-icon"></i>'
}

async function toggleDocPanel(app) {
  const id = app.applicationId
  if (docPanelId.value === id) {
    docPanelId.value = null
    return
  }
  docPanelId.value = id
  uploadForm.value  = { documentType: '', file: null }
  uploadError.value  = ''
  uploadSuccess.value = false
  await loadDocs(id)
}

async function loadDocs(appId) {
  docLoading.value = true
  try {
    const token = localStorage.getItem('customer_token')
    const res = await api.get(`/api/loan-documents/${appId}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    docs.value = { ...docs.value, [appId]: res.data.data || [] }
  } catch {
    // 載入失敗時保留原有資料，避免送出按鈕因此消失
    if (!docs.value[appId]) {
      docs.value = { ...docs.value, [appId]: [] }
    }
  } finally {
    docLoading.value = false
  }
}

function onFileChange(e) {
  uploadForm.value.file = e.target.files[0] || null
}

async function submitDocuments(appId) {
  submittingAppId.value = appId
  try {
    const token = localStorage.getItem('customer_token')
    await api.post(`/api/loan-documents/${appId}/submit`, null, {
      headers: { Authorization: `Bearer ${token}` },
    })
    // 重新載入申請清單以取得最新 documentsSubmittedAt
    await load()
  } catch (e) {
    alert(e.response?.data?.message || '送出失敗，請稍後再試')
  } finally {
    submittingAppId.value = null
  }
}

async function deleteDoc(documentId, appId) {
  if (!confirm('確定要刪除此文件嗎？此操作無法復原。')) return
  deletingId.value = documentId
  try {
    const token = localStorage.getItem('customer_token')
    await api.delete(`/api/loan-documents/${documentId}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    await loadDocs(appId)
  } catch (e) {
    alert(e.response?.data?.message || '刪除失敗，請稍後再試')
  } finally {
    deletingId.value = null
  }
}

async function submitUpload(appId) {
  if (!uploadForm.value.documentType || !uploadForm.value.file) return
  uploading.value    = true
  uploadError.value  = ''
  uploadSuccess.value = false
  try {
    const token = localStorage.getItem('customer_token')
    const fd = new FormData()
    fd.append('documentType', uploadForm.value.documentType)
    fd.append('file', uploadForm.value.file)
    const res = await api.post(`/api/loan-documents/${appId}/upload`, fd, {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'multipart/form-data',
      },
    })
    uploadSuccess.value = true
    uploadForm.value    = { documentType: '', file: null }

    // 樂觀更新：上傳成功後立刻把新文件加進 docs，確保送出按鈕不消失
    const newDoc = res.data?.data
    if (newDoc) {
      docs.value = {
        ...docs.value,
        [appId]: [...(docs.value[appId] || []), newDoc],
      }
    }

    await loadDocs(appId)   // 重整清單（同步伺服器狀態）
  } catch (e) {
    uploadError.value = e.response?.data?.message || '上傳失敗，請稍後再試'
  } finally {
    uploading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/7.0.1/css/all.min.css');

/* ── Variables ── */
.loan-status-page {
  --accent:    #A65A4D;
  --primary:   #5C6B5F;
  --pk:        #3F4A42;
  --bg:        #F5F1EA;
  --surface:   #FDFAF6;
  --surface-2: #EAE4DA;
  --border:    #D6CEC3;
  --ink:       #2B2B2B;
  --muted:     #A89A8E;
  --muted-2:   #6E6259;

  min-height: 100vh;
  background: var(--bg);
  font-family: 'Noto Sans TC', sans-serif;
  color: var(--ink);
}

/* ── Header ── */
.page-header {
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  padding: 28px 0;
}
.page-header-inner {
  max-width: 860px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.page-title-block {
  display: flex;
  align-items: center;
  gap: 14px;
}
.page-icon  { font-size: 28px; line-height: 1; }
.page-title { font-size: 22px; font-weight: 700; color: var(--ink); margin: 0 0 3px; }
.page-subtitle { font-size: 13px; color: var(--muted-2); margin: 0; }

.btn-apply {
  padding: 10px 22px;
  background: var(--primary);
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
  white-space: nowrap;
}
.btn-apply:hover { background: var(--pk); }

/* ── Body ── */
.page-body {
  max-width: 860px;
  margin: 0 auto;
  padding: 32px 24px 64px;
}

/* ── States ── */
.state-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 64px 0;
  text-align: center;
}
.state-icon  { font-size: 40px; }
.state-text  { font-size: 15px; color: var(--muted-2); }
.btn-retry, .btn-apply-sm {
  margin-top: 4px;
  padding: 9px 22px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-retry    { background: var(--surface-2); color: var(--muted-2); }
.btn-apply-sm { background: var(--primary); color: #fff; }
.btn-apply-sm:hover { background: var(--pk); }

/* ── Skeleton ── */
.skeleton-list { width: 100%; display: flex; flex-direction: column; gap: 16px; }
.skel-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 22px 24px;
  display: flex; flex-direction: column; gap: 12px;
}
.sk {
  background: var(--surface-2);
  border-radius: 6px;
  animation: shimmer 1.2s ease-in-out infinite alternate;
}
.sk-tag   { height: 22px; width: 80px;  border-radius: 20px; }
.sk-line  { height: 14px; width: 100%; }
.sk-short { height: 12px; width: 55%; }
@keyframes shimmer { from { opacity: 0.7; } to { opacity: 0.35; } }

/* ── App Card ── */
.app-list { display: flex; flex-direction: column; gap: 16px; }

.app-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  transition: box-shadow 0.15s;
}
.app-card:hover { box-shadow: 0 4px 20px rgba(92, 107, 95, 0.10); }

/* Top row */
.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.app-id {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 13px;
  color: var(--muted-2);
  letter-spacing: 0.04em;
}

/* Status badge */
.status-badge {
  display: inline-block;
  padding: 3px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.02em;
}
.st-pending   { background: rgba(166,140,0,0.10);   color: #7a6000; }
.st-contact   { background: rgba(55,100,180,0.10);   color: #2a5aad; }
.st-review    { background: rgba(106,80,160,0.10);   color: #5a3aad; }
.st-approved  { background: rgba(40,150,80,0.12);    color: #1a7a40; }
.st-rejected  { background: rgba(166,90,77,0.12);    color: #A65A4D; }
.st-cancelled { background: rgba(130,130,130,0.10);  color: #6e6e6e; }
.st-disbursed { background: rgba(30,160,180,0.12);   color: #107080; }
.st-closed    { background: rgba(80,80,80,0.08);     color: #555;    }

/* Info grid */
.card-info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px 16px;
}
@media (max-width: 640px) {
  .card-info-grid { grid-template-columns: 1fr 1fr; }
}
.info-item {
  background: var(--surface-2);
  border-radius: 8px;
  padding: 9px 12px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.info-label {
  font-size: 10px;
  color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
  letter-spacing: 0.05em;
}
.info-val { font-size: 13px; font-weight: 600; color: var(--ink); }
.info-val.accent { color: var(--primary); font-family: 'IBM Plex Mono', monospace; }

/* ── Contact Banner ── */
.contact-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 10px;
  border: 1px solid;
}
.contact-banner-icon { font-size: 20px; flex-shrink: 0; }
.contact-banner-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.contact-banner-label {
  font-size: 13px;
  font-weight: 700;
  line-height: 1.3;
}
.contact-banner-desc {
  font-size: 12px;
  line-height: 1.4;
  opacity: 0.8;
}
.contact-banner-time {
  font-size: 11px;
  font-family: 'IBM Plex Mono', monospace;
  white-space: nowrap;
  opacity: 0.65;
  flex-shrink: 0;
}

/* Banner variants */
.banner-gray  { background: rgba(130,130,130,0.06); border-color: rgba(130,130,130,0.20); color: #555; }
.banner-blue  { background: rgba(55,100,180,0.07);  border-color: rgba(55,100,180,0.22);  color: #2a5aad; }
.banner-green { background: rgba(40,150,80,0.07);   border-color: rgba(40,150,80,0.22);   color: #1a7a40; }
.banner-gold  { background: rgba(140,115,60,0.08);  border-color: rgba(140,115,60,0.22);  color: #7a6000; }
.banner-red   { background: rgba(166,90,77,0.07);   border-color: rgba(166,90,77,0.22);   color: #A65A4D; }

/* Footer */
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  border-top: 1px solid var(--border);
}
.footer-item { font-size: 12px; color: var(--muted); }

/* Loan account button（已撥款） */
.btn-loan-account {
  padding: 6px 16px;
  background: rgba(30,160,180,0.10);
  color: #107080;
  border: 1.5px solid #107080;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  white-space: nowrap;
}
.btn-loan-account:hover {
  background: #107080;
  color: #fff;
}

/* Resubmit button */
.btn-resubmit {
  padding: 6px 16px;
  background: transparent;
  color: var(--accent);
  border: 1.5px solid var(--accent);
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  white-space: nowrap;
}
.btn-resubmit:hover {
  background: var(--accent);
  color: #fff;
}
.btn-resubmit.active {
  background: var(--accent);
  color: #fff;
}
.doc-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  background: #fff;
  color: var(--accent);
  border-radius: 50%;
  font-size: 11px;
  font-weight: 700;
  margin-left: 4px;
}
.btn-resubmit.active .doc-badge {
  background: rgba(255,255,255,0.25);
  color: #fff;
}

/* ── 補件面板 ── */
.doc-panel {
  margin-top: 12px;
  border-top: 1px dashed var(--border);
  padding-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.doc-panel-section { display: flex; flex-direction: column; gap: 10px; }
.doc-section-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: 0.5px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.doc-quota {
  font-size: 11px;
  font-weight: 600;
  color: var(--muted);
  background: var(--surface-2);
  padding: 1px 7px;
  border-radius: 10px;
}
.doc-quota-full {
  font-size: 12px;
  color: #c0392b;
  background: #fdf0ee;
  border: 1px solid #f5c6c0;
  border-radius: 8px;
  padding: 10px 14px;
}
.doc-loading { font-size: 13px; color: var(--muted); }
.doc-empty   { font-size: 13px; color: var(--muted); font-style: italic; }

.doc-list { display: flex; flex-direction: column; gap: 8px; }
.doc-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  background: rgba(245,241,234,0.7);
  border: 1px solid var(--border);
  border-radius: 10px;
}
.doc-icon { font-size: 20px; flex-shrink: 0; line-height: 1.4; }
:deep(.doc-type-icon) { color: #7B4F2E; font-size: 20px; }
.doc-info  { display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.doc-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--primary);
  text-decoration: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.doc-name:hover { text-decoration: underline; }
.doc-meta { font-size: 11px; color: var(--muted); }
.doc-panel-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding-top: 4px;
}
.doc-submit-final-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: #f5ede6;
  color: #7B4F2E;
  border: 1.5px solid #c8a98a;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
}
.doc-submit-final-btn:hover:not(:disabled) {
  background: #7B4F2E;
  color: #fff;
  border-color: #7B4F2E;
}
.doc-submit-final-btn:disabled { opacity: 0.55; cursor: not-allowed; }
.doc-submitted-hint {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #1a7a40;
  font-weight: 600;
}

.doc-del-btn {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fdf0ee;
  color: #c0392b;
  border: 1px solid #f5c6c0;
  border-radius: 7px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  margin-left: auto;
}
.doc-del-btn:hover:not(:disabled) { background: #c0392b; color: #fff; border-color: #c0392b; }
.doc-del-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* 上傳表單 */
.doc-upload-form {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}
.doc-select {
  flex: 0 0 160px;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 13px;
  background: #fff;
  color: var(--ink);
  cursor: pointer;
}
.doc-file-btn {
  flex: 1;
  min-width: 160px;
  padding: 8px 12px;
  border: 1px dashed var(--border);
  border-radius: 8px;
  font-size: 12px;
  color: var(--muted-2);
  background: rgba(255,255,255,0.7);
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: border-color 0.15s;
}
.doc-file-btn:hover { border-color: var(--primary); color: var(--primary); }
.doc-submit-btn {
  flex: 0 0 auto;
  padding: 8px 18px;
  background: var(--primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
  white-space: nowrap;
}
.doc-submit-btn:hover:not(:disabled) { background: var(--pk); }
.doc-submit-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.doc-upload-error   { font-size: 12px; color: #c0392b; margin: 2px 0 0; }
.doc-upload-success { font-size: 12px; color: #27ae60; margin: 2px 0 0; font-weight: 600; }

/* slide-down transition */
.slide-down-enter-active, .slide-down-leave-active {
  transition: all 0.25s ease;
  overflow: hidden;
}
.slide-down-enter-from, .slide-down-leave-to {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
}
.slide-down-enter-to, .slide-down-leave-from {
  opacity: 1;
  max-height: 600px;
}
</style>
