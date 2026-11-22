<template>
  <!-- 公共组件：搜索联想框（4.2.3.2 节：输入联想 + 热门词兜底） -->
  <div class="search-bar">
    <input v-model="keyword" :placeholder="placeholder" @input="onInput" @keyup.enter="doSearch"
           @focus="showPanel = true" @blur="hideLater" />
    <button class="search-btn" @click="doSearch">搜索</button>
    <div v-if="showPanel && (suggestions.length || hot.length)" class="panel">
      <template v-if="keyword">
        <p class="panel-tip">联想结果</p>
        <a v-for="(s, i) in suggestions" :key="'s' + i" class="panel-item" @mousedown.prevent="pick(s)">
          {{ s.title }}<span class="tag-pill gold" style="margin-left:6px">★{{ s.score }}</span>
        </a>
      </template>
      <template v-else-if="hot.length">
        <p class="panel-tip">热门搜索</p>
        <a v-for="(h, i) in hot" :key="'h' + i" class="panel-item hot" @mousedown.prevent="goHot(h)">
          {{ h.title }}
        </a>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../api'

const props = defineProps({ placeholder: { type: String, default: '搜索番剧，支持拼音首字母，如 fls → 芙莉莲' } })
const emit = defineEmits(['select'])

const keyword = ref('')
const showPanel = ref(false)
const suggestions = ref([])
const hot = ref([])
let timer = null

onMounted(async () => {
  const res = await api.hotWords().catch(() => null)
  if (res?.data) hot.value = res.data.map(t => ({ title: t }))
})

function onInput() {
  clearTimeout(timer)
  timer = setTimeout(async () => {
    if (!keyword.value.trim()) { suggestions.value = []; return }
    const res = await api.suggest(keyword.value.trim()).catch(() => null)
    if (res?.data) {
      suggestions.value = res.data.map(t => {
        const found = hot.value.find(h => h.title === t)
        return { title: t, score: found?.score }
      }).filter(s => !s.score).length ? res.data.map(t => ({ title: t })) : res.data.map(t => ({ title: t }))
      // 补充分数来自联想详情不可得时留空，仅展示标题
      suggestions.value = res.data.map(t => ({ title: t }))
    }
    showPanel.value = true
  }, 300)
}

function pick(s) {
  keyword.value = ''
  suggestions.value = []
  showPanel.value = false
  // 联想返回标题，先查 id
  api.animeList({ keyword: s.title, pageSize: 1 }).then(res => {
    if (res?.data?.list?.length) emit('select', res.data.list[0].id)
  })
}

function goHot(h) {
  showPanel.value = false
  api.animeList({ keyword: h.title, pageSize: 1 }).then(res => {
    if (res?.data?.list?.length) emit('select', res.data.list[0].id)
  })
}

function doSearch() {
  if (keyword.value.trim()) emit('search', keyword.value.trim())
  showPanel.value = false
}

function hideLater() {
  setTimeout(() => { showPanel.value = false }, 200)
}
</script>

<style scoped>
.search-bar { position: relative; display: flex; width: 100%; }
.search-bar input { flex: 1; height: 36px; padding: 0 14px; border: 1px solid #e3e6f0; border-radius: 18px 0 0 18px; outline: none; font-size: 13px; background: #fff; transition: border-color .15s; }
.search-bar input:focus { border-color: var(--primary); }
.search-btn { width: 64px; height: 36px; border: none; border-radius: 0 18px 18px 0; background: var(--primary); color: #fff; font-size: 13px; }
.search-btn:hover { background: #5a4bd1; }
.panel { position: absolute; top: 42px; left: 0; right: 0; z-index: 50; background: #fff; border-radius: var(--radius-ctl); box-shadow: 0 8px 24px rgba(45,52,54,.12); padding: 8px 4px; }
.panel-tip { padding: 4px 12px; font-size: 11px; color: var(--text-3); }
.panel-item { display: flex; align-items: center; padding: 8px 12px; border-radius: var(--radius-ctl); font-size: 13px; cursor: pointer; }
.panel-item:hover { background: var(--primary-bg); }
</style>

<script>
export default { name: 'SearchBar' }
</script>
