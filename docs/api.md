# RAG 知识库系统 - API 接口文档

| 文档版本 | 修改日期 | 修改内容 | 作者 |
| :--- | :--- | :--- | :--- |
| V1.0 | 2026-07-22 | 初版：完整接口定义 | Codex |

---

## 一、通用说明

### 1.1 基础地址

    http://localhost:8080/api

### 1.2 统一响应格式

所有接口响应均包裹在 Result<T> 中：

    {"code": 200, "message": "操作成功", "data": null}

| 字段 | 类型 | 必含 | 说明 |
| :--- | :--- | :--- | :--- |
| code | Integer | 是 | 状态码，200 成功，其他失败 |
| message | String | 失败时必含 | 提示信息 |
| data | T | 成功时必含 | 具体业务数据 |

### 1.3 状态码 (code)

| code | 含义 | 说明 |
| :--- | :--- | :--- |
| 200 | 成功 | 请求处理成功 |
| 400 | 参数错误 | 缺少字段、格式错误、重名冲突 |
| 404 | 资源不存在 | 知识库/文档/会话不存在 |
| 500 | 服务器错误 | 系统异常 |

---

## 二、健康检查

### GET /api/health

功能：检查后端服务是否正常运行。

请求参数：无

响应：

    {"code": 200, "data": {"status": "ok"}}

data.status: String, 固定返回 "ok"

---

## 三、知识库管理

### 3.1 获取知识库列表
GET /api/knowledge-bases

功能：分页获取所有知识库，支持名称搜索，附带文档总数。

请求参数（Query）：
| 参数 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| keyword | String | 否 | - | 名称模糊搜索 |
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页条数 |

响应：

    [
      {"id": 1, "name": "产品手册", "docCount": 12, "createdAt": "2026-07-15 10:30:00"}
    ]

字段说明（data 数组每项）：
| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| id | Long | 知识库 ID |
| name | String | 知识库名称 |
| docCount | Integer | 文档总数（动态统计） |
| createdAt | String | 创建时间 yyyy-MM-dd HH:mm:ss |

### 3.2 创建知识库
POST /api/knowledge-bases

功能：创建新知识库。名称唯一不可为空。

请求体：{"name":"产品手册"}
- name: String, 必填, 1-100字符, 不可重复

响应：同上 data 格式
错误码：400 名称为空/已存在, 500 创建失败

### 3.3 获取单个知识库
GET /api/knowledge-bases/{id}

路径参数: id (Long) - 知识库 ID
错误码: 404 不存在

### 3.4 删除知识库
DELETE /api/knowledge-bases/{id}

功能：删除知识库及关联数据（文档/ES chunks/会话/消息）。物理删除不可恢复。

级联删除范围：
- document (kb_id) MySQL 物理删除
- conversation (kb_id) MySQL 物理删除
- chunks (kb_id) ES 批量删除

响应：{"code":200,"data":null}
错误码: 404 不存在, 500 删除失败

---

## 四、文档管理

### 4.1 获取文档列表
GET /api/knowledge-bases/{kbId}/documents

功能：获取指定知识库下的文档列表，按上传时间倒序。

请求参数（Query）: page (默认1), size (默认10)

响应：

    [
      {"id": 103, "fileName": "更新日志.txt", "kbId": 1, "size": 51200,
       "chunkSize": 500, "overlap": 50, "status": "processing",
       "uploadedAt": "2026-07-22 08:00:00"}
    ]

字段说明：
| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| id | Long | 文档 ID |
| fileName | String | 原始文件名 |
| kbId | Long | 所属知识库 ID |
| size | Long | 文件大小（字节） |
| chunkSize | Integer | 切分长度 |
| overlap | Integer | 重叠长度 |
| status | String | pending/processing/completed/failed |
| uploadedAt | String | 上传时间 |

状态流转: pending -> processing -> completed/failed

### 4.2 上传文档
POST /api/knowledge-bases/{kbId}/documents

功能：上传 .txt 文件并配置切分参数，触发异步处理。

请求体（multipart/form-data）：
| 参数 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| file | File | 是 | - | .txt, <=50MB, UTF-8 |
| chunkSize | Integer | 是 | 500 | 200/500/1000/1500 |
| overlap | Integer | 是 | 50 | >=0, <chunkSize |

响应：同 4.1, status 初始为 pending
错误码: 400 非txt/超50MB/参数错误, 404 kb不存在, 500 失败

### 4.3 删除文档
DELETE /api/knowledge-bases/{kbId}/documents/{docId}

路径参数: kbId (Long), docId (Long)
级联删除: ES chunks (doc_id) 批量删除
响应：{"code":200,"data":null}
错误码: 404 不存在, 500 删除失败

---

## 五、会话管理

### 5.1 获取会话列表
GET /api/knowledge-bases/{kbId}/sessions

响应：

    [
      {"id": 1, "kbId": 1, "title": "产品功能", "messageCount": 4,
       "createdAt": "2026-07-22 10:00:00", "updatedAt": "2026-07-22 10:05:00"}
    ]

字段：id(Long), kbId(Long), title(String), messageCount(Integer), createdAt(String), updatedAt(String)

### 5.2 创建会话
POST /api/knowledge-bases/{kbId}/sessions

功能：在指定知识库下创建新的空白会话。请求体：无

响应：{"code":200,"data":{"id":3,"kbId":1,"title":"新对话","messages":[],"createdAt":"2026-07-22 10:10:00","updatedAt":"2026-07-22 10:10:00"}}

### 5.3 获取会话详情
GET /api/knowledge-bases/{kbId}/sessions/{sessionId}

功能：获取会话完整信息，含所有消息（时间正序）。

响应：

    {
      "id": 1, "kbId": 1, "title": "产品功能",
      "messages": [
        {"id": 1, "role": "user", "content": "产品有哪些功能？", "citations": null,
         "createdAt": "2026-07-22 10:00:00"},
        {"id": 2, "role": "assistant", "content": "产品主要功能包括知识管理...",
         "citations": [{"docId": 101, "docName": "产品介绍.txt",
          "snippet": "本产品是基于 AI 的知识管理解决方案...", "score": 0.92}],
         "createdAt": "2026-07-22 10:00:05"}
      ],
      "createdAt": "2026-07-22 10:00:00", "updatedAt": "2026-07-22 10:05:00"
    }

MessageVO: id(Long), role(String: user/assistant), content(String), citations(Array), createdAt(String)
CitationVO: docId(Long), docName(String), snippet(String), score(Double, 0-1)

### 5.4 删除会话
DELETE /api/knowledge-bases/{kbId}/sessions/{sessionId}

功能：删除会话及其所有消息。物理删除。
级联: conversation -> message MySQL 物理删除
响应：{"code":200,"data":null}

---

## 六、RAG 问答

### POST /api/chat/send

功能：发送用户消息，执行 RAG 流程（向量化 -> ES kNN 检索 -> LLM 生成），返回引用来源。

请求体：{"sessionId":1,"message":"产品有哪些功能？","topK":5}

字段：
| 字段 | 类型 | 必填 | 默认值 | 限制 | 说明 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| sessionId | Long | 是 | - | - | 当前会话 ID |
| message | String | 是 | - | 1-5000字 | 用户问题 |
| topK | Integer | 否 | 5 | 1-20 | 检索返回片段数 |

RAG 内部流程：
1. 校验会话存在且属于当前知识库
2. EmbeddingClient 将 message 转化为 1024 维向量
3. ES chunks 索引 kNN 检索 (filter: kb_id, size: topK)
4. 拼接上下文文本
5. System Prompt + Context + 问题 -> LLM
6. 保存 user/assistant 消息到 MySQL
7. 返回带引用来源的回答

响应：

    {
      "userMsg": {"id": 3, "role": "user", "content": "产品有哪些功能？",
       "citations": null, "createdAt": "2026-07-22 10:05:00"},
      "aiMsg": {"id": 4, "role": "assistant",
       "content": "产品主要功能包括知识管理和智能问答...",
       "citations": [
         {"docId": 101, "docName": "产品介绍.txt",
          "snippet": "基于 AI 的知识管理解决方案...", "score": 0.92},
         {"docId": 102, "docName": "FAQ.txt",
          "snippet": "如何上传文档？...", "score": 0.78}
       ],
       "createdAt": "2026-07-22 10:05:03"}
    }

ChatResponseVO: userMsg(MessageVO), aiMsg(MessageVO, 含 citations)

行为说明：
- 首次发消息时 title 自动更新为前20字
- Top-K 实时生效，每次请求独立传递
- 引用按相关度降序，数量 <= topK

错误码: 400 消息为空/topK越界, 404 会话不存在, 500 AI/检索异常

---

## 七、错误响应汇总

参数校验失败: {"code":400,"message":"知识库名称不能为空","data":null}
资源不存在: {"code":404,"message":"知识库不存在","data":null}
业务冲突: {"code":400,"message":"知识库名称已存在","data":null}
服务器错误: {"code":500,"message":"服务器内部错误，请稍后重试","data":null}

---

## 八、接口速查表

| 方法 | 路径 | 功能 | 分页 | 搜索 |
| :--- | :--- | :--- | :---: | :---: |
| GET | /api/health | 健康检查 | - | - |
| GET | /api/knowledge-bases | 知识库列表 | Y | 名称 |
| POST | /api/knowledge-bases | 创建知识库 | - | - |
| GET | /api/knowledge-bases/{id} | 知识库详情 | - | - |
| DELETE | /api/knowledge-bases/{id} | 删除知识库 | - | - |
| GET | /api/knowledge-bases/{kbId}/documents | 文档列表 | Y | - |
| POST | /api/knowledge-bases/{kbId}/documents | 上传文档 | - | - |
| DELETE | /api/knowledge-bases/{kbId}/documents/{docId} | 删除文档 | - | - |
| GET | /api/knowledge-bases/{kbId}/sessions | 会话列表 | - | - |
| POST | /api/knowledge-bases/{kbId}/sessions | 创建会话 | - | - |
| GET | /api/knowledge-bases/{kbId}/sessions/{sessionId} | 会话详情 | - | - |
| DELETE | /api/knowledge-bases/{kbId}/sessions/{sessionId} | 删除会话 | - | - |
| POST | /api/chat/send | 发送消息 | - | - |

# Generated docs/api.md at D:\self_rag\docs\gen_api.py