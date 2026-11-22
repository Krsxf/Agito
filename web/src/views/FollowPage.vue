<template>
  <!-- 追番管理（界面文档 4.7 节：一键确认条 + 双视图卡片列表/周历） -->
  <div class="follow-page">
    <div class="card confirm-bar">
      ⏱ 上次看到第几集？为《{{ confirmAnime?.title || '请选择番剧' }}》一键确认：
      <select v-model="confirmAnimeId" class="mini-select">
        <option v-for="f in list" :key="f.animeId" :value="f.animeId">{{ f.title }}</option>
      </select>
      <button class="btn btn-primary btn-mini" v-for="e in quickEpisodes" :key="e"
              :disabled="!confirmAnimeId" @click="confirm(e)">第 {{ e }} 集</button>
      <input v-model.number="manualEpisode" class="mini-input" placeholder="手动输入" type="number" min="0">
      <button class="btn btn-accent btn-mini" :disabled="!confirmAnimeId" @click="confirm(manualEpisode)">确认</button>
    </div>

    <p class="stat-line">当前追番 {{ stat.totalCount }} 部 · 本周更新 {{ stat.weekUpdate }} 部 · 数据更新至 {{ today }}</p>

    <div class="dual-view">
      <div class="card left-col">
        <div class="card-title"><span>📌 我的追番</span><span class="sub">进度条 + NEW 角标</span></div>
        <div v-if="!list.length" class="empty-state">还没有追番，去<a @click="$router.push('/home')">首页推荐</a>挑一部吧</div>
        <div v-for="f in list" :key="f.animeId" class="follow-item">
          <div class="f-main">
            <p class="f-title">
              <router-link :to="`/anime/${f.animeId}`" class="f-link">{{ f.title }}</router-link>
              <span v-if="f.isNew" class="tag-pill danger">NEW 更新</span>
            </p>
            <p class="f-meta">
              看到第 {{ f.currentEpisode }} / {{ f.totalEpisode || '?' }} 集
              <span v-if="f.nextEpisodeTime">· 下集 {{ f.nextEpisodeTime.slice(5, 10) }}</span>
            </p>
            <div class="progress"><div :style="{ width: f.progress + '%' }"></div></div>
          </div>
          <div class="f-ops">
            <button class="btn btn-accent btn-mini" @click="watchAnime(f)">继续观看</button>
            <button class="btn btn-ghost btn-mini" @click="unfollow(f)">取消追番</button>
          </div>
        </div>
      </div>

      <div class="card right-col">
        <div class="card-title"><span>📅 周视图追番日历</span></div>
        <calendar-week :animes="calendarData" @pick="id => $router.push(`/anime/${id}`)" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'
import { useUserStore } from '../stores/user'
import CalendarWeek from '../components/CalendarWeek.vue'

const store = useUserStore()
const router = useRouter()

const list = ref([])
const stat = ref({ totalCount: 0, weekUpdate: 0 })
const confirmAnimeId = ref(null)
const manualEpisode = ref(null)
const today = new Date().toISOString().slice(0, 10).replace(/-/g, '-')

const confirmAnime = computed(() => list.value.find(f => f.animeId === confirmAnimeId.value))
const quickEpisodes = computed(() => {
  const cur = confirmAnime.value?.currentEpisode || 0
  return [cur + 1, cur + 2].filter(e => e >= 1)
})

const calendarData = computed(() =>
  list.value.filter(f => f.followStatus === 1).map(f => ({ animeId: f.animeId, title: f.title, weekday: f.weekday })))

async function load() {
  if (!store.isLogin) { router.push('/login'); return }
  const res = await api.myFollows().catch(() => null)
  if (res?.data) {
    list.value = res.data.list || []
    stat.value = { totalCount: res.data.totalCount, weekUpdate: res.data.weekUpdate }
    if (!confirmAnimeId.value && list.value.length) confirmAnimeId.value = list.value[0].animeId
  }
}

async function confirm(episode) {
  if (episode == null || episode < 0) return ElMessage.warning('请输入有效集数')
  await api.confirmProgress(confirmAnimeId.value, episode).catch(() => null)
  ElMessage.success('已记录：看到第 ' + episode + ' 集')
  load()
}

async function watchAnime(f) {
  const res = await api.animeDetail(f.animeId).catch(() => null)
  const p = res?.data?.platforms?.find(x => x.linkStatus !== 0)
  if (p) {
    const jr = await api.jump(f.animeId, p.platformName).catch(() => null)
    if (jr?.data?.url) {
      window.open(jr.data.url, '_blank', 'noopener')
      const next = (f.currentEpisode || 0) + 1
      ElMessage.info(`建议从第 ${next} 集继续观看`)
    }
  } else {
    ElMessage.warning('暂无可用观看平台')
  }
}

async function unfollow(f) {
  try {
    await ElMessageBox.confirm(`确定取消追番《${f.title}》吗？`, '二次确认', { type: 'warning' })
    await api.unfollow(f.animeId)
    ElMessage.success('已取消追番')
    load()
  } catch { /* 用户取消 */ }
}

onMounted(load)
</script>

<style scoped>
.confirm-bar { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; background: #fff8ef; border: 1px dashed #f3c882; font-size: 13px; }
.confirm-bar .btn { background: #fff; }
.mini-select { height: 28px; border: 1px solid #e3e6f0; border-radius: var(--radius-ctl); padding: 0 6px; }
.mini-input { width: 90px; height: 28px; border: 1px solid #e3e6f0; border-radius: var(--radius-ctl); padding: 0 8px; }
.stat-line { margin: 12px 2px; font-size: 12px; color: var(--text-3); }
.dual-view { display: grid; grid-template-columns: 1.4fr 1fr; gap: 16px; align-items: start; }
.follow-item { display: flex; align-items: center; gap: 14px; padding: 12px 0; border-bottom: 1px solid #f2f4f8; }
.follow-item:last-child { border-bottom: none; }
.f-main { flex: 1; min-width: 0; }
.f-title { font-weight: 600; margin-bottom: 4px; display: flex; align-items: center; gap: 8px; }
.f-link { color: var(--text-1); }
.f-link:hover { color: var(--primary); }
.f-meta { font-size: 12px; color: var(--text-3); margin-bottom: 6px; }
.f-ops { display: flex; gap: 8px; }
.empty-state a { color: var(--primary); cursor: pointer; }
@media (max-width: 900px) { .dual-view { grid-template-columns: 1fr; } }
</style>

<script>
export default { name: 'FollowPage' }
</script>
