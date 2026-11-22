<template>
  <!-- 后台管理（界面文档 4.10 节：运营概览 + 番剧管理表格 + 审核 + 参数配置） -->
  <div class="admin-page">
    <div class="admin-mark">⚙ 管理端</div>

    <div class="overview">
      <div class="ov-card card"><p class="ov-num">{{ overview.animeCount }}</p><p class="ov-label">番剧总量</p></div>
      <div class="ov-card card"><p class="ov-num">{{ overview.userCount }}</p><p class="ov-label">注册用户</p></div>
      <div class="ov-card card"><p class="ov-num">{{ overview.todayJump }}</p><p class="ov-label">今日跳转</p></div>
      <div class="ov-card card warn"><p class="ov-num">{{ overview.invalidLinks }}</p><p class="ov-label">失效链接</p></div>
      <div class="ov-card card"><p class="ov-num">{{ overview.pendingReview }}</p><p class="ov-label">待审核</p></div>
    </div>

    <div class="card">
      <div class="card-title"><span>📚 番剧管理</span></div>
      <div class="filter-bar">
        <span class="filter-label">状态：</span>
        <span class="pill" :class="{ on: stateFilter === null }" @click="stateFilter = null; loadAnimes()">全部</span>
        <span class="pill" :class="{ on: stateFilter === 0 }" @click="stateFilter = 0; loadAnimes()">待审核</span>
        <span class="pill" :class="{ on: stateFilter === 1 }" @click="stateFilter = 1; loadAnimes()">已上线</span>
        <span class="pill" :class="{ on: stateFilter === 2 }" @click="stateFilter = 2; loadAnimes()">已下架</span>
        <input v-model="kw" class="kw-input" placeholder="番剧名称关键词" @keyup.enter="loadAnimes()">
        <button class="btn btn-primary btn-mini" @click="loadAnimes()">查询</button>
        <button class="btn btn-accent btn-mini" @click="openAnimeEdit(null)">＋ 新增番剧</button>
      </div>
      <table class="table">
        <thead><tr><th>ID</th><th>番剧名称</th><th>年份</th><th>评分</th><th>状态</th><th>观看平台</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="a in animes" :key="a.id">
            <td>{{ a.id }}</td>
            <td class="t-title">{{ a.title }}</td>
            <td>{{ a.year }}</td>
            <td><span class="tag-pill gold">★{{ a.totalScore || '-' }}</span></td>
            <td><span class="tag-pill" :class="stateClass(a.state)">{{ stateText(a.state) }}</span></td>
            <td class="t-platforms">
              {{ a.platforms }}
              <span v-if="a.invalidLinks > 0" class="invalid">（{{ a.invalidLinks }} 条失效）</span>
            </td>
            <td class="t-ops">
              <button class="btn btn-primary btn-mini" @click="openAnimeEdit(a)">编辑</button>
              <button v-if="a.state !== 1" class="btn btn-ghost btn-mini" @click="setState(a, 1)">上架</button>
              <button v-if="a.state === 1" class="btn btn-ghost btn-mini" @click="setState(a, 2)">下架</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="pagination">
        <button :disabled="pageNum <= 1" @click="pageNum--; loadAnimes()">上一页</button>
        <button class="cur">{{ pageNum }}</button>
        <button @click="pageNum++; loadAnimes()">下一页</button>
      </div>
    </div>

    <div class="two-col">
      <div class="card">
        <div class="card-title"><span>📋 人工审核队列 <span class="sub">用户提交 / 数据导入，审核时可修改信息与链接</span></span></div>
        <div v-if="!reviews.length" class="empty-state">队列为空</div>
        <div v-for="r in reviews" :key="r.id" class="review-item">
          <p class="r-src">
            <span class="tag-pill accent" v-if="r.sourceType === 'USER'">用户提交</span>
            <span class="tag-pill info" v-else>数据导入</span>
            <span v-if="r.submitter" class="r-by">提交人：{{ r.submitter.nickname || r.submitter.username }}</span>
          </p>
          <p class="r-payload">{{ payloadTitle(r.payload) }}</p>
          <p v-if="r.platformUrl" class="r-url">🔗 {{ r.platformName || '未填平台' }}：<a :href="r.platformUrl" target="_blank" rel="noopener" class="r-link">{{ r.platformUrl }}</a></p>
          <p v-if="r.note" class="r-note">💬 {{ r.note }}</p>
          <p v-if="r.reason && r.sourceType !== 'USER'" class="r-reason">⚠ {{ r.reason }}</p>
          <div class="r-ops">
            <button class="btn btn-primary btn-mini" @click="openEdit(r)">编辑信息</button>
            <button class="btn btn-accent btn-mini" @click="passDirect(r)">直接通过</button>
            <button class="btn btn-ghost btn-mini" @click="reject(r)">驳回</button>
          </div>
        </div>
      </div>

      <!-- 审核编辑弹窗：管理员可修改作品信息与跳转链接 -->
      <el-dialog v-model="editVisible" title="审核编辑 · 修改作品信息与链接后上架" width="480px">
        <div class="field"><label>番剧名称 *</label><input v-model="editForm.title"></div>
        <div class="field"><label>别名</label><input v-model="editForm.alias"></div>
        <div class="field-row">
          <div class="field"><label>年份</label><input v-model="editForm.year" type="number"></div>
          <div class="field"><label>地区</label><input v-model="editForm.region"></div>
        </div>
        <div class="field"><label>制作公司</label><input v-model="editForm.studio"></div>
        <div class="field"><label>简介</label><textarea v-model="editForm.description" rows="2"></textarea></div>
        <div class="field"><label>评分（0-10，可留空）</label><input v-model="editForm.totalScore" type="number" step="0.1" max="10"></div>
        <div class="field"><label>平台名称</label><input v-model="editForm.platformName" placeholder="如：哔哩哔哩"></div>
        <div class="field"><label>跳转链接（可修改）</label><input v-model="editForm.platformUrl"></div>
        <p class="sg-tip">⚠ 通过后将按此信息创建番剧条目（立即上线），链接将挂载为观看平台外链</p>
        <template #footer>
          <button class="btn btn-ghost" @click="editVisible = false">取消</button>
          <button class="btn btn-accent" @click="passEdited">修改并通过上架</button>
        </template>
      </el-dialog>

      <!-- 番剧编辑弹窗：管理员随时修改番剧信息 + 封面 + 观看平台链接 -->
      <el-dialog v-model="animeEditVisible" :title="animeEdit.id ? '编辑番剧 · ' + animeEdit.title : '新增番剧'" width="560px" top="5vh">
        <!-- 封面：上传存服务器，全站用户可见 -->
        <div class="cover-row">
          <div class="cover-preview" :class="{ empty: !animeEdit.coverUrl }">
            <img v-if="animeEdit.coverUrl" :src="animeEdit.coverUrl" alt="封面预览">
            <span v-else>暂无封面</span>
          </div>
          <div class="cover-ops">
            <label class="cover-label">封面图（≤2MB，png/jpg/webp/gif，存服务器全站可见）</label>
            <div class="cover-btns">
              <button class="btn btn-primary btn-mini" @click="pickCover">选择图片上传</button>
              <button class="btn btn-ghost btn-mini" v-if="animeEdit.coverUrl" @click="animeEdit.coverUrl = ''">清除封面</button>
            </div>
            <input v-model="animeEdit.coverUrl" class="cover-url-input" placeholder="或填写封面 URL（/uploads/… 或 http(s)://…）">
            <input type="file" ref="coverFileRef" accept="image/png,image/jpeg,image/webp,image/gif" style="display:none" @change="uploadCoverFile">
            <p v-if="coverUploading" class="cover-tip">上传中…</p>
          </div>
        </div>

        <div class="field"><label>番剧名称 *</label><input v-model="animeEdit.title"></div>
        <div class="field"><label>别名（逗号分隔，供检索）</label><input v-model="animeEdit.alias"></div>
        <div class="field-row">
          <div class="field"><label>首播年份</label><input v-model="animeEdit.year" type="number"></div>
          <div class="field"><label>地区</label>
            <select v-model="animeEdit.region" class="sel">
              <option value="">未设置</option><option>日本</option><option>中国</option><option>欧美</option>
            </select>
          </div>
        </div>
        <div class="field-row">
          <div class="field"><label>播出状态</label>
            <select v-model.number="animeEdit.airStatus" class="sel">
              <option :value="1">连载中</option><option :value="2">已完结</option><option :value="3">待上线</option>
            </select>
          </div>
          <div class="field"><label>数据状态</label>
            <select v-model.number="animeEdit.state" class="sel">
              <option :value="0">待审核</option><option :value="1">已上线</option><option :value="2">已下架</option>
            </select>
          </div>
        </div>
        <div class="field-row">
          <div class="field"><label>总集数</label><input v-model="animeEdit.episodeCount" type="number"></div>
          <div class="field"><label>单集时长</label><input v-model="animeEdit.duration" placeholder="如 24min"></div>
        </div>
        <div class="field-row">
          <div class="field"><label>制作公司</label><input v-model="animeEdit.studio"></div>
          <div class="field"><label>导演</label><input v-model="animeEdit.director"></div>
        </div>
        <div class="field"><label>主要声优（逗号分隔）</label><input v-model="animeEdit.voiceActor"></div>
        <div class="field"><label>简介</label><textarea v-model="animeEdit.description" rows="3"></textarea></div>
        <div class="field"><label>综合评分（0-10）</label><input v-model="animeEdit.totalScore" type="number" step="0.1" max="10"></div>

        <p class="pf-title">▶ 观看平台链接（可修改保存，失效标红）</p>
        <div v-for="p in platformsOfEdit" :key="p.id" class="pf-row" :class="{ invalid: p.linkStatus === 0 }">
          <input v-model="p.platformName" class="pf-name" placeholder="平台名">
          <input v-model="p.url" class="pf-url" placeholder="链接">
          <button class="btn btn-primary btn-mini" @click="savePlatform(p)">保存链接</button>
        </div>
        <div class="pf-row pf-add">
          <input v-model="newPlatform.name" class="pf-name" placeholder="新平台名">
          <input v-model="newPlatform.url" class="pf-url" placeholder="新链接 http(s)://…">
          <button class="btn btn-accent btn-mini" @click="addPlatformRow">＋ 添加</button>
        </div>
        <p v-if="!platformsOfEdit.length" class="pf-empty">暂无平台链接</p>

        <template #footer>
          <button class="btn btn-ghost" @click="animeEditVisible = false">取消</button>
          <button class="btn btn-primary" @click="saveAnimeEdit">保存番剧信息</button>
        </template>
      </el-dialog>

      <div class="card">
        <div class="card-title"><span>📥 Excel 批量导入 / 导出</span></div>
        <p class="io-tip">列：标题 | 别名 | 年份 | 地区 | 总集数 | 制作公司 | 简介 | 综合评分<br>年份缺失或评分异常的记录将进入人工审核队列</p>
        <input type="file" ref="fileRef" accept=".xlsx,.xls" class="file-input" @change="importFile">
        <div class="io-btns">
          <button class="btn btn-primary" @click="$refs.fileRef.click()">选择 Excel 导入</button>
          <button class="btn btn-ghost" @click="exportExcel">导出全部为 Excel</button>
        </div>
        <div v-if="importResult" class="import-result">
          导入完成：共 {{ importResult.total }} 条 · 直接入库 {{ importResult.success }} · 进审核队列 {{ importResult.review }} · 失败 {{ importResult.fail }}
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-title"><span>🎚 推荐参数配置 <span class="sub">保存后自下一次推荐计算任务执行时生效</span></span></div>
      <div v-for="f in factors" :key="f.key" class="factor-row">
        <label>{{ f.label }}</label>
        <input type="range" :min="0" :max="1" :step="0.05" v-model.number="cfg[f.key]">
        <span class="f-val">{{ cfg[f.key]?.toFixed(2) }}</span>
      </div>
      <p class="sum-tip">当前权重之和：{{ sum.toFixed(2) }}（须为 1.00）</p>
      <div class="cfg-ops">
        <button class="btn btn-primary" :disabled="Math.abs(sum - 1) > 0.01" @click="saveCfg">保存参数</button>
        <button class="btn btn-accent" @click="recalc">手动触发全量重算</button>
        <span v-if="cfg.lastRecalc" class="last-recalc">上次重算：{{ cfg.lastRecalc }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'
import { useUserStore } from '../stores/user'

const store = useUserStore()

const overview = ref({})
const animes = ref([])
const reviews = ref([])
const pageNum = ref(1)
const stateFilter = ref(null)
const kw = ref('')
const fileRef = ref()
const importResult = ref(null)

const cfg = reactive({ wProfile: 0.4, wCf: 0.3, wHot: 0.2, wRandom: 0.1, lastRecalc: null })
const factors = [
  { key: 'wProfile', label: '画像匹配' },
  { key: 'wCf', label: '协同过滤' },
  { key: 'wHot', label: '热门补偿' },
  { key: 'wRandom', label: '随机探索' }
]
const sum = computed(() => Number(cfg.wProfile) + Number(cfg.wCf) + Number(cfg.wHot) + Number(cfg.wRandom))

async function loadOverview() {
  const res = await api.adminOverview().catch(() => null)
  if (res?.data) overview.value = res.data
}

async function loadAnimes() {
  const res = await api.adminAnimes({
    pageNum: pageNum.value, pageSize: 10,
    state: stateFilter.value, keyword: kw.value || undefined
  }).catch(() => null)
  if (res?.data) animes.value = res.data.list || []
}

async function loadReviews() {
  const res = await api.reviewList().catch(() => null)
  if (res?.data) reviews.value = res.data.list || []
}

async function loadCfg() {
  const res = await api.getRecConfig().catch(() => null)
  if (res?.data) {
    cfg.wProfile = Number(res.data.wProfile) || 0.4
    cfg.wCf = Number(res.data.wCf) || 0.3
    cfg.wHot = Number(res.data.wHot) || 0.2
    cfg.wRandom = Number(res.data.wRandom) || 0.1
    cfg.lastRecalc = res.data.lastRecalc
  }
}

async function setState(a, state) {
  await api.changeState(a.id, state).catch(() => null)
  ElMessage.success(state === 1 ? '已上架' : '已下架')
  loadAnimes(); loadOverview()
}

async function passDirect(r) {
  await api.reviewPass(r.id).catch(() => null)
  ElMessage.success('审核通过，已发布上线')
  loadReviews(); loadOverview(); loadAnimes()
}

// 审核编辑弹窗：预填提交数据，管理员修改后通过
const editVisible = ref(false)
const editForm = ref({})
let editId = null

function openEdit(r) {
  editId = r.id
  const base = (() => { try { return JSON.parse(r.payload || '{}') } catch { return {} } })()
  editForm.value = {
    title: base.title || '',
    alias: base.alias || '',
    year: base.year || '',
    region: base.region || '',
    studio: base.studio || '',
    description: base.description || '',
    totalScore: '',
    platformName: r.platformName || '',
    platformUrl: r.platformUrl || ''
  }
  editVisible.value = true
}

async function passEdited() {
  const f = editForm.value
  if (!f.title?.trim()) return ElMessage.warning('番剧名称不能为空')
  await api.reviewPass(editId, {
    title: f.title, alias: f.alias, year: f.year || null, region: f.region,
    studio: f.studio, description: f.description, totalScore: f.totalScore || null,
    platformName: f.platformName, platformUrl: f.platformUrl
  }).catch(() => null)
  editVisible.value = false
  ElMessage.success('已按编辑内容上架')
  loadReviews(); loadOverview(); loadAnimes()
}

async function reject(r) {
  await api.reviewReject(r.id).catch(() => null)
  ElMessage.success('已驳回')
  loadReviews()
}

// ---------------- 番剧编辑（管理员随时修改） ----------------
const animeEditVisible = ref(false)
const animeEdit = ref({})
const platformsOfEdit = ref([])
const newPlatform = ref({ name: '', url: '' })

async function openAnimeEdit(a) {
  if (a) {
    // 编辑已有番剧：拉详情预填全字段（含封面与平台链接）
    const res = await api.animeDetail(a.id).catch(() => null)
    const d = res?.data
    if (d) {
      animeEdit.value = {
        id: d.id, title: d.title, alias: d.alias || '', year: d.year, region: d.region || '',
        coverUrl: d.coverUrl || '',
        airStatus: d.airStatus, state: a.state, episodeCount: d.episodeCount, duration: d.duration || '',
        studio: d.studio || '', director: d.director || '', voiceActor: d.voiceActor || '',
        description: d.description || '', totalScore: d.totalScore
      }
      platformsOfEdit.value = (d.platforms || []).map(p => ({ ...p, isPaid: p.isPaid, regionLimit: p.regionLimit }))
    }
  } else {
    // 新增番剧
    animeEdit.value = { title: '', alias: '', year: null, region: '日本', coverUrl: '', airStatus: 2, state: 0, episodeCount: null, duration: '24min', studio: '', director: '', voiceActor: '', description: '', totalScore: null }
    platformsOfEdit.value = []
  }
  newPlatform.value = { name: '', url: '' }
  animeEditVisible.value = true
}

// ---------------- 封面上传（存服务器，全站用户同一 URL 可见） ----------------
const coverFileRef = ref()
const coverUploading = ref(false)

function pickCover() { coverFileRef.value && coverFileRef.value.click() }

async function uploadCoverFile(e) {
  const file = e.target.files[0]
  e.target.value = ''
  if (!file) return
  if (file.size > 2 * 1024 * 1024) return ElMessage.warning('封面图片不超过 2MB')
  coverUploading.value = true
  const fd = new FormData()
  fd.append('file', file)
  const res = await api.uploadCover(fd).catch(() => null)
  coverUploading.value = false
  if (res?.data?.url) {
    animeEdit.value.coverUrl = res.data.url
    ElMessage.success('封面已上传，保存番剧信息后生效')
  }
}

async function saveAnimeEdit() {
  const f = animeEdit.value
  if (!f.title?.trim()) return ElMessage.warning('番剧名称不能为空')
  await api.saveAnime({
    id: f.id, title: f.title, alias: f.alias, year: f.year || null, region: f.region,
    coverUrl: f.coverUrl || '', airStatus: f.airStatus, state: f.state,
    episodeCount: f.episodeCount || null, duration: f.duration,
    studio: f.studio, director: f.director, voiceActor: f.voiceActor,
    description: f.description, totalScore: f.totalScore || null
  }).catch(() => null)
  animeEditVisible.value = false
  ElMessage.success('番剧信息已保存')
  loadAnimes(); loadOverview()
}

async function savePlatform(p) {
  if (!p.platformName?.trim()) return ElMessage.warning('平台名称不能为空')
  if (!p.url?.trim() || !/^https?:\/\//.test(p.url)) return ElMessage.warning('链接须以 http(s):// 开头')
  await api.fixPlatform(p.id, p.url).catch(() => null)
  ElMessage.success(`「${p.platformName}」链接已更新并置为有效`)
}

async function addPlatformRow() {
  const { name, url } = newPlatform.value
  if (!name?.trim()) return ElMessage.warning('请填写平台名称')
  if (!url?.trim() || !/^https?:\/\//.test(url)) return ElMessage.warning('链接须以 http(s):// 开头')
  await api.addPlatform(animeEdit.value.id, name.trim(), url.trim()).catch(() => null)
  platformsOfEdit.value.push({ id: null, platformName: name.trim(), url: url.trim(), linkStatus: 1 })
  newPlatform.value = { name: '', url: '' }
  ElMessage.success('平台链接已添加')
  loadAnimes()
}

async function importFile(e) {
  const file = e.target.files[0]
  if (!file) return
  const fd = new FormData()
  fd.append('file', file)
  const res = await api.importExcel(fd).catch(() => null)
  if (res?.data) {
    importResult.value = res.data
    ElMessage.success('导入完成')
    loadAnimes(); loadReviews(); loadOverview()
  }
  e.target.value = ''
}

async function exportExcel() {
  // 通过 axios blob 下载（走统一 request，带 token）
  const { default: request } = await import('../api/request')
  const res = await request.post('/admin/export', null, { responseType: 'blob' }).catch(() => null)
  if (res) {
    const url = URL.createObjectURL(res)
    const a = document.createElement('a')
    a.href = url; a.download = 'anime-export.xlsx'; a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出完成')
  }
}

async function saveCfg() {
  await api.saveRecConfig({
    wProfile: cfg.wProfile, wCf: cfg.wCf, wHot: cfg.wHot, wRandom: cfg.wRandom
  }).catch(() => null)
  ElMessage.success('已保存，自下一次推荐计算任务执行时生效')
}

async function recalc() {
  try {
    await ElMessageBox.confirm('全量重算约 30 分钟内完成，期间推荐结果将逐步更新，确定执行？', '手动重算确认', { type: 'warning' })
    await api.recalc().catch(() => null)
    ElMessage.success('重算任务已触发')
    loadCfg()
  } catch { }
}

const stateText = s => ({ 0: '待审核', 1: '已上线', 2: '已下架' })[s]
const stateClass = s => ({ 0: 'gold', 1: 'success', 2: 'danger' })[s]
const payloadTitle = p => { try { return JSON.parse(p).title } catch { return p } }

onMounted(() => {
  loadOverview(); loadAnimes(); loadReviews(); loadCfg()
})
</script>

<style scoped>
.admin-mark { display: inline-flex; align-items: center; gap: 6px; margin-bottom: 12px; padding: 4px 12px; border-radius: 12px; background: #2d3436; color: #ffeaa7; font-size: 12px; font-weight: 700; }
.overview { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; margin-bottom: 16px; }
.ov-card { text-align: center; padding: 14px 8px; }
.ov-num { font-size: 26px; font-weight: 800; color: var(--primary); }
.ov-card.warn .ov-num { color: var(--danger); }
.ov-label { font-size: 11px; color: var(--text-3); }
.table { width: 100%; border-collapse: collapse; font-size: 12px; }
.table th { text-align: left; padding: 8px 6px; color: var(--text-3); font-weight: 600; border-bottom: 1px solid #eef0f6; }
.table td { padding: 9px 6px; border-bottom: 1px solid #f2f4f8; vertical-align: middle; }
.table tr:hover td { background: var(--primary-bg); }
.t-title { font-weight: 600; max-width: 220px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.t-platforms { font-size: 11px; color: var(--text-2); }
.invalid { color: var(--danger); font-weight: 700; }
.t-ops { display: flex; gap: 6px; }
.kw-input { height: 28px; width: 180px; border: 1px solid #e3e6f0; border-radius: var(--radius-ctl); padding: 0 8px; margin-left: 8px; }
.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 16px; align-items: start; }
.review-item { padding: 10px 0; border-bottom: 1px dashed #eef0f6; }
.r-src { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.r-by { font-size: 12px; color: var(--text-2); }
.r-payload { font-size: 13px; font-weight: 600; margin-bottom: 4px; }
.r-url { font-size: 12px; color: var(--text-3); margin-bottom: 4px; word-break: break-all; }
.r-link { color: var(--info); }
.r-note { font-size: 12px; color: var(--text-2); background: var(--primary-bg); border-radius: var(--radius-ctl); padding: 6px 8px; margin-bottom: 6px; }
.r-reason { font-size: 12px; color: var(--danger); margin-bottom: 4px; }
.r-ops { display: flex; gap: 8px; }
.field { margin-bottom: 10px; }
.field label { display: block; margin-bottom: 4px; font-size: 12px; color: var(--text-2); }
.field input, .field textarea { width: 100%; padding: 7px 9px; border: 1px solid #e3e6f0; border-radius: var(--radius-ctl); outline: none; font-family: inherit; font-size: 13px; }
.field input:focus, .field textarea:focus { border-color: var(--primary); }
.field textarea { resize: vertical; }
.field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.sel { width: 100%; padding: 7px 9px; border: 1px solid #e3e6f0; border-radius: var(--radius-ctl); outline: none; font-family: inherit; font-size: 13px; background: #fff; }
.pf-title { margin: 14px 0 8px; font-size: 13px; font-weight: 700; color: var(--primary); }
.cover-row { display: flex; gap: 14px; margin-bottom: 14px; padding-bottom: 12px; border-bottom: 1px dashed #eef0f6; }
.cover-preview { width: 115px; height: 154px; flex-shrink: 0; border-radius: var(--radius-ctl); overflow: hidden; border: 1px solid #e3e6f0; background: var(--primary-bg); display: flex; align-items: center; justify-content: center; color: var(--text-3); font-size: 12px; }
.cover-preview img { width: 100%; height: 100%; object-fit: cover; }
.cover-ops { flex: 1; min-width: 0; }
.cover-label { display: block; font-size: 12px; color: var(--text-2); margin-bottom: 8px; }
.cover-btns { display: flex; gap: 8px; margin-bottom: 8px; }
.cover-url-input { width: 100%; padding: 6px 8px; border: 1px dashed #e3e6f0; border-radius: var(--radius-ctl); font-size: 12px; outline: none; }
.cover-url-input:focus { border-color: var(--primary); border-style: solid; }
.cover-tip { margin-top: 6px; font-size: 11px; color: var(--info); }
.pf-row { display: flex; gap: 6px; margin-bottom: 6px; align-items: center; }
.pf-row.invalid .pf-name, .pf-row.invalid .pf-url { border-color: var(--danger); background: #fff5f5; }
.pf-name { width: 110px; padding: 6px 8px; border: 1px solid #e3e6f0; border-radius: var(--radius-ctl); font-size: 12px; }
.pf-url { flex: 1; padding: 6px 8px; border: 1px solid #e3e6f0; border-radius: var(--radius-ctl); font-size: 12px; }
.pf-add .pf-name { border-style: dashed; }
.pf-empty { font-size: 12px; color: var(--text-3); padding: 4px 0; }
.sg-tip { padding: 8px 10px; border-radius: var(--radius-ctl); background: #ffe9e9; color: #c0392b; font-size: 12px; line-height: 1.6; }
.io-tip { font-size: 12px; color: var(--text-3); line-height: 1.7; margin-bottom: 10px; }
.file-input { display: none; }
.io-btns { display: flex; gap: 10px; }
.import-result { margin-top: 12px; padding: 8px 12px; border-radius: var(--radius-ctl); background: #e0f7f1; color: var(--success); font-size: 12px; }
.factor-row { display: flex; align-items: center; gap: 14px; padding: 8px 0; }
.factor-row label { width: 70px; font-size: 13px; }
.factor-row input[type=range] { flex: 1; accent-color: var(--primary); }
.f-val { width: 36px; text-align: right; font-weight: 700; color: var(--primary); }
.sum-tip { font-size: 12px; color: var(--text-3); margin: 8px 0 14px; }
.cfg-ops { display: flex; align-items: center; gap: 12px; }
.last-recalc { font-size: 12px; color: var(--text-3); }
@media (max-width: 900px) { .two-col, .overview { grid-template-columns: 1fr 1fr; } }
</style>

<script>
export default { name: 'AdminPage' }
</script>
