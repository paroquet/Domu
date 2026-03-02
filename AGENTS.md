# Domu 家庭餐饮应用 — 技术方案

## 项目概述

家庭餐饮管理应用，支持菜谱管理、家庭成员点菜、买菜计划生成等功能。

---

## 功能模块

### 1. 用户系统
- 邮箱 + 密码注册/登录（无第三方登录）
- 创建家庭、通过邀请码邀请成员加入
- 查看个人信息和家庭信息

### 2. 菜谱功能
- 菜谱的增删改查（家庭所有成员均可操作）
- 菜谱支持图片/封面（本地存储）
- 通过链接公开分享菜谱（无需登录可访问）
- 做菜记录：做菜时间、使用菜谱、心得、图片

### 3. 点菜功能
- 为家庭成员点菜
- 查看家庭成员点菜记录
- 根据点菜菜谱聚合生成买菜计划

---

## 技术栈

| 层次 | 技术 |
|------|------|
| 前端 | React + TypeScript + Vite |
| UI 组件 | shadcn/ui + Tailwind CSS |
| 状态管理 | Zustand + React Query |
| PWA | vite-plugin-pwa |
| 后端 | Spring Boot 4 + Kotlin |
| 认证 | Spring Security + JWT（HttpOnly Cookie）|
| ORM | Spring Data JPA + Hibernate |
| 数据库 | SQLite（Volume 挂载持久化）|
| 容器化 | Docker 单容器 + Docker Compose |

---

## 部署架构与构建部署

### 运行架构

```
docker-compose up
        │
        └── domu-app（单容器）
              ├── /api/v1/**     → REST API
              ├── /files/**      → 图片静态文件服务
              └── /**            → React SPA（含 PWA）
```

**前端集成方式**：React 构建产物通过 Gradle 构建钩子复制到 `resources/static/`，打入 Spring Boot JAR，由 Spring Boot 同时提供静态文件服务和 API 服务。

---

### 本地开发（dev）

**后端**：激活 `dev` profile，数据目录为项目根目录下 `./data`

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=dev'
# 数据库: ./data/domu.db
# 上传图片: ./data/uploads/
```

配置文件：`application-dev.yml`
```yaml
spring:
  datasource:
    url: jdbc:sqlite:./data/domu.db
  jpa:
    show-sql: true
app:
  upload:
    dir: ./data/uploads
  base-url: http://localhost:8080
```

**前端**：`vite.config.ts` 在 dev server 模式下自动将 `/api` 和 `/files` 请求转发到后端 `localhost:8080`，无需额外配置

```bash
cd frontend
npm run dev    # 代理转发到 localhost:8080
```

---

### 服务端部署（prod）

**多阶段 Docker 构建**：

参考 `./Dockerfile`

**后端 prod profile**，数据目录为 `/app/data`（Docker Volume 挂载）：

配置文件：`./backend/src/main/resources/application-prod.yml`

**前端**：prod 构建产物由 Spring Boot 直接提供静态服务，API 请求同源，无需代理转发。

**启动**：

```bash
cp .env.example .env    # 配置 JWT_SECRET
# 参考 ./docker-compose.yml
docker compose up -d
# 数据持久化位置（宿主机）:
#   ./data/domu.db        SQLite 数据库
#   ./data/uploads/       上传的图片
```

---

## 数据模型

```sql
-- 用户
users
  id            INTEGER PRIMARY KEY
  email         TEXT UNIQUE NOT NULL
  password_hash TEXT NOT NULL
  name          TEXT NOT NULL
  avatar_path   TEXT
  created_at    DATETIME

-- 家庭
families
  id          INTEGER PRIMARY KEY
  name        TEXT NOT NULL
  invite_code TEXT UNIQUE NOT NULL   -- UUID，用于邀请
  created_at  DATETIME

-- 家庭成员
family_members
  family_id  INTEGER REFERENCES families
  user_id    INTEGER REFERENCES users
  role       TEXT NOT NULL           -- ADMIN | MEMBER
  joined_at  DATETIME
  PRIMARY KEY (family_id, user_id)

-- 菜谱
recipes
  id               INTEGER PRIMARY KEY
  title            TEXT NOT NULL
  description      TEXT
  ingredients      TEXT             -- JSON: [{name, amount, unit}]
  steps            TEXT             -- JSON: [{order, description, image_path}]
  cover_image_path TEXT
  author_id        INTEGER REFERENCES users
  family_id        INTEGER REFERENCES families
  share_token      TEXT UNIQUE      -- UUID，公开分享链接
  created_at       DATETIME
  updated_at       DATETIME

-- 做菜记录
cooking_records
  id         INTEGER PRIMARY KEY
  recipe_id  INTEGER REFERENCES recipes
  user_id    INTEGER REFERENCES users
  family_id  INTEGER REFERENCES families
  cooked_at  DATETIME
  notes      TEXT
  images     TEXT                   -- JSON: [image_path]
  created_at DATETIME

-- 点菜
orders
  id           INTEGER PRIMARY KEY
  family_id    INTEGER REFERENCES families
  ordered_by   INTEGER REFERENCES users
  ordered_for  INTEGER REFERENCES users
  recipe_id    INTEGER REFERENCES recipes
  planned_date DATE
  status       TEXT                 -- PENDING | DONE | CANCELLED
  created_at   DATETIME
```

> 买菜计划不单独存表，按日期聚合 orders 的 recipe.ingredients 实时计算。

---

## 权限模型

| 操作 | ADMIN | MEMBER |
|------|-------|--------|
| 查看家庭信息 | ✅ | ✅ |
| 邀请/踢出成员、修改成员角色 | ✅ | ❌ |
| 菜谱 增/删/改/查 | ✅ | ✅ |
| 做菜记录 增/删/改/查 | ✅ | ✅ |
| 点菜 | ✅ | ✅ |
| 查看买菜计划 | ✅ | ✅ |

---

## 项目结构

```
domu/
├── frontend/                        # React 前端
│   ├── src/
│   │   ├── pages/
│   │   │   ├── auth/                # 登录 / 注册
│   │   │   ├── recipes/             # 菜谱列表 / 详情 / 编辑
│   │   │   ├── cooking-records/     # 做菜记录
│   │   │   ├── family/              # 家庭管理
│   │   │   ├── orders/              # 点菜 + 买菜计划
│   │   │   └── share/               # 公开分享页（无需登录）
│   │   ├── components/
│   │   ├── api/                     # axios + React Query hooks
│   │   └── stores/                  # Zustand（auth / family 状态）
│   ├── public/
│   └── vite.config.ts
│
├── backend/                         # Spring Boot 后端（Kotlin）
│   └── src/main/kotlin/com/domu/
│       ├── config/                  # SecurityConfig, JwtConfig, WebConfig
│       ├── controller/              # REST Controllers
│       ├── service/
│       ├── repository/              # Spring Data JPA Repositories
│       ├── model/                   # JPA Entities（data class）
│       ├── dto/                     # Request / Response DTOs（data class）
│       └── exception/
│
├── docker-compose.yml
├── Dockerfile
└── build.gradle.kts                 # Kotlin DSL，构建时先 npm build，再打 JAR
```

---

## API 接口设计

### 认证
```
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/logout
POST /api/v1/auth/refresh
```

### 用户
```
GET  /api/v1/users/me
PUT  /api/v1/users/me
```

### 家庭
```
POST   /api/v1/families                              # 创建家庭
GET    /api/v1/families/{id}                         # 家庭详情
POST   /api/v1/families/{id}/invite-code             # 生成邀请码（ADMIN）
POST   /api/v1/families/join                         # 用邀请码加入
GET    /api/v1/families/{id}/members                 # 成员列表
PUT    /api/v1/families/{id}/members/{uid}/role      # 修改角色（ADMIN）
DELETE /api/v1/families/{id}/members/{uid}           # 踢出成员（ADMIN）
```

### 菜谱
```
GET    /api/v1/recipes                               # 家庭菜谱列表
POST   /api/v1/recipes                               # 创建菜谱
GET    /api/v1/recipes/{id}                          # 菜谱详情
PUT    /api/v1/recipes/{id}                          # 编辑菜谱
DELETE /api/v1/recipes/{id}                          # 删除菜谱
POST   /api/v1/recipes/{id}/share                    # 生成公开分享链接
GET    /api/v1/recipes/shared/{token}                # 公开访问（无需登录）
```

### 图片
```
POST   /api/v1/files/upload                          # 上传图片，返回路径
GET    /files/{filename}                             # 访问图片（静态服务，无版本号）
```

### 做菜记录
```
GET    /api/v1/cooking-records                       # 列表（支持按菜谱/用户筛选）
POST   /api/v1/cooking-records                       # 创建记录
GET    /api/v1/cooking-records/{id}
PUT    /api/v1/cooking-records/{id}
DELETE /api/v1/cooking-records/{id}
```

### 点菜 & 买菜计划
```
GET    /api/v1/orders?date=YYYY-MM-DD               # 某天的点菜列表
POST   /api/v1/orders                               # 点菜
PUT    /api/v1/orders/{id}/status                   # 更新状态
DELETE /api/v1/orders/{id}                          # 取消
GET    /api/v1/orders/shopping-plan?date=YYYY-MM-DD # 聚合买菜清单
```

## 注意事项
