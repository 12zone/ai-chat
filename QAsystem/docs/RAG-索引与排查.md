# RAG 向量索引与排查

本文说明本项目中「知识库 → 向量检索 → 对话」的数据流、如何重建索引、以及数据库与 SQL 侧的常见优化思路。

## 1. 数据流概览

1. **文件入库**：上传或导入后，正文写入 `files` 表（`content` 等字段）。
2. **向量化**：将 `content` 按固定大小切块，调用 **OpenAI Embeddings API**（配置见 `spring.ai.openai`、`app.embedding.openai.model`）得到向量。
3. **索引存储**：
   - 当前实现中，在 `milvus` 配置下若未连上 Milvus，会使用 **内存中的 fallback 索引**（进程内 Map，重启后清空）。
   - 因此 **每次应用启动或手动「重建向量索引」** 都会从数据库重新拉取文件并嵌入，保证检索有数据（前提是 **API Key 有效、网络可达、模型名正确** 且 `content` 非空）。

## 2. 何时需要重建索引

- 应用重启后对话「搜不到」刚入库的内容。
- 曾出现嵌入失败（配额、鉴权、网络），部分文件未进索引。
- 批量 SQL 修改了 `files.content`，未走应用上传逻辑。
- **更换嵌入模型**（`app.embedding.openai.model`）或向量维度（`app.vector-search.dimension`）后，必须 **全量重建**，否则维度不一致无法检索。

**操作方式**：

- **HTTP（管理员）**：`POST /api/ingestion/reindex` — 全量从数据库重建内存向量索引。  
- **HTTP（管理员）**：`GET /api/ingestion/index-stats` — 查看库中文件数与当前内存中向量块数量。  
- **前端**：文件管理页（有写权限时）提供「重建向量索引」「索引统计」按钮。

## 3. 前置条件

- 在 `application.yml` 或环境中配置 **`spring.ai.openai.api-key`**；可选 **`spring.ai.openai.base-url`**（兼容 OpenAI 官方或代理，须符合 `/v1/embeddings` 路径）。
- **`app.embedding.openai.model`** 与 **`app.vector-search.dimension`** 与实际模型输出维度一致（默认 `text-embedding-ada-002` 为 **1536** 维）。
- 数据库中待索引行的 **`content` 不为空**（空内容不会产生有效块）。

### 3.2 重建后 `indexedChunkCount` 仍为 0（有文件但无块）

接口 `POST /api/ingestion/reindex` 成功时，响应 `data` 中除 `dbFileCount`、`indexedChunkCount` 外，还有：

| 字段 | 含义 |
|------|------|
| `filesProcessed` | 本次循环中从库取到的未删文件条数（与分页遍历一致） |
| `filesSkippedEmptyContent` | `content` 为空或仅空白，未调用嵌入 |
| `filesSkippedException` | 单文件嵌入或索引过程抛错（见日志 `reindex skip`） |

**对照思路**：若 `dbFileCount > 0` 且 `indexedChunkCount == 0`，先看 `filesSkippedEmptyContent` 是否等于 `filesProcessed`（说明正文全空）；否则重点看 `filesSkippedException` 与后端日志中的 **OpenAI** 报错（401/429、响应体错误字段等）。

**Hibernate 日志预期**：全量重建且每批 1 个文件时，应对每个未删文件各出现一次 `select ... from files ... limit 1`（及 offset 递增）。若只有 `count` 与极少次 `files` 查询，需确认请求是否真正跑完、库中 `is_deleted` 是否为 false。

**嵌入自检（curl，勿提交真实 Key 到公共环境）**：将 `YOUR_KEY` 替换为有效 API Key，模型与配置一致：

```bash
curl -sS https://api.openai.com/v1/embeddings \
  -H "Authorization: Bearer YOUR_KEY" \
  -H "Content-Type: application/json" \
  -d '{"model":"text-embedding-ada-002","input":"test"}'
```

若使用代理，将 URL 换为与 `spring.ai.openai.base-url` 一致的 host + `/embeddings`。正常响应含 `data[0].embedding` 数组。

### 3.1 重建时出现 `OutOfMemoryError: Java heap space`

- **原因**：一次性 `SELECT` 出所有文件的 `content` 再在堆上建向量，峰值内存 ≈ 全部正文 + 全部向量块；文件多或单篇极大时容易撑爆堆。
- **实现对策**：重建改为 **分页从库加载**（当前为每批 1 个文件），且分页查询使用 **独立只读事务**（`REQUIRES_NEW`），避免 Open-Session-In-View 在一次 HTTP 请求里长期持有所有已加载实体；**全量重建全局互斥**，避免与启动时的异步重建并发。索引块向量使用 **`float[]`** 降低常驻堆占用；单篇正文超过约 **28 万字符** 时仅索引前缀；切块窗口略增大以减少块数量。
- **仍 OOM 时**：说明 **索引常驻内存** 已超过堆上限，需 **增大 JVM `-Xmx`**，或改为 **Milvus 等外置向量库**、减少入库正文体积。

## 4. `files` 表与 SQL 优化思路

以下为通用建议，具体列名以你库中 `File` 实体 / 迁移脚本为准。

### 4.1 查询与分页

- 列表、管理端分页：对 **排序字段、筛选字段** 建索引（如 `upload_time`、`category`、`deleted`）。
- 避免 `SELECT *` 在大表上全表扫描；仅选列表需要的列。

### 4.2 全文与 RAG

- 若需 **关键词检索** 与向量混合：可对标题、正文做 **FULLTEXT**（MySQL InnoDB 8+）或专用搜索引擎；向量侧仍走嵌入索引。
- **大文本**：`content` 很长时，注意单行大小与备份策略；历史版本可考虑拆表（主表 + 正文扩展表）。

### 4.3 维护

- 定期 `ANALYZE TABLE` / 统计信息更新（视数据库而定），便于优化器选择索引。
- 批量更新 `content` 后，务必执行一次 **重建向量索引**（或等待启动时的引导任务，若已配置）。

## 5. 常见问题

| 现象 | 可能原因 | 处理 |
|------|----------|------|
| 对话检索不到文档 | 内存索引未构建或已清空 | 调 `POST /api/ingestion/reindex` 或重启后等待引导任务 |
| `indexedChunkCount` 为 0 | 无文件、或 `content` 全空、或嵌入全部失败 | 看 `filesSkipped*`、§3.2、检查 api-key / 模型 / 网络 |
| 重建很慢 | 全量嵌入、块数多、OpenAI 限流 | 属正常；注意配额与重试策略 |
| 403 | 非管理员调用管理接口 | 使用管理员账号或调整 `SecurityConfig`（按安全策略） |
| 重建时 OOM / Tomcat Poller 报 heap | 索引总量超过堆 | 已分页重建；仍失败则加大 `-Xmx` 或外置向量库 |
| 前端「超时」断开 | 代理或网关超时过短 | 前端 `timeout: 0`；开发代理 `proxyTimeout: 0`；生产调大网关读超时 |

## 6. 相关代码入口（便于阅读）

- 嵌入：`OpenAiEmbeddingService`
- 引导 / 全量重建逻辑：`IngestionService` / `IngestionServiceImpl`
- HTTP：`IngestionController`（`/api/ingestion/reindex`、`/api/ingestion/index-stats`）
- 向量与 fallback：`VectorSearchServiceImpl`
- 权限：`SecurityConfig` 中对上述路径的 `ADMIN` 配置

---

*文档随实现演进，若接口或存储从「纯内存」改为 Milvus 持久化，请同步更新本节「索引存储」与重建说明。*
