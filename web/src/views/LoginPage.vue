<template>
  <!-- 登录/注册页（界面文档 4.3 节：左品牌区 + 右表单区，内嵌 12 类兴趣标签） -->
  <div class="login-page">
    <div class="brand-side">
      <h1>追番助手</h1>
      <p class="slogan">基于多元数据的个性化追番推荐与补番助手</p>
      <ul class="features">
        <li>🎬 多源评分聚合，一部番的口碑一目了然</li>
        <li>🎯 协同过滤 × 内容过滤，推荐懂你所爱</li>
        <li>📌 追番进度一键确认，再也不会忘记看到第几集</li>
        <li>📚 补番清单拖拽排序，高分神作不漏补</li>
      </ul>
      <p class="guest-entry" @click="guestBrowse">游客浏览热门榜单 →</p>
    </div>

    <div class="form-side">
      <div class="form-card card">
        <div class="tab-row">
          <button :class="{ on: mode === 'login' }" @click="mode = 'login'">去登录</button>
          <button :class="{ on: mode === 'register' }" @click="mode = 'register'">注册</button>
        </div>

        <template v-if="mode === 'login'">
          <div class="field"><label>用户名</label><input v-model="loginForm.username" placeholder="boss / admin"></div>
          <div class="field"><label>密码</label><input v-model="loginForm.password" type="password" placeholder="admin123"></div>
          <button class="btn btn-primary submit" @click="doLogin">登 录</button>
        </template>

        <template v-else>
          <div class="field"><label>用户名</label><input v-model="regForm.username" placeholder="3~20 位字符"></div>
          <div class="field"><label>密码</label><input v-model="regForm.password" type="password" placeholder="6~20 位"></div>
          <div class="field"><label>昵称（可选）</label><input v-model="regForm.nickname"></div>
          <div class="field">
            <label>兴趣标签 <span class="tip">(可多选，用于冷启动推荐)</span></label>
            <div class="interests">
              <span v-for="t in tags" :key="t.id" class="pill" :class="{ on: regForm.interestCodes.includes(t.tagCode) }"
                    @click="toggle(t.tagCode)">{{ t.tagName }}</span>
            </div>
          </div>
          <p class="cold-tip">💡 新用户行为不足时，推荐将按「兴趣标签匹配 + 热门补偿」策略出内容，行为积累后自动升级个性化推荐</p>
          <button class="btn btn-accent submit" @click="doRegister">注册并登录</button>
        </template>

        <p class="security-note">🔒 密码 BCrypt 加密存储 · 敏感信息脱敏 · 仅聚合元数据与正版外链</p>
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

const router = useRouter()
const store = useUserStore()

const mode = ref('login')
const loginForm = ref({ username: 'boss', password: 'admin123' })
const regForm = ref({ username: '', password: '', nickname: '', interestCodes: [] })
const tags = ref([])

onMounted(async () => {
  const res = await api.interests().catch(() => null)
  if (res?.data) tags.value = res.data
})

function toggle(code) {
  const i = regForm.value.interestCodes.indexOf(code)
  i >= 0 ? regForm.value.interestCodes.splice(i, 1) : regForm.value.interestCodes.push(code)
}

async function doLogin() {
  if (!loginForm.value.username || !loginForm.value.password) return ElMessage.warning('请输入用户名与密码')
  const res = await api.login(loginForm.value).catch(() => null)
  if (res?.data?.token) {
    store.setToken(res.data.token)
    await store.fetchUserInfo().catch(() => null)
    // 登录成功 → 记录欢迎动画触发标记（BaseLayout 检测播放）
    sessionStorage.setItem('welcome', JSON.stringify({ name: store.userInfo?.nickname || store.userInfo?.username || '', isNew: false }))
    ElMessage.success('登录成功')
    router.push('/home')
  }
}

async function doRegister() {
  const f = regForm.value
  if (!f.username || !f.password) return ElMessage.warning('请填写用户名与密码')
  if (f.username.length < 3 || f.password.length < 6) return ElMessage.warning('用户名≥3位，密码≥6位')
  const res = await api.register(f).catch(() => null)
  if (res?.data?.token) {
    store.setToken(res.data.token)
    await store.fetchUserInfo().catch(() => null)
    sessionStorage.setItem('welcome', JSON.stringify({ name: store.userInfo?.nickname || store.userInfo?.username || '', isNew: true }))
    ElMessage.success('注册成功，已自动登录')
    router.push('/home')
  }
}

function guestBrowse() { router.push('/rank') }
</script>

<style scoped>
.login-page { min-height: 100vh; display: flex; }
.brand-side { flex: 1.1; padding: 12vh 8%; color: #fff; background: linear-gradient(135deg, #6C5CE7 0%, #FD79A8 100%); }
.brand-side h1 { font-size: 44px; letter-spacing: 4px; margin-bottom: 10px; }
.slogan { font-size: 15px; opacity: .92; margin-bottom: 40px; }
.features { display: grid; gap: 16px; font-size: 14px; opacity: .95; }
.guest-entry { margin-top: 48px; font-size: 13px; cursor: pointer; opacity: .85; }
.guest-entry:hover { opacity: 1; text-decoration: underline; }
.form-side { flex: 1; display: flex; align-items: center; justify-content: center; padding: 40px; background: var(--bg); }
.form-card { width: 400px; max-width: 92vw; }
.tab-row { display: flex; gap: 8px; margin-bottom: 20px; }
.tab-row button { flex: 1; height: 38px; border: none; border-radius: var(--radius-ctl); background: var(--primary-bg); color: var(--text-2); font-size: 14px; }
.tab-row button.on { background: var(--primary); color: #fff; font-weight: 600; }
.field { margin-bottom: 14px; }
.field label { display: block; margin-bottom: 6px; font-size: 13px; color: var(--text-2); }
.field .tip { color: var(--text-3); font-weight: 400; font-size: 11px; }
.field input { width: 100%; height: 38px; padding: 0 12px; border: 1px solid #e3e6f0; border-radius: var(--radius-ctl); outline: none; }
.field input:focus { border-color: var(--primary); }
.interests { display: flex; flex-wrap: wrap; gap: 8px; }
.interests .pill.on { background: var(--accent); border-color: var(--accent); }
.cold-tip { margin: 10px 0 16px; padding: 10px 12px; border-radius: var(--radius-ctl); background: #e8f3ff; color: #0984e3; font-size: 12px; line-height: 1.6; }
.submit { width: 100%; height: 42px; font-size: 15px; }
.security-note { margin-top: 14px; font-size: 11px; color: var(--text-3); text-align: center; }
</style>

<script>
export default { name: 'LoginPage' }
</script>
