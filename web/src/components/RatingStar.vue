<template>
  <!-- 公共组件：1~10 星级评分（界面文档 4.6 节） -->
  <div class="rating-star">
    <span v-for="i in 10" :key="i" class="star" :class="{ on: i <= hover || (i <= current && !hover) }"
          @mouseenter="hover = i" @mouseleave="hover = 0" @click="pick(i)">{{ i }}</span>
    <span class="picked" v-if="current">已评 {{ current }} 分</span>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const props = defineProps({ modelValue: { type: Number, default: 0 } })
const emit = defineEmits(['update:modelValue', 'rated'])
const current = ref(props.modelValue)
const hover = ref(0)

function pick(i) {
  current.value = i
  emit('update:modelValue', i)
  emit('rated', i)
}
</script>

<style scoped>
.rating-star { display: inline-flex; align-items: center; gap: 4px; }
.star { width: 26px; height: 26px; display: inline-flex; align-items: center; justify-content: center; border-radius: 50%; border: 1px solid #e3e6f0; font-size: 12px; color: var(--text-3); cursor: pointer; transition: all .15s; user-select: none; }
.star.on { background: linear-gradient(135deg, var(--gold), var(--accent)); border-color: transparent; color: #fff; font-weight: 700; }
.picked { margin-left: 8px; font-size: 12px; color: var(--success); }
</style>

<script>
export default { name: 'RatingStar' }
</script>
