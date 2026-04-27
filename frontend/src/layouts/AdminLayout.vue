<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider
      v-model:collapsed="collapsed"
      collapsible
      :width="200"
      :collapsed-width="80"
      style="position: fixed; left: 0; top: 0; bottom: 0; z-index: 100"
    >
      <div style="height: 32px; margin: 16px; color: white; text-align: center; font-size: 18px">
        {{ collapsed ? '爪哇' : '爪哇銀行' }}
      </div>
      <a-menu theme="dark" mode="inline" :selected-keys="selectedKeys">
        <a-menu-item key="admin-home" @click="$router.push('/admin')">
          <HomeOutlined />
          <span>首頁</span>
        </a-menu-item>
        <a-menu-item key="admin-accounts" @click="$router.push('/admin/accounts')">
          <BankOutlined />
          <span>帳戶管理</span>
        </a-menu-item>
        <a-menu-item key="admin-transfers" @click="$router.push('/admin/transfers')">
          <SwapOutlined />
          <span>轉帳</span>
        </a-menu-item>
        <a-menu-item key="admin-trans-logs" @click="$router.push('/admin/trans-logs')">
          <FileTextOutlined />
          <span>交易紀錄</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>

    <a-layout :style="{ marginLeft: collapsed ? '80px' : '200px', transition: 'margin-left 0.2s' }">
      <a-layout-header style="background: #fff; padding: 0 16px; font-size: 16px">
        管理端
      </a-layout-header>
      <a-layout-content style="margin: 16px">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { HomeOutlined, BankOutlined, SwapOutlined, FileTextOutlined } from '@ant-design/icons-vue'

const collapsed = ref(false)
const route = useRoute()

const selectedKeys = computed(() => [route.name])
</script>
