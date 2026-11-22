<template>
  <!-- 公共组件：追番周视图日历（界面文档 4.7 节，7 格周历） -->
  <div class="week-calendar">
    <div v-for="(d, i) in days" :key="i" class="day-col" :class="{ today: i === todayIdx }">
      <p class="day-name">{{ d }}<span v-if="i === todayIdx" class="today-tag">今天</span></p>
      <div class="day-cell">
        <div v-for="a in byDay(i)" :key="a.animeId" class="day-anime" @click="$emit('pick', a.animeId)">
          {{ a.title }}
        </div>
        <p v-if="!byDay(i).length" class="day-empty">-</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // [{ animeId, title, weekday }] weekday: 1周一 ~ 7周日
  animes: { type: Array, default: () => [] }
})
defineEmits(['pick'])

const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const todayIdx = (new Date().getDay() + 6) % 7   // 周一为 0

const byDay = i => props.animes.filter(a => a.weekday === i + 1)
</script>

<style scoped>
.week-calendar { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8px; }
.day-col { border-radius: var(--radius-ctl); overflow: hidden; border: 1px solid #eef0f6; }
.day-col.today { border-color: var(--primary); box-shadow: 0 0 0 2px rgba(108,92,231,.15); }
.day-name { padding: 6px 8px; font-size: 12px; font-weight: 600; color: var(--text-2); background: #f7f8fc; display: flex; justify-content: space-between; align-items: center; }
.today-tag { font-size: 10px; color: #fff; background: var(--primary); border-radius: 6px; padding: 1px 6px; }
.day-cell { min-height: 120px; padding: 6px; background: #fff; }
.day-anime { margin-bottom: 6px; padding: 6px 8px; border-radius: var(--radius-ctl); background: var(--primary-bg); color: var(--primary); font-size: 12px; cursor: pointer; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.day-anime:hover { background: var(--primary); color: #fff; }
.day-empty { text-align: center; color: var(--text-3); padding-top: 30px; }
</style>

<script>
export default { name: 'CalendarWeek' }
</script>
