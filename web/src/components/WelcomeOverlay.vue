<template>
  <!-- 登录欢迎动画：全屏浮层，2.6 秒后自动淡出（登录/注册成功后触发） -->
  <transition name="welcome-fade">
    <div v-if="visible" class="welcome-overlay" @click="dismiss">
      <div class="stars">
        <span v-for="i in 12" :key="i" :class="'s' + i"></span>
      </div>
      <div class="welcome-core">
        <div class="logo-bounce">番</div>
        <h1 class="hello">{{ greeting }}，{{ name }}！</h1>
        <p class="sub">{{ isNew ? '欢迎加入追番助手，开始你的追番之旅吧' : '欢迎回来，今天想看点什么呢' }}</p>
        <div class="loading-bar"><div></div></div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const props = defineProps({
  name: { type: String, default: '旅行者' },
  isNew: { type: Boolean, default: false }
})

const visible = ref(false)
const greeting = ref('')

function dismiss() { visible.value = false }

onMounted(() => {
  const h = new Date().getHours()
  greeting.value = h < 6 ? '夜深了' : h < 11 ? '早上好' : h < 14 ? '中午好' : h < 18 ? '下午好' : '晚上好'
  // 直接置 true（不经 rAF）：后台标签页 rAF 不会执行，会卡住动画
  visible.value = true
  setTimeout(() => { visible.value = false }, 3200)
})
</script>

<style scoped>
.welcome-overlay { position: fixed; inset: 0; z-index: 3000; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, rgba(108,92,231,.96), rgba(253,123,168,.92)); backdrop-filter: blur(6px); cursor: pointer; }

/* 中心内容 */
.welcome-core { text-align: center; color: #fff; animation: coreIn .7s cubic-bezier(.22,1.4,.4,1); }
.logo-bounce { width: 84px; height: 84px; margin: 0 auto 22px; border-radius: 26px; background: rgba(255,255,255,.18); border: 2px solid rgba(255,255,255,.55); display: flex; align-items: center; justify-content: center; font-size: 40px; font-weight: 800; box-shadow: 0 12px 36px rgba(0,0,0,.22); animation: logoPop .8s cubic-bezier(.18,1.6,.36,1); }
.hello { font-size: 34px; letter-spacing: 2px; margin-bottom: 10px; text-shadow: 0 4px 18px rgba(0,0,0,.25); }
.sub { font-size: 14px; opacity: .92; letter-spacing: 1px; }
.loading-bar { width: 180px; height: 4px; margin: 26px auto 0; border-radius: 2px; background: rgba(255,255,255,.28); overflow: hidden; }
.loading-bar > div { height: 100%; width: 40%; border-radius: 2px; background: #fff; animation: slide 1.1s ease-in-out infinite; }

/* 背景漂浮星星 */
.stars span { position: absolute; border-radius: 50%; background: rgba(255,255,255,.55); animation: floatUp linear infinite; }
.stars .s1 { left: 8%;  width: 6px;  height: 6px;  animation-duration: 5s;  animation-delay: 0s; }
.stars .s2 { left: 18%; width: 4px;  height: 4px;  animation-duration: 7s;  animation-delay: 1.2s; }
.stars .s3 { left: 28%; width: 8px;  height: 8px;  animation-duration: 6s;  animation-delay: .6s; }
.stars .s4 { left: 38%; width: 5px;  height: 5px;  animation-duration: 8s;  animation-delay: 2s; }
.stars .s5 { left: 48%; width: 7px;  height: 7px;  animation-duration: 5.5s;animation-delay: .3s; }
.stars .s6 { left: 58%; width: 4px;  height: 4px;  animation-duration: 7.5s;animation-delay: 1.6s; }
.stars .s7 { left: 68%; width: 6px;  height: 6px;  animation-duration: 6.5s;animation-delay: .8s; }
.stars .s8 { left: 78%; width: 5px;  height: 5px;  animation-duration: 5.2s;animation-delay: 2.4s; }
.stars .s9 { left: 88%; width: 7px;  height: 7px;  animation-duration: 7.2s;animation-delay: 1s; }
.stars .s10 { left: 12%; width: 4px; height: 4px;  animation-duration: 8.4s;animation-delay: 3s; }
.stars .s11 { left: 62%; width: 4px; height: 4px;  animation-duration: 8.8s;animation-delay: 2.2s; }
.stars .s12 { left: 82%; width: 5px; height: 5px;  animation-duration: 6.8s;animation-delay: 1.4s; }

/* 进出场过渡 */
.welcome-fade-enter-active { transition: opacity .45s ease; }
.welcome-fade-leave-active { transition: opacity .5s ease; }
.welcome-fade-enter-from, .welcome-fade-leave-to { opacity: 0; }

@keyframes coreIn { from { transform: translateY(26px) scale(.94); opacity: 0; } to { transform: none; opacity: 1; } }
@keyframes logoPop { 0% { transform: scale(.2); } 60% { transform: scale(1.18); } 100% { transform: scale(1); } }
@keyframes slide { 0% { transform: translateX(-120%); } 100% { transform: translateX(320%); } }
@keyframes floatUp { 0% { transform: translateY(105vh); opacity: 0; } 12% { opacity: .9; } 88% { opacity: .5; } 100% { transform: translateY(-8vh); opacity: 0; } }

@media (prefers-reduced-motion: reduce) {
  .stars span { animation: none; display: none; }
  .logo-bounce, .welcome-core, .loading-bar > div { animation: none; }
}
</style>

<script>
export default { name: 'WelcomeOverlay' }
</script>
