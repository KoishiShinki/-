# Chronicle Studio · 世界线档案馆

把游戏截图整理成世界线，再写成可以阅读的故事。支持截图识别、事件与阶段管理、AI 章节、角色参考图、插图生成、世界线问答和公开阅读。

本项目从原项目中提取业务并重构为 **Vue 3 + 独立 Spring Boot API**。不依赖若依运行框架。保留世界线、AI 叙事及其业务管理后台，移除若依通用管理模块。数据库包含 12 张业务表和独立的账号、会话、媒体归属表。

## 功能

| 工作流 | 内容 |
| --- | --- |
| 世界线 | 创建、编辑、私有/公开切换、封面、统计与历史分歧 |
| 截图与事件 | 上传、AI 识别、草稿确认/驳回、事件编辑、叙事片段与事件关系 |
| 阶段与知识 | 自动分阶段、事件绑定、阶段摘要、国家状态、人物档案和参考肖像 |
| 小说与插图 | 阶段章节生成、编辑、发布、Markdown/DOCX 导出、提示词和图片生成、采用/弃用、章节封面 |
| 问答与任务 | 世界线上下文问答、历史记录、任务状态及实际失败重试 |
| 公开与管理 | 公开世界线/章节/插图库、个人资料、内容管理与 AI 配置状态 |

## 快速体验

需要 JDK 17+、Maven 3.9+、Node.js 22.12+。以下方式使用**临时内存数据库**，重启后数据消失，适合体验及开发验证。

在后端目录运行：

```sh
cd backend
mvn spring-boot:run "-Dspring-boot.run.useTestClasspath=true" "-Dspring-boot.run.profiles=test"
```

另一个终端启动前端：

```sh
cd frontend
npm ci
npm run dev
```

打开终端显示的本地地址，默认 `http://localhost:5173`。先注册账号；密码至少 12 个字符。AI 默认关闭，未配置时会明确返回不可用，不生成伪造结果。

需要业务管理后台时，在后端启动前设置 `ADMIN_USERNAME` 与 `ADMIN_PASSWORD` 环境变量。仅在该用户名不存在时创建管理员，不覆盖已有账号，也没有预置密码。

## MySQL 与 Docker

本地 MySQL 使用 `backend/src/main/resources/schema.sql` 初始化。创建数据库时使用 `utf8mb4` 字符集和 `utf8mb4_0900_as_cs` 排序规则，保留用户名大小写语义。设置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 后执行 `mvn spring-boot:run`，不启用 test profile。示例连接地址：

```text
jdbc:mysql://127.0.0.1:3306/chronicle?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC
```

也可在安装 Docker Compose 后启动完整环境：

```sh
cp .env.example .env
# 编辑 .env，为数据库配置独立口令；管理员配置按需填写。
docker compose up --build -d
```

Windows PowerShell 用 `Copy-Item .env.example .env` 复制配置。Compose 地址默认 `http://localhost:8088`，数据库及上传目录保存在独立卷中。生产入口使用 HTTPS，并将 `APP_ORIGIN` 与 `SECURE_COOKIE` 设置为真实部署值。

## 关键配置

| 环境变量 | 用途 |
| --- | --- |
| `PORT` | API 端口，默认 8080 |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | MySQL 连接，只在服务端配置 |
| `DB_INIT` | 是否初始化缺失表，默认 `always`；后续结构演进需显式迁移 |
| `STORAGE_ROOT` | 上传及生成图片目录，默认 `./data/uploads` |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | 可选管理员引导，只用于首次创建 |
| `APP_ORIGIN` / `SECURE_COOKIE` | 浏览器来源与 HTTPS Cookie 设置 |
| `AI_ENABLED` | 启用真实 AI 服务，默认 `false` |
| `SILICONFLOW_API_KEY` / `SILICONFLOW_BASE_URL` | 文本与视觉接口配置 |
| `TEXT_MODEL` / `VISION_MODEL` | 文本与视觉模型；按账户可用模型配置 |
| `OPENAI_API_KEY` / `OPENAI_BASE_URL` / `IMAGE_MODEL` | 兼容图片生成/编辑接口的配置 |
| `VITE_PROXY_TARGET` | 前端开发代理目标，默认 `http://127.0.0.1:8080` |

根目录 `.env` 由 Docker Compose 读取；直接运行 Maven 时请自行设置环境变量。AI 密钥不能放入任何 `VITE_` 变量。管理员页面显示配置状态，不提供把密钥写入数据库的接口。

Windows 本地环境可把私有配置保存在源码目录的同级 `<项目目录名>-local/.env` 中，分别在两个终端运行：

```powershell
.\scripts\start-local.ps1 -Service api
.\scripts\start-local.ps1 -Service web
```

也可用 `-EnvironmentFile` 指定源码目录外的配置文件。脚本只读取已知的环境变量，不执行配置内容。已有数据库完成初始化后，建议设置 `DB_INIT=never`，并使用仅具有业务表读写权限的专用数据库账号。

## 验证与发布

```sh
python scripts/test_release_check.py
python scripts/release_check.py
```

后端执行 `mvn verify`，前端执行 `npm test` 和 `npm run build`。测试使用合成数据、本地模拟 AI 服务与内存数据库；不需要真实 API 密钥。当前验收结果及限制见 [验证记录](docs/VALIDATION.md)。

生成不含构建产物、运行数据及旧 Git 历史的发行包：

```sh
python scripts/package_release.py --output ../chronicle-studio-source.zip
```

- [本地数据迁移](docs/MIGRATION.md)
- [架构与边界](docs/ARCHITECTURE.md)
- [功能与接口对应](docs/API_COMPATIBILITY.md)
- [开源发布检查](docs/OPEN_SOURCE_CHECKLIST.md)
- [安全报告](SECURITY.md) 与 [第三方许可](THIRD_PARTY_NOTICES.md)

原有 `/public/action/view`、`like`、`favorite` 是未实现持久化的预留接口，不计为点赞/收藏功能。原 AI 配置的空保存接口已改为明确拒绝；服务配置通过环境变量管理。真实 AI 模型的账户权限、费用和内容结果仍需部署者使用自己的配置验证。

采用目标公开仓库选择的 Apache License 2.0，相关来源代码的原 RuoYi MIT 归属声明保留在 `licenses/RuoYi-MIT.txt`。保留许可证不代表应用依赖若依运行时。指定的设计与性能 skill 仅在本次重构会话临时使用，未安装到全局，也未打包到项目。
