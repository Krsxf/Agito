<template>
  <!-- 番剧中心（界面文档 4.5 节：多维筛选 + 拼音联想 + 网格分页） -->
  <div class="center-page">
    <div class="card">
      <div class="card-title">
        <span>🎬 番剧中心</span>
        <span class="title-right">
          <span class="sub">共 {{ total }} 部</span>
          <button class="btn btn-accent btn-mini" @click="openSuggest">＋ 求上架</button>
        </span>
      </div>

      <div class="search-row">
        <search-bar @select="go" @search="kw => { query.keyword = kw; load() }" />
      </div>

      <div class="filter-bar">
        <span class="filter-label">类型：</span>
        <span class="pill" :class="{ on: !query.tag }" @click="query.tag = ''; load()">全部</span>
        <span v-for="t in tags" :key="t" class="pill" :class="{ on: query.tag === t }" @click="setTag(t)">{{ t }}</span>
      </div>
      <div class="filter-bar">
        <span class="filter-label">年份：</span>
        <span class="pill" :class="{ on: !query.year }" @click="query.year = null; load()">全部</span>
        <span v-for="y in years" :key="y" class="pill" :class="{ on: query.year === y }" @click="query.year = y; load()">{{ y }}</span>
      </div>
      <div class="filter-bar">
        <span class="filter-label">地区：</span>
        <span class="pill" :class="{ on: !query.region }" @click="query.region = ''; load()">全部</span>
        <span v-for="r in regions" :key="r" class="pill" :class="{ on: query.region === r }" @click="query.region = r; load()">{{ r }}</span>
        <span class="filter-label" style="margin-left:16px">状态：</span>
        <span class="pill" :class="{ on: !query.airStatus }" @click="query.airStatus = null; load()">全部</span>
        <span class="pill" :class="{ on: query.airStatus === 1 }" @click="setStatus(1)">连载中</span>
        <span class="pill" :class="{ on: query.airStatus === 2 }" @click="setStatus(2)">已完结</span>
        <span class="pill" :class="{ on: query.airStatus === 3 }" @click="setStatus(3)">待上线</span>
      </div>

      <div v-if="loading" class="empty-state">加载中…</div>
      <div v-else-if="!list.length" class="empty-state">
        <p>🔍 没有找到相关番剧</p>
        <p class="hot-words">试试热门词：<a v-for="h in hot" :key="h" @click="query.keyword = h; load()">{{ h }}</a></p>
        <p class="hot-words">没找到想要的？<a @click="openSuggest">点这里求上架 →</a></p>
      </div>
      <div v-else class="anime-grid">
        <anime-card v-for="a in list" :key="a.id" :anime="a" @click="go" @jump="go" @follow="follow(a.id)" />
      </div>

      <div class="pagination">
        <button :disabled="query.pageNum <= 1" @click="query.pageNum--; load()">上一页</button>
        <button class="cur">{{ query.pageNum }} / {{ Math.max(1, Math.ceil(total / query.pageSize)) }}</button>
        <button :disabled="query.pageNum >= Math.ceil(total / query.pageSize)" @click="query.pageNum++; load()">下一页</button>
      </div>
    </div>

    <!-- 求上架弹窗：用户提交想上架的番剧 + 跳转链接，管理员审核后上架 -->
    <el-dialog v-model="suggestVisible" title="求上架 · 提交番剧补充建议" width="480px">
      <div class="field"><label>番剧名称 *</label><input v-model="sgForm.title" placeholder="想看但站内还没有的番剧"></div>
      <div class="field"><label>别名（可选）</label><input v-model="sgForm.alias" placeholder="日文原名 / 英文名"></div>
      <div class="field"><label>观看链接 *</label><input v-model="sgForm.platformUrl" placeholder="你知道的正版观看页面地址"></div>
      <div class="field"><label>平台名称（可选）</label><input v-model="sgForm.platformName" placeholder="如：哔哩哔哩"></div>
      <div class="field"><label>备注意见（可选）</label><textarea v-model="sgForm.note" rows="2" placeholder="想对管理员说的话"></textarea></div>
      <p class="sg-tip">💡 提交后进入管理员审核队列，审核通过后自动上架并挂载你提供的链接；管理员可能修改作品信息与链接</p>
      <template #footer>
        <button class="btn btn-ghost" @click="suggestVisible = false">取消</button>
        <button class="btn btn-accent" @click="submitSuggest">提交建议</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { useUserStore } from '../stores/user'
import AnimeCard from '../components/AnimeCard.vue'
import SearchBar from '../components/SearchBar.vue'

const store = useUserStore()
const router = useRouter()

const list = ref([])
const total = ref(0)
const loading = ref(false)
const hot = ref([])
const years = [2026, 2023, 2022, 2021, 2020, 2019, 2014, 2013, 2011, 2009, 2008, 2006, 2002, 1993]
const regions = ['日本', '中国']
const tags = ['热血', '恋爱', '搞笑', '科幻', '奇幻', '悬疑', '治愈', '日常', '运动', '机战', '音乐', '历史', '冒险', '战斗', '家庭', '喜剧', '剧情', '校园', '励志', '忍者', '幕末', '命运', '都市', '末世', '妖怪', '五行', '古风', '神鬼', '短篇', '国风', '玄幻', '修仙']

const query = ref({
  keyword: '', tag: '', year: null, region: '', airStatus: null,
  pageNum: 1, pageSize: 20, sortBy: 'heat'
})

const go = id => router.push(`/anime/${id}`)

// 求上架弹窗
const suggestVisible = ref(false)
const sgForm = ref({ title: '', alias: '', platformUrl: '', platformName: '', note: '' })

function openSuggest() {
  if (!store.isLogin) { ElMessage.warning('请先登录后再提交建议'); return router.push('/login') }
  suggestVisible.value = true
}

async function submitSuggest() {
  const f = sgForm.value
  if (!f.title.trim()) return ElMessage.warning('请填写番剧名称')
  if (!f.platformUrl.trim()) return ElMessage.warning('请填写观看链接')
  await api.suggest(f).catch(() => null)
  suggestVisible.value = false
  sgForm.value = { title: '', alias: '', platformUrl: '', platformName: '', note: '' }
}

async function load() {
  loading.value = true
  const res = await api.animeList(query.value).catch(() => null)
  if (res?.data) { list.value = res.data.list || []; total.value = res.data.total || 0 }
  loading.value = false
}

async function follow(id) {
  if (!store.isLogin) { ElMessage.warning('写操作请先登录'); return router.push('/login') }
  await api.follow(id).catch(() => null)
  ElMessage.success('已加入追番')
}

const setTag = t => { query.value.tag = query.value.tag === t ? '' : t; load() }
const setStatus = s => { query.value.airStatus = query.value.airStatus === s ? null : s; load() }

onMounted(async () => {
  load()
  const res = await api.hotWords().catch(() => null)
  if (res?.data) hot.value = res.data
})
</script>

<style scoped>
.search-row { max-width: 480px; margin-bottom: 6px; }
.hot-words a { color: var(--primary); cursor: pointer; }
.hot-words a:hover { text-decoration: underline; }
.title-right { display: flex; align-items: center; gap: 12px; }
.field { margin-bottom: 12px; }
.field label { display: block; margin-bottom: 5px; font-size: 13px; color: var(--text-2); }
.field input, .field textarea { width: 100%; padding: 8px 10px; border: 1px solid #e3e6f0; border-radius: var(--radius-ctl); outline: none; font-family: inherit; font-size: 13px; }
.field input:focus, .field textarea:focus { border-color: var(--primary); }
.field textarea { resize: vertical; }
.sg-tip { padding: 8px 10px; border-radius: var(--radius-ctl); background: #e8f3ff; color: #0984e3; font-size: 12px; line-height: 1.6; }
</style>

<script>
export default { name: 'AnimeCenterPage' }
</script>
