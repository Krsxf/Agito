<template>
  <!-- 首页推荐（界面文档 4.4 节：三栏目卡片流 + 换一批 + 新番速递） -->
  <div class="home-page">
    <div v-if="store.isLogin && data.coldStart" class="cold-banner card">
      🌱 你还是新用户：推荐按「兴趣标签 + 热门补偿」策略出内容，评分/收藏/追番积累 20 条行为后将升级为个性化协同过滤推荐
    </div>

    <section class="card">
      <div class="card-title">
        <span>🔥 今日推荐 <span class="sub">综合画像与行为得分</span></span>
        <button class="btn btn-ghost btn-mini" @click="refresh('today')">⟳ 换一批</button>
      </div>
      <div class="anime-grid">
        <anime-card v-for="a in data.today" :key="a.id" :anime="a" :followed="isFollowed(a.id)"
                   @click="go" @jump="go" @follow="follow(a.id)" />
      </div>
    </section>

    <section class="card">
      <div class="card-title">
        <span>💡 猜你喜欢 <span class="sub">协同过滤 × 内容过滤融合</span></span>
        <button class="btn btn-ghost btn-mini" @click="refresh('guess')">⟳ 换一批</button>
      </div>
      <div class="anime-grid">
        <anime-card v-for="a in data.guess" :key="a.id" :anime="a" :followed="isFollowed(a.id)"
                   @click="go" @jump="go" @follow="follow(a.id)" />
      </div>
    </section>

    <section class="card">
      <div class="card-title"><span>🚀 新番速递 <span class="sub">当季新番与待上线</span></span></div>
      <div class="filter-bar">
        <span class="filter-label">按星期筛选：</span>
        <span class="pill" :class="{ on: weekday === 0 }" @click="weekday = 0">全部</span>
        <span v-for="(d, i) in days" :key="i" class="pill" :class="{ on: weekday === i + 1 }" @click="weekday = i + 1">{{ d }}</span>
      </div>
      <div class="new-list">
        <div v-for="n in filteredNew" :key="n.id" class="new-item" @click="go(n.id)">
          <div class="new-name">{{ n.title }}</div>
          <div class="new-meta">
            <span class="tag-pill">{{ days[(n.weekday || 1) - 1] }}更新</span>
            <span v-if="n.episodeCount" class="tag-pill info">{{ n.episodeCount }}集</span>
            <span v-if="n.studio" class="studio">{{ n.studio }}</span>
          </div>
          <button class="btn btn-primary btn-mini" @click.stop="follow(n.id)">+ 追番</button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { useUserStore } from '../stores/user'
import AnimeCard from '../components/AnimeCard.vue'

const store = useUserStore()
const router = useRouter()
const data = ref({ today: [], guess: [], newAnime: [], coldStart: false })
const follows = ref([])
const weekday = ref(0)
const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

const filteredNew = computed(() =>
  weekday.value ? data.value.newAnime.filter(n => n.weekday === weekday.value) : data.value.newAnime)

const isFollowed = id => follows.value.some(f => f.animeId === id)
const go = id => router.push(`/anime/${id}`)

async function load() {
  const res = await api.homeRecommend().catch(() => null)
  if (res?.data) data.value = res.data
  if (store.isLogin) {
    const f = await api.myFollows().catch(() => null)
    if (f?.data) follows.value = f.data.list || []
  }
}

async function refresh(col) {
  const res = await api.refreshBatch(col).catch(() => null)
  if (res?.data) data.value = { ...data.value, ...res.data }
  ElMessage.success('已换一批')
}

async function follow(id) {
  if (!store.isLogin) return notLogin()
  await api.follow(id).catch(() => null)
  const f = await api.myFollows().catch(() => null)
  if (f?.data) follows.value = f.data.list || []
  store.fetchUnread()
  ElMessage.success('已加入追番')
}

function notLogin() {
  ElMessage.warning('游客可自由浏览，写操作请先登录')
  router.push('/login')
}

onMounted(load)
</script>

<style scoped>
.cold-banner { margin-bottom: 16px; padding: 12px 16px; background: #fdf3dd; color: #8a6d1a; font-size: 13px; border-radius: var(--radius-card); }
.new-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 12px; }
.new-item { display: flex; align-items: center; gap: 10px; padding: 12px 14px; border-radius: var(--radius-ctl); border: 1px solid #eef0f6; background: #fff; cursor: pointer; transition: all .15s; }
.new-item:hover { border-color: var(--primary); box-shadow: var(--card-shadow); }
.new-name { flex: 1; font-weight: 600; font-size: 13px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.new-meta { display: flex; align-items: center; gap: 6px; }
.studio { font-size: 11px; color: var(--text-3); }
</style>

<script>
export default { name: 'HomePage' }
</script>
