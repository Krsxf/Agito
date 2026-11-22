<template>
  <!-- 排行榜（界面文档 4.9 节：三榜 Tab + 周月总 + 指标说明卡） -->
  <div class="rank-page">
    <div class="card">
      <div class="card-title">
        <span>🏆 排行榜 <span class="sub">榜单每小时缓存更新</span></span>
        <span class="period-switch">
          <span class="pill" :class="{ on: period === 1 }" @click="period = 1; load()">周榜</span>
          <span class="pill" :class="{ on: period === 2 }" @click="period = 2; load()">月榜</span>
          <span class="pill" :class="{ on: period === 3 }" @click="period = 3; load()">总榜</span>
        </span>
      </div>

      <div class="tab-row">
        <button v-for="t in tabs" :key="t.v" :class="{ on: rankType === t.v }" @click="rankType = t.v; load()">{{ t.name }}</button>
      </div>

      <div v-if="!list.length" class="empty-state">该时间范围内暂无数据</div>
      <div v-for="item in shownList" :key="item.id" class="rank-item" @click="$router.push(`/anime/${item.id}`)">
        <span class="medal" :class="['r' + item.rankNo, { plain: item.rankNo > 3 }]">{{ item.rankNo }}</span>
        <div class="r-cover" :style="coverOf(item.id)"></div>
        <div class="r-main">
          <p class="r-title">{{ item.title }}</p>
          <p class="r-meta">{{ item.year }} · {{ item.episodeCount || '?' }}集 · 追番 {{ fmt(item.followCount) }}</p>
        </div>
        <div class="r-score">
          <p class="num">{{ item.score }}</p>
          <p class="label">{{ rankType === 3 ? '评分' : '热度' }}</p>
        </div>
        <button class="btn btn-primary btn-mini" @click.stop="follow(item.id)">追番</button>
      </div>
      <p v-if="list.length > 8" class="more-tip" @click="expanded = !expanded">
        {{ expanded ? '收起' : `展开全部 Top${list.length}（共 ${list.length} 条）` }}
      </p>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'

const store = useUserStore()
const router = useRouter()

const tabs = [
  { v: 1, name: '综合热度榜' },
  { v: 2, name: '新番榜' },
  { v: 3, name: '经典高分榜' }
]
const rankType = ref(1)
const period = ref(3)
const list = ref([])
const expanded = ref(false)

const shownList = ref([])
watch(list, l => { shownList.value = expanded.value ? l : l.slice(0, 8) }, { immediate: true })
watch(expanded, e => { shownList.value = e ? list.value : list.value.slice(0, 8) })

const palettes = [['#6C5CE7', '#FD79A8'], ['#FDCB6E', '#e84393'], ['#74B9FF', '#6C5CE7'], ['#00B894', '#FDCB6E']]
const coverOf = id => ({ background: `linear-gradient(135deg, ${palettes[(id || 0) % 4][0]}, ${palettes[(id || 0) % 4][1]})` })
const fmt = n => n == null ? '-' : n >= 1e4 ? (n / 1e4).toFixed(1) + '万' : String(n)

async function load() {
  const res = await api.rankList(rankType.value, period.value).catch(() => null)
  if (res?.data) list.value = res.data.list || []
  shownList.value = list.value.slice(0, 8)
}

async function follow(id) {
  if (!store.isLogin) { ElMessage.warning('写操作请先登录'); return router.push('/login') }
  await api.follow(id).catch(() => null)
  ElMessage.success('已追番')
}

onMounted(load)
</script>

<style scoped>
.period-switch { display: flex; gap: 6px; }
.tab-row { display: flex; gap: 8px; margin-bottom: 16px; }
.tab-row button { flex: 1; height: 40px; border: none; border-radius: var(--radius-ctl); background: var(--bg); color: var(--text-2); font-size: 14px; transition: all .15s; }
.tab-row button.on { background: var(--primary); color: #fff; font-weight: 700; box-shadow: 0 4px 12px rgba(108,92,231,.35); }
.rank-item { display: flex; align-items: center; gap: 14px; padding: 12px 8px; border-bottom: 1px solid #f2f4f8; cursor: pointer; border-radius: var(--radius-ctl); }
.rank-item:hover { background: var(--primary-bg); }
.medal { width: 28px; height: 28px; border-radius: 8px; display: inline-flex; align-items: center; justify-content: center; color: #fff; font-weight: 800; font-size: 13px; flex-shrink: 0; }
.medal.r1 { background: linear-gradient(135deg, #FFD700, #e8a200); }
.medal.r2 { background: linear-gradient(135deg, #C0C0C0, #909399); }
.medal.r3 { background: linear-gradient(135deg, #cd7f32, #a0522d); }
.medal.plain { background: #eef0f6; color: var(--text-3); }
.r-cover { width: 44px; height: 58px; border-radius: var(--radius-ctl); flex-shrink: 0; }
.r-main { flex: 1; min-width: 0; }
.r-title { font-weight: 600; margin-bottom: 2px; }
.r-meta { font-size: 11px; color: var(--text-3); }
.r-score { text-align: center; min-width: 64px; }
.r-score .num { font-size: 18px; font-weight: 800; color: #e67e22; }
.r-score .label { font-size: 10px; color: var(--text-3); }
.more-tip { text-align: center; padding: 12px; color: var(--primary); font-size: 13px; cursor: pointer; }
</style>

<script>
export default { name: 'RankPage' }
</script>
