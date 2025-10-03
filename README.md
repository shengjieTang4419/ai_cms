# Spring AI CMS - AI学习项目

## 📚 项目简介

这是一个基于 **Spring AI** 和 **Spring AI Alibaba** 构建的智能对话系统，旨在学习和实践现代 AI 应用开发技术。项目集成了大语言模型（LLM）、向量数据库、记忆管理等核心技术，实现了完整的 AI 对话系统。

### 🎯 项目目标

- 学习 Spring AI 框架及其生态
- 掌握 LLM 应用开发的核心技术
- 实践 RAG（检索增强生成）模式
- 探索向量数据库在 AI 场景中的应用
- 构建生产级 AI 对话系统

---

## 🏗️ 技术架构

### 核心技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.4.0 | 应用框架 |
| Spring AI | 1.0.0 | AI 框架核心 |
| Spring AI Alibaba | 1.0.0.3 | 阿里云 AI 集成 |
| Java | 17 | 编程语言 |
| PostgreSQL + PgVector | Latest | 向量数据库 |
| Redis | Latest | 会话记忆存储 |
| MongoDB | Latest | 聊天历史持久化 |
| 通义千问（DashScope） | qwen-turbo | LLM 模型 |

### 项目结构

```
cms/
├── cloud-common/                    # 公共模块
│   ├── cloud-common-core/          # 核心工具
│   ├── cloud-common-datasource/    # 数据源配置
│   ├── cloud-common-mongodb/       # MongoDB 集成
│   ├── cloud-common-redis/         # Redis 集成
│   └── cloud-common-security/      # 安全认证（JWT）
├── cloud-modules/                   # 业务模块
│   ├── cloud-ai-chat/              # AI 聊天核心模块 ⭐
│   └── cloud-modules-system/       # 系统管理模块
└── pom.xml                         # 父项目配置
```

---

## 🚀 核心功能

### 1. AI 对话调度

#### 1.1 非流式对话
- **简单对话**：一次性返回完整响应
- 适用场景：简短问答、快速查询
- API 示例：
  ```java
  @GetMapping("/simple/chat")
  public String simpleChat(@RequestParam String query) {
      return aiChatService.simpleChat(query);
  }
  ```

#### 1.2 流式对话（SSE）
- **实时流式响应**：逐字返回，提升用户体验
- 基于 Reactor 响应式编程
- 自动保存对话历史到 MongoDB
- API 示例：
  ```java
  @GetMapping("/simple/streamChat")
  public Flux<String> streamChat(
      @RequestParam String query, 
      @RequestParam String sessionId
  ) {
      return aiChatService.streamChat(query, sessionId);
  }
  ```

**技术实现亮点**：
```java
Flux<String> contentFlux = chatClient.prompt(query)
    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
    .stream().content();
```

---

### 2. 记忆与上下文管理

#### 2.1 短期记忆（Redis）
- **基于 Redis 的滑动窗口记忆**
- 自动保留最近 20 轮对话
- 跨请求持久化，支持会话恢复

**核心配置**：
```java
@Bean
public MessageWindowChatMemory chatMemory(
    RedissonRedisChatMemoryRepository repository
) {
    return MessageWindowChatMemory.builder()
        .chatMemoryRepository(repository)
        .maxMessages(20)  // 保留最近20条消息
        .build();
}
```

#### 2.2 长期记忆（MongoDB）
- **完整对话历史归档**
- 支持按会话/用户检索
- 存储 RAG 增强标记

**数据模型**：
```java
@Document(collection = "chat_messages")
public class ChatMessage {
    private String sessionId;      // 会话ID
    private Long userId;           // 用户ID
    private MessageType messageType; // USER/ASSISTANT
    private String content;        // 消息内容
    private Boolean isRagEnhanced; // 是否RAG增强
    private LocalDateTime createdAt;
}
```

#### 2.3 会话管理
- **自动生成会话标题**：使用 LLM 总结对话主题
- **会话列表管理**：支持多会话并发
- **消息计数统计**：追踪会话活跃度

---

### 3. LLM 增强（RAG）

#### 3.1 RAG 工作流程

```
用户提问
    ↓
向量相似度检索（PgVector）
    ↓
找到相关文档片段
    ↓
构建增强提示词
    ↓
LLM 生成回答
    ↓
标记为 RAG 增强回答
```

#### 3.2 核心实现

**相似度检索**：
```java
public Flux<String> ragStreamChat(String query, String sessionId) {
    // 1. 从向量库检索相关文档
    List<Document> relevantDocs = vectorStore.similaritySearch(query);
    
    // 2. 构建增强提示词
    String enhancedPrompt = buildRagPrompt(query, relevantDocs);
    
    // 3. 流式返回增强回答
    return streamChat(enhancedPrompt, sessionId, true, query);
}
```

**提示词增强模板**：
```java
private String buildRagPrompt(String userQuery, List<Document> docs) {
    String context = docs.stream()
        .map(Document::getText)
        .collect(Collectors.joining("\n\n"));
    
    return String.format("""
        基于以下参考文档回答用户问题。
        如果文档中没有相关信息，请基于你的知识回答，并说明信息来源。
        
        参考文档：
        %s
        
        用户问题：%s
        
        请提供准确、有用的回答：
        """, context, userQuery);
}
```

---

### 4. 向量数据库（PgVector）

#### 4.1 技术选型
- **PostgreSQL + pgvector 扩展**
- 支持高维向量存储与相似度搜索
- 与关系型数据无缝集成

#### 4.2 文档处理流程

```
文档上传
    ↓
格式识别（PDF/Word/PPT/Markdown/TXT）
    ↓
内容提取（Apache Tika）
    ↓
语义化分块（保留上下文）
    ↓
向量化（Embedding Model）
    ↓
存储到 PgVector
```

#### 4.3 智能文本分块

**语义化分块策略**：
- 按段落边界分割（保持语义完整）
- 动态块大小：短文档 500 字符，长文档 1000 字符
- 块重叠：200 字符重叠，避免语义断裂
- 中文优化：按句子标点符号智能分割

```java
private List<String> chunkTextBySemantic(
    String text, 
    int maxChunkSize, 
    int overlap
) {
    // 1. 按段落分割
    String[] paragraphs = text.split("\n\\s*\n");
    
    // 2. 组装块，保持语义完整性
    // 3. 处理超大段落（按句子分割）
    // 4. 添加重叠文本，保持上下文连续性
    
    return chunks;
}
```

#### 4.4 支持的文档格式

| 格式 | 扩展名 | 解析器 |
|------|--------|--------|
| PDF | .pdf | Apache PDFBox |
| Word | .doc, .docx | Apache POI |
| PowerPoint | .ppt, .pptx | Apache POI |
| Markdown | .md | Tika |
| HTML | .html, .htm | Tika |
| 纯文本 | .txt, .csv | Tika |

---

### 5. AI 模型配置

#### 5.1 默认模型配置

```java
@Bean
public ChatClient chatClient(ChatClient.Builder builder) {
    return builder
        .defaultSystem("你是一个博学的智能聊天助手，请根据用户提问回答！")
        .defaultAdvisors(
            new SimpleLoggerAdvisor(),  // 日志记录
            MessageChatMemoryAdvisor.builder(memory).build()  // 记忆管理
        )
        .defaultOptions(DashScopeChatOptions.builder()
            .withModel("qwen-turbo")    // 通义千问 Turbo
            .withTopP(0.7)              // 多样性控制
            .build())
        .build();
}
```

#### 5.2 可调参数

- **模型选择**：qwen-turbo / qwen-plus / qwen-max
- **温度（Temperature）**：控制回答随机性
- **TopP**：控制采样范围
- **MaxTokens**：最大响应长度

---

### 6. 安全认证（JWT）

#### 6.1 架构特点

- **无状态认证**：用户信息存储在 JWT 中
- **条件化加载**：System 模块完整认证，Chat 模块简化认证
- **跨模块共享**：统一的 JWT Token

#### 6.2 工作流程

**System 模块**（完整认证）：
```
用户登录 → 验证密码 → 生成 JWT（含 userId）
    ↓
请求带 Token → 验证签名 → 查询数据库 → 加载完整权限
```

**Chat 模块**（轻量认证）：
```
请求带 Token → 验证签名 → 直接解析 userId/username
```

详细架构请参考：[SECURITY-ARCHITECTURE.md](./SECURITY-ARCHITECTURE.md)

---

## 📊 数据流转

### 完整对话流程

```
┌──────────┐
│ 用户请求  │
└─────┬────┘
      ↓
┌─────────────────┐
│ AuthTokenFilter │  验证 JWT，解析用户信息
└─────┬───────────┘
      ↓
┌──────────────────┐
│ AiChatController │  接收请求
└─────┬────────────┘
      ↓
┌─────────────────┐
│ AIChatService   │  业务逻辑
└─────┬───────────┘
      ↓
  ┌───┴───┐
  │ RAG? │
  └───┬───┘
  是 ↓     ↓ 否
┌─────────┐  ┌────────────┐
│向量检索  │  │ 直接对话   │
└─────┬───┘  └─────┬──────┘
      ↓            ↓
  ┌───┴────────────┴───┐
  │  ChatClient 调用   │  → 通义千问 API
  └────────┬───────────┘
           ↓
  ┌────────────────────┐
  │  Flux<String>      │  流式返回
  │  (响应式流)         │
  └────────┬───────────┘
           ↓
  ┌────────────────────┐
  │  保存到 MongoDB    │  完整对话历史
  │  更新 Redis 记忆   │  最近20轮对话
  └────────────────────┘
```

---

## 🛠️ 快速开始

### 1. 环境要求

- Java 17+
- Maven 3.8+
- PostgreSQL 14+ (with pgvector extension)
- Redis 6+
- MongoDB 4.4+

### 2. 数据库准备

**PostgreSQL**：
```sql
-- 安装 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 创建数据库
CREATE DATABASE cms;
```

**Redis**：
```bash
# 启动 Redis（默认端口 6379）
redis-server
```

**MongoDB**：
```bash
# 启动 MongoDB（默认端口 27017）
mongod --dbpath /your/data/path
```

### 3. 配置文件

修改 `cloud-modules/cloud-ai-chat/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/cms
    username: your_username
    password: your_password
  
  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_password
    
    mongodb:
      uri: mongodb://root:password@localhost:27017/aiChat?authSource=admin
  
  ai:
    dashscope:
      api-key: your_dashscope_api_key  # 阿里云通义千问 API Key
```

### 4. 启动应用

```bash
# 1. 构建项目
mvn clean package

# 2. 启动 AI 聊天服务
cd cloud-modules/cloud-ai-chat
mvn spring-boot:run

# 3. 访问服务
# 端口：18080
```

### 5. API 测试

**简单对话**：
```bash
curl "http://localhost:18080/api/aiChat/simple/chat?query=你好"
```

**流式对话**：
```bash
curl "http://localhost:18080/api/aiChat/simple/streamChat?query=介绍一下Spring AI&sessionId=test-session-001"
```

**RAG 增强对话**：
```bash
# 1. 上传文档
curl -X POST "http://localhost:18080/api/document/upload" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@/path/to/your/document.pdf" \
  -F "title=Spring AI 文档" \
  -F "description=Spring AI 官方文档"

# 2. RAG 对话
curl "http://localhost:18080/api/aiChat/rag/streamChat?query=Spring AI 是什么&sessionId=test-session-001" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 🎓 学习要点

### 1. Spring AI 核心概念

- **ChatClient**：统一的聊天客户端抽象
- **Advisor**：增强器（记忆、日志、RAG等）
- **VectorStore**：向量存储抽象
- **DocumentReader**：文档读取器
- **EmbeddingModel**：向量化模型

### 2. RAG 最佳实践

- ✅ 文本分块保持语义完整性
- ✅ 合理的块大小和重叠
- ✅ 使用元数据过滤
- ✅ 相似度阈值调优
- ✅ 上下文长度控制

### 3. 记忆管理策略

- **短期记忆**：高频访问，使用 Redis
- **长期记忆**：归档存储，使用 MongoDB
- **滑动窗口**：限制上下文长度，控制 Token 消耗
- **会话隔离**：不同会话独立记忆

### 4. 流式响应优化

- 使用响应式编程（Reactor）
- 及时释放资源
- 错误处理与降级
- 背压（Backpressure）管理

---

## 📝 技术亮点

### 1. 响应式编程
- 基于 Project Reactor
- 非阻塞 I/O
- 提升并发性能

### 2. 智能分块算法
- 段落级语义保持
- 句子边界识别
- 动态重叠策略

### 3. 多模态文档支持
- 统一的解析接口
- 支持 10+ 文档格式
- 智能内容提取

### 4. 灵活的认证机制
- 条件化用户加载
- 模块化安全配置
- 无状态 JWT

---

## 🗺️ 后续规划

### 短期目标
- [ ] 添加对话导出功能
- [ ] 实现多模态支持（图片、语音）
- [ ] 优化向量检索算法
- [ ] 添加聊天统计分析

### 长期目标
- [ ] 支持多 LLM 模型切换
- [ ] 实现 Agent 工作流
- [ ] 添加知识图谱
- [ ] 构建 AI 应用市场

---

## 📚 参考资料

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Spring AI Alibaba](https://github.com/alibaba/spring-ai-alibaba)
- [阿里云通义千问](https://dashscope.aliyun.com/)
- [PgVector 文档](https://github.com/pgvector/pgvector)
- [Apache Tika](https://tika.apache.org/)

---

## 👨‍💻 开发者

**Author**: shengjie.tang  
**Version**: 1.0.0  
**Last Update**: 2025-10-01

---

## 📄 License

本项目仅用于学习和研究，不得用于商业用途。

---

## 🙏 致谢

感谢 Spring AI 团队和阿里云提供的优秀框架和服务！

