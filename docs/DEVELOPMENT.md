# 开发指南

## 项目结构

```text
backend/    Spring Boot API、业务逻辑、数据库结构和集成测试
frontend/   Vue 页面、路由、请求封装和前端测试
docs/       架构、迁移、开发和发布文档
scripts/    本地启动、迁移计划生成和发布检查工具
```

前端开发服务器监听 `localhost:5173`，通过代理将 `/api` 请求转发到后端并移除该前缀；`/profile` 用于图片访问。默认后端端口为 `8080`，可通过 `PORT` 修改，同时相应调整 `VITE_PROXY_TARGET`。

## 本地配置

根目录 `.env` 由 Docker Compose 读取。直接运行 Maven 时，请使用进程环境变量；不应将真实配置写入 `application.yml`。

Windows 可以使用 `scripts/start-local.ps1` 加载仓库外的私有配置。脚本默认读取与源码目录同级的 `<项目目录名>-local/.env`，也可以用 `-EnvironmentFile` 指定路径。例如，项目目录为 `chronicle-studio` 时，可以创建同级 `chronicle-studio-local/.env`，内容为：

```dotenv
PORT=8080
DB_URL=jdbc:mysql://127.0.0.1:3306/chronicle?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC
DB_USERNAME=chronicle
DB_PASSWORD=<填写自己的数据库口令>
DB_INIT=always
STORAGE_ROOT=../../chronicle-studio-local/uploads
APP_ORIGIN=http://localhost:5173
SECURE_COOKIE=false
AI_ENABLED=false
VITE_PROXY_TARGET=http://127.0.0.1:8080
```

使用自己创建的数据库账号，并替换口令占位符。数据库初始化完成后，可以设置 `DB_INIT=never`。脚本只接受应用所用的环境变量；不要直接复制包含 MySQL 容器管理变量的 Compose 配置。

在仓库根目录的两个终端分别运行：

```powershell
.\scripts\start-local.ps1 -Service api
.\scripts\start-local.ps1 -Service web
```

指定其他配置文件时：

```powershell
.\scripts\start-local.ps1 -Service api -EnvironmentFile ..\private-config\.env
```

`STORAGE_ROOT` 的相对路径以**后端进程的工作目录**为基准。使用脚本时工作目录为 `backend`；需要将图片放在源码目录外时，使用绝对路径或相应的 `../../` 相对路径。配置内容只作为环境变量读取，不会作为脚本执行。

## 运行测试

后端：

```sh
cd backend
mvn verify
```

后端集成测试使用 H2 和本地模拟的 AI 提供方，覆盖身份认证、资源归属、公开访问、媒体权限、截图草稿处理、图片生成流程、重试和文档导出，无需真实 API 密钥。

前端：

```sh
cd frontend
npm ci
npm test
npm run build
```

前端测试覆盖请求去重、会话隔离、取消请求、登录失效处理、Blob 导出和媒体地址处理。页面或交互变更还应检查桌面和移动端的显示与操作。

在仓库根目录检查待发布源码：

```sh
python scripts/test_release_check.py
python scripts/release_check.py
```

发布检查需要 Python 3.10+，只输出问题位置与类别。目录符号链接用例在系统不允许创建符号链接时会跳过。GitHub Actions 会执行发布检查、后端测试和前端测试与构建。

## 修改与贡献

- 提交说明使用中文，清楚描述修改结果，例如“修复章节导出时的文件名编码”。
- 修改接口时同步检查对应的前端调用、权限边界和返回结构；分页与普通响应的约定见 [架构说明](ARCHITECTURE.md)。
- 涉及业务逻辑的变更补充必要测试；测试数据使用合成内容，不添加真实账号、截图、密钥或私人作品。
- 数据库结构变更应提供迁移步骤。`schema.sql` 中的 `CREATE TABLE IF NOT EXISTS` 不会自动修改已存在的表。
- AI 模拟测试用于检查请求和业务流程。真实服务的模型支持、配额及生成结果，需要在自己的私有环境中验证。
- 提交前检查 diff，避免将依赖目录、编译产物和本地配置一起提交。

发布流程见 [发布检查清单](OPEN_SOURCE_CHECKLIST.md)。发现可能的安全问题，请按 [安全说明](../SECURITY.md) 私下报告。
