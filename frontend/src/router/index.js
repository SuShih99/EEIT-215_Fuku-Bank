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
        {
          path: 'card-types',
          name: 'admin-card-types',
          //http://localhost:5173/admin/card-types
          component: () => import('../views/admin/CardTypeListView.vue'),
        },
        {
          path: 'card-applications',
          name: 'admin-card-applications',
          //http://localhost:5173/admin/card-applications
          component: () => import('../views/admin/CardApplicationList.vue'),
        },

        // 貸款功能相關
        {
          // 客戶端申請功能測試
          path: 'loan-apply',
          name: 'loan-apply',
          //http://localhost:5173/admin/loan-applications
          component: () => import('../views/admin/LoanApplyView.vue'),
        },
        {
          path: 'loan-applications',
          name: 'loan-applications',
          //http://localhost:5173/admin/loan-applications
          component: () => import('../views/admin/LoanApplicationView.vue'),
        },
      ],
    },

    // === 客戶端頁面（未來做） ===
    // {
    //   path: '/user',
    //   component: () => import('../layouts/UserLayout.vue'),
    //   children: [
    //

    // === 客戶端信用卡頁面===
    // {
    //   path: '/user/cards',
    //   name: 'user-cards',
    //   component: () => import('../views/user/CardListView.vue'),
    // }

    //    ],
    // },

    // === 客戶端貸款申請頁面===
    // {
    //   path: '/user/loans',
    //   name: 'user-loans',
    //   component: () => import('../views/user/LoanApplyView.vue'),
    // }

    //    ],
    // },
  ],
})

export default router
