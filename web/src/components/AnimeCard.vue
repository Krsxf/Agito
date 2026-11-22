<template>
  <!-- 公共组件：番剧卡片（props: anime；emit: click/favorite/follow） -->
  <div class="anime-card card" @click="$emit('click', anime.id)">
    <div class="cover" :style="coverStyle">
      <img v-if="anime.coverUrl" :src="anime.coverUrl" :alt="anime.title" class="cover-img" loading="lazy">
      <span v-if="anime.airStatus === 1" class="status-pill onair">连载中</span>
      <span v-else-if="anime.airStatus === 2" class="status-pill done">已完结</span>
      <span v-else-if="anime.airStatus === 3" class="status-pill soon">待上线</span>
      <span v-if="isNew" class="new-badge">NEW</span>
      <span class="title-in-cover">{{ anime.title }}</span>
    </div>
    <div class="info">
      <p class="name" :title="anime.title">{{ anime.title }}</p>
      <div class="meta">
        <span v-if="anime.tag" class="tag-pill">{{ anime.tag }}</span>
        <span v-if="anime.score" class="score">
          <span class="score-star">★</span><span class="score-num">{{ anime.score }}</span>
        </span>
      </div>
      <div class="ops">
        <button class="btn btn-accent btn-mini" @click.stop="$emit('jump', anime.id)">去观看</button>
        <button class="btn btn-ghost btn-mini" @click.stop="$emit('follow', anime.id)">{{ followed ? '已追番' : '+ 追番' }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  anime: { type: Object, required: true },
  followed: { type: Boolean, default: false },
  isNew: { type: Boolean, default: false }
})
defineEmits(['click', 'jump', 'follow'])

// 封面：渐变占位（合规边界：不热链外部封面图，coverUrl 为空时本地渐变兜底）
const palettes = [
  ['#6C5CE7', '#FD79A8'], ['#74B9FF', '#6C5CE7'], ['#00B894', '#74B9FF'],
  ['#FDCB6E', '#FD79A8'], ['#a29bfe', '#fd79a8'], ['#55efc4', '#81ecec']
]
const coverStyle = computed(() => {
  const p = palettes[(props.anime.id || 0) % palettes.length]
  return { background: `linear-gradient(135deg, ${p[0]}, ${p[1]})` }
})
</script>

<style scoped>
.anime-card { padding: 0; overflow: hidden; cursor: pointer; transition: all .2s; }
.anime-card:hover { transform: translateY(-3px); box-shadow: var(--card-shadow-hover); }
.cover { position: relative; height: 180px; display: flex; align-items: flex-end; padding: 12px; color: #fff; }
/* 真实封面（服务器上传，全站可见）：铺满并压暗渐变以保证文字可读 */
.cover-img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.cover::after { content: ''; position: absolute; inset: 0; background: linear-gradient(180deg, transparent 30%, rgba(0,0,0,.55)); pointer-events: none; }
.title-in-cover, .status-pill, .new-badge { position: relative; z-index: 1; }
.title-in-cover { font-size: 15px; font-weight: 700; text-shadow: 0 2px 8px rgba(0,0,0,.35); line-height: 1.4; max-height: 2.8em; overflow: hidden; }
.status-pill { position: absolute; top: 10px; right: 10px; padding: 2px 8px; border-radius: 9px; font-size: 11px; font-weight: 600; }
.onair { background: rgba(0, 184, 148, .92); }
.done { background: rgba(116, 185, 255, .92); }
.soon { background: rgba(253, 203, 110, .95); color: #6b4f00; }
.new-badge { position: absolute; top: 10px; left: 10px; padding: 2px 7px; border-radius: 8px; background: #FF7675; font-size: 10px; font-weight: 700; letter-spacing: .5px; }
.info { padding: 12px 14px 14px; }
.name { font-size: 14px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-bottom: 8px; }
.meta { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; min-height: 20px; }
.ops { display: flex; gap: 8px; }
.ops .btn { flex: 1; }
</style>

<script>
export default { name: 'AnimeCard' }
</script>
