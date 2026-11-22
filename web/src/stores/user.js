import { defineStore } from 'pinia'
import { api } from '../api'

/** 全局状态：登录态、用户信息、提醒红点（界面文档 2.3 节 Pinia 职责） */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null'),
    unreadCount: 0
  }),
  getters: {
    isLogin: s => !!s.token,
    isAdmin: s => s.userInfo?.isAdmin === true
  },
  actions: {
    setToken(token) {
      this.token = token
      localStorage.setItem('token', token)
    },
    async fetchUserInfo() {
      if (!this.token) return
      const res = await api.me().catch(() => null)
      if (res && res.data) {
        this.userInfo = res.data
        localStorage.setItem('userInfo', JSON.stringify(res.data))
      }
    },
    async fetchUnread() {
      if (!this.token) { this.unreadCount = 0; return }
      const res = await api.unreadCount().catch(() => null)
      if (res && res.data) this.unreadCount = res.data.count || 0
    },
    clear() {
      this.token = ''
      this.userInfo = null
      this.unreadCount = 0
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
})
