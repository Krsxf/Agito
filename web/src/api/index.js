import request from './request'

/** 接口模块：与后端 Controller 一一对应（Knife4j 文档 http://localhost:8080/doc.html） */
export const api = {
  // 认证
  login: data => request.post('/auth/login', data),
  register: data => request.post('/auth/register', data),
  logout: () => request.post('/auth/logout'),
  me: () => request.get('/auth/me'),
  interests: () => request.get('/auth/interests'),

  // 首页推荐 / 换一批
  homeRecommend: () => request.get('/home/recommend'),
  refreshBatch: column => request.post(`/anime/refresh?column=${column}`),

  // 番剧中心 / 详情 / 搜索
  animeList: params => request.get('/anime/list', { params }),
  animeDetail: id => request.get(`/anime/detail/${id}`),
  suggest: keyword => request.get('/anime/search', { params: { keyword } }),
  hotWords: () => request.get('/anime/hot-words'),

  // 排行榜
  rankList: (rankType, period) => request.get('/rank/list', { params: { rankType, period } }),

  // 跳转
  jump: (animeId, platform) => request.post(`/jump/${animeId}?platform=${encodeURIComponent(platform)}`),

  // 用户行为
  rate: (animeId, score) => request.post(`/action/rate/${animeId}?score=${score}`),
  favorite: (animeId, on) => request.post(`/action/favorite/${animeId}?on=${on}`),
  follow: animeId => request.post(`/action/follow/${animeId}`),
  confirmProgress: (animeId, episode) => request.post(`/action/follow/${animeId}/progress?episode=${episode}`),
  unfollow: animeId => request.delete(`/action/follow/${animeId}`),
  myFollows: () => request.get('/action/follows'),
  addCatchup: animeId => request.post(`/action/catchup/${animeId}`),
  catchupDone: (animeId, dropped) => request.post(`/action/catchup/${animeId}/done?dropped=${dropped}`),
  myCatchup: () => request.get('/action/catchup'),
  reorderCatchup: ids => request.post('/action/catchup/reorder', ids),
  myFavorites: () => request.get('/action/favorites'),
  myRatings: () => request.get('/action/ratings'),
  suggest: data => request.post('/action/suggest', data),
  suggestMine: () => request.get('/action/suggest/mine'),

  // 提醒
  notifyList: () => request.get('/notify/list'),
  unreadCount: () => request.get('/notify/unread-count'),
  readAll: () => request.post('/notify/read-all'),

  // 后台管理
  adminOverview: () => request.get('/admin/overview'),
  adminAnimes: params => request.get('/admin/anime', { params }),
  saveAnime: data => request.post('/admin/anime', data),
  addPlatform: (animeId, platformName, url) => request.post(
    `/admin/platform?animeId=${animeId}&platformName=${encodeURIComponent(platformName)}&url=${encodeURIComponent(url)}`),
  uploadCover: fd => request.post('/admin/upload-cover', fd, { headers: { 'Content-Type': 'multipart/form-data' } }),
  changeState: (id, state) => request.post(`/admin/anime/${id}/state?state=${state}`),
  fixPlatform: (id, url) => request.post(`/admin/platform/${id}/fix?url=${encodeURIComponent(url)}`),
  reviewList: () => request.get('/admin/review'),
  reviewPass: (id, edited) => request.post(`/admin/review/${id}/pass`, edited || null),
  reviewReject: id => request.post(`/admin/review/${id}/reject`),
  importExcel: fd => request.post('/admin/import', fd, { headers: { 'Content-Type': 'multipart/form-data' } }),
  getRecConfig: () => request.get('/admin/rec-config'),
  saveRecConfig: data => request.post('/admin/rec-config', data),
  recalc: () => request.post('/admin/rec-config/recalc'),
  adminLogs: params => request.get('/admin/logs', { params }),
  notifyTest: () => request.post('/admin/notify-test')
}
