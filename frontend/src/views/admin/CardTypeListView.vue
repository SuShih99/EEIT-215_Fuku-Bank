<script setup>
import { ref, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import api from '@/api/axios'
import { createCardType, uploadImage,  deleteCardType } from '@/api/cardtype'

const cardTypes = ref([])
const loading = ref(true)
// const response = await api.get('/api/admin/card-types')
// cardTypes.value = response.data
// loading.value = false

//上傳圖片
const file = ref(null)
// 表單資料
const form = ref({
  cardTypeName: '',
})
// modal控制
const open = ref(false)
//==工具函式==
// 取得圖片路徑
const getImageUrl = (path) => {
  return `${api.defaults.baseURL}/${path}`
}
//處理圖片
const handleFileChange = (e) => {
  // if (file.value.size > 5 * 1024 * 1024) {
  // message.error('圖片不能超過 5MB')
  // return
// }
  
  file.value = e.target.files[0]
}


// 表格欄位定義
const columns = [
  { title: 'ID', dataIndex: 'cardTypeId', key: 'cardTypeId', width: 80, align: 'center' },
  { title: '卡片名稱', dataIndex: 'cardTypeName', key: 'cardTypeName' },
  {
    title: '卡片圖片',
    dataIndex: 'cardImageUrl',
    key: 'cardImageUrl',
    width: 150,
    align: 'center',
  },
  { title: '操作', key: 'action', width: 200, align: 'center' },
]

// 取得資料
const fetchCardTypes = async () => {
  loading.value = true
  try {
    const response = await api.get('/api/admin/card-types')
    console.log(response.data)
    cardTypes.value = response.data
  } catch (error) {
    message.error('讀取資料失敗: ' + (error.response?.data?.message || error.message))
  } finally {
    loading.value = false
  }
}
//處理新增
const handleCreate = async () => {
  try {
    console.log(file.value)
    const formData = new FormData()
    formData.append('file', file.value)

    const res = await uploadImage(formData)
    const imageUrl = res.data.url

    await createCardType({
      cardTypeName: form.value.cardTypeName,
      cardImageUrl: imageUrl,
    })

    message.success('新增成功')
    open.value = false
    await fetchCardTypes()
  } catch (error) {
    message.error(error.response?.data?.message || '新增失敗')
  }
}

// 刪除處理
const handleDelete = (id) => {
  Modal.confirm({
    title: '確定要刪除此卡別嗎？',
    content: '此操作不可復原，若該卡別已被信用卡使用則無法刪除。',
    okText: '確定刪除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await deleteCardType(id)
        message.success('刪除成功')
        fetchCardTypes()
      } catch (error) {
        // 這裡會抓到後端 CardTypeService 丟出的 BusinessException (例如：卡別使用中)
        message.error(error.response?.data?.message || '刪除失敗')
        throw error
      }
    },
  })
}

onMounted(() => {
  fetchCardTypes()
})
</script>

<template>
  <div style="padding: 24px">
    <div
      style="
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
      "
    >
      <h2>卡別管理</h2>
      <div style="display: flex; gap: 8px">
        <a-button type="primary" @click="open = true">新增卡別</a-button>
        <a-button @click="fetchCardTypes">重新整理</a-button>
      </div>
    </div>

    <a-table
      :columns="columns"
      :data-source="cardTypes"
      :loading="loading"
      row-key="cardTypeId"
      bordered
    >
      <template #bodyCell="{ column, record }">
        <!-- 圖片顯示處理 -->
        <template v-if="column.key === 'cardImageUrl'">
          <a-image
            v-if="record.cardImageUrl"
            :width="100"
            :src="getImageUrl(record.cardImageUrl)"
            :preview="true"
          />
          <span v-else>無圖片</span>
        </template>

        <!-- 操作按鈕處理 -->
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button size="small" @click="() => message.info('編輯 ID: ' + record.cardTypeId)"
              >編輯</a-button
            >
            <a-button size="small" danger @click="handleDelete(record.cardTypeId)">刪除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- modal -->
    <a-modal v-model:open="open" title="新增卡別" @ok="handleCreate">
      <a-form layout="vertical">
        <a-form-item label="卡片名稱">
          <a-input v-model:value="form.cardTypeName" />
        </a-form-item>

        <a-form-item label="圖片">
          <input type="file" @change="handleFileChange" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
