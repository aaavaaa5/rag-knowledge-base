# RAG 知识库系统

一个用于学习和练习的 RAG 知识库项目，基于 Spring Boot 3、Spring AI、Elasticsearch 和 Vue 3，支持知识库管理、文档异步处理、向量检索、带引用来源的问答和 SSE 流式输出。

## 功能

- 知识库与文档管理
- 文档异步切片、向量化和 Elasticsearch 入库
- 基于 kNN 的知识库问答
- 回答引用来源展示
- 会话与消息历史
- SSE 文档状态推送和流式回答
- 当前支持 `.txt` 文件，后续扩展 PDF、Word、Markdown 等格式

## 架构

```text
Vue 3 + Element Plus
        |
        | REST API / SSE
        v
Spring Boot 3
  |             |
  v             v
MySQL       Elasticsearch
业务数据     切片与向量
        |
        v
Spring AI + Ollama
Embedding / Chat
```

## 技术栈

### 后端

- Java 17
- Spring Boot 3.2.5
- Spring AI 0.8.1
- MyBatis-Plus 3.5.5
- MySQL 8.x
- Elasticsearch 8.13.4
- Ollama
- Lombok

### 前端

- Vue 3
- Vite 5
- Element Plus 2
- Vue Router 4
- Axios
- pnpm

## 项目结构

```text
self_rag/
├── backend/                    # Spring Boot 后端
│   ├── src/                    # Java 源码与配置
│   └── uploads/                # 文档上传目录
├── frontend/                   # Vue 3 前端
├── docs/                       # 设计、接口和项目文档
├── db_rag.sql                  # 数据库初始化脚本
└── README.md
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- pnpm 8+
- MySQL 8+
- Elasticsearch 8.x
- Ollama

### 1. 初始化数据库

```bash
mysql -uroot -p < db_rag.sql
```

默认数据库名为 `kbs`。

### 2. 配置后端

根据本地环境修改 `backend/src/main/resources/application.yml` 中的数据库连接、Elasticsearch 和 Ollama 配置。

默认依赖地址：

- MySQL：`localhost:3306`
- Elasticsearch：`localhost:9201`
- Ollama：`localhost:11434`

### 3. 拉取模型

```bash
ollama pull nomic-embed-text
ollama pull qwen2.5:7b
```

### 4. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：`http://localhost:8080`

健康检查：

```bash
curl http://localhost:8080/api/health
```

### 5. 启动前端

```bash
cd frontend
pnpm install
pnpm dev
```

前端默认地址：`http://localhost:5173`

前端环境变量参考 `frontend/.env.example`：

```env
VITE_API_BASE_URL=http://localhost:8080
```

## 核心接口

| 方法 | 接口 | 说明 |
| --- | --- | --- |
| GET | `/api/health` | 健康检查 |
| GET | `/api/knowledge-bases` | 查询知识库 |
| POST | `/api/knowledge-bases` | 创建知识库 |
| DELETE | `/api/knowledge-bases/{id}` | 删除知识库 |
| GET | `/api/knowledge-bases/{kbId}/documents` | 查询文档 |
| POST | `/api/knowledge-bases/{kbId}/documents` | 上传文档 |
| DELETE | `/api/knowledge-bases/{kbId}/documents/{docId}` | 删除文档 |
| POST | `/api/chat/send` | RAG 问答 |
| POST | `/api/chat/send/stream` | SSE 流式问答 |

## 后续规划

- 扩展 `.md`、`.pdf`、`.docx`、`.html` 等文件类型解析
- 为不同文件类型实现固定长度、段落、Token 和语义分片策略
- 引入分片能力，按知识库或用户维度拆分业务数据
- 优化文档处理任务的重试、幂等和失败恢复
- 增加缓存、限流、鉴权和监控

## 接口文档

- [接口文档](docs/api.md)

## 许可证

仅供学习与培训使用。
