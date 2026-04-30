<script setup>
import { ref, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { getApplications, deleteApplication, updateApplicationStatus,updateApplicationRemark } from '@/api/cardApplication'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { DownOutlined } from '@ant-design/icons-vue'

const router = useRouter()

const goDetail = (record) => {
  router.push(`/admin/card-applications/${record.applicationId}`)
}

const applications = ref([])
const loading = ref(false)

//分頁機
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
})

const remarkInput = ref('')
const remarkModalVisible = ref(false)
const currentRecord = ref(null)

const openRemarkModal = (record) => {
  currentRecord.value = record
  remarkInput.value = record.remark || ''
  remarkModalVisible.value = true
}

//修改備註
const handleUpdateRemark = async () => {
  try {
    await updateApplicationRemark(currentRecord.value.applicationId, remarkInput.value)
    message.success('備註更新成功')
    remarkModalVisible.value = false
    await fetchData()
  } catch (error) {
    message.error('備註更新失敗')
  }
}



// 表格欄位
const columns = [
  { title: 'ID', dataIndex: 'applicationId', key: 'applicationId' },
  {
    title: '申請日期',
    dataIndex: 'applyDate',
    key: 'applyDate',
    customRender: (record) => {
      return dayjs(record.applyDate).format('YYYY-MM-DD HH:mm')
    },
  },
  { title: '使用者', dataIndex: 'customerName', key: 'customerName' },
  { title: '備註', dataIndex: 'remark', key: 'remark' },
  { title: '狀態', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action' },
]
//取得資料
const fetchData = async () => {
  loading.value = true
  try {
    const response = await getApplications({
      page: pagination.value.current - 1,
      size: pagination.value.pageSize,
    })
    applications.value = response.data.content

    console.log('applications:', applications.value)

    pagination.value.total = response.data.totalElements
  } catch (error) {
    console.log(error)
  } finally {
    loading.value = false
  }
}
//刪除資料
const handleDelete = async (record) => {
  Modal.confirm({
    title: '確定刪除嗎?',
    onOk: async () => {
      try {
        await deleteApplication(record.applicationId)
        await fetchData()
        message.success('刪除成功')
        fetchData()
      } catch (error) {
        console.log(error)
      }
    },
  })
}
//核准資料
const handleApprove = (record) => {
  handleUpdate(record.applicationId, 'APPROVED')
  message.success('核准成功')
}
//拒絕資料
const handleReject = (record) => {
  handleUpdate(record.applicationId, 'REJECTED')
  message.success('拒絕成功')
}

const handleUpdate = async (id, status) => {
  try {
    await updateApplicationStatus(id, status)
    fetchData()
  } catch (error) {
    console.log(error)
  }
}

//分頁
const handlePageChange = (page) => {
  pagination.value.current = page.current
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>
<template>
  <div>
    <h2>信用卡申請管理</h2>
    <a-table
      :columns="columns"
      :data-source="applications"
      :loading="loading"
      :pagination="pagination"
      row-key="applicationId"
      @change="handlePageChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-dropdown>
            <a-button>
              操作
              <DownOutlined />
            </a-button>

            <template #overlay>
              <a-menu>
                <a-menu-item @click="handleApprove(record)"> 核准 </a-menu-item>
                
                <a-menu-item @click="handleReject(record)"> 拒絕 </a-menu-item>
                <a-menu-item @click="openRemarkModal(record)"> 修改備註 </a-menu-item>

                <a-menu-item danger @click="handleDelete(record)"> 刪除 </a-menu-item>

                <a-menu-item @click="goDetail(record)"> 查看明細 </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </template>
      </template>
    </a-table>
    <!-- modal -->
    <a-modal
      v-model:open="remarkModalVisible"
      title="修改備註"
      @ok="handleUpdateRemark"
      @cancel="remarkModalVisible = false"
    >
    <a-input v-model:value="remarkInput" placeholder="請輸入備註" autofocus></a-input>
    </a-modal>
  </div>
</template>
