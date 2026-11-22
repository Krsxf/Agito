# 追番助手

基于多元数据的个性化追番推荐与补番助手 —— 软件工程综合实践项目（Agito）

> 系统整合哔哩哔哩、豆瓣动画、MyAnimeList 等多元数据源构建统一番剧信息库，基于用户画像与「协同过滤 + 内容过滤」融合算法提供个性化推荐，配套追番管理、补番助手、排行榜等工具化功能。**系统不承载任何视频内容**，观看环节通过网页跳转引导用户至外部正版视频平台完成观看（内容合规边界，对应需求规格说明书 2.2 节）。

## 技术栈

| 层 | 技术 | 说明 |
|---|---|---|
| 应用层 | Vue 3.4 + Vite 5 + Vue Router 4 + Pinia + Axios + Element Plus | 前后端分离 MVC 变体（视图层） |
| 服务层 | Spring Boot 3.3.5 + MyBatis-Plus 3.5 + Sa-Token 1.38 + POI 5.2 + Knife4j 4.5 | Controller-Service-Repository 分层 |
| 数据层 | MySQL 8.0（业务）· Redis 6.0（缓存）· Elasticsearch 7.x（检索） | 本机缺省 Redis/ES 时自动降级（见下） |
| 接入层 | Nginx 1.24 | HTTPS 终结、静态资源、反向代理、限流 |

## 目录结构

```
anime-helper/
├── sql/                          数据库脚本
│   ├── 01_schema.sql             建库建表（20 张表，utf8mb4）
│   ├── 02_init_data.sql          演示数据（26 部番剧 + 三源评分 + 平台外链）
│   ├── 03_admin.sql              管理员账号（boss/admin123，BCrypt）
│   └── 04_review_enhance.sql     审核队列增强（用户提交闭环）
├── server/                       Spring Boot 后端（IDEA 打开）
│   └── src/main/java/com/agito/anime/
│       ├── common/               统一出参 / 错误码 / 全局异常
│       ├── config/               Sa-Token 拦截器 / RBAC 数据源 / 静态映射 / 缓存
│       ├── entity/ mapper/       20 表实体 + MyBatis-Plus Mapper
│       ├── service/              8 大服务（认证/推荐/番剧/榜单/跳转/行为/后台/日志）
│       └── controller/           8 个控制器（游客白名单放行）
└── web/                          Vue 3 前端（HBuilderX / VS Code 打开）
    └── src/
        ├── layouts/BaseLayout.vue    四区域布局（顶部60/侧边200/内容/底部44）
        ├── components/               6 公共组件（卡片/搜索/周历/评分/跳转/欢迎动画）
        ├── views/                    9 页面（登录/首页/番剧中心/详情/追番/补番/排行/用户中心/后台）
        ├── api/                      统一 request（token 注入 + 错误码处理）
        ├── stores/                   Pinia（登录态/用户信息/提醒红点）
        └── router/                   路由 + 登录守卫 + 管理端 RBAC 守卫
```

## 快速启动

### 1. 数据库

```bash
# MySQL 8.0（root 密码按实际修改）
mysql -uroot -p < sql/01_schema.sql
mysql -uroot -p < sql/02_init_data.sql
mysql -uroot -p < sql/03_admin.sql
mysql -uroot -p < sql/04_review_enhance.sql
```

### 2. 后端（server/，IDEA 打开或命令行）

```bash
# 配置：src/main/resources/application.yml（数据库账号密码）
mvn package -DskipTests
java -jar target/anime-helper-server-1.0.0.jar
# 启动于 http://localhost:8080，接口文档 http://localhost:8080/doc.html
```

⚠️ **本机路径含非 ASCII 字符（如 `D:\TELE—AI\...`）时，`mvn spring-boot:run` 无法加载主类**，必须用上面的 `package + java -jar` 方式启动（路径含长破折号时 jar 建议先复制到纯 ASCII 路径再运行）。

### 3. 前端（web/）

```bash
npm install
npm run dev        # http://localhost:5174，代理 /api 与 /uploads 到后端 8080
```

### 4. 演示账号

| 账号 | 密码 | 角色 |
|---|---|---|
| boss | admin123 | ADMIN（可进后台管理） |
| 任前台注册 | - | USER（12 类兴趣标签冷启动） |

## 核心功能

### 用户端
- **首页推荐**：今日推荐 / 猜你喜欢 / 新番速递三栏目，支持换一批；冷启动自动降级为「兴趣标签 + 热门补偿」
- **番剧中心**：类型/年份/地区/状态多维筛选 + 关键词搜索联想 + 游客只读
- **番剧详情**：综合加权 + 豆瓣/B站/MAL 四联评分卡、属性网格、热度趋势、正版平台跳转（跳转前校验链接）
- **追番管理**：进度一键确认（"上次看到第 X 集"）、周视图日历、NEW 更新角标
- **补番助手**：进度横幅（完成百分比）、多条件筛选、清单拖拽排序、补番/弃番
- **排行榜**：综合热度/新番/经典高分三榜 + 周月总切换，四因子权重说明
- **用户中心**：兴趣画像、我的评分/收藏/番剧建议提交记录、更新提醒红点
- **求上架**：用户提交想看的番剧 + 跳转链接 → 管理员审核（可修改信息与链接）→ 上架 → 用户中心可查审核状态
- **登录欢迎动画**：按时段问候 + 漂浮星星 + 毛玻璃浮层（3.2 秒自动淡出）

### 管理端
- **运营概览**：番剧总量/注册用户/今日跳转/失效链接/待审核
- **番剧管理**：增删改查、**随时编辑番剧信息与封面**、上架/下架、失效链接修复
- **封面上传**：图片存服务器静态目录（UUID 文件名 + 扩展名白名单 + 2MB 限制），全站用户经同一 URL 可见
- **人工审核**：区分「用户提交/数据导入」来源；审核弹窗可修改作品信息与跳转链接后上架
- **Excel 导入导出**：管道-过滤器模式（导入→清洗→归一化→审核→入库）
- **推荐参数**：四因子权重滑块（画像/协同/热门/随机，和须为 1）、手动全量重算
- **日志管理**：四类日志（登录/操作/跳转/导入）DB 留存 + Logback 滚动 180 天

## 数据合规声明

- **不做真实爬虫**：数据链路为「本地 Excel/JSON 导入 → 清洗 → 评分归一化 → 人工审核 → 入库」管道，演示数据为人工整理样例；正式环境经官方开放 API 与数据合作方获取
- **仅聚合元数据**：不承载任何视频内容，观看跳转仅指向白名单正版平台（哔哩哔哩/腾讯视频/爱奇艺/优酷/芒果TV）
- **不热链外部图片**：封面经管理员上传至本站服务器；无封面时本地渐变占位
- **日志最小化**：跳转与操作日志不含个人敏感信息

## 部署说明（生产环境）

### Nginx 配置要点

```nginx
server {
    listen 443 ssl;
    server_name your-domain.com;

    # 前端静态资源（npm run build 产物）
    location / {
        root /var/www/anime-helper/dist;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 反向代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # 上传静态资源（封面）：路由到后端，
    # 也可改为 root 指向上传目录直接托管（anime.upload.dir 配置一致）
    location /uploads/ {
        proxy_pass http://127.0.0.1:8080;
        # 或：root /data/anime-helper/upload; （去掉 /uploads 前缀需 rewrite）
    }

    # 限流（接入层职责）
    limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
}
```

### 三台服务器部署（需求 5.1.1 节）
- 应用服务器：Nginx + 前端静态资源 + 后端 jar
- 数据库服务器：MySQL 8.0（配置 Redis/ES 后由 application.yml 切换）
- 采集任务服务器：独立部署，跑数据导入管道（避免导入高峰影响在线服务）

### Redis / Elasticsearch 切换
本机未装 Redis/ES 时自动降级（缓存用 Caffeine、检索用 MySQL LIKE）。安装后：
- 缓存：`anime.cache.type: redis` + `spring.data.redis` 连接配置
- 检索：`anime.search.type: elasticsearch` + ES 7.10.x 连接配置

## 开发注意事项（实测踩坑记录）

1. **SERVER_PORT 环境变量会覆盖** application.yml 的 `server.port`——端口异常时检查系统环境变量
2. **路径含非 ASCII 字符**（`—` 等）：PowerShell 5.1 按 GBK 误读导致命令行脚本损坏——脚本需存为 UTF-8 BOM；`mvn spring-boot:run` 无法运行时改 `package + java -jar`；SQL/jar 遇路径问题时复制到 %TEMP% 纯 ASCII 路径执行
3. **Vite 改代码后页面不更新**：非 ASCII 路径下文件监听失效——重启 `npm run dev` 即可（本工程已内置「构建代次戳 + no-store」根治依赖缓存混用，但源码热更新仍受监听影响）
4. **MySQL 保留字**：`rating` 等表名在拼接 SQL 时须加反引号
5. **后台标签页**：`requestAnimationFrame` 会被暂停，动画初始值不要放在 rAF 回调里

## 需求覆盖对照

| 需求文档条目 | 实现位置 |
|---|---|
| 4.2.3.1 换一批 | RecommendServiceImpl.refreshBatch |
| 4.2.3.2 拼音搜索联想 | SearchBar + AnimeService.suggest |
| 4.2.3.3 追番更新提醒 | Notification + BaseLayout 红点 |
| 4.2.3.5 排行榜四因子 | RankServiceImpl（0.35/0.30/0.20/0.15） |
| 4.2.3.6 链接失效处理 | JumpService HEAD 校验 + PlatformJump 标红 |
| 4.2.3.8 批量导入导出 | AdminService POI（管道-过滤器） |
| 4.3.3 时间特性 | Caffeine/Redis 缓存命中秒级响应 |
| 4.5 账号安全 | BCrypt + 5 次锁定 30 分钟 + token 2h |
| 3.4.1 数据闭环 | 评分/收藏/追番/跳转 → behavior_count → 推荐升级 |
| 3.4.3 冷启动降级 | 兴趣标签过滤 + 热门补偿（< 20 条行为） |
| 3.5 采集合规 | 白名单跳转 + 人工审核队列，不做真实爬虫 |
| 5.4 四区域布局 | BaseLayout（60/200/自适应/44px） |
| 用户提交番剧闭环 | suggest → review(EDIT) → 上架 → 用户中心查询 |

---

项目交付：Agito（GitHub: Krsxf/Agito）· 课程设计小组 · 2026.11

> AI生成
