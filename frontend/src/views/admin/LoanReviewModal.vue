<template>
  <teleport to="body">
    <transition name="modal-fade">
      <div v-if="modelValue" class="modal-overlay" @click.self="close">
        <div class="modal" @click.stop>

          <!-- ── Header ── -->
          <div class="modal-header">
            <div class="header-left">
              <span class="modal-icon">🗂️</span>
              <div>
                <div class="modal-title">審核填單</div>
                <div class="modal-sub">
                  <span class="id-chip">{{ app?.applicationId }}</span>
                  <span class="applicant-hint">
                    {{
                      app?.applicantName || (app?.cif ? `CIF: ${app.cif}` : (app?.customerId ? `#${app.customerId}` : ''))
                    }}
                  </span>
                  <span v-if="review" class="review-status-badge" :class="reviewStatusClass">
                    {{ reviewStatusLabel }}
                  </span>
                </div>
              </div>
            </div>
            <button class="close-btn" @click="close">✕</button>
          </div>

          <div class="modal-body">
            <div class="modal-columns">

              <!-- ── 左欄：申請資訊 + 現有填單摘要 ── -->
              <div class="info-panel">

                <!-- 申請資訊 -->
                <div class="section-title">申請資訊</div>
                <div class="info-grid">
                  <div class="info-row">
                    <span class="info-label">貸款類型</span>
                    <span class="type-badge" :class="'type-' + app?.applyType">
                      {{ LOAN_TYPE_NAME[app?.applyType] || app?.applyType }}
                    </span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">申請金額</span>
                    <span class="info-val amount">{{ formatAmount(app?.applyAmount) }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">申請期數</span>
                    <span class="info-val">{{ app?.applyPeriod }} 個月</span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">申請利率</span>
                    <span class="info-val rate">{{ formatRate(app?.rate) }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">申請人</span>
                    <span class="info-val">
                      {{
                        app?.applicantName || (app?.cif ? `${app.cif}` : (app?.customerId ? `#${app.customerId}` : '—'))
                      }}
                    </span>
                  </div>
                  <div class="info-row" v-if="app?.applicantPhone">
                    <span class="info-label">電話</span>
                    <span class="info-val mono">{{ app.applicantPhone }}</span>
                  </div>
                  <div class="info-row" v-if="app?.applicantEmail">
                    <span class="info-label">Email</span>
                    <span class="info-val mono small">{{ app.applicantEmail }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">申請時間</span>
                    <span class="info-val mono small">{{ formatDateTime(app?.createTime) }}</span>
                  </div>
                </div>

                <!-- 現有填單摘要 -->
                <template v-if="reviewLoading">
                  <div class="section-title" style="margin-top:20px">現有填單</div>
                  <div class="review-skel">
                    <div v-for="i in 4" :key="i" class="sk-row">
                      <div class="sk sk-label"></div>
                      <div class="sk sk-val"></div>
                    </div>
                  </div>
                </template>

                <template v-else-if="review">
                  <div class="section-divider"></div>
                  <div class="section-title">
                    現有填單
                    <span class="review-id mono">{{ review.reviewId }}</span>
                  </div>
                  <div class="info-grid">
                    <div class="info-row">
                      <span class="info-label">確認金額</span>
                      <span class="info-val amount">{{ formatAmount(form.confirmedAmount) }}</span>
                    </div>
                    <div class="info-row">
                      <span class="info-label">確認期數</span>
                      <span class="info-val">{{ form.confirmedPeriod ? form.confirmedPeriod + ' 個月' : '—' }}</span>
                    </div>
                    <div class="info-row">
                      <span class="info-label">確認利率</span>
                      <span class="info-val rate">
                        {{ form.confirmedRate && !isNaN(parseFloat(form.confirmedRate))
                          ? parseFloat(form.confirmedRate).toFixed(2) + '%'
                          : '—' }}
                      </span>
                    </div>
                    <div class="info-row">
                      <span class="info-label">擔保品備注</span>
                      <span class="info-val">{{ form.collateralNote || '—' }}</span>
                    </div>
                    <div class="info-row">
                      <span class="info-label">填單行員</span>
                      <span class="info-val mono">{{ review.empId || '—' }}</span>
                    </div>
                    <div class="info-row">
                      <span class="info-label">填單時間</span>
                      <span class="info-val mono small">{{
                          formatDateTime(review.reviewTime)
                        }}</span>
                    </div>
                    <div class="info-row" v-if="review.submittedTime">
                      <span class="info-label">送審時間</span>
                      <span class="info-val mono small">{{
                          formatDateTime(review.submittedTime)
                        }}</span>
                    </div>
                    <div class="info-row" v-if="review.reviewNote">
                      <span class="info-label">審核備注</span>
                      <span class="info-val">{{ review.reviewNote }}</span>
                    </div>
                  </div>
                </template>

                <template v-else-if="!reviewLoading">
                  <div class="section-divider"></div>
                  <div class="no-review">
                    <span class="no-review-icon">📋</span>
                    <span>尚未建立填單草稿</span>
                  </div>
                </template>

              </div>

              <!-- ── 右欄：填單表單 ── -->
              <div class="form-panel">
                <div class="section-title">
                  {{ review ? '更新草稿' : '建立草稿' }}
                </div>

                <!-- Alert -->
                <transition name="alert-fade">
                  <div v-if="alert.show" class="form-alert" :class="'alert-' + alert.type">
                    <span>{{ alert.type === 'success' ? '✅' : '❌' }}</span>
                    <span>{{ alert.msg }}</span>
                  </div>
                </transition>

                <!-- 已送審提示 -->
                <div v-if="isSubmitted" class="submitted-banner">
                  <span class="submitted-icon">✅</span>
                  <div>
                    <div class="submitted-title">此填單已送審</div>
                    <div class="submitted-sub">送審後無法修改，如需調整請洽主管</div>
                  </div>
                </div>

                <!-- Form -->
                <fieldset class="form-fields" :disabled="isSubmitted">

                  <div class="field-row">
                    <div class="field">
                      <label class="field-label">確認金額（TWD）<span class="req">*</span></label>
                      <input
                        class="field-input"
                        type="number"
                        v-model.number="form.confirmedAmount"
                        placeholder="e.g. 500000"
                        :class="{ error: v$.confirmedAmount }"
                      />
                      <span class="field-hint" v-if="app?.applyAmount">
                        申請金額：{{ formatAmount(app.applyAmount) }}
                      </span>
                    </div>
                    <div class="field">
                      <label class="field-label">確認期數（月）<span class="req">*</span></label>
                      <input
                        class="field-input"
                        type="number"
                        v-model.number="form.confirmedPeriod"
                        placeholder="e.g. 12"
                        :class="{ error: v$.confirmedPeriod }"
                      />
                      <span class="field-hint" v-if="app?.applyPeriod">
                        申請期數：{{ app.applyPeriod }} 個月
                      </span>
                    </div>
                  </div>

                  <div class="field">
                    <label class="field-label">確認利率（%）<span class="req">*</span></label>
                    <div class="rate-input-wrap">
                      <input
                        class="field-input"
                        type="text"
                        inputmode="decimal"
                        v-model="form.confirmedRate"
                        placeholder="可填至小數點後兩位"
                        :class="{ error: v$.confirmedRate }"
                      />
                      <span class="rate-unit">%</span>
                    </div>
                    <span class="field-hint" v-if="app?.rate">
                      申請利率：{{ formatRate(app.rate) }}
                    </span>
                  </div>

                  <div class="field">
                    <label class="field-label">擔保品備注</label>
                    <textarea
                      class="field-textarea"
                      v-model="form.collateralNote"
                      placeholder="填寫擔保品說明，例如：不動產、車輛..."
                      rows="3"
                    ></textarea>
                  </div>

                  <div class="field">
                    <label class="field-label">填單行員</label>
                    <div class="emp-id-display">
                      <span class="emp-id-badge">{{ form.empId || '—' }}</span>
<!--                      <span class="emp-id-hint">由登入帳號自動帶入</span>-->
                    </div>
                  </div>

                  <!-- 差異提示 -->
                  <div class="diff-hints" v-if="hasDiff">
                    <div class="diff-title">⚠ 與原始申請差異</div>
                    <div class="diff-row" v-if="amountDiff !== null">
                      <span class="diff-label">金額</span>
                      <span class="diff-val" :class="amountDiff > 0 ? 'up' : 'down'">
                        {{ amountDiff > 0 ? '▲' : '▼' }} {{ formatAmount(Math.abs(amountDiff)) }}
                      </span>
                    </div>
                    <div class="diff-row" v-if="periodDiff !== null">
                      <span class="diff-label">期數</span>
                      <span class="diff-val" :class="periodDiff > 0 ? 'up' : 'down'">
                        {{ periodDiff > 0 ? '▲' : '▼' }} {{ Math.abs(periodDiff) }} 個月
                      </span>
                    </div>
                    <div class="diff-row" v-if="rateDiff !== null">
                      <span class="diff-label">利率</span>
                      <span class="diff-val" :class="rateDiff > 0 ? 'up' : 'down'">
                        {{ rateDiff > 0 ? '▲' : '▼' }} {{ Math.abs(rateDiff).toFixed(2) }}%
                      </span>
                    </div>
                  </div>

                </fieldset>

                <!-- 驗證錯誤提示 -->
                <div class="validate-err" v-if="submitted && hasValidateErr">
                  請填寫所有必填欄位
                </div>

                <!-- Action buttons -->
                <div class="form-actions" v-if="!isSubmitted">
                  <button class="btn btn-ghost" @click="resetForm"
                          :disabled="saveLoading || submitLoading">
                    重置
                  </button>
                  <button class="btn btn-draft" @click="saveDraft"
                          :disabled="saveLoading || submitLoading">
                    <span v-if="saveLoading" class="spin">⟳</span>
                    <span v-else>💾</span>
                    儲存草稿
                  </button>
                  <button
                    class="btn btn-submit"
                    @click="submitReview"
                    :disabled="saveLoading || submitLoading || !review"
                    :title="!review ? '請先儲存草稿後才能送審' : ''"
                  >
                    <span v-if="submitLoading" class="spin">⟳</span>
                    <span v-else>🚀</span>
                    送審
                  </button>
                </div>

                <div class="form-hint-row" v-if="!review && !isSubmitted">
                  <span class="hint-icon">ℹ️</span> 請先儲存草稿，才能執行送審
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
import {ref, reactive, computed, watch} from 'vue'
import axios from '@/api/axios'
import api from "@/api/axios";

// ── Props / Emits ──
const props = defineProps({
  modelValue: Boolean,
  app: Object,
})
const emit = defineEmits(['update:modelValue', 'review-updated'])

// ── Constants ──

const LOAN_TYPE_NAME = {
  PERSONAL: '個人信貸', CAR: '汽車貸款', MOTOR: '機車貸款', STUDENT: '學貸',
  BUSINESS: '創業貸款', HOUSE: '房屋貸款', LAND: '土地貸款',
}

// ── State ──
const review = ref(null)
const reviewLoading = ref(false)
const saveLoading = ref(false)
const submitLoading = ref(false)
const submitted = ref(false)

const form = reactive({
  confirmedAmount: null,
  confirmedPeriod: null,
  confirmedRate: '',   // 文字欄位，空字串為未填
  collateralNote: '',
  empId: '',
})

const alert = reactive({show: false, type: 'success', msg: ''})

// ── Computed ──
const isSubmitted = computed(() => review.value?.reviewStatus === 'SUBMITTED')

const reviewStatusLabel = computed(() => ({
  DRAFT: '草稿',
  SUBMITTED: '已送審',
}[review.value?.reviewStatus] || review.value?.reviewStatus))

const reviewStatusClass = computed(() => ({
  DRAFT: 'rs-draft',
  SUBMITTED: 'rs-submitted',
}[review.value?.reviewStatus] || ''))

// 驗證
const v$ = computed(() => ({
  confirmedAmount: submitted.value && !form.confirmedAmount,
  confirmedPeriod: submitted.value && !form.confirmedPeriod,
  confirmedRate: submitted.value && (!form.confirmedRate || isNaN(parseFloat(form.confirmedRate))),
}))
const hasValidateErr = computed(() => Object.values(v$.value).some(Boolean))

// 差異計算（與原申請比較）
const amountDiff = computed(() => {
  if (!form.confirmedAmount || !props.app?.applyAmount) return null
  const d = form.confirmedAmount - props.app.applyAmount
  return d !== 0 ? d : null
})
const periodDiff = computed(() => {
  if (!form.confirmedPeriod || !props.app?.applyPeriod) return null
  const d = form.confirmedPeriod - props.app.applyPeriod
  return d !== 0 ? d : null
})
const rateDiff = computed(() => {
  if (!form.confirmedRate || !props.app?.rate) return null
  // form.confirmedRate 為 % 字串（e.g. "3.14"），app.rate 為 decimal（e.g. 0.0314）→ 統一換成 %
  const applyRatePct = parseFloat(props.app.rate) * 100
  const d = parseFloat((parseFloat(form.confirmedRate) - applyRatePct).toFixed(4))
  return d !== 0 ? d : null
})
const hasDiff = computed(() =>
  amountDiff.value !== null || periodDiff.value !== null || rateDiff.value !== null
)

// ── Watch：開啟時載入 ──
watch(() => props.modelValue, async (open) => {
  if (open && props.app?.applicationId) {
    await fetchReview()
    form.empId = JSON.parse(localStorage.getItem('auth_user'))?.empId || ''
  }
})

// ── Methods ──
async function fetchReview() {
  reviewLoading.value = true
  review.value = null
  try {
    const res = await api.get(
      `/api/admin/loan-applications/${props.app.applicationId}/review`
    )
    if (res.data.success && res.data.data) {
      review.value = res.data.data
      prefillForm(res.data.data)
    }
  } catch (e) {
    // 404 = 尚無填單，不顯示錯誤
    review.value = null
  } finally {
    reviewLoading.value = false
  }
}

function prefillForm(r) {
  form.confirmedAmount = r.confirmedAmount ?? null
  form.confirmedPeriod = r.confirmedPeriod ?? null
  // DB 儲存 decimal（0.0314），顯示時轉為百分比字串（"3.14"）
  form.confirmedRate = r.confirmedRate != null
    ? String(parseFloat((parseFloat(r.confirmedRate) * 100).toFixed(4)))
    : ''
  form.collateralNote = r.collateralNote ?? ''
  form.empId = r.empId ?? ''
}

function resetForm() {
  if (review.value) {
    prefillForm(review.value)
    form.empId = JSON.parse(localStorage.getItem('auth_user'))?.empId || ''
  } else {
    Object.assign(form, {
      confirmedAmount: null,
      confirmedPeriod: null,
      confirmedRate: '',
      collateralNote: '',
      empId: ''
    })
    form.empId = JSON.parse(localStorage.getItem('auth_user'))?.empId || ''
  }
  submitted.value = false
  alert.show = false
}

async function saveDraft() {
  submitted.value = true
  if (hasValidateErr.value) return

  saveLoading.value = true
  alert.show = false
  try {
    await api.post(
      `/api/admin/loan-applications/${props.app.applicationId}/review`,
      {
        confirmedAmount: form.confirmedAmount,
        confirmedPeriod: form.confirmedPeriod,
        // 輸入的是百分比字串，儲存時解析並轉回 decimal
        confirmedRate: form.confirmedRate
          ? parseFloat((parseFloat(form.confirmedRate) / 100).toFixed(6))
          : null,
        collateralNote: form.collateralNote,
        empId: form.empId,
      }
    )
    showAlert('success', '草稿已儲存')
    submitted.value = false
    await fetchReview()
    emit('review-updated', props.app)
  } catch (e) {
    showAlert('error', e.response?.data?.message || '儲存失敗，請稍後再試')
  } finally {
    saveLoading.value = false
  }
}

async function submitReview() {
  if (!review.value) return
  submitLoading.value = true
  alert.show = false
  try {
    await api.patch(
      `/api/admin/loan-applications/${props.app.applicationId}/review/submit`
    )
    showAlert('success', '已成功送審！')
    await fetchReview()
    emit('review-updated', props.app)
  } catch (e) {
    showAlert('error', e.response?.data?.message || '送審失敗，請稍後再試')
  } finally {
    submitLoading.value = false
  }
}

function showAlert(type, msg) {
  Object.assign(alert, {show: true, type, msg})
  if (type === 'success') setTimeout(() => {
    alert.show = false
  }, 4000)
}

function close() {
  emit('update:modelValue', false)
}

// ── Formatters ──
function formatAmount(n) {
  return n ? '$ ' + Number(n).toLocaleString('zh-TW') : '—'
}

function formatRate(r) {
  return r != null ? (parseFloat(r) * 100).toFixed(2) + '%' : '—'
}

function formatDateTime(d) {
  return d ? d.replace('T', ' ').substring(0, 16) : '—'
}
</script>

<style scoped>
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/7.0.1/css/all.min.css');

/* ── Variables（對齊 admin-theme）── */
.modal-overlay {
  --accent: #5C6B5F;
  --accent-dim: rgba(92, 107, 95, 0.10);
  --accent-lt: rgba(92, 107, 95, 0.20);
  --surface: #ffffff;
  --surface-2: #f0f2f0;
  --border: #dde1de;
  --border-2: #c8cdc9;
  --ink: #2B2B2B;
  --ink-2: #333333;
  --muted: #8c9891;
  --muted-2: #5a6a5e;
  --primary: #5C6B5F;
  --primary-dk: #4A574D;
  --red: #C0392B;
  --green: #4A8C5C;
  --gold: #8C7355;
}

/* ── Overlay ── */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 500;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

/* ── Modal ── */
.modal {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 16px;
  width: 100%;
  max-width: 900px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.18);
  overflow: hidden;
}

/* Transitions */
.modal-fade-enter-active, .modal-fade-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}

.modal-fade-enter-from, .modal-fade-leave-to {
  opacity: 0;
  transform: scale(0.97) translateY(8px);
}

.alert-fade-enter-active, .alert-fade-leave-active {
  transition: opacity 0.2s;
}

.alert-fade-enter-from, .alert-fade-leave-to {
  opacity: 0;
}

/* ── Header ── */
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.modal-icon {
  font-size: 22px;
}

.modal-title {
  font-family: 'Noto Serif TC', serif;
  font-size: 16px;
  font-weight: 700;
  color: var(--ink);
}

.modal-sub {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  flex-wrap: wrap;
}

.id-chip {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 11px;
  color: var(--accent);
  background: var(--accent-dim);
  border: 1px solid var(--accent-lt);
  padding: 2px 8px;
  border-radius: 4px;
}

.applicant-hint {
  font-size: 12px;
  color: var(--muted-2);
}

.review-status-badge {
  font-size: 10px;
  font-family: 'IBM Plex Mono', monospace;
  padding: 2px 8px;
  border-radius: 20px;
  border: 1px solid;
  font-weight: 600;
}

.rs-draft {
  color: var(--gold);
  border-color: #C4B090;
  background: #F5EFE6;
}

.rs-submitted {
  color: var(--primary-dk);
  border-color: #A5B8A9;
  background: #ECF0EC;
}

.close-btn {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: transparent;
  cursor: pointer;
  color: var(--muted-2);
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.close-btn:hover {
  background: var(--surface-2);
  color: var(--ink);
}

/* ── Body ── */
.modal-body {
  flex: 1;
  overflow: hidden;
}

.modal-columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  height: 100%;
}

/* ── Left Panel ── */
.info-panel {
  border-right: 1px solid var(--border);
  overflow-y: auto;
  max-height: 80vh;
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.section-title {
  font-size: 11px;
  font-weight: 600;
  color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.review-id {
  font-size: 10px;
  color: var(--muted);
  font-weight: 400;
}

.section-divider {
  border: none;
  border-top: 1px solid var(--border);
  margin: 18px 0;
}

.info-grid {
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.info-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.info-label {
  font-size: 12px;
  color: var(--muted-2);
  white-space: nowrap;
  flex-shrink: 0;
  font-family: 'IBM Plex Mono', monospace;
}

.info-val {
  font-size: 13px;
  color: var(--ink-2);
  text-align: right;
  word-break: break-all;
}

.info-val.amount {
  font-family: 'IBM Plex Mono', monospace;
  font-weight: 600;
  color: var(--ink);
}

.info-val.rate {
  font-family: 'IBM Plex Mono', monospace;
  color: var(--green);
  font-weight: 500;
}

.info-val.mono {
  font-family: 'IBM Plex Mono', monospace;
}

.info-val.small {
  font-size: 11px;
  color: var(--muted-2);
}

/* Type badge */
.type-badge {
  font-size: 11px;
  font-family: 'IBM Plex Mono', monospace;
  padding: 2px 8px;
  border-radius: 4px;
  border: 1px solid var(--border);
  background: var(--surface-2);
  color: var(--muted-2);
  display: inline-block;
}

.type-PERSONAL {
  color: #7A5C3A;
  border-color: #dde1de;
  background: #f4f5f7;
}

.type-CAR {
  color: #3F5F5A;
  border-color: #B5CECA;
  background: #EEF3F2;
}

.type-MOTOR {
  color: #7A4A38;
  border-color: #D4B8AE;
  background: #F5EDE9;
}

.type-STUDENT {
  color: #4A6B5C;
  border-color: #B5CCBF;
  background: #EEF3EF;
}

.type-BUSINESS {
  color: #5C5074;
  border-color: #C4BCDA;
  background: #F0EEF5;
}

.type-HOUSE {
  color: #3D5C58;
  border-color: #AECBC7;
  background: #EBF2F1;
}

.type-LAND {
  color: #5a6a5e;
  border-color: #dde1de;
  background: #f0f2f0;
}

/* Skeleton */
.review-skel {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 4px;
}

.sk-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sk {
  background: linear-gradient(90deg, #e8eae8 25%, #d5dad6 50%, #e8eae8 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
  border-radius: 4px;
}

.sk-label {
  width: 60px;
  height: 12px;
}

.sk-val {
  width: 100px;
  height: 12px;
}

@keyframes shimmer {
  to {
    background-position: -200% 0;
  }
}

/* No review */
.no-review {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--muted);
  padding: 14px 0;
}

.no-review-icon {
  font-size: 18px;
  opacity: 0.5;
}

/* ── Right Panel ── */
.form-panel {
  overflow-y: auto;
  max-height: 80vh;
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Fields */
.form-fields {
  border: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-fields:disabled {
  opacity: 0.6;
  pointer-events: none;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.field-label {
  font-size: 12px;
  color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
  letter-spacing: 0.04em;
}

.req {
  color: var(--red);
  margin-left: 2px;
}

.field-input, .field-textarea {
  background: var(--surface);
  border: 1px solid var(--border-2);
  border-radius: 8px;
  color: var(--ink);
  font-family: 'Noto Sans TC', sans-serif;
  font-size: 13px;
  padding: 9px 12px;
  outline: none;
  width: 100%;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.field-input:focus, .field-textarea:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-dim);
}

.field-input.error {
  border-color: var(--red);
  box-shadow: 0 0 0 3px rgba(192, 57, 43, 0.12);
}

.field-textarea {
  resize: vertical;
  min-height: 80px;
}

.field-hint {
  font-size: 11px;
  color: var(--muted);
  font-family: 'IBM Plex Mono', monospace;
}

.rate-input-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rate-input-wrap .field-input {
  flex: 1;
}

.rate-unit {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 15px;
  font-weight: 600;
  color: var(--green);
  white-space: nowrap;
  flex-shrink: 0;
}

/* Emp ID display */
.emp-id-display {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: 8px;
}
.emp-id-badge {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
}
.emp-id-hint {
  font-size: 11px;
  color: var(--muted);
}

/* Diff hints */
.diff-hints {
  padding: 12px 14px;
  border-radius: 8px;
  background: #F5EFE6;
  border: 1px solid #C4B090;
}

.diff-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--gold);
  margin-bottom: 8px;
}

.diff-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
}

.diff-label {
  font-size: 12px;
  color: var(--gold);
  font-family: 'IBM Plex Mono', monospace;
}

.diff-val {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 12px;
  font-weight: 600;
}

.diff-val.up {
  color: var(--red);
}

.diff-val.down {
  color: var(--green);
}

/* Submitted banner */
.submitted-banner {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 8px;
  background: #ECF0EC;
  border: 1px solid #A5B8A9;
}

.submitted-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.submitted-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--primary-dk);
  margin-bottom: 2px;
}

.submitted-sub {
  font-size: 12px;
  color: var(--primary);
}

/* Alert */
.form-alert {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
}

.alert-success {
  background: rgba(92, 107, 95, 0.08);
  border: 1px solid rgba(92, 107, 95, 0.3);
  color: var(--primary-dk);
}

.alert-error {
  background: rgba(192, 57, 43, 0.08);
  border: 1px solid rgba(192, 57, 43, 0.3);
  color: var(--red);
}

/* Validate error */
.validate-err {
  font-size: 12px;
  color: var(--red);
  display: flex;
  align-items: center;
  gap: 4px;
}

/* Form hint row */
.form-hint-row {
  font-size: 12px;
  color: var(--muted-2);
  display: flex;
  align-items: center;
  gap: 6px;
}

.hint-icon {
  font-size: 14px;
}

/* Action buttons */
.form-actions {
  display: flex;
  gap: 8px;
  padding-top: 4px;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  border-radius: 8px;
  font-family: 'Noto Sans TC', sans-serif;
  cursor: pointer;
  transition: all 0.15s;
  font-weight: 500;
  font-size: 13px;
  padding: 9px 16px;
}

.btn-ghost {
  background: var(--surface-2);
  color: var(--muted-2);
  border: 1px solid var(--border);
}

.btn-ghost:hover {
  border-color: var(--border-2);
  color: var(--ink);
}

.btn-ghost:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn-draft {
  flex: 1;
  background: #F5EFE6;
  color: var(--gold);
  border: 1px solid #C4B090;
}

.btn-draft:hover:not(:disabled) {
  background: #EAE0D0;
  border-color: var(--gold);
}

.btn-draft:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn-submit {
  flex: 1;
  background: var(--accent);
  color: #fff;
}

.btn-submit:hover:not(:disabled) {
  background: var(--primary-dk);
}

.btn-submit:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* Misc */
.spin {
  animation: spin 0.8s linear infinite;
  display: inline-block;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
