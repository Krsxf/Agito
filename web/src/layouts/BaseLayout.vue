<template>
  <!-- 四区域布局：顶部导航 / 侧边菜单 / 内容区 / 底部信息（5.4 节） -->
  <div class="app-shell">
    <header class="top-nav">
      <router-link to="/home" class="brand">
        <span class="brand-logo">番</span>追番助手
      </router-link>
      <search-bar class="nav-search" @select="goDetail" />
      <div class="nav-right">
        <router-link v-if="store.isLogin" to="/user" class="nav-bell" title="更新提醒">
          🔔<span v-if="store.unreadCount > 0" class="dot">{{ store.unreadCount > 99 ? '99+' : store.unreadCount }}</span>
        </router-link>
        <template v-if="store.isLogin">
          <router-link to="/user" class="nav-user">{{ store.userInfo?.nickname || store.userInfo?.username }}</router-link>
          <button class="btn btn-ghost btn-mini" @click="logout">退出</button>
        </template>
        <router-link v-else to="/login" class="btn btn-primary btn-mini">登录 / 注册</router-link>
      </div>
    </header>

    <div class="layout-body">
      <aside class="side-menu">
        <router-link v-for="m in menus" :key="m.path" :to="m.path" class="menu-item"
                    :class="{ active: isActive(m.path) }">
          <span>{{ m.icon }}</span>{{ m.label }}
          <span v-if="m.path === '/user' && store.unreadCount > 0" class="badge">{{ store.unreadCount }}</span>
        </router-link>
      </aside>

      <main class="content-area">
        <router-view />
      </main>
    </div>

    <footer class="app-footer">
      <span>追番助手 · 基于多元数据的个性化追番推荐与补番助手</span>
      <span>数据更新至 {{ today }}</span>
      <span>仅聚合元数据与正版外链跳转，不承载视频内容</span>
    </footer>

    <!-- 登录/注册成功后的欢迎动画（一次性，sessionStorage 触发） -->
    <welcome-overlay v-if="welcome" :name="welcome.name" :is-new="welcome.isNew" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { api } from '../api'
import SearchBar from '../components/SearchBar.vue'
import WelcomeOverlay from '../components/WelcomeOverlay.vue'

const store = useUserStore()
const router = useRouter()
const route = useRoute()
const today = new Date().toISOString().slice(0, 10).replace(/-/g, '.')

const menus = computed(() => {
  const list = [
    { path: '/home', icon: '🏠', label: '首页推荐' },
    { path: '/center', icon: '🎬', label: '番剧中心' },
    { path: '/follow', icon: '📌', label: '追番管理' },
    { path: '/catchup', icon: '📚', label: '补番助手' },
    { path: '/rank', icon: '🏆', label: '排行榜' },
    { path: '/user', icon: '👤', label: '用户中心' }
  ]
  if (store.isAdmin) list.push({ path: '/admin', icon: '⚙️', label: '后台管理' })
  return list
})

const isActive = p => route.path === p || (p === '/home' && route.path === '/')

const goDetail = id => router.push(`/anime/${id}`)

async function logout() {
  await api.logout().catch(() => null)
  store.clear()
  router.push('/login')
}

const welcome = ref(null)

onMounted(() => {
  store.fetchUserInfo().catch(() => null)
  store.fetchUnread().catch(() => null)
  // 欢迎动画：登录/注册成功后带一次性标记进入（sessionStorage 每标签页一次）
  try {
    const raw = sessionStorage.getItem('welcome')
    if (raw) {
      welcome.value = JSON.parse(raw)
      sessionStorage.removeItem('welcome')
    }
  } catch { /* 忽略异常标记 */ }
})
</script>

<style scoped>
.nav-search { flex: 1; max-width: 420px; }
.nav-right { margin-left: auto; display: flex; align-items: center; gap: 14px; }
.nav-bell { position: relative; font-size: 18px; }
.nav-bell .dot { position: absolute; top: -6px; right: -12px; min-width: 16px; height: 16px; padding: 0 4px; border-radius: 8px; background: #FF7675; color: #fff; font-size: 10px; line-height: 16px; text-align: center; }
.nav-user { font-weight: 600; color: var(--primary); }
</style>

<script>
export default { name: 'BaseLayout' }
</script>
