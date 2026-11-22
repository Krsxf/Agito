<template>
  <!-- 用户中心（界面文档 4.2 节旁支：收藏/评分/兴趣画像/提醒） -->
  <div class="user-page">
    <div class="card profile">
      <div class="avatar">{{ (info?.nickname || info?.username || '?').slice(0, 1) }}</div>
      <div class="p-info">
        <h2>{{ info?.nickname || info?.username }}</h2>
        <p class="p-meta">@{{ info?.username }} · 行为数据 {{ info?.behaviorCount || 0 }} 条
          <span v-if="(info?.behaviorCount || 0) < 20" class="tag-pill gold">冷启动中</span>
          <span v-else class="tag-pill success">个性化推荐已激活</span>
        </p>
        <div class="p-tags">
          <span class="filter-label">兴趣画像：</span>
          <span v-for="t in info?.interests || []" :key="t" class="tag-pill accent">{{ t }}</span>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-title">
        <span>🔔 更新提醒 <span class="sub">最长延迟 24 小时</span></span>
        <button v-if="notifies.length" class="btn btn-ghost btn-mini" @click="readAll">全部已读</button>
      </div>
      <div v-if="!notifies.length" class="empty-state">暂无新提醒</div>
      <p v-for="n in notifies" :key="n.id" class="notify-item" :class="{ unread: !n.isRead }">
        <span class="n-dot" v-if="!n.isRead"></span>{{ n.content }}
        <span class="n-time">{{ (n.createTime || '').slice(0, 16) }}</span>
      </p>
    </div>

    <div class="card">
      <div class="card-title"><span>📮 我的番剧建议 <span class="sub">提交想上架的番剧，管理员审核后上架</span></span></div>
      <div v-if="!suggestions.length" class="empty-state">还没有提交记录，去<a @click="$router.push('/center')">番剧中心</a>的「求上架」提交吧</div>
      <div v-for="s in suggestions" :key="s.id" class="row-item sg-row">
        <span class="i-title">{{ s.title }}</span>
        <span class="tag-pill gold" v-if="s.status === 0">待审核</span>
        <span class="tag-pill success" v-else-if="s.status === 1">已上架</span>
        <span class="tag-pill danger" v-else>已驳回</span>
        <span class="i-meta">{{ (s.createTime || '').slice(0, 10) }}</span>
      </div>
    </div>

    <div class="two-col">
      <div class="card">
        <div class="card-title"><span>⭐ 我的评分</span><span class="sub">{{ ratings.length }} 条</span></div>
        <div v-if="!ratings.length" class="empty-state">还没有评分记录</div>
        <div v-for="r in ratings" :key="r.animeId" class="row-item" @click="$router.push(`/anime/${r.animeId}`)">
          <span class="i-title">{{ r.title }}</span>
          <span class="tag-pill gold">我评 ★{{ r.myScore }}</span>
          <span class="i-meta">站均 {{ r.avgScore }}</span>
        </div>
      </div>

      <div class="card">
        <div class="card-title"><span>❤ 我的收藏</span><span class="sub">{{ favorites.length }} 部</span></div>
        <div v-if="!favorites.length" class="empty-state">还没有收藏</div>
        <div v-for="f in favorites" :key="f.animeId" class="row-item" @click="$router.push(`/anime/${f.animeId}`)">
          <span class="i-title">{{ f.title }}</span>
          <span class="tag-pill gold">★{{ f.score }}</span>
          <span class="i-meta">{{ f.year }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { useUserStore } from '../stores/user'

const store = useUserStore()
const router = useRouter()

const info = ref(null)
const ratings = ref([])
const favorites = ref([])
const notifies = ref([])
const suggestions = ref([])

async function load() {
  if (!store.isLogin) { router.push('/login'); return }
  info.value = store.userInfo
  const [rt, fv, nt, sg] = await Promise.all([
    api.myRatings().catch(() => null),
    api.myFavorites().catch(() => null),
    api.notifyList().catch(() => null),
    api.suggestMine().catch(() => null)
  ])
  if (rt?.data) ratings.value = rt.data.list || []
  if (fv?.data) favorites.value = fv.data.list || []
  if (nt?.data) notifies.value = nt.data || []
  if (sg?.data) suggestions.value = sg.data.list || []
}

async function readAll() {
  await api.readAll().catch(() => null)
  store.fetchUnread()
  ElMessage.success('已全部标记为已读')
  load()
}

onMounted(load)
</script>

<style scoped>
.profile { display: flex; gap: 18px; align-items: center; }
.avatar { width: 64px; height: 64px; border-radius: 20px; background: linear-gradient(135deg, var(--primary), var(--accent)); color: #fff; font-size: 26px; font-weight: 800; display: flex; align-items: center; justify-content: center; }
.p-info h2 { font-size: 20px; margin-bottom: 4px; }
.p-meta { font-size: 12px; color: var(--text-3); margin-bottom: 8px; display: flex; align-items: center; gap: 8px; }
.p-tags { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; }
.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 16px; align-items: start; }
.row-item { display: flex; align-items: center; gap: 10px; padding: 9px 6px; border-bottom: 1px solid #f2f4f8; cursor: pointer; }
.row-item:hover { background: var(--primary-bg); }
.i-title { flex: 1; font-size: 13px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.i-meta { font-size: 11px; color: var(--text-3); }
.notify-item { padding: 8px 6px; border-bottom: 1px dashed #f2f4f8; font-size: 13px; display: flex; align-items: center; gap: 6px; }
.sg-row { cursor: default; }
.empty-state a { color: var(--primary); cursor: pointer; }
.notify-item.unread { color: var(--danger); font-weight: 600; }
.n-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--danger); }
.n-time { margin-left: auto; font-size: 11px; color: var(--text-3); font-weight: 400; }
@media (max-width: 800px) { .two-col { grid-template-columns: 1fr; } }
</style>

<script>
export default { name: 'UserCenterPage' }
</script>
