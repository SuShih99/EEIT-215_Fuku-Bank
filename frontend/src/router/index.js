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
      meta: { requiresAuth: true },
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
          path: 'employees',
          name: 'admin-employees',
          //http://localhost:5173/admin/employees
          component: () => import('../views/admin/EmployeeListView.vue'),
        },
        {
          path: 'customers',
          name: 'admin-customers',
          //http://localhost:5173/admin/customers
          component: () => import('../views/admin/CustomerListView.vue'),
        },
        {
          path: 'card-types',
          name: 'admin-card-types',
          //http://localhost:5173/admin/card-types
          component: () => import('../views/admin/CardTypeListView.vue'),

        },
        {
          path:'card-applications',
          name:'admin-card-applications',
          //http://localhost:5173/admin/card-applications
          component: () => import('../views/admin/CardApplicationList.vue'),
        },
        {
          path: 'risk-events',
          name: 'admin-risk-events',
          //http://localhost:5173/admin/risk-events
          component: () => import('../views/admin/RiskEventView.vue'),
        },
        {
          name:'admin-card-application-detail',
          path:'/admin/card-applications/:id',
          component: () => import('../views/admin/CardApplicationDetailView.vue'),
        },
        {
          path: 'cards',
          name: 'admin-cards',
          component: () => import('../views/admin/CardView.vue'),
        },
        // 貸款功能相關
        {
          // 客戶端申請功能測試
          path: 'loan-apply',
          name: 'loan-apply',
          //http://localhost:5173/admin/loan-apply
          component: () => import('../views/user/LoanApplyView.vue'),
        },
        {
          path: 'loan-applications',
          name: 'loan-applications',
          //http://localhost:5173/admin/loan-applications
          component: () => import('../views/admin/LoanApplicationView.vue'),
        },
        {
          path: 'blacklist',
          name: 'admin-blacklist',
          //http://localhost:5173/admin/blacklist
          component: () => import('../views/admin/BlackListView.vue'),
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
    //   component: () => import('../views/user/CardTypeListView.vue'),
    // }


    //    ],
    // },




  ],
})

// === 路由守衛：未登入的人不能進管理端 ===
router.beforeEach(async (to) => {
  // 檢查目標路由（或其父路由）是否需要登入
  if (to.matched.some((record) => record.meta.requiresAuth)) {
    const authUser = localStorage.getItem('auth_user')
    if (!authUser) {
      // localStorage 沒資料 → 一定沒登入
      return { name: 'admin-login' }
    }

    // localStorage 有資料 → 再跟後端確認 Session 是否還活著
    try {
      const { checkAuth } = await import('@/api/auth')
      await checkAuth()
      // Session 有效 → 放行
    } catch {
      // Session 過期或無效 → 清掉 localStorage，導回登入頁
      localStorage.removeItem('auth_user')
      return { name: 'admin-login' }
    }
  }
  return true
})

export default router
