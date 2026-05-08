import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // === 客戶端：登入前 ===
    {
      path: '/',
      name: 'landing',
      component: () => import('../views/user/LandingView.vue'),
    },
    {
      path: '/login',
      name: 'user-login',
      component: () => import('../views/user/UserLoginView.vue'),
    },
    {
      path: '/register',
      name: 'user-register',
      component: () => import('../views/user/RegisterView.vue'),
    },
    {
      path: '/reset-password',
      name: 'reset-password',
      component: () => import('../views/user/ResetPasswordView.vue'),
    },

    // === 客戶端：登入後 ===
    {
      path: '/user',
      component: () => import('../layouts/UserLayout.vue'),
      meta: { requiresCustomerAuth: true },
      children: [
        {
          path: '',
          redirect: { name: 'user-home' },
        },
        {
          path: 'home',
          name: 'user-home',
          component: () => import('../views/user/UserHomeView.vue'),
        },
        {
          path: 'profile',
          name: 'user-profile',
          component: () => import('../views/user/UserProfileView.vue'),
        },
        {
          path: 'card-types',
          name: 'user-card-types',
          component: () => import('../views/user/CardTypeListView.vue'),
        },
        {
          path:'card-applications',
          name:'user-card-applications',
          component: () => import('../views/user/CardApplicationForm.vue'),
        },
        {
          path:'card-txns',
          name:'user-card-txns',
          component: () => import('../views/user/CardTxnView.vue'),
        },
        {
          path: 'card-bills',
          name: 'user-card-bills',
          component: () => import('../views/user/CardBillView.vue'),
        }
        ,
        {
          path: 'account-application',
          name: 'user-account-application',
          component: () => import('../views/user/AccountApplicationView.vue'),
        },
        
      
      ],
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
          path: 'account-applications',
          name: 'admin-account-applications',
          component: () => import('../views/admin/AccountApplicationListView.vue'),
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
          meta: { requiresAdmin: true },
          component: () => import('../views/admin/EmployeeListView.vue'),
        },
        {
          path: 'employees/create',
          name: 'admin-employees-create',
          meta: { requiresAdmin: true },
          component: () => import('../views/admin/EmployeeCreateView.vue'),
        },
        {
          path: 'customers',
          name: 'admin-customers',
          component: () => import('../views/admin/CustomerListView.vue'),
        },
        {
          path: 'customers/create',
          name: 'admin-customers-create',
          component: () => import('../views/admin/CustomerCreateView.vue'),
        },
        {
          path: 'card-types',
          name: 'admin-card-types',
          component: () => import('../views/admin/CardTypeListView.vue'),
        },
        {
          path:'card-applications',
          name:'admin-card-applications',
          component: () => import('../views/admin/CardApplicationList.vue'),
        },
        {
          path:'card-txns',
          name:'admin-card-txns',
          //http://localhost:5173/admin/card-txns
          component: () => import('../views/admin/CardTxnView.vue'),
        },
        {
          path: 'card-bills',
          name: 'admin-card-bills',
          //http://localhost:5173/admin/card-bills
          component: () => import('../views/admin/CardBillView.vue'),
        },
        {
          path: 'risk-events',
          name: 'admin-risk-events',
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
          path: 'loan-apply',
          name: 'loan-apply',
          component: () => import('../views/user/LoanApplyView.vue'),
        },
        {
          path: 'loan-applications',
          name: 'loan-applications',
          component: () => import('../views/admin/LoanApplicationView.vue'),
        },
        {
          path: 'blacklist',
          name: 'admin-blacklist',
          component: () => import('../views/admin/BlackListView.vue'),
        },
        {
          path: 'logs',
          name: 'admin-logs',
          meta: { requiresAdmin: true },
          component: () => import('../views/admin/SystemLogView.vue'),
        },
      ],
    },

    // === 403 頁面 ===
    {
      path: '/forbidden',
      name: 'forbidden',
      component: {
        template: `
          <div style="display:flex;align-items:center;justify-content:center;min-height:100vh;background:#f5f6fa">
            <div style="text-align:center">
              <h1 style="font-size:72px;color:#ff4d4f;margin:0">403</h1>
              <p style="font-size:18px;color:#666">您沒有權限存取此頁面</p>
              <a-button type="primary" @click="$router.push('/')">回到首頁</a-button>
            </div>
          </div>
        `,
      },
    },
  ],
})

// === 路由守衛 ===
router.beforeEach(async (to) => {
  // --- 客戶端路由守衛 ---
  if (to.matched.some((record) => record.meta.requiresCustomerAuth)) {
    const customerToken = localStorage.getItem('customer_token')
    if (!customerToken) {
      return { name: 'user-login' }
    }
  }

  // --- 管理端路由守衛 ---
  if (to.matched.some((record) => record.meta.requiresAuth)) {
    const authUser = localStorage.getItem('auth_user')
    if (!authUser) {
      return { name: 'admin-login' }
    }

    let parsedUser = null
    try {
      parsedUser = JSON.parse(authUser)
      if (parsedUser.role === 'CUSTOMER') {
        return { name: 'forbidden' }
      }
    } catch {
      // ignore parse error
    }

    // --- 檢查系統管理員權限 (僅系統與日誌需要) ---
    if (to.matched.some((record) => record.meta.requiresAdmin)) {
      if (parsedUser) {
        // 放寬後的業務模組已拔除 requiresAdmin
        // 所以會進入這裡的只剩 employees 跟 logs，這些保留給資安與系統管理員
        const systemAdminRoles = ['ISSA', 'CISO', 'SYS_SUPER', 'SYS_STAFF']
        if (!systemAdminRoles.includes(parsedUser.roleCode)) {
          return { name: 'forbidden' } 
        }
      } else {
        return { name: 'admin-login' }
      }
    }

    try {
      const { checkAuth } = await import('@/api/auth')
      await checkAuth()
    } catch {
      localStorage.removeItem('auth_user')
      return { name: 'admin-login' }
    }
  }

  return true
})

export default router