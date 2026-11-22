import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 构建代次戳：每次 dev server 启动生成唯一标记，追加到所有源码转换产物，
// 使 ETag 必然变化，杜绝浏览器 304 复用旧转换产物导致的依赖多代次混用
const GEN = Date.now().toString(36)
const genStamp = () => ({
  name: 'gen-stamp',
  enforce: 'post',
  transform(code, id) {
    if (id.includes('node_modules')) return null
    return { code: code + `\n/*gen:${GEN}*/`, map: null }
  }
})

// Vite 配置：本地开发代理后端 8080（生产由 Nginx 反向代理同源）
export default defineConfig({
  plugins: [vue(), genStamp()],
  server: {
    port: 5174,
    // 本地开发禁用一切浏览器缓存
    headers: { 'Cache-Control': 'no-store, must-revalidate' },
    // 目录经 junction 链接访问时 realpath 与 root 不一致，需关闭缓存校验
    fs: { cachedChecks: false },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // 封面等上传静态资源：统一经前端域代理到后端，全站用户同一 URL 可见
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
