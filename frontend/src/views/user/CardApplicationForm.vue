<script setup>
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { createApplication, getMyApplications } from '@/api/cardApplication'

const listLoading = ref(false)
const submitting = ref(false)
const applications = ref([])
const form = ref({
  remark: '',
})

const columns = [
  {
    key: 'applicationId',
    label: '申請編號'
  },
  {
    key: 'applyDate',
    label: '申請時間'
  },
  {
    key: 'status',
    label: '狀態'
  },
  {
    key: 'remark',
    label: '備註'
  }
]


const getStatusType = (status) => {
  switch (status) {
    case 'APPROVED':
      return 'success'
    case 'REJECTED':
      return 'danger'
    default:
      return 'warning'
  }
}

const getStatusText = (status) => {
  switch (status) {
    case 'APPROVED':
      return '核准'
    case 'REJECTED':
      return '拒絕'
    default:
      return '審核中'
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

const submitApplication = async () => {
  submitting.value = true

  try {
    await createApplication({
      remark: form.value.remark,
    })
    message.success('信用卡申請已送出')
    form.value.remark = ''
    await fetchApplications()
  } catch (error) {
    console.error(error)
    message.error(error.response?.data?.message || '信用卡申請送出失敗')
  } finally {
    submitting.value = false
  }
}

onMounted(fetchApplications)
</script>

<template>
  <div class="card-application-page">
    <section class="application-form-section">
      <div class="page-head">
        <div>
          <h2 class="page-title">信用卡申請</h2>
          <p class="page-subtitle">送出申請後，可在下方查看審核進度。</p>
        </div>
      </div>

      <form class="application-form" @submit.prevent="submitApplication">
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
    </section>

    <section class="application-list-section">
      <div class="section-head">
        <h3 class="section-title">我的申請紀錄</h3>
        <button class="jb-btn jb-btn-secondary jb-btn-sm" type="button" @click="fetchApplications">
          重新整理
        </button>
      </div>

      <div v-if="listLoading" class="state-panel">
        載入中...
      </div>

      <div v-else-if="applications.length === 0" class="state-panel">
        目前沒有信用卡申請紀錄。
      </div>

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
            <tr v-for="app in applications" :key="app.applicationId">
              <td v-for="column in columns" :key="column.key">
                <span
                  v-if="column.key === 'status'"
                  class="status-pill"
                  :class="`status-${getStatusType(app.status)}`"
                >
                  {{ getStatusText(app.status) }}
                </span>

                <template v-else>
                  {{ app[column.key] || '-' }}
                </template>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
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

.field textarea {
  width: 100%;
  resize: vertical;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: var(--space-3);
  background: var(--bg-card);
  color: var(--text-primary);
  font: inherit;
  font-weight: 400;
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

@media (max-width: 700px) {
  .page-head,
  .section-head {
    flex-direction: column;
  }
}
</style>
