# Chronicle Studio · 世界线档案馆

[![测试与构建](https://github.com/KoishiShinki/-/actions/workflows/ci.yml/badge.svg)](https://github.com/KoishiShinki/-/actions/workflows/ci.yml)
[使用指南](#快速开始) · [AI 配置](#接入-ai) · [开发文档](docs/DEVELOPMENT.md) · [问题反馈](https://github.com/KoishiShinki/-/issues)

**把一局游戏，写成一个世界的历史。**

Chronicle Studio 是一个可以自行部署的世界线记录与 AI 叙事应用，适合记录策略游戏进程、整理架空历史和创作世界观故事。上传游戏截图，确认识别出的事件，再把时间线组织成阶段、人物档案和小说章节；完成的作品可以通过公开阅读页面分享。

前端使用 Vue 3，后端使用独立的 Spring Boot API。账号、世界线与图片保存在自己的数据库和文件存储中。

## 可以做什么

| 功能 | 使用方式 |
| --- | --- |
| 世界线与时间线 | 管理多条世界线，记录关键事件、历史分歧及事件之间的关系 |
| 截图识别 | 从游戏截图中提取事件草稿，人工确认或修改后写入时间线 |
| 世界观档案 | 整理国家状态、阵营、人物经历与角色参考肖像 |
| AI 叙事 | 按阶段生成摘要和小说章节，继续编辑正文、标题与写作风格 |
| 插图创作 | 生成插图、使用角色参考图，选择采用的图片并设置章节封面 |
| 阅读与导出 | 公开世界线和章节，浏览已采用的插图，导出 Markdown 或 Word 文档 |
| 问答与管理 | 围绕世界线内容提问，查看 AI 任务状态，管理自己的创作与站点业务内容 |

创作流程：

```mermaid
flowchart LR
    A["上传游戏截图"] --> B["识别并确认事件"]
    B --> C["整理时间线与阶段"]
    C --> D["编写章节与生成插图"]
    D --> E["公开阅读或导出作品"]
```

## 快速开始

### 本地体验

准备 **JDK 17+、Maven 3.9+、Node.js 22.12+**，然后获取源码：

```sh
git clone https://github.com/KoishiShinki/-.git chronicle-studio
cd chronicle-studio
```

在仓库根目录打开两个终端。第一个终端启动后端：

```sh
cd backend
mvn spring-boot:run "-Dspring-boot.run.useTestClasspath=true" "-Dspring-boot.run.profiles=test"
```

第二个终端启动前端：

```sh
cd frontend
npm ci
npm run dev
```

访问 **http://localhost:5173**，注册账号并创建第一条世界线。注册密码至少需要 12 个字符。

此方式使用 H2 内存数据库，**重启后数据库内容会清空**，适合体验和开发。需要长期保存作品时，请使用下面的 MySQL 部署方式。

### Docker Compose 部署

准备 Docker 和 Docker Compose，在仓库根目录复制配置：

```sh
cp .env.example .env
```

PowerShell 使用 `Copy-Item .env.example .env`。编辑 `.env`，填写 `DB_PASSWORD` 和 `MYSQL_ROOT_PASSWORD`，然后启动：

```sh
docker compose up --build -d
```

访问 **http://localhost:8088**。Compose 会启动前端、后端和 MySQL，数据库与上传图片分别存放在持久化卷中。

需要管理后台时，在首次启动前填写 `ADMIN_USERNAME` 和 `ADMIN_PASSWORD`；该用户名不存在时才会创建管理员。已注册账号不会因为修改这两个变量而自动升为管理员。

对外部署时使用 HTTPS，并设置与访问地址一致的 `APP_ORIGIN` 和 `SECURE_COOKIE=true`。

### 使用已有 MySQL

创建 MySQL 8 数据库，使用 `utf8mb4` 字符集与 `utf8mb4_0900_as_cs` 排序规则。设置后端环境变量 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`，再运行 `mvn spring-boot:run`，无需启用 `test` profile。

连接地址示例：

```text
jdbc:mysql://127.0.0.1:3306/chronicle?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC
```

初次启动根据 [schema.sql](backend/src/main/resources/schema.sql) 创建缺失的表。初始化完成后可设置 `DB_INIT=never`，并为日常运行配置仅具有业务库读写权限的数据库账号。

从旧版迁移账号、世界线和图片，请参阅 [迁移指南](docs/MIGRATION.md)。

## 接入 AI

AI 默认关闭。未配置时可以手动管理世界线和阅读作品；截图识别、内容生成、问答和图片生成需要接入相应服务。

| 配置 | 用途 |
| --- | --- |
| `AI_ENABLED=true` | 启用 AI 功能 |
| `SILICONFLOW_API_KEY`、`SILICONFLOW_BASE_URL` | 文本与视觉服务的密钥和接口地址 |
| `TEXT_MODEL`、`VISION_MODEL` | 文本生成与截图识别使用的模型 |
| `OPENAI_API_KEY`、`OPENAI_BASE_URL`、`IMAGE_MODEL` | 兼容 OpenAI 图片生成及编辑接口的服务 |

Docker Compose 从根目录 `.env` 读取配置；直接运行 Maven 时需要设置进程环境变量，Maven 不会自动加载该文件。修改配置后重启后端。

根据所用服务的模型可用性、接口格式和额度选择配置。图片编辑还需要提供方支持参考图上传。**密钥只配置在后端，不要放入 `VITE_` 变量、前端源码或 Git 提交。**

## 配置参考

完整模板见 [.env.example](.env.example)，常用选项如下：

| 环境变量 | 默认值或用途 |
| --- | --- |
| `PORT` | 后端端口，默认 `8080` |
| `APP_PORT` | Compose 对外端口，默认 `8088` |
| `DB_INIT` | `always` 创建缺失的表；`never` 关闭启动初始化 |
| `STORAGE_ROOT` | 图片存储目录，默认 `./data/uploads` |
| `APP_ORIGIN` | 前端访问地址；本地开发默认 `http://localhost:5173` |
| `SECURE_COOKIE` | 使用 HTTPS 时设为 `true` |
| `VITE_PROXY_TARGET` | 前端开发代理目标，默认 `http://127.0.0.1:8080` |

Windows 用户还可以将后端配置放在仓库之外的私有文件中，使用 [本地启动脚本](scripts/start-local.ps1)。具体用法见 [开发指南](docs/DEVELOPMENT.md)。

## 技术与文档

- **前端**：Vue 3、Vue Router、Vite、Axios。
- **后端**：Spring Boot 3、Spring Security、MyBatis。
- **数据与文件**：MySQL 8、本地文件存储；H2 用于开发体验和自动化测试。

| 文档 | 内容 |
| --- | --- |
| [架构说明](docs/ARCHITECTURE.md) | 模块边界、认证、媒体访问及接口约定 |
| [开发指南](docs/DEVELOPMENT.md) | 本地配置、测试命令与贡献方式 |
| [迁移指南](docs/MIGRATION.md) | 从旧版迁移业务数据和账号 |
| [接口兼容说明](docs/API_COMPATIBILITY.md) | 业务接口与兼容范围 |
| [发布检查](docs/OPEN_SOURCE_CHECKLIST.md) | 维护者发布前的源码与配置检查 |
| [安全说明](SECURITY.md) | 部署注意事项与漏洞报告方式 |

欢迎通过 [Issues](https://github.com/KoishiShinki/-/issues) 反馈问题或提出建议，也欢迎提交 Pull Request。问题复现请使用合成数据，并移除密码、密钥和私人创作内容。

## 许可证

项目使用 [Apache License 2.0](LICENSE)。来源于 RuoYi 的相关代码保留 [原 MIT 许可与版权声明](licenses/RuoYi-MIT.txt)，其他依赖适用各自的许可证，详见 [第三方声明](THIRD_PARTY_NOTICES.md)。
