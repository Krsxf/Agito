<template>
  <!-- 番剧详情（界面文档 4.6 节：英雄区 + 四联评分卡 + 属性网格 + 趋势 + 操作行） -->
  <div v-if="d" class="detail-page">
    <div class="card hero">
      <div class="cover" :style="coverStyle">
        <img v-if="d.coverUrl" :src="d.coverUrl" :alt="d.title" class="cover-img">
        <span class="cover-title">{{ d.title }}</span>
      </div>
      <div class="hero-info">
        <h1>{{ d.title }} <span class="tag-pill gold" v-if="d.airStatus === 1">连载中</span><span class="tag-pill info" v-else-if="d.airStatus === 2">已完结</span><span class="tag-pill success" v-else>待上线</span></h1>
        <p class="alias" v-if="d.alias">{{ d.alias }}</p>
        <div class="tag-row">
          <span v-for="t in d.tags" :key="t" class="tag-pill">{{ t }}</span>
        </div>

        <div class="score-board">
          <div class="score-main">
            <p class="label">综合加权</p>
            <p class="num">{{ d.totalScore || '-' }}</p>
            <p class="people">{{ d.ratingCount }} 人评分</p>
          </div>
          <div v-for="s in d.scoreBoard?.filter(x => x.sourceName !== '综合加权')" :key="s.sourceName" class="score-src">
            <p class="label">{{ s.sourceName }}</p>
            <p class="num">{{ s.score }}</p>
            <p class="people">{{ s.ratingCount }} 人</p>
          </div>
        </div>

        <div class="attr-grid">
          <div><label>集数</label><span>{{ d.episodeCount || '-' }}</span></div>
          <div><label>单集时长</label><span>{{ d.duration || '-' }}</span></div>
          <div><label>制作公司</label><span>{{ d.studio || '-' }}</span></div>
          <div><label>导演</label><span>{{ d.director || '-' }}</span></div>
          <div><label>主要声优</label><span>{{ d.voiceActor || '-' }}</span></div>
          <div><label>首播</label><span>{{ d.year }} · {{ d.region }}</span></div>
          <div><label>播放量</label><span>{{ fmtNum(d.playCount) }}</span></div>
          <div><label>追番人数</label><span>{{ fmtNum(d.followCount) }}</span></div>
        </div>

        <p class="desc">{{ d.description }}</p>

        <p class="section-label">▶ 可观看平台（按推荐度排序，跳转前自动校验链接）</p>
        <platform-jump :anime-id="d.id" :platforms="d.platforms" @feedback="feedback" />

        <div class="action-row">
          <rating-star v-if="store.isLogin" @rated="rate" class="rate-box" />
          <button class="btn btn-primary" @click="doFollow">{{ followed ? '✓ 追番中' : '+ 加入追番' }}</button>
          <button class="btn btn-ghost" @click="addCatchup">＋ 加入补番清单</button>
          <button class="btn btn-ghost" @click="doFavorite">{{ faved ? '★ 已收藏' : '☆ 收藏' }}</button>
        </div>
        <p v-if="!store.isLogin" class="login-tip">游客可自由浏览 · 评分/收藏/追番等写操作请先 <router-link to="/login">登录</router-link></p>
      </div>
    </div>

    <div class="card">
      <div class="card-title"><span>📈 热度趋势 <span class="sub">近 8 周播放量（演示数据）</span></span></div>
      <div class="trend-chart">
        <div v-for="(v, i) in trend" :key="i" class="bar" :style="{ height: v + '%' }"><span>{{ v }}</span></div>
      </div>
    </div>

    <div class="card">
      <div class="card-title"><span>🎯 同类型推荐</span></div>
      <div class="anime-grid">
        <anime-card v-for="a in similar" :key="a.id" :anime="a" @click="go" @jump="go" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { useUserStore } from '../stores/user'
import AnimeCard from '../components/AnimeCard.vue'
import RatingStar from '../components/RatingStar.vue'
import PlatformJump from '../components/PlatformJump.vue'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const d = ref(null)
const followed = ref(false)
const faved = ref(false)
const similar = ref([])

const coverStyle = computed(() => {
  const p = [['#6C5CE7', '#FD79A8'], ['#74B9FF', '#6C5CE7']][(d.value?.id || 0) % 2]
  return { background: `linear-gradient(135deg, ${p[0]}, ${p[1]})` }
})

// 近 8 周热度演示趋势
const trend = computed(() => {
  const base = Math.min(98, Math.round((d.value?.heatScore || 500) / 10))
  return Array.from({ length: 8 }, (_, i) => Math.max(12, Math.min(98, base + Math.round(Math.sin(i * 1.2) * 18))))
})

const go = id => router.push(`/anime/${id}`)
const fmtNum = n => n == null ? '-' : n >= 1e8 ? (n / 1e8).toFixed(1) + '亿' : n >= 1e4 ? (n / 1e4).toFixed(0) + '万' : String(n)

async function load() {
  const res = await api.animeDetail(route.params.id).catch(() => null)
  if (res?.data) d.value = res.data
  if (store.isLogin) {
    const [fl, fv, sm] = await Promise.all([
      api.myFollows().catch(() => null),
      api.myFavorites().catch(() => null),
      api.animeList({ tag: d.value?.tags?.[0], pageSize: 6 }).catch(() => null)
    ])
    followed.value = !!fl?.data?.list?.some(f => f.animeId === d.value.id)
    faved.value = !!fv?.data?.list?.some(f => f.animeId === d.value.id)
    similar.value = (sm?.data?.list || []).filter(a => a.id !== d.value.id).slice(0, 5)
  } else if (d.value?.tags?.length) {
    const sm = await api.animeList({ tag: d.value.tags[0], pageSize: 6 }).catch(() => null)
    similar.value = (sm?.data?.list || []).filter(a => a.id !== d.value.id).slice(0, 5)
  }
}

async function rate(score) {
  await api.rate(d.value.id, score).catch(() => null)
  ElMessage.success('评分成功，推荐将更新')
}

async function doFollow() {
  if (!store.isLogin) return router.push('/login')
  await api.follow(d.value.id).catch(() => null)
  followed.value = true
  ElMessage.success('已加入追番，可在追番管理维护进度')
}

async function addCatchup() {
  if (!store.isLogin) return router.push('/login')
  await api.addCatchup(d.value.id).catch(() => null)
  ElMessage.success('已加入补番清单')
}

async function doFavorite() {
  if (!store.isLogin) return router.push('/login')
  faved.value ? await api.favorite(d.value.id, false) : await api.favorite(d.value.id, true)
  faved.value = !faved.value
}

function feedback() { ElMessage.info('已反馈管理员，将尽快修复链接') }

onMounted(load)
watch(() => route.params.id, () => { if (route.name === 'detail') load() })
</script>

<style scoped>
.hero { display: flex; gap: 22px; }
.cover { width: 230px; height: 308px; flex-shrink: 0; border-radius: var(--radius-card); display: flex; align-items: flex-end; padding: 16px; color: #fff; position: relative; overflow: hidden; }
.cover-img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.cover::after { content: ''; position: absolute; inset: 0; background: linear-gradient(180deg, transparent 35%, rgba(0,0,0,.55)); }
.cover-title { position: relative; z-index: 1; font-size: 18px; font-weight: 700; text-shadow: 0 2px 10px rgba(0,0,0,.4); }
.hero-info { flex: 1; min-width: 0; }
.hero-info h1 { font-size: 22px; display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.alias { color: var(--text-3); font-size: 12px; margin-bottom: 10px; }
.tag-row { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 16px; }
.score-board { display: grid; grid-template-columns: 1.4fr repeat(3, 1fr); gap: 10px; margin-bottom: 16px; }
.score-main { background: linear-gradient(135deg, rgba(108,92,231,.08), rgba(253,123,168,.08)); border: 1px solid rgba(108,92,231,.25); }
.score-main, .score-src { border-radius: var(--radius-ctl); padding: 10px 12px; text-align: center; }
.score-src { background: #f7f8fc; }
.score-board .label { font-size: 11px; color: var(--text-3); margin-bottom: 4px; }
.score-board .num { font-size: 22px; font-weight: 800; color: #e67e22; }
.score-board .people { font-size: 10px; color: var(--text-3); }
.attr-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px 16px; margin-bottom: 14px; }
.attr-grid div { font-size: 12px; }
.attr-grid label { color: var(--text-3); margin-right: 8px; }
.desc { font-size: 13px; line-height: 1.8; color: var(--text-2); margin-bottom: 18px; }
.section-label { font-size: 13px; font-weight: 600; margin-bottom: 10px; }
.action-row { display: flex; align-items: center; gap: 12px; margin-top: 18px; flex-wrap: wrap; }
.rate-box { margin-right: 8px; }
.login-tip { margin-top: 12px; font-size: 12px; color: var(--text-3); }
.login-tip a { color: var(--primary); }
.trend-chart { display: flex; align-items: flex-end; gap: 10px; height: 160px; padding: 0 6px; }
.bar { flex: 1; background: linear-gradient(180deg, var(--primary-light), var(--primary)); border-radius: 6px 6px 0 0; position: relative; transition: height .3s; }
.bar span { position: absolute; top: -20px; left: 50%; transform: translateX(-50%); font-size: 10px; color: var(--text-3); }
</style>

<script>
export default { name: 'AnimeDetailPage' }
</script>
