<template>
  <!-- 公共组件：平台跳转（权重排序 + 付费/限区标注 + 失效隐藏，4.2.3.6 节链接失效处理） -->
  <div class="platform-jump">
    <template v-for="p in validPlatforms" :key="p.platformName">
      <button v-if="p.linkStatus !== 0" class="jump-btn" :class="{ main: p === validPlatforms[0] }"
              @click="jump(p)" :disabled="jumping">
        {{ p.platformName }}
        <span v-if="p.isPaid" class="tag-pill gold">付费</span>
        <span v-if="p.regionLimit" class="tag-pill info">限地区</span>
      </button>
    </template>
    <p v-for="p in invalidPlatforms" :key="'bad-' + p.platformName" class="invalid-tip">
      {{ p.platformName }}：<span class="danger">链接失效</span>
      <a v-if="isAdmin" class="feedback-link" @click="$emit('feedback')">反馈管理员</a>
    </p>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { useUserStore } from '../stores/user'

const props = defineProps({ animeId: { type: Number, required: true }, platforms: { type: Array, default: () => [] } })
defineEmits(['feedback'])

const store = useUserStore()
const isAdmin = computed(() => store.isAdmin)
const jumping = ref(false)

const validPlatforms = computed(() => props.platforms.filter(p => p.linkStatus !== 0))
const invalidPlatforms = computed(() => props.platforms.filter(p => p.linkStatus === 0))

async function jump(p) {
  // 跳转前校验链接有效性：失效时红色提示并隐藏入口（3.3 节确认规范）
  jumping.value = true
  try {
    const res = await api.jump(props.animeId, p.platformName)
    if (res?.data?.url) {
      window.open(res.data.url, '_blank', 'noopener')
      ElMessage.success(`正在跳转 ${p.platformName}，观片愉快！`)
    }
  } catch (e) {
    // 3003 链接失效由 request 拦截器统一提示
  } finally {
    jumping.value = false
  }
}
</script>

<style scoped>
.platform-jump { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; }
.jump-btn { display: inline-flex; align-items: center; gap: 8px; height: 38px; padding: 0 18px; border: none; border-radius: 19px; background: #fff; border: 1px solid #e3e6f0; font-size: 14px; color: var(--text-1); transition: all .18s; }
.jump-btn:hover { border-color: var(--primary); box-shadow: var(--card-shadow); transform: translateY(-1px); }
.jump-btn.main { background: linear-gradient(135deg, var(--accent), #e84393); border: none; color: #fff; font-weight: 700; }
.jump-btn.main:hover { filter: brightness(1.06); }
.invalid-tip { font-size: 12px; color: var(--text-3); }
.danger { color: #FF7675; font-weight: 600; }
.feedback-link { color: var(--info); cursor: pointer; text-decoration: underline; margin-left: 4px; }
</style>

<script>
export default { name: 'PlatformJump' }
</script>
