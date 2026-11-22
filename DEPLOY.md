# 追番助手 · 部署文档

> 适用版本 v1.0.0 · 目标环境 Linux（三台服务器形态）或单机演示

## 一、环境要求

| 组件 | 版本 | 必须 | 说明 |
|---|---|---|---|
| JDK | 17+ | ✅ | 后端运行 |
| MySQL | 8.0+ | ✅ | 业务数据 |
| Node.js | 18+ | ✅ | 前端构建 |
| Nginx | 1.24+ | ✅ | 接入层 |
| Redis | 6.0+ | ⭕ | 未装自动降级 Caffeine |
| Elasticsearch | 7.10.x | ⭕ | 未装自动降级 MySQL LIKE |

## 二、数据库初始化

```bash
mysql -uroot -p < sql/01_schema.sql          # 建库 anime_helper（utf8mb4）+ 20 表
mysql -uroot -p < sql/02_init_data.sql       # 演示数据（26 番剧/评分/外链/角色/标签）
mysql -uroot -p < sql/03_admin.sql           # 管理员 boss/admin123
mysql -uroot -p < sql/04_review_enhance.sql  # 审核队列增强列
```

## 三、后端部署

```bash
cd server
# 修改配置
vim src/main/resources/application.yml
#   spring.datasource.username/password
#   anime.upload.dir（封面等上传目录，建议指向持久化磁盘，如 /data/anime-helper/upload）
#   切换 Redis：anime.cache.type: redis + spring.data.redis.host/port
#   切换 ES：anime.search.type: elasticsearch + spring.elasticsearch.uris

# 打包（路径含非 ASCII 字符时，先复制工程到纯 ASCII 路径再执行）
mvn package -DskipTests

# 运行（注意 SERVER_PORT 环境变量会覆盖 application.yml 端口，必要时显式指定）
SERVER_PORT=8080 java -jar target/anime-helper-server-1.0.0.jar
```

验证：`curl http://localhost:8080/api/anime/hot-words` 返回 `code:0`；接口文档 `http://localhost:8080/doc.html`。

## 四、前端构建与部署

```bash
cd web
npm install
npm run build          # 产出 dist/
# 将 dist/ 内容发布到 Web 服务器静态目录
```

## 五、Nginx 完整配置

```nginx
# 限流定义（接入层职责，http 块）
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;
limit_req_zone $binary_remote_addr zone=static_limit:10m rate=50r/s;

server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate     /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;

    # ---------- 前端静态资源 ----------
    location / {
        root /var/www/anime-helper/dist;
        try_files $uri $uri/ /index.html;
        limit_req zone=static_limit burst=100 nodelay;
    }

    # ---------- 后端 API ----------
    location /api/ {
        limit_req zone=api_limit burst=20 nodelay;
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        client_max_body_size 10m;   # Excel 导入/封面上传
    }

    # ---------- 上传静态资源（封面，全站可见）----------
    location /uploads/ {
        # 方案A：经后端路由（零改动）
        proxy_pass http://127.0.0.1:8080;
        expires 30d;
        add_header Cache-Control "public, immutable";

        # 方案B：Nginx 直托管（性能更好，与 anime.upload.dir 指向同一目录）
        # alias /data/anime-helper/upload/;
    }
}
```

> ⚠️ `/uploads/` 必须保持与开发环境一致的路径结构：` upload/cover/xxx.png` → URL
> `/uploads/cover/xxx.png`（anime.upload.dir = /data/anime-helper/upload 时）。
> 数据库中封面存的是相对路径（`/uploads/...`），**不要**改写前缀，否则历史数据图片失效。

## 六、数据层增强（可选）

### 切换 Redis
```yaml
anime:
  cache:
    type: redis
spring:
  data:
    redis:
      host: 10.0.0.2
      port: 6379
      # password: ...
```

### 切换 Elasticsearch（7.10.x）
```yaml
anime:
  search:
    type: elasticsearch
spring:
  elasticsearch:
    uris: http://10.0.0.3:9200
```
> ES 选中 7.10.x 保持 Apache-2.0 许可（7.11 起为 Elastic License 双许可）。
> 建议配套 IK 中文分词 + 拼音插件实现中日文分词与拼音首字母搜索（需求 4.2.3.2）。

## 七、三台服务器形态（需求 5.1.1 节）

| 服务器 | 部署内容 | 说明 |
|---|---|---|
| 应用服务器 | Nginx + dist/ + 后端 jar | 对外服务 |
| 数据库服务器 | MySQL（+Redis/ES 按需） | 内网，仅应用服务器可访问 |
| 采集任务服务器 | 数据导入管道（Excel/JSON → 审核 → 入库） | 独立部署避免导入高峰影响在线服务 |

## 八、运维要点

- **日志**：Logback 滚动留存 180 天（application.yml `logging.logback.rollingpolicy`）；DB 日志按 `log_type` 筛查
- **定时任务**：每小时榜单刷新、每日 3:30 推荐全量重算（@Scheduled）；手动重算入口在后台管理
- **备份**：MySQL 每日全量 + binlog；`anime.upload.dir` 目录纳入备份（封面资产）
- **账号锁定**：密码连错 5 次锁 30 分钟（内存计数），生产多实例部署建议改 Redis 计数

## 九、常见问题

| 现象 | 原因与解法 |
|---|---|
| 后端起在非 8080 端口 | `SERVER_PORT` 环境变量覆盖了配置，显式 `SERVER_PORT=8080 java -jar` |
| Windows 下 `mvn spring-boot:run` 报找不到主类 | 工程路径含非 ASCII 字符；改 `mvn package` 后 `java -jar`，必要时 jar 复制到纯 ASCII 路径 |
| 前端改代码页面不更新 | 非 ASCII 路径下 Vite 文件监听失效；重启 `npm run dev` |
| 封面 404 | Nginx `location /uploads/` 未配置或 `anime.upload.dir` 前缀与历史数据不一致 |
| Excel 导入 413 | Nginx `client_max_body_size` 增大（已配 10m） |
| 链接全部失效误报 | HEAD 探测对部分站点不准；在后台用「修复链接」重置，或调整 JumpServiceImpl 探测策略 |

## 十、合规与安全

- 系统仅聚合元数据并提供**正版平台白名单外链跳转**，不承载视频内容
- 数据采集走「导入 + 人工审核」管道，不做真实网络爬取
- 密码 BCrypt 加密、token 2 小时、RBAC（ADMIN/USER）、平台白名单、上传文件 UUID 命名 + 扩展名白名单 + 大小限制

> AI生成
