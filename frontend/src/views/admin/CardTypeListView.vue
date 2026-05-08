<script setup>
import { ref, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import api from '@/api/axios'
import { getCardTypes,createCardType, uploadImage,  deleteCardType,updateCardType } from '@/api/cardtype'
import { PlusOutlined, SyncOutlined } from '@ant-design/icons-vue'

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
  cashbackRate:0,
})
// 編輯id
const editingId = ref(null)
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
  { title: '回饋率(%)', dataIndex: 'cashbackRate', key: 'cashbackRate' },
  { title: '操作', key: 'action', width: 140, align: 'right', fixed: 'right' },
]

// 取得資料
const fetchCardTypes = async () => {
  loading.value = true
  try {
    const data = await getCardTypes()
    cardTypes.value = data
    console.log(data);
    
  } catch (error) {
    message.error(error.response?.data?.message || '讀取資料失敗')
  } finally {
    loading.value = false
  }
}
// 新增卡別
// 新增 / 更新
const handleCreate = async () => {
  try {
    if (!form.value.cardTypeName) {
      message.error('請輸入卡片名稱')
      return
    }

    let imageUrl = null

    // 有選圖片才上傳
    if (file.value) {
      const formData = new FormData()
      formData.append('file', file.value)

      const imageRes = await uploadImage(formData)
      imageUrl = imageRes.url
    }

    if (editingId.value) {
      // 更新
      await updateCardType(editingId.value, {
        cardTypeName: form.value.cardTypeName,
        cashbackRate: form.value.cashbackRate,
        cardImageUrl: imageUrl, // 可為 null（看後端）
      })
      message.success('更新成功')
    } else {
      // 新增
      if (!imageUrl) {
        message.error('請選擇圖片')
        return
      }

      await createCardType({
        cardTypeName: form.value.cardTypeName,
        cashbackRate: form.value.cashbackRate,
        cardImageUrl: imageUrl,
      })
      message.success('新增成功')
    }

    // reset
    open.value = false
    editingId.value = null
    form.value.cardTypeName = ''
    form.value.cashbackRate = 0
    file.value = null

    await fetchCardTypes()
  } catch (error) {
    message.error(error.response?.data?.message || '新增失敗')
  }
}
// 編輯處理
const handleEdit = (record)=>{
  editingId.value = record.cardTypeId
  form.value.cardTypeName = record.cardTypeName
  form.value.cashbackRate = record.cashbackRate
  open.value = true
  file.value = null
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
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">卡別管理</h2>
    </div>

    <!-- 頂部 F 橫劃：主操作 -->
    <div class="action-bar" style="justify-content: flex-end;">
      <div class="global-actions">
        <a-button class="rounded-btn btn-ghost" @click="fetchCardTypes">
          <template #icon><SyncOutlined /></template>
          重新整理
        </a-button>
        <a-button type="primary" class="rounded-btn" @click="open = true">
          <template #icon><PlusOutlined /></template>
          新增卡別
        </a-button>
      </div>
    </div>

    <a-table
      :columns="columns"
      :data-source="cardTypes"
      :loading="loading"
      row-key="cardTypeId"
      class="custom-table"
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
          <div class="action-cell">
            <a-button type="link" class="action-btn edit-btn" @click="handleEdit(record)">編輯</a-button>
            <a-divider type="vertical" />
            <a-button type="link" class="action-btn suspend-btn" @click="handleDelete(record.cardTypeId)">刪除</a-button>
          </div>
        </template>
      </template>
    </a-table>
    <!-- modal -->
    <a-modal v-model:open="open" :title="editingId ? '編輯卡別' : '新增卡別'" @ok="handleCreate">
      <a-form layout="vertical">
        <a-form-item label="卡片名稱">
          <a-input v-model:value="form.cardTypeName" />
        </a-form-item>

        <a-form-item label="圖片">
          <input type="file" @change="handleFileChange" />
        </a-form-item>
        <a-form-item label="回饋率(%)">
          <a-input-number v-model:value="form.cashbackRate" min="0" max="100"/>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.suspend-btn {
  color: #ff4d4f;
}
.suspend-btn:hover {
  color: #d9363e;
  background-color: rgba(255, 77, 79, 0.05);
}
</style>
