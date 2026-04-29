<template>
  <div style="padding: 20px">

    <a-select
    v-model:value="statusFilter"
    placeholder="事件類型"
    style="width: 150px"
    allow-clear
    >
    <a-select-option value="">貸款</a-select-option>
    <a-select-option value="">轉帳</a-select-option>
    <a-select-option value="">登入</a-select-option>

  </a-select>

  <a-input-search
    v-model:value="searchText"
    placeholder="搜尋事件類型..."
    style="width: 200px; margin-bottom: 16px"
    @search="loadData"
  />
    <a-table
      :columns="columns"
      :data-source="dataSource"
      :pagination="pagination"
      :loading="loading"
      @change="handleTableChange"
      row-key="id"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import axios from 'axios';

const dataSource = ref([]);
const loading = ref(false);
const searchText = ref('');

const columns = [
  { title: 'ID', dataIndex: 'id' },
  { title: '事件類型', dataIndex: 'eventType' },
  {title: '風險等級', dataIndex: 'riskLevel' },
  { title: '採取行動', dataIndex: 'actionTaken' },
  { title: '時間', dataIndex: 'createdAt' }
];

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
});

const loadData = async () => {
  loading.value = true;
  try {
    const res = await axios.get('/api/admin/riskevent', {
      params: {
        page: pagination.current - 1, // 後端從 0 開始
        size: pagination.pageSize,
        eventType: searchText.value || null
      }
    });
    // 對接 Spring Page 物件
    dataSource.value = res.data.content;
    pagination.total = res.data.totalElements;
  } finally {
    loading.value = false;
  }
};

const handleTableChange = (pag) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  loadData();
};

onMounted(() => loadData());
</script>
