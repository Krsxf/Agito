<template>
  <!-- 补番助手（界面文档 4.8 节：进度横幅 + 多条件筛选 + 拖拽清单 + 运营位） -->
  <div class="catchup-page">
    <div class="banner">
      <div class="banner-text">
        <p class="b-title">补番进度</p>
        <p class="b-main">已补 <b>{{ banner.done }}</b> / {{ banner.total }} 部 · <b>{{ banner.percent }}%</b></p>
        <p class="b-sub">累计补番约 {{ Math.round(banner.minutes / 60) }} 小时 · 弃番 {{ banner.dropped }} 部（不计入进度）</p>
      </div>
      <div class="banner-progress"><div :style="{ width: banner.percent + '%' }"></div></div>
      <button class="btn b-btn" @click="goNext">去补下一部 →</button>
    </div>

    <div class="card">
      <div class="card-title"><span>🔎 补番推荐 <span class="sub">自动排除你已看过的作品</span></span></div>
      <div class="filter-bar">
        <span class="filter-label">年份：</span>
        <span class="pill" :class="{ on: !query.yearFrom }" @click="reset('year')">全部</span>
        <span class="pill" :class="{ on: query.yearFrom === 2010 }" @click="setYear(2010)">2010 后</span>
        <span class="pill" :class="{ on: query.yearFrom === 2000 }" @click="setYear(2000)">2000 后</span>
        <span class="pill" :class="{ on: query.yearFrom === 1990 }" @click="setYear(1990)">经典老番</span>
        <span class="filter-label">类型：</span>
        <span class="pill" :class="{ on: !query.tag }" @click="reset('tag')">全部</span>
        <span v-for="t in topTags" :key="t" class="pill" :class="{ on: query.tag === t }" @click="query.tag = t; load()">{{ t }}</span>
      </div>
      <div class="filter-bar">
        <span class="filter-label">评分 ≥</span>
        <span v-for="s in [8, 8.5, 9, 9.5]" :key="s" class="pill" :class="{ on: query.scoreFrom === s }" @click="query.scoreFrom = s; load()">{{ s }}</span>
        <span class="filter-label">集数 ≤</span>
        <span v-for="e in [12, 24, 50, 100]" :key="e" class="pill" :class="{ on: query.episodeTo === e }" @click="query.episodeTo = e; load()">{{ e }}</span>
      </div>
      <div class="anime-grid">
        <div v-for="a in candidates" :key="a.id" class="anime-card card" @click="$router.push(`/anime/${a.id}`)">
          <div class="mini-cover" :style="coverOf(a.id)"></div>
          <div class="c-info">
            <p class="c-title">{{ a.title }}</p>
            <p class="c-meta"><span class="tag-pill gold">★{{ a.totalScore }}</span><span class="tag-pill info">{{ a.year }}</span><span v-if="a.episodeCount" class="tag-pill">{{ a.episodeCount }}集</span></p>
            <button class="btn btn-primary btn-mini" @click.stop="add(a.id)">＋ 加入清单</button>
          </div>
        </div>
      </div>
    </div>

    <div class="two-col">
      <div class="card">
        <div class="card-title"><span>📋 我的补番清单</span><span class="sub">拖拽排序调整优先级（↑↓）</span></div>
        <div v-if="!mine.length" class="empty-state">清单为空，从上方推荐挑几部吧</div>
        <div v-for="(m, i) in mine" :key="m.animeId" class="list-item" draggable="true"
             @dragstart="dragIndex = i" @dragover.prevent @drop="drop(i)">
          <span class="order">{{ i + 1 }}</span>
          <div class="m-main">
            <p class="m-title">
              <router-link :to="`/anime/${m.animeId}`">{{ m.title }}</router-link>
              <span v-if="m.status === 2" class="tag-pill success">已完成</span>
              <span v-else-if="m.status === 3" class="tag-pill danger">已弃番</span>
            </p>
            <p class="m-meta">★{{ m.score }} · {{ m.year }} · {{ m.episodeCount || '?' }}集</p>
          </div>
          <div class="m-ops">
            <button class="btn btn-accent btn-mini" @click="watch(m)">去观看</button>
            <button class="btn btn-primary btn-mini" v-if="m.status !== 2" @click="done(m, false)">完成</button>
            <button class="btn btn-ghost btn-mini" v-if="m.status === 1" @click="done(m, true)">弃番</button>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-title"><span>🏆 高分补番榜</span></div>
        <div v-for="b in topBoards" :key="b.name" class="board-item">
          <p class="b-name">{{ b.name }}</p>
          <p class="b-list" v-for="a in b.items" :key="a.id">
            <a @click="$router.push(`/anime/${a.id}`)">{{ a.title }}</a> <span class="score-num">★{{ a.score }}</span>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { useUserStore } from '../stores/user'

const store = useUserStore()
const router = useRouter()

const banner = ref({ done: 0, total: 0, percent: 0, minutes: 0, dropped: 0 })
const mine = ref([])
const candidates = ref([])
const topBoards = ref([])
const dragIndex = ref(null)
const topTags = ['热血', '奇幻', '悬疑', '治愈', '恋爱', '科幻']

const query = ref({ yearFrom: null, tag: '', scoreFrom: null, episodeTo: null, pageNum: 1, pageSize: 10, airStatus: 2, sortBy: 'score' })

const palettes = [['#6C5CE7', '#FD79A8'], ['#74B9FF', '#6C5CE7'], ['#00B894', '#74B9FF'], ['#FDCB6E', '#FD79A8']]
const coverOf = id => ({ background: `linear-gradient(135deg, ${palettes[(id || 0) % 4][0]}, ${palettes[(id || 0) % 4][1]})` })

const nextPending = computed(() => mine.value.find(m => m.status === 1))

async function load() {
  if (!store.isLogin) { router.push('/login'); return }
  const [mc, cl, r3] = await Promise.all([
    api.myCatchup().catch(() => null),
    api.animeList({ ...query.value, tag: query.value.tag || undefined }).catch(() => null),
    api.rankList(3, 3).catch(() => null)
  ])
  if (mc?.data) {
    mine.value = mc.data.list || []
    banner.value = mc.data.banner || banner.value
  }
  if (cl?.data) candidates.value = (cl.data.list || []).filter(a => !mine.value.some(m => m.animeId === a.id))
  if (r3?.data) {
    topBoards.value = [
      { name: '2026 评分 TOP', items: r3.data.list.slice(0, 3).map(a => ({ id: a.id, title: a.title, score: a.score })) },
      { name: '豆瓣 9.0+ 神作', items: r3.data.list.filter(a => a.score >= 9.3).slice(0, 3).map(a => ({ id: a.id, title: a.title, score: a.score })) },
      { name: '入坑友好单季番', items: (cl.data?.list || []).filter(a => a.episodeCount <= 26).slice(0, 3).map(a => ({ id: a.id, title: a.title, score: a.totalScore })) }
    ]
  }
}

const reset = k => { if (k === 'year') query.value.yearFrom = null; if (k === 'tag') query.value.tag = ''; load() }
const setYear = y => { query.value.yearFrom = query.value.yearFrom === y ? null : y; load() }

async function add(id) {
  await api.addCatchup(id).catch(() => null)
  ElMessage.success('已加入补番清单')
  load()
}

async function done(m, dropped) {
  if (dropped) {
    await api.catchupDone(m.animeId, true)
    ElMessage.info('已标记弃番（不计入进度）')
  } else {
    await api.catchupDone(m.animeId, false)
    ElMessage.success('补番完成！可以去评分了')
  }
  load()
}

async function watch(m) {
  const res = await api.animeDetail(m.animeId).catch(() => null)
  const p = res?.data?.platforms?.find(x => x.linkStatus !== 0)
  if (p) {
    const jr = await api.jump(m.animeId, p.platformName).catch(() => null)
    if (jr?.data?.url) window.open(jr.data.url, '_blank', 'noopener')
  } else ElMessage.warning('暂无可用观看平台')
}

function goNext() {
  if (nextPending.value) router.push(`/anime/${nextPending.value.animeId}`)
  else ElMessage.info('清单已全部完成，从推荐区挑下一部吧')
}

async function drop(i) {
  if (dragIndex.value === null || dragIndex.value === i) return
  const arr = [...mine.value]
  const [moved] = arr.splice(dragIndex.value, 1)
  arr.splice(i, 0, moved)
  mine.value = arr
  await api.reorderCatchup(arr.map(m => m.animeId)).catch(() => null)
  ElMessage.success('优先级已更新')
}

onMounted(load)
</script>

<style scoped>
.banner { display: flex; align-items: center; gap: 20px; padding: 20px 24px; margin-bottom: 16px; border-radius: var(--radius-card); background: linear-gradient(135deg, rgba(108,92,231,.12), rgba(253,123,168,.12)); box-shadow: var(--card-shadow); }
.banner-text { flex: 1; }
.b-title { font-size: 12px; color: var(--text-3); }
.b-main { font-size: 20px; font-weight: 800; color: var(--primary); margin: 4px 0; }
.b-main b { color: var(--accent); }
.b-sub { font-size: 12px; color: var(--text-3); }
.banner-progress { width: 200px; }
.banner-progress, .banner-progress > div { height: 12px; border-radius: 6px; }
.banner-progress { background: rgba(255,255,255,.6); overflow: hidden; }
.b-btn { height: 40px; padding: 0 22px; background: linear-gradient(135deg, var(--primary), var(--accent)); color: #fff; border: none; border-radius: 20px; font-size: 14px; font-weight: 600; }
.anime-card { padding: 0; overflow: hidden; cursor: pointer; display: flex; flex-direction: column; }
.mini-cover { height: 110px; }
.c-info { padding: 10px 12px; }
.c-title { font-size: 13px; font-weight: 600; margin-bottom: 6px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.c-meta { display: flex; gap: 6px; margin-bottom: 8px; }
.two-col { display: grid; grid-template-columns: 1.3fr 1fr; gap: 16px; align-items: start; margin-top: 16px; }
.list-item { display: flex; align-items: center; gap: 12px; padding: 10px 8px; border-bottom: 1px solid #f2f4f8; cursor: grab; }
.list-item:hover { background: var(--primary-bg); }
.order { width: 22px; height: 22px; border-radius: 50%; background: var(--primary-bg); color: var(--primary); font-size: 12px; display: inline-flex; align-items: center; justify-content: center; font-weight: 700; }
.m-main { flex: 1; min-width: 0; }
.m-title { font-size: 13px; font-weight: 600; display: flex; gap: 6px; align-items: center; }
.m-title a { color: var(--text-1); }
.m-title a:hover { color: var(--primary); }
.m-meta { font-size: 11px; color: var(--text-3); margin-top: 2px; }
.m-ops { display: flex; gap: 6px; }
.board-item { padding: 10px 0; border-bottom: 1px dashed #eef0f6; }
.b-name { font-size: 13px; font-weight: 700; color: var(--primary); margin-bottom: 6px; }
.b-list { font-size: 12px; padding: 3px 0; }
.b-list a { cursor: pointer; }
.b-list a:hover { color: var(--primary); }
@media (max-width: 900px) { .two-col { grid-template-columns: 1fr; } .banner { flex-wrap: wrap; } }
</style>

<script>
export default { name: 'CatchupPage' }
</script>
