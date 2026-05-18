<template>
  <teleport to="body">
    <transition name="modal-fade">
      <div v-if="modelValue" class="modal-overlay" @click.self="close">
        <div class="modal" @click.stop>

          <!-- ── Header ── -->
          <div class="modal-header">
            <div class="header-left">
              <span class="modal-icon"><i class="fa-solid fa-square-phone"></i></span>
              <div>
                <div class="modal-title">聯繫紀錄</div>
                <div class="modal-sub">
                  <span class="id-chip">{{ app?.applicationId }}</span>
                  <span class="applicant-hint">
                    {{ app?.applicantName || (app?.cif ? `CIF: ${app.cif}` : (app?.customerId ? `#${app.customerId}` : '')) }}
                  </span>
                </div>
              </div>
            </div>
            <button class="close-btn" @click="close"><i class="fa-solid fa-x"></i></button>
          </div>

          <div class="modal-body">
            <div class="modal-columns">

              <!-- ── 左欄：歷史紀錄 ── -->
              <div class="log-panel">
                <div class="panel-title">
                  歷史聯繫紀錄
                  <span class="log-count" v-if="!logsLoading">{{ logs.length }} 筆</span>
                </div>

                <!-- Loading -->
                <div v-if="logsLoading" class="log-loading">
                  <div v-for="i in 3" :key="i" class="log-skel">
                    <div class="sk sk-tag"></div>
                    <div class="sk sk-line"></div>
                    <div class="sk sk-short"></div>
                  </div>
                </div>

                <!-- Empty -->
                <div v-else-if="logs.length === 0" class="log-empty">
                  <div class="empty-icon"><i class="fa-solid fa-inbox"></i></div>
                  <p>尚無聯繫紀錄</p>
                </div>

                <!-- Log list -->
                <div v-else class="log-list">
                  <div
                    v-for="(log, idx) in logs"
                    :key="log.logId"
                    class="log-item"
                    :class="'item-' + log.contactStatus"
                    :style="{ animationDelay: idx * 30 + 'ms' }"
                  >
                    <div class="log-item-head">
                      <span class="log-status-badge" :class="STATUS_CLASS[log.contactStatus]">
                        {{ STATUS_LABEL[log.contactStatus] || log.contactStatus }}
                      </span>
                      <span class="log-channel" :class="'ch-' + log.contactChannel">
                        <i :class="CHANNEL_ICON[log.contactChannel]"></i> {{ log.contactChannel }}
                      </span>
                    </div>
                    <div class="log-note" v-if="log.note">{{ log.note }}</div>
                    <div class="log-note empty-note" v-else>（無備注）</div>
                    <div class="log-meta">
                      <span class="log-emp"><i class="fa-solid fa-user"></i> {{ log.empId || '—' }}</span>
                      <span class="log-time">{{ formatDateTime(log.contactTime) }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- ── 右欄：新增聯繫 ── -->
              <div class="form-panel">
                <div class="panel-title">新增聯繫紀錄</div>

                <!-- Alert -->
                <transition name="alert-fade">
                  <div v-if="formAlert.show" class="form-alert" :class="'alert-' + formAlert.type">
                    <span><i :class="formAlert.type === 'success' ? 'fa-solid fa-circle-check' : 'fa-solid fa-circle-xmark'"></i></span>
                    <span>{{ formAlert.msg }}</span>
                  </div>
                </transition>

                <!-- Form -->
                <div class="form-body">
                  <div class="field">
                    <label class="field-label">行員 ID<span class="req">*</span></label>
                    <input
                      class="field-input"
                      v-model="form.empId"
                      placeholder="e.g. EMP001"
                      :class="{ error: submitted && !form.empId }"
                    />
                    <span class="field-err" v-if="submitted && !form.empId">請填寫行員 ID</span>
                  </div>

                  <div class="field-row">
                    <div class="field">
                      <label class="field-label">聯繫狀態<span class="req">*</span></label>
                      <div class="styled-select-wrap">
                        <select
                          class="field-select"
                          v-model="form.contactStatus"
                          :class="{ error: submitted && !form.contactStatus }"
                        >
                          <option value="">— 選擇 —</option>
                          <option
                            v-for="s in CONTACT_STATUS_OPTIONS"
                            :key="s.value"
                            :value="s.value"
                          >{{ s.label }}</option>
                        </select>
                        <span class="select-caret">▾</span>
                      </div>
                      <span class="field-err" v-if="submitted && !form.contactStatus">請選擇聯繫狀態</span>
                    </div>

                    <div class="field">
                      <label class="field-label">聯繫管道<span class="req">*</span></label>
                      <div class="channel-btns">
                        <button
                          v-for="ch in CHANNEL_OPTIONS"
                          :key="ch.value"
                          class="ch-btn"
                          :class="{ active: form.contactChannel === ch.value }"
                          @click="form.contactChannel = ch.value"
                          type="button"
                        >
                          <i :class="ch.icon"></i> {{ ch.label }}
                        </button>
                      </div>
                      <span class="field-err" v-if="submitted && !form.contactChannel">請選擇聯繫管道</span>
                    </div>
                  </div>

                  <div class="field">
                    <label class="field-label">備注</label>
                    <textarea
                      class="field-textarea"
                      v-model="form.note"
                      placeholder="填寫本次聯繫的補充說明..."
                      rows="4"
                    ></textarea>
                  </div>

                  <!-- 狀態預覽 -->
                  <div class="status-preview" v-if="form.contactStatus">
                    <span class="preview-label">狀態預覽</span>
                    <span class="log-status-badge" :class="STATUS_CLASS[form.contactStatus]">
                      {{ STATUS_LABEL[form.contactStatus] }}
                    </span>
                  </div>

                  <button
                    class="submit-btn"
                    @click="submitLog"
                    :disabled="submitLoading"
                  >
                    <span v-if="submitLoading" class="spin">⟳</span>
                    <span v-else>＋</span>
                    新增聯繫紀錄
                  </button>
                </div>
              </div>

            </div>
          </div>

        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { ref, watch, reactive } from 'vue'
import api from '@/api/axios'

// ── Props / Emits ──
const props = defineProps({
  modelValue: Boolean,   // v-model 控制開關
  app: Object,           // 傳入的申請資料
})
const emit = defineEmits(['update:modelValue', 'log-added'])

// ── Constants ──
const currentEmpId = (() => {
  try { return JSON.parse(localStorage.getItem('auth_user'))?.empId || '' }
  catch { return '' }
})()

const STATUS_LABEL = {
  NOT_CONTACTED: '未聯繫',
  ATTEMPTED:     '嘗試中',
  REACHED:       '已接通',
  CONFIRMED:     '已確認',
  DECLINED:      '已放棄',
}
const STATUS_CLASS = {
  NOT_CONTACTED: 'badge-gray',
  ATTEMPTED:     'badge-blue',
  REACHED:       'badge-green',
  CONFIRMED:     'badge-gold',
  DECLINED:      'badge-red',
}
const CONTACT_STATUS_OPTIONS = [
  { value: 'NOT_CONTACTED', label: '未聯繫'  },
  { value: 'ATTEMPTED',     label: '嘗試中'  },
  { value: 'REACHED',       label: '已接通'  },
  { value: 'CONFIRMED',     label: '已確認'  },
  { value: 'DECLINED',      label: '已放棄'  },
]
const CHANNEL_OPTIONS = [
  { value: 'PHONE', label: '電話', icon: 'fa-solid fa-square-phone' },
  { value: 'EMAIL', label: 'Email', icon: 'fa-solid fa-envelope'    },
  { value: 'SMS',   label: 'SMS',  icon: 'fa-solid fa-comment-sms'  },
]
const CHANNEL_ICON = { PHONE: 'fa-solid fa-square-phone', EMAIL: 'fa-solid fa-envelope', SMS: 'fa-solid fa-comment-sms' }

// ── State ──
const logs        = ref([])
const logsLoading = ref(false)
const submitLoading = ref(false)
const submitted   = ref(false)

const form = reactive({
  empId: '',
  contactStatus: '',
  contactChannel: 'PHONE',
  note: '',
})
const formAlert = reactive({ show: false, type: 'success', msg: '' })

// ── Watch：開啟 modal 時載入紀錄 ──
watch(() => props.modelValue, (open) => {
  if (open && props.app?.applicationId) {
    fetchLogs()
    resetForm()
    form.empId = currentEmpId
  }
})

// ── Methods ──
async function fetchLogs() {
  logsLoading.value = true
  try {
    const res = await api.get(
      `/api/admin/loan-applications/${props.app.applicationId}/contact-logs`
    )
    logs.value = res.data.success ? res.data.data : []
  } catch (e) {
    logs.value = []
  } finally {
    logsLoading.value = false
  }
}

async function submitLog() {
  submitted.value = true

  // 驗證必填
  if (!form.empId || !form.contactStatus || !form.contactChannel) return

  submitLoading.value = true
  formAlert.show = false
  try {
    await api.post(
      `/api/admin/loan-applications/${props.app.applicationId}/contact-logs`,
      {
        empId:          form.empId,
        contactStatus:  form.contactStatus,
        contactChannel: form.contactChannel,
        note:           form.note,
      }
    )
    showAlert('success', '聯繫紀錄新增成功')
    resetForm()
    await fetchLogs()                   // 重新載入紀錄
    emit('log-added', props.app)        // 通知父元件刷新列表
  } catch (e) {
    showAlert('error', e.response?.data?.message || '新增失敗，請稍後再試')
  } finally {
    submitLoading.value = false
  }
}

function resetForm() {
  Object.assign(form, { empId: '', contactStatus: '', contactChannel: 'PHONE', note: '' })
  form.empId = currentEmpId
  submitted.value = false
}

function showAlert(type, msg) {
  Object.assign(formAlert, { show: true, type, msg })
  setTimeout(() => { formAlert.show = false }, 4000)
}

function close() {
  emit('update:modelValue', false)
}

// ── Formatters ──
function formatDateTime(d) {
  if (!d) return '—'
  return d.replace('T', ' ').substring(0, 16)
}
</script>

<style scoped>
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/7.0.1/css/all.min.css');

/* ── Variables（對齊 admin-theme）── */
.modal-overlay {
  --accent:     #5C6B5F;
  --accent-dim: rgba(92, 107, 95, 0.10);
  --accent-lt:  rgba(92, 107, 95, 0.20);
  --surface:    #ffffff;
  --surface-2:  #f0f2f0;
  --border:     #dde1de;
  --border-2:   #c8cdc9;
  --ink:        #2B2B2B;
  --ink-2:      #333333;
  --muted:      #8c9891;
  --muted-2:    #5a6a5e;
  --primary:    #5C6B5F;
  --primary-dk: #4A574D;
  --red:        #C0392B;
  --green:      #4A8C5C;
  --gold:       #8C7355;
}

/* ── Overlay ── */
.modal-overlay {
  position: fixed; inset: 0; z-index: 500;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
  padding: 24px;
}

/* ── Modal ── */
.modal {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 16px;
  width: 100%; max-width: 860px;
  max-height: 88vh;
  display: flex; flex-direction: column;
  box-shadow: 0 24px 64px rgba(0,0,0,0.18);
  overflow: hidden;
}

/* Transitions */
.modal-fade-enter-active, .modal-fade-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}
.modal-fade-enter-from, .modal-fade-leave-to {
  opacity: 0; transform: scale(0.97) translateY(8px);
}
.alert-fade-enter-active, .alert-fade-leave-active { transition: opacity 0.2s, max-height 0.3s; }
.alert-fade-enter-from, .alert-fade-leave-to { opacity: 0; max-height: 0; }

/* ── Header ── */
.modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 22px;
  border-bottom: 1px solid var(--border);
  background: var(--surface);
  flex-shrink: 0;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.modal-icon  { font-size: 22px; }
.modal-title {
  font-family: 'Noto Serif TC', serif;
  font-size: 16px; font-weight: 700; color: var(--ink);
}
.modal-sub { display: flex; align-items: center; gap: 8px; margin-top: 3px; }
.id-chip {
  font-family: 'IBM Plex Mono', monospace; font-size: 11px;
  color: var(--accent); background: var(--accent-dim);
  border: 1px solid var(--accent-lt); padding: 2px 8px; border-radius: 4px;
}
.applicant-hint { font-size: 12px; color: var(--muted-2); }
.close-btn {
  width: 30px; height: 30px; border-radius: 8px;
  border: 1px solid var(--border); background: transparent;
  cursor: pointer; color: var(--muted-2); font-size: 14px;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.15s;
}
.close-btn:hover { background: var(--surface-2); color: var(--ink); border-color: var(--border-2); }

/* ── Body ── */
.modal-body { flex: 1; overflow: hidden; }
.modal-columns {
  display: grid; grid-template-columns: 1fr 1fr;
  height: 100%; min-height: 0;
}

/* ── Left Panel ── */
.log-panel {
  border-right: 1px solid var(--border);
  display: flex; flex-direction: column;
  overflow: hidden; max-height: 75vh;
}
.panel-title {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 18px 12px;
  font-size: 12px; font-weight: 600; color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
  letter-spacing: 0.1em; text-transform: uppercase;
  border-bottom: 1px solid var(--border);
  background: var(--surface-2); flex-shrink: 0;
}
.log-count {
  font-size: 11px; color: var(--muted);
  background: var(--surface); border: 1px solid var(--border);
  padding: 1px 8px; border-radius: 10px;
}

/* Loading skeleton */
.log-loading { padding: 14px 18px; display: flex; flex-direction: column; gap: 12px; }
.log-skel    { display: flex; flex-direction: column; gap: 6px; }
.sk {
  background: linear-gradient(90deg, #e8eae8 25%, #d5dad6 50%, #e8eae8 75%);
  background-size: 200% 100%; animation: shimmer 1.4s infinite;
  border-radius: 4px; flex-shrink: 0;
}
.sk-tag   { height: 20px; width: 72px; }
.sk-line  { height: 13px; width: 100%; }
.sk-short { height: 11px; width: 60%; }
@keyframes shimmer { to { background-position: -200% 0; } }

/* Empty */
.log-empty { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px 20px; color: var(--muted); }
.empty-icon { font-size: 32px; margin-bottom: 8px; opacity: 0.4; }
.log-empty p { font-size: 13px; }

/* Log list */
.log-list { flex: 1; overflow-y: auto; padding: 12px 18px; display: flex; flex-direction: column; gap: 10px; }
.log-item {
  border-radius: 8px; padding: 12px 14px;
  border: 1px solid var(--border);
  background: var(--surface);
  border-left: 3px solid var(--border-2);
  animation: logIn 0.2s both;
}
@keyframes logIn { from { opacity: 0; transform: translateY(5px); } to { opacity: 1; transform: translateY(0); } }
.item-REACHED   { border-left-color: var(--green); }
.item-CONFIRMED { border-left-color: var(--gold);  }
.item-ATTEMPTED { border-left-color: var(--blue);  }
.item-DECLINED  { border-left-color: var(--red);   }
.item-NOT_CONTACTED { border-left-color: var(--muted); }

.log-item-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }

.log-status-badge {
  font-size: 11px; font-family: 'IBM Plex Mono', monospace;
  padding: 3px 9px; border-radius: 20px; border: 1px solid;
  font-weight: 500; display: inline-block;
}
.badge-gray  { color: var(--muted-2); border-color: var(--border-2); background: var(--surface-2); }
.badge-blue  { color: #3F5F5A; border-color: #9BBAB6; background: #EEF3F2; }
.badge-green { color: var(--primary-dk); border-color: #A5B8A9; background: #ECF0EC; }
.badge-gold  { color: var(--gold); border-color: #C4B090; background: #F5EFE6; }
.badge-red   { color: var(--red); border-color: rgba(192,57,43,0.4); background: rgba(192,57,43,0.08); }

.log-channel {
  font-size: 11px; font-family: 'IBM Plex Mono', monospace;
  color: var(--muted-2); background: var(--surface-2);
  padding: 2px 8px; border-radius: 4px;
  border: 1px solid var(--border);
}
.log-note { font-size: 13px; color: var(--ink-2); line-height: 1.5; margin-bottom: 8px; white-space: pre-wrap; }
.empty-note { color: var(--muted); font-style: italic; }
.log-meta { display: flex; align-items: center; justify-content: space-between; }
.log-emp  { font-size: 11px; color: var(--muted-2); font-family: 'IBM Plex Mono', monospace; }
.log-time { font-size: 11px; color: var(--muted);   font-family: 'IBM Plex Mono', monospace; }

/* ── Right Panel ── */
.form-panel {
  display: flex; flex-direction: column;
  overflow-y: auto; max-height: 75vh;
}
.form-panel .panel-title {
  border-right: none;
}
.form-body { padding: 16px 18px; display: flex; flex-direction: column; gap: 14px; }

/* Fields */
.field { display: flex; flex-direction: column; gap: 5px; }
.field-label {
  font-size: 12px; color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace; letter-spacing: 0.04em;
}
.req { color: var(--red); margin-left: 2px; }
.field-input, .field-select, .field-textarea {
  background: var(--surface);
  border: 1px solid var(--border-2);
  border-radius: 8px; color: var(--ink);
  font-family: 'Noto Sans TC', sans-serif;
  font-size: 13px; padding: 9px 12px; outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
  width: 100%;
}
.field-input:focus, .field-select:focus, .field-textarea:focus {
  border-color: var(--accent); box-shadow: 0 0 0 3px var(--accent-dim);
}
.field-input.error, .field-select.error { border-color: var(--red); }
.field-textarea { resize: vertical; min-height: 90px; }
.field-err { font-size: 11px; color: var(--red); }

.field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }

.styled-select-wrap { position: relative; }
.field-select { appearance: none; cursor: pointer; }
.select-caret {
  position: absolute; right: 10px; top: 50%; transform: translateY(-50%);
  font-size: 10px; color: var(--muted-2); pointer-events: none;
}

/* Channel buttons */
.channel-btns { display: flex; gap: 6px; }
.ch-btn {
  flex: 1; padding: 7px 6px; border-radius: 8px;
  font-size: 12px; font-family: 'Noto Sans TC', sans-serif;
  cursor: pointer; border: 1px solid var(--border-2);
  background: var(--surface); color: var(--muted-2);
  transition: all 0.15s; white-space: nowrap;
}
.ch-btn:hover  { border-color: var(--accent); color: var(--accent); }
.ch-btn.active { border-color: var(--accent); background: var(--accent-dim); color: var(--accent); font-weight: 600; }

/* Status preview */
.status-preview {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; border-radius: 8px;
  background: var(--surface-2); border: 1px solid var(--border);
}
.preview-label { font-size: 12px; color: var(--muted-2); font-family: 'IBM Plex Mono', monospace; }

/* Alert */
.form-alert {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; border-radius: 8px; font-size: 13px;
  overflow: hidden;
}
.alert-success { background: rgba(92,107,95,0.08); border: 1px solid rgba(92,107,95,0.3); color: var(--primary-dk); }
.alert-error   { background: rgba(192,57,43,0.08); border: 1px solid rgba(192,57,43,0.3); color: var(--red); }

/* Submit */
.submit-btn {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  width: 100%; padding: 10px;
  background: var(--accent); color: #fff;
  border: none; border-radius: 8px;
  font-size: 14px; font-family: 'Noto Sans TC', sans-serif;
  font-weight: 600; cursor: pointer; transition: all 0.15s;
}
.submit-btn:hover:not(:disabled) { background: var(--primary-dk); }
.submit-btn:disabled { opacity: 0.45; cursor: not-allowed; }

/* Misc */
.spin { animation: spin 0.8s linear infinite; display: inline-block; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
