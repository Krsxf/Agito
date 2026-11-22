import { createRouter, createWebHistory } from 'vue-router'

/**
 * 路由与登录守卫（5.5.3 节：token 失效引导重新登录；
 * 管理端路由仅 ADMIN 可入）
 */
const routes = [
  { path: '/login', name: 'login', component: () => import('../views/LoginPage.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('../layouts/BaseLayout.vue'),
    children: [
      { path: '', redirect: '/home' },
      { path: 'home', name: 'home', component: () => import('../views/HomePage.vue') },
      { path: 'center', name: 'center', component: () => import('../views/AnimeCenterPage.vue') },
      { path: 'anime/:id', name: 'detail', component: () => import('../views/AnimeDetailPage.vue') },
      { path: 'follow', name: 'follow', component: () => import('../views/FollowPage.vue') },
      { path: 'catchup', name: 'catchup', component: () => import('../views/CatchupPage.vue') },
      { path: 'rank', name: 'rank', component: () => import('../views/RankPage.vue') },
      { path: 'user', name: 'user', component: () => import('../views/UserCenterPage.vue') },
      {
        path: 'admin',
        name: 'admin',
        component: () => import('../views/AdminPage.vue'),
        meta: { admin: true }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  // 游客可浏览只读内容；写操作在页内引导登录（2.2 节容错原则）
  if (to.meta.admin) {
    const info = JSON.parse(localStorage.getItem('userInfo') || 'null')
    if (!token || !info?.isAdmin) return next('/login')
  }
  next()
})

export default router
