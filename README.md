# AI Chat（QAsystem）（1.0）

基于 **RAG（检索增强生成）** 的规章制度问答系统：后端负责文档入库、向量检索与大模型对话；前端提供登录、对话、文件管理与个人资料等页面。仓库内为 **前后端分离** 的两个子项目。

| 目录 | 说明 |
|------|------|
| [QAsystem](QAsystem/) | Spring Boot 3.5 + Java 17 后端 API |
| [qasystem-web](qasystem-web/) | Vue 3 + Vite 7 前端 |

---

## 功能概览

- **用户与权限**：JWT 无状态认证；角色 `ADMIN` / `USER`（导入、部分文件写操作等仅管理员）。
- **对话**：结合向量检索上下文，支持 OpenAI / DeepSeek 等（见 Spring AI 配置）。
- **知识库**：文件上传与列表；向量化与检索（Milvus 或本地 fallback，见配置与文档）。
- **数据导入**：可选在 `QAsystem/src/main/resources/data.json` 放置导入数据（该文件默认 **不提交 Git**

---

## 环境要求

| 组件 | 版本或说明 |
|------|------------|
| JDK | 17 |
| Maven | 使用项目自带 `mvnw` / `mvnw.cmd` 即可 |
| Node.js | `^20.19.0` 或 `>=22.12.0`（见 `qasystem-web/package.json`） |
| MySQL | 与 `application.yml` 中数据源一致；需先创建数据库（如 `qasystem`） |
| 大模型与嵌入 | 配置 **DeepSeek**（对话）与 **OpenAI**（嵌入等）的 API Key |

---

## 后端（QAsystem）

### 1. 配置文件

仓库中提供模板 **`QAsystem/src/main/resources/application.example.yml`**，请勿直接提交含密钥的 `application.yml`。

在 `QAsystem` 目录下复制并编辑：

```bash
cd QAsystem
copy src\main\resources\application.example.yml src\main\resources\application.yml
```

按需填写：

- `spring.datasource.*`：MySQL 连接
- `spring.ai.openai.api-key`、`spring.ai.deepseek.api-key`
- `app.security.jwt.secret-base64`：JWT 签名用 Base64（HS256 要求解码后长度 ≥ 32 字节；可用 `openssl rand -base64 48` 生成）

### 2. 启动

```bash
cd QAsystem
.\mvnw.cmd spring-boot:run
```

默认端口 **8080**

### 3. 默认账号

首次启动若数据库中无对应用户，会自动创建：

| 用户名 | 默认密码 | 角色 |
|--------|------------|------|
| `admin` | `admin123` | 管理员 + 用户 |
| `user` | `user123` | 普通用户 |

---

## 前端（qasystem-web）

### 1. 安装与开发

```bash
cd qasystem-web
npm install
npm run dev
```

开发环境下，Vite 将 **`/api` 代理到 `http://127.0.0.1:8080`**（见 `vite.config.js`），请保持后端已启动。

### 2. 生产构建

```bash
npm run build
```

产物在 `dist/`，可部署到任意静态资源服务器，并确保 **`/api` 请求**能转发到后端（或由网关统一路由）。

---

## API 前缀说明

后端接口以 **`/api`** 为前缀，例如：

- `/api/auth/**`：注册、登录等（匿名可访问部分路径）
- `/api/chat/**`：对话（需登录）
- `/api/files/**`：文件相关
- `/api/ingestion/**`：入库、重建索引等（部分仅管理员）

具体权限规则见 `SecurityConfig`。

---

## 常见问题

1. **前端连不上后端**：确认本机 `8080` 已监听，且浏览器访问的是 Vite 开发地址（由代理转发 `/api`）。
2. **对话或检索无结果**：检查 OpenAI Key、嵌入模型名与向量维度是否一致；必要时使用管理员账号调用「重建向量索引」（暂未配置key  1.0）
