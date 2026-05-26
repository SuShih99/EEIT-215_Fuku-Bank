<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { confirmLinePay } from '@/api/userCardTxn'

const loading = ref(true)
const success = ref(false)

const route = useRoute()
const router = useRouter()
const statusMessage = ref('Processing LINE Pay...')
const errorMessage = ref('')

onMounted(async () => {
  try {
    const transactionId = route.query.transactionId
    const orderId = route.query.orderId || sessionStorage.getItem('linepay_order_id')

    if (!transactionId) {
      throw new Error('Missing LINE Pay transactionId. Please return from the LINE Pay payment page.')
    }
    if (!orderId) {
      throw new Error('Missing LINE Pay orderId. Please start the payment again.')
    }

    await confirmLinePay(transactionId, orderId)

    sessionStorage.removeItem('linepay_order_id')
    success.value = true
    loading.value = false

    statusMessage.value = 'LINE Pay payment success'
  
    setTimeout(()=>{
      router.push('/user/card-txns')

    },2000)
  } catch (err) {
    console.error(err)
    const message = err.response?.data?.message || err.message || 'LINE Pay confirm failed'
    errorMessage.value = message
    statusMessage.value = 'LINE Pay confirm failed'
  }finally{
    loading.value = false
  }
})
</script>

<template>
  <div class="min-h-screen bg-slate-100 flex items-center justify-center px-4">
    <div class="w-full max-w-md rounded-3xl bg-white p-10 text-center shadow-xl">

      <!-- Loading -->
      <template v-if="loading">
        <div class="mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-emerald-100">
          <LoadingOutlined class="text-4xl text-emerald-500" />
        </div>

        <h2 class="mb-3 text-2xl font-bold text-slate-800">
          付款確認中
        </h2>

        <p class="text-base text-slate-500">
          正在確認 LINE Pay 交易，請稍候...
        </p>
      </template>

      <!-- Success -->
      <template v-else-if="success">
        <div class="mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-emerald-100">
          <CheckCircleOutlined class="text-5xl text-emerald-500" />
        </div>

        <h2 class="mb-3 text-3xl font-bold text-emerald-600">
          付款成功
        </h2>

        <p class="mb-8 text-base text-slate-500">
          LINE Pay 交易已完成，即將返回交易紀錄頁。
        </p>

        <button
          class="w-full rounded-xl bg-emerald-500 px-6 py-3 text-base font-semibold text-white shadow-md transition hover:bg-emerald-600 active:scale-95"
          @click="router.push('/user/card-txns')"
        >
          查看交易紀錄
        </button>

        <p class="mt-5 text-sm text-slate-400">
          系統將自動跳轉...
        </p>
      </template>

      <!-- Failed -->
      <template v-else>
        <div class="mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-red-100">
          <CloseCircleOutlined class="text-5xl text-red-500" />
        </div>

        <h2 class="mb-3 text-3xl font-bold text-red-600">
          付款失敗
        </h2>

        <p class="mb-8 break-words text-base text-slate-500">
          {{ errorMessage }}
        </p>

        <button
          class="w-full rounded-xl bg-red-500 px-6 py-3 text-base font-semibold text-white shadow-md transition hover:bg-red-600 active:scale-95"
          @click="router.push('/user/card-txns')"
        >
          返回交易紀錄
        </button>
      </template>

    </div>
  </div>
</template>
