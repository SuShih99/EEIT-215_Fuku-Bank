<script setup>
import { ref, onMounted } from 'vue'
import {message,Modal} from 'ant-design-vue'
import api from '@/api/axios';
import { useRoute } from 'vue-router';

const loading = ref(false)

const route = useRoute()
const items=ref([])

const fetchDetail = async () => {
    const id = route.params.id
    loading.value = true
    const res = await api.get(`/api/admin/card-application-items/${id}`)
    items.value = res.data
    loading.value = false
}
const columns = [
    { title: '卡別', dataIndex: 'cardTypeName', key: 'cardTypeName' },
    { title: '額度(元)', dataIndex: 'approvedLimit', key: 'approvedLimit',
    customRender:({text})=>{
        return text ? Number(text).toLocaleString() : '無'
    } },
    { title: '年費(元)', dataIndex: 'annualFee', key: 'annualFee',
    customRender:({text})=>{
        return text ? Number(text).toLocaleString() : '0'
    }},
    { title: '結果', dataIndex: 'result', key: 'result' },

]



onMounted(() => {
    fetchDetail()
})


</script>
<template>
    <div>
    <h2>申請明細</h2>

    <a-table
      :columns="columns"
      :data-source="items"
      row-key="itemId"
      bordered
    >
    </a-table>
  </div>
</template>