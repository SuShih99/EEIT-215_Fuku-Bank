import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // === 客戶端登入 ===
    {
      path: '/',
      name: 'user-login',
      component: () => import('../views/user/UserLoginView.vue'),
    },

    // === 管理端登入 ===
    {
      path: '/admin/login',
      name: 'admin-login',
      component: () => import('../views/admin/AdminLoginView.vue'),
    },

    // === 管理端頁面（登入後） ===
    {
      path: '/admin',
      component: () => import('../layouts/AdminLayout.vue'),
      children: [
        {
          path: '',
          name: 'admin-home',
          component: () => import('../views/admin/AdminHomeView.vue'),
        },
        {
          path: 'accounts',
          name: 'admin-accounts',
          component: () => import('../views/admin/AccountListView.vue'),
        },
        {
          path: 'transfers',
          name: 'admin-transfers',
          component: () => import('../views/admin/TransferView.vue'),
        },
        {
          path: 'trans-logs',
          name: 'admin-trans-logs',
          component: () => import('../views/admin/TransLogView.vue'),
        },
      ],
    },

    // === 客戶端頁面（未來做） ===
    // {
    //   path: '/user',
    //   component: () => import('../layouts/UserLayout.vue'),
    //   children: [ ... ],
    // },
  ],
})

export default router
