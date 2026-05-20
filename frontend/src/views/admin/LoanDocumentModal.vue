<template>
  <teleport to="body">
    <transition name="modal-fade">
      <div v-if="modelValue" class="modal-overlay" @click.self="close">
        <div class="modal" @click.stop>

          <!-- ── Header ── -->
          <div class="modal-header">
            <div class="header-left">
              <span class="modal-icon"><i class="fa-solid fa-folder"></i></span>
              <div>
                <div class="modal-title">上傳文件</div>
                <div class="modal-sub">
                  <span class="id-chip">{{ app?.applicationId }}</span>
                  <span class="applicant-hint">
                    {{ app?.cif ? `CIF: ${app.cif}` : (app?.customerId ? `#${app.customerId}` : '') }}
                  </span>
                </div>
              </div>
            </div>
            <button class="close-btn" @click="close"><i class="fa-solid fa-x"></i></button>
          </div>

          <!-- ── Body ── -->
          <div class="modal-body">

            <!-- Loading -->
            <div v-if="loading" class="doc-loading">
              <div v-for="i in 3" :key="i" class="doc-skel">
                <div class="sk sk-tag"></div>
                <div class="sk sk-line"></div>
              </div>
            </div>

            <!-- Empty -->
            <div v-else-if="docs.length === 0" class="doc-empty">
              <div class="empty-icon"><i class="fa-solid fa-inbox"></i></div>
              <p>目前尚未上傳文件</p>
            </div>

            <!-- Document list -->
            <div v-else class="doc-list">
              <div
                v-for="d in docs"
                :key="d.documentId"
                class="doc-item"
              >
                <span class="doc-icon" v-html="fileIcon(d.originalName)"></span>
                <div class="doc-info">
                  <a :href="d.fileUrl" target="_blank" class="doc-name">
                    {{ d.originalName || '（未命名）' }}
                  </a>
                  <div class="doc-meta-row">
                    <span class="doc-type-badge">
                      {{ DOC_TYPE_MAP[d.documentType] || d.documentType }}
                    </span>
                    <span class="doc-meta">
                      上傳者：{{ d.uploadedBy }}
                    </span>
                    <span class="doc-meta">
                      {{ formatDateTime(d.uploadTime) }}
                    </span>
                  </div>
                </div>
                <div class="doc-actions">
                  <a :href="d.fileUrl" target="_blank" class="doc-action-btn doc-view-btn" title="開啟檔案">
                    <i class="fa-solid fa-eye"></i>
                  </a>
                  <a
                    :href="d.fileUrl"
                    :download="d.originalName || d.documentId"
                    class="doc-action-btn doc-dl-btn"
                    title="下載檔案"
                  >
                    <i class="fa-solid fa-file-arrow-down"></i>
                  </a>
                </div>
              </div>
            </div>

          </div>

          <!-- ── Footer ── -->
          <div class="modal-footer">
            <span class="footer-count" v-if="!loading">共 {{ docs.length }} 份文件</span>
            <button class="btn-close" @click="close">關閉</button>
          </div>

        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { ref, watch } from 'vue'
import api from '@/api/axios'

const props = defineProps({
  modelValue: Boolean,
  app: Object,
})
const emit = defineEmits(['update:modelValue'])

const docs    = ref([])
const loading = ref(false)

const DOC_TYPE_MAP = {
  ID_CARD:         '身分證',
  INCOME_CERT:     '收入證明',
  EMPLOYMENT_CERT: '在職證明',
  BANK_STATEMENT:  '銀行存摺',
  PROPERTY_CERT:   '不動產謄本',
  TITLE_DEED:      '所有權狀',
  OTHER:           '其他',
}

function fileIcon(name) {
  if (!name) return '<i class="fa-solid fa-file doc-type-icon"></i>'
  const ext = name.split('.').pop()?.toLowerCase()
  if (ext === 'pdf') return '<i class="fa-solid fa-file-pdf doc-type-icon"></i>'
  if (['jpg', 'jpeg', 'png'].includes(ext)) return '<i class="fa-solid fa-file-image doc-type-icon"></i>'
  return '<i class="fa-solid fa-file doc-type-icon"></i>'
}

function formatDateTime(t) {
  if (!t) return '—'
  return new Date(t).toLocaleString('zh-TW', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  })
}

async function loadDocs(appId) {
  if (!appId) return
  loading.value = true
  docs.value = []
  try {
    const authUser = localStorage.getItem('auth_user')
    const token    = authUser ? JSON.parse(authUser).token : null
    const res = await api.get(`/api/admin/loan-documents/${appId}`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    })
    docs.value = res.data.data || []
  } catch {
    docs.value = []
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (open && props.app?.applicationId) {
      loadDocs(props.app.applicationId)
    } else {
      docs.value = []
    }
  },
)

function close() {
  emit('update:modelValue', false)
}
</script>

<style scoped>
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/7.0.1/css/all.min.css');

/* ── Overlay ── */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 900;
  padding: 24px;
}
.modal {
  background: #fff;
  border-radius: 16px;
  width: 100%;
  max-width: 560px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0,0,0,0.18);
  overflow: hidden;
}

/* ── Header ── */
.modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #eee;
  flex-shrink: 0;
}
.header-left  { display: flex; align-items: flex-start; gap: 12px; }
.modal-icon   { font-size: 24px; line-height: 1; }
.modal-title  { font-size: 18px; font-weight: 700; color: #1a1a1a; }
.modal-sub    { display: flex; align-items: center; gap: 8px; margin-top: 4px; flex-wrap: wrap; }
.id-chip {
  font-size: 13px;
  font-family: 'Courier New', monospace;
  background: #f0f4ff;
  color: #3a5bd9;
  border-radius: 5px;
  padding: 2px 7px;
  letter-spacing: 0.3px;
}
.applicant-hint { font-size: 14px; color: #888; }
.close-btn {
  background: none;
  border: none;
  color: #aaa;
  font-size: 18px;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 6px;
  transition: background 0.15s;
}
.close-btn:hover { background: #f5f5f5; color: #555; }

/* ── Body ── */
.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

/* Loading skeletons */
.doc-loading { display: flex; flex-direction: column; gap: 10px; }
.doc-skel    { background: #f5f5f5; border-radius: 10px; padding: 12px; }
.sk          { background: #e8e8e8; border-radius: 4px; animation: pulse 1.2s infinite; }
.sk-tag      { height: 14px; width: 80px; margin-bottom: 8px; }
.sk-line     { height: 12px; width: 100%; }
@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.5} }

/* Empty */
.doc-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 40px 0;
  color: #aaa;
}
.empty-icon { font-size: 36px; }
.doc-empty p { font-size: 14px; margin: 0; }

/* Document list */
.doc-list { display: flex; flex-direction: column; gap: 10px; }
.doc-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: #fafafa;
  border: 1px solid #ebebeb;
  border-radius: 12px;
  transition: background 0.15s;
}
.doc-item:hover { background: #f3f6ff; }
.doc-icon { font-size: 22px; flex-shrink: 0; }
:deep(.doc-type-icon) { color: #5C6B5F; font-size: 22px; }
.doc-info { flex: 1; min-width: 0; }
.doc-name {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: #3a5bd9;
  text-decoration: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 5px;
}
.doc-name:hover { text-decoration: underline; }
.doc-meta-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.doc-type-badge {
  font-size: 12px;
  font-weight: 700;
  padding: 2px 7px;
  background: #eef2ff;
  color: #3a5bd9;
  border-radius: 4px;
  white-space: nowrap;
}
.doc-meta { font-size: 13px; color: #aaa; white-space: nowrap; }
.doc-actions {
  flex-shrink: 0;
  display: flex;
  gap: 6px;
}
.doc-action-btn {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  text-decoration: none;
  font-size: 14px;
  font-weight: 700;
  transition: background 0.15s;
}
.doc-view-btn {
  background: rgba(92, 107, 95, 0.10);
  color: #5C6B5F;
}
.doc-view-btn:hover { background: rgba(92, 107, 95, 0.20); }
.doc-dl-btn {
  background: rgba(92, 107, 95, 0.10);
  color: #5C6B5F;
}
.doc-dl-btn:hover { background: rgba(92, 107, 95, 0.20); }

/* ── Footer ── */
.modal-footer {
  padding: 14px 24px;
  border-top: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  background: #fafafa;
}
.footer-count { font-size: 14px; color: #aaa; }
.btn-close {
  padding: 8px 22px;
  background: #5C6B5F;
  color: #fff;
  border: none;
  border-radius: 9px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-close:hover { background: #3F4A42; }

/* ── Modal Transition ── */
.modal-fade-enter-active, .modal-fade-leave-active { transition: all 0.2s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; transform: scale(0.96); }
</style>
