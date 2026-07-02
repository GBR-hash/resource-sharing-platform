import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'ResourceList',
        component: () => import('@/views/ResourceList.vue')
      },
      {
        path: 'resources',
        name: 'ResourceListAlt',
        component: () => import('@/views/ResourceList.vue')
      },
      {
        path: 'resource/:id',
        name: 'ResourceDetail',
        component: () => import('@/views/ResourceDetail.vue')
      },
      {
        path: 'upload',
        name: 'UploadResource',
        component: () => import('@/views/UploadResource.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('@/views/Profile.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'ops-agent',
        name: 'OpsAgent',
        component: () => import('@/views/OpsAgent.vue')
      },
      {
        path: 'admin',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/UserManagement.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: 'admin/resources',
        name: 'AdminResources',
        component: () => import('@/views/admin/ResourceManagement.vue'),
        meta: { requiresAdmin: true }
      }
    ]
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/403.vue')
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/404.vue')
  }
]

const router = createRouter({
  history: createWebHistory('/resource/'),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  let user = {}
  try {
    const userStr = localStorage.getItem('user')
    if (userStr && userStr !== 'undefined') {
      user = JSON.parse(userStr)
    }
  } catch (e) {
    user = {}
  }

  if (to.meta.requiresAuth && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && user.role !== 1) {
    return '/403'
  }
  if ((to.path === '/login' || to.path === '/register') && token) {
    return '/'
  }
})

export default router

