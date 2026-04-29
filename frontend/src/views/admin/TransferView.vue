<template>
  <div style="padding: 24px">
    <h2>交易操作</h2>

    <!-- 交易類型切換 -->
    <div style="margin-bottom: 16px">
      <a-radio-group v-model:value="txType" button-style="solid" @change="handleClear">
        <a-radio-button value="transfer">轉帳</a-radio-button>
        <a-radio-button value="deposit">存款</a-radio-button>
        <a-radio-button value="withdraw">提款</a-radio-button>
        <a-radio-button value="reversal">沖正</a-radio-button>
      </a-radio-group>
    </div>

    <a-card style="width: 100%; max-width: 600px">
      <a-form layout="vertical">
        <!-- 轉帳：來源 + 目的帳號 -->
        <template v-if="txType === 'transfer'">
          <a-form-item label="來源帳號">
            <a-input v-model:value="form.fromAccountNumber" placeholder="請輸入來源帳號" allow-clear />
          </a-form-item>
          <a-form-item label="目的帳號">
            <a-input v-model:value="form.toAccountNumber" placeholder="請輸入目的帳號" allow-clear />
          </a-form-item>
        </template>

        <!-- 沖正：只需交易編號 -->
        <template v-else-if="txType === 'reversal'">
          <a-form-item label="原始交易編號">
            <a-input v-model:value="form.originalReferenceId" placeholder="請輸入要沖正的交易編號" allow-clear />
          </a-form-item>
        </template>

        <!-- 存提款：只需一個帳號 -->
        <template v-else>
          <a-form-item label="帳號">
            <a-input v-model:value="form.accountNumber" placeholder="請輸入帳號" allow-clear />
          </a-form-item>
        </template>

        <!-- 金額：沖正不需要 -->
        <a-form-item v-if="txType !== 'reversal'" label="金額">
          <a-input-number
            v-model:value="form.amount"
            placeholder="請輸入金額"
            :min="1"
            style="width: 100%"
          />
        </a-form-item>

        <a-form-item :label="txType === 'reversal' ? '沖正原因' : '備註'">
          <a-input v-model:value="form.note" placeholder="選填" allow-clear />
        </a-form-item>

        <div style="display: flex; gap: 8px">
          <a-button type="primary" :loading="loading" @click="handleSubmit" :danger="txType === 'reversal'">
            {{ txType === 'transfer' ? '確認轉帳' : txType === 'deposit' ? '確認存款' : txType === 'withdraw' ? '確認提款' : '確認沖正' }}
          </a-button>
          <a-button danger @click="handleClear">清除</a-button>
        </div>
      </a-form>
    </a-card>

    <!-- 轉帳結果 -->
    <a-card v-if="result && txType === 'transfer'" style="width: 100%; max-width: 600px; margin-top: 16px" title="轉帳結果">
      <p>
        交易編號: {{ result.referenceId }}
        <a-button size="small" style="margin-left: 8px" @click="copyReferenceId">複製</a-button>
      </p>
      <p>來源帳戶餘額: {{ formatAmount(result.fromAccountBalance) }}</p>
      <p>目的帳戶餘額: {{ formatAmount(result.toAccountBalance) }}</p>
      <p>交易時間: {{ formatTime(result.transferredAt) }}</p>
    </a-card>

    <!-- 存提款結果 -->
    <a-card v-if="result && (txType === 'deposit' || txType === 'withdraw')" style="width: 100%; max-width: 600px; margin-top: 16px" :title="txType === 'deposit' ? '存款結果' : '提款結果'">
      <p>
        交易編號: {{ result.referenceId }}
        <a-button size="small" style="margin-left: 8px" @click="copyReferenceId">複製</a-button>
      </p>
      <p>帳號: {{ result.accountNumber }}</p>
      <p>交易金額: {{ formatAmount(result.amount) }}</p>
      <p>交易後餘額: {{ formatAmount(result.balance) }}</p>
      <p>交易時間: {{ formatTime(result.transactedAt) }}</p>
    </a-card>

    <!-- 沖正結果 -->
    <a-card v-if="result && txType === 'reversal'" style="width: 100%; max-width: 600px; margin-top: 16px" title="沖正結果">
      <p>
        沖正交易編號: {{ result.reversalReferenceId }}
        <a-button size="small" style="margin-left: 8px" @click="copyReversalReferenceId">複製</a-button>
      </p>
      <p>被沖正的原始編號: {{ result.originalReferenceId }}</p>
      <p>沖正時間: {{ formatTime(result.reversedAt) }}</p>
      <a-divider />
      <p style="font-weight: bold; margin-bottom: 8px">沖正明細：</p>
      <div v-for="(detail, index) in result.details" :key="index" style="margin-bottom: 8px; padding: 8px; background: #fafafa; border-radius: 4px">
        <p>帳號: {{ detail.accountNumber }}</p>
        <p>沖正金額: {{ formatAmount(detail.reversedAmount) }}</p>
        <p>沖正後餘額: {{ formatAmount(detail.balanceAfter) }}</p>
      </div>
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { transfer, deposit, withdraw, reversal } from '@/api/account'

const txType = ref('transfer')
const loading = ref(false)
const result = ref(null)

const form = reactive({
  fromAccountNumber: '',
  toAccountNumber: '',
  accountNumber: '',
  originalReferenceId: '',
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
  form.accountNumber = ''
  form.originalReferenceId = ''
  form.amount = null
  form.note = ''
  result.value = null
}

function copyReferenceId() {
  navigator.clipboard.writeText(result.value.referenceId).then(() => {
    message.success('已複製交易編號')
  })
}

function copyReversalReferenceId() {
  navigator.clipboard.writeText(result.value.reversalReferenceId).then(() => {
    message.success('已複製沖正交易編號')
  })
}

async function handleSubmit() {
  loading.value = true
  result.value = null
  try {
    let res

    if (txType.value === 'transfer') {
      if (!form.fromAccountNumber || !form.toAccountNumber || !form.amount) {
        message.warning('請填寫必要欄位')
        loading.value = false
        return
      }
      res = await transfer({
        fromAccountNumber: form.fromAccountNumber,
        toAccountNumber: form.toAccountNumber,
        amount: form.amount,
        note: form.note || null,
      })
      message.success('轉帳成功')
    } else if (txType.value === 'deposit') {
      if (!form.accountNumber || !form.amount) {
        message.warning('請填寫必要欄位')
        loading.value = false
        return
      }
      res = await deposit({
        accountNumber: form.accountNumber,
        amount: form.amount,
        note: form.note || null,
      })
      message.success('存款成功')
    } else if (txType.value === 'withdraw') {
      if (!form.accountNumber || !form.amount) {
        message.warning('請填寫必要欄位')
        loading.value = false
        return
      }
      res = await withdraw({
        accountNumber: form.accountNumber,
        amount: form.amount,
        note: form.note || null,
      })
      message.success('提款成功')
    } else {
      // 沖正
      if (!form.originalReferenceId) {
        message.warning('請填寫原始交易編號')
        loading.value = false
        return
      }
      res = await reversal({
        originalReferenceId: form.originalReferenceId,
        reason: form.note || null,
      })
      message.success('沖正成功')
    }

    result.value = res.data.data
  } catch (err) {
    message.error(err.response?.data?.message || '操作失敗')
  } finally {
    loading.value = false
  }
}
</script>
