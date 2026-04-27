<template>
  <div style="padding: 24px">
    <h2>轉帳</h2>

    <a-card style="width: 100%; max-width: 600px">
      <a-form layout="vertical">
        <a-form-item label="來源帳號">
          <a-input v-model:value="form.fromAccountNumber" placeholder="請輸入來源帳號" allow-clear />
        </a-form-item>

        <a-form-item label="目的帳號">
          <a-input v-model:value="form.toAccountNumber" placeholder="請輸入目的帳號" allow-clear />
        </a-form-item>

        <a-form-item label="金額">
          <a-input-number
            v-model:value="form.amount"
            placeholder="請輸入金額"
            :min="1"
            style="width: 100%"
          />
        </a-form-item>

        <a-form-item label="備註">
          <a-input v-model:value="form.note" placeholder="選填" allow-clear />
        </a-form-item>

        <div style="display: flex; gap: 8px">
          <a-button type="primary" :loading="loading" @click="handleTransfer">
            確認轉帳
          </a-button>
          <a-button danger @click="handleClear">清除</a-button>
        </div>
      </a-form>
    </a-card>

    <a-card v-if="result" style="width: 100%; max-width: 600px; margin-top: 16px" title="轉帳結果">
      <p>
        交易編號: {{ result.referenceId }}
        <a-button size="small" style="margin-left: 8px" @click="copyReferenceId">複製</a-button>
      </p>
      <p>來源帳戶餘額: {{ formatAmount(result.fromAccountBalance) }}</p>
      <p>目的帳戶餘額: {{ formatAmount(result.toAccountBalance) }}</p>
      <p>交易時間: {{ formatTime(result.transferredAt) }}</p>
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { transfer } from '@/api/account'

const loading = ref(false)
const result = ref(null)

const form = reactive({
  fromAccountNumber: '',
  toAccountNumber: '',
  amount: null,
  note: '',
})

function formatAmount(value) {
  if (value == null) return '-'
  return Number(value).toLocaleString()
}

function formatTime(value) {
  if (!value) return '-'
  return value.replace('T', ' ').substring(0, 19)
}

function handleClear() {
  form.fromAccountNumber = ''
  form.toAccountNumber = ''
  form.amount = null
  form.note = ''
  result.value = null
}

function copyReferenceId() {
  navigator.clipboard.writeText(result.value.referenceId).then(() => {
    message.success('已複製交易編號')
  })
}

async function handleTransfer() {
  if (!form.fromAccountNumber || !form.toAccountNumber || !form.amount) {
    message.warning('請填寫必要欄位')
    return
  }

  loading.value = true
  result.value = null
  try {
    const res = await transfer({
      fromAccountNumber: form.fromAccountNumber,
      toAccountNumber: form.toAccountNumber,
      amount: form.amount,
      note: form.note || null,
    })
    result.value = res.data.data
    message.success('轉帳成功')
  } catch (err) {
    message.error(err.response?.data?.message || '轉帳失敗')
  } finally {
    loading.value = false
  }
}
</script>