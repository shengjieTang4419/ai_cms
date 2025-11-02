# Spring AI CMS - 智能个性化对话系统

## 📚 项目简介

这是一个基于 **Spring AI** 和 **Spring AI Alibaba** 构建的**智能个性化对话系统**，集成了大语言模型（LLM）、向量数据库、用户画像、个性推荐等核心技术，实现了完整的生产级 AI 应用。

### 🎯 项目目标

- 学习 Spring AI 框架及其生态
- 掌握 LLM 应用开发的核心技术
- 实践 RAG（检索增强生成）模式
- 探索向量数据库在 AI 场景中的应用
- 构建基于用户画像的个性化推荐系统
- 实现多模型自动切换（SPI机制）
- 打造生产级 AI 对话系统

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
| Redis | Latest | 会话记忆/缓存 |
| MongoDB | Latest | 聊天历史/图片元数据 |
| MinIO | 8.5.9 | 对象存储（图片） |
| 通义千问（DashScope） | qwen-turbo/vl-plus | LLM 模型（文本/多模态） |
| DashScope SDK | 2.21.8 | OCR、联网搜索 |
| Apache Tika | 2.9.2 | 文档解析 |

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
│   │   ├── provider/               # SPI模型提供者
│   │   ├── service/                # 业务服务层
│   │   ├── mcp/                    # MCP工具（天气、推荐等）
│   │   ├── controller/             # REST API
│   │   └── domain/                 # 领域模型
│   ├── cloud-ai-membership/        # 会员标签模块
│   └── cloud-modules-system/       # 系统管理模块
└── pom.xml                         # 父项目配置
```

---

## 🚀 核心功能

### 1. SPI机制 - 多模型自动切换 🔥

#### 1.1 设计理念
- **插件化扩展**：基于 Java SPI 思想，实现模型提供者的热插拔
- **自动发现**：Spring 容器自动扫描并加载所有 `ModelProvider` 实现
- **智能路由**：根据请求上下文（图片、RAG、联网）自动选择最合适的模型

#### 1.2 核心接口

```java
public interface ModelProvider {
    String getModelName();          // 模型唯一标识
    String getDisplayName();        // 模型显示名称
    boolean supportsVision();       // 是否支持图片
    boolean supportsStream();       // 是否支持流式
    ChatClient getChatClient();     // 获取ChatClient实例
    int getPriority();              // 优先级（数字越小越高）
}
```

#### 1.3 已实现模型

| 模型 | 能力 | 优先级 | 使用场景 |
|------|-----|-------|---------|
| qwen-turbo | 文本对话、流式、Function Calling | 10 | 普通对话、工具调用 |
| qwen-vl-plus | 多模态（图文）、流式 | 5 | 图片识别、图文混合对话 |

#### 1.4 智能模型选择

```java
// 自动选择：有图片 → Vision模型，无图片 → 文本模型
ChatContext context = ChatContext.builder()
    .query(query)
    .imageUrls(imageList)
    .ragEnhanced(false)
    .build();

ModelProvider provider = modelSelector.selectModelProvider(context);
```

**技术亮点**：
- ✅ **零配置扩展**：新增模型只需实现接口并加 `@Component`
- ✅ **线程安全**：每个 Provider 单例，ChatClient 双重检查锁初始化
- ✅ **优雅降级**：模型不存在时自动回退到默认模型

---

### 2. 图片处理 - MinIO + OCR 🖼️

#### 2.1 完整流程

```
图片上传 → MinIO存储 → DashScope OCR → 文字提取 → MongoDB元数据 → 增强对话
```

#### 2.2 核心功能

**支持的上传方式**：
- 📤 单张/批量上传（MultipartFile）
- 📤 Base64 上传
- 📤 自动生成访问URL

**OCR 文字识别**：
- 自动识别图片中的文字
- 异步处理，不阻塞上传
- OCR 结果存储到 MongoDB

**智能对话增强**：
```java
// 自动将OCR文字附加到用户提问
String enhancedQuery = query + "\n\n从图片中提取的文字：\n" + ocrText;
```

#### 2.3 技术实现

**MinIO 配置**：
```yaml
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: images
```

**图片元数据**：
```java
@Document(collection = "images")
public class Image {
    private String id;              // MongoDB ID
    private String fileUrl;         // MinIO访问URL
    private String ocrText;         // OCR识别文字
    private String ocrStatus;       // OCR状态
    private Long userId;            // 上传用户
}
```

---

### 3. 联网功能 - 实时信息获取 🌐

#### 3.1 DashScope 全网搜索

**启用方式**：
```java
DashScopeChatOptions requestOptions = DashScopeChatOptions.builder()
    .withEnableSearch(true)  // 开启全网搜索
    .build();

Flux<String> result = chatClient.prompt(query)
    .options(requestOptions)
    .stream().content();
```

**特点**：
- ✅ 实时获取最新信息
- ✅ 自动引用来源
- ✅ 请求级别控制（不影响其他请求）

#### 3.2 天气查询工具（Function Calling）

**集成高德地图天气 API**：
```java
@Tool(name = "get_weather", description = "获取指定城市的实时天气信息")
public String getWeather(@ToolParam String cityName) {
    // 1. 智能匹配城市编码
    CityInfo cityInfo = cityInfoService.getCityInfoByName(cityName);
    
    // 2. 调用天气API
    WeatherResponse weather = weatherService.getWeather(cityInfo.getCode());
    
    // 3. 返回格式化结果
    return String.format("%s天气：%s，温度：%s℃", ...);
}
```

**智能城市匹配**：
- 支持多种输入：上海、上海市、浦东新区、上海市浦东新区
- 数据库预存全国城市编码
- 模糊匹配 + 优先级排序

---

### 4. 用户画像系统 👤

#### 4.1 数据模型

**基础画像**（`UserProfile`）：
```java
{
  "userId": 1,
  "gender": "男",
  "age": 28,
  "location": "上海",
  "occupation": 101,        // 职业代码：软件工程师
  "hobbies": ["编程", "旅游", "摄影"]
}
```

**综合标签**（`UserTags`）：
```java
{
  "userId": 1,
  "tagName": "编程",
  "baseWeight": 1.0,        // 来自画像
  "chatWeight": 5.0,        // 来自聊天
  "fusionWeight": 4.0,      // 重叠加权
  "totalWeight": 10.0,      // 总权重
  "sourceType": "FUSION"    // PROFILE/CHAT/FUSION
}
```

#### 4.2 标签生成

**来源一：用户画像**
- 职业 → 相关标签（程序员 → 编程、算法、架构）
- 爱好 → 直接标签
- 年龄 → 人群标签（年轻人、职场精英）

**来源二：聊天行为**
- AI 分析对话内容
- 提取关键主题词
- 自动生成聊天标签

#### 4.3 权重计算规则 ⚖️

| 场景 | 权重计算 | 说明 |
|-----|---------|-----|
| 纯画像标签 | baseWeight = 1.0 | 来自用户资料 |
| 纯聊天标签 | chatWeight = +1.0 | 每次对话累加 |
| 重叠标签（同标签快速上浮）| fusionWeight = +2.0 | 画像与聊天重叠时额外加权 |
| 年龄标签 | baseWeight = 0.2 | 降低人口统计标签影响 |

**举例**：
```
程序员用户聊"编程" → baseWeight(1) + chatWeight(1) + fusionWeight(2) = 4
非程序员用户聊"编程" → chatWeight(1) = 1
程序员用户聊"美食" → chatWeight(1) = 1
```

---

### 5. 标签衰减机制 - 保持时效性 ⏱️

#### 5.1 半因子衰减

**定时任务**（每月1号凌晨2点）：
```java
@Scheduled(cron = "0 0 2 1 * ?")
public void monthlyWeightDecay() {
    for (UserTags tag : allTags) {
        tag.setBaseWeight(tag.getBaseWeight() * 0.5);
        tag.setChatWeight(tag.getChatWeight() * 0.5);
        tag.setFusionWeight(tag.getFusionWeight() * 0.5);
    }
}
```

#### 5.2 衰减效果

| 时间 | 初始权重 | 1个月后 | 2个月后 | 3个月后 |
|-----|---------|--------|--------|--------|
| 热点标签 | 10.0 | 5.0 | 2.5 | 1.25 |
| 持续讨论 | 10.0 → 11.0 | 5.5 → 6.5 | 3.25 | - |

**设计目标**：
- ✅ 过期热点快速下浮（半衰期1个月）
- ✅ 持续活跃标签保持高权重
- ✅ 避免历史数据污染推荐结果

---

### 6. 首页个性推荐 🎯

#### 6.1 工作流程

```
用户ID → 获取热门标签（Top 5）→ AI生成推荐提问 → Redis缓存24小时
```

#### 6.2 核心实现

```java
public Map<String, Object> generatePersonalizedRecommendations(Long userId, int limit) {
    // 1. 获取用户热门标签
    List<UserTags> hotTags = userProfileService.getHotTags(userId, limit);
    
    // 2. 检查Redis缓存
    String cacheKey = "recommendation:user:" + userId + ":" + weightKey;
    Map<String, Object> cached = redissonClient.getBucket(cacheKey).get();
    if (cached != null) return cached;
    
    // 3. AI生成推荐提问
    String prompt = String.format("""
        根据以下用户兴趣标签，为每个标签生成一个吸引人的、个性化的推荐问题。
        
        用户兴趣标签：%s
        
        例如：
        旅游 -> 3天云南日照金山全路程攻略Get！
        编程 -> 论MESI嗅探机制的作用和原理
        """, tagNames);
    
    List<String> questions = aiChatService.simpleChat(prompt);
    
    // 4. 缓存24小时
    redissonClient.getBucket(cacheKey).set(result, 24, TimeUnit.HOURS);
    
    return result;
}
```

#### 6.3 缓存策略

**缓存Key设计**：
```
recommendation:user:{userId}:{weightSum/10}
```

**特点**：
- ✅ 权重变化不大时复用缓存（以10为区间）
- ✅ 减少AI调用次数
- ✅ 24小时过期，保证新鲜度

---

### 7. 对话个性引导 💬

#### 7.1 智能追问

**在对话结束时自动推荐相关话题**：

```java
// 异步生成个性化推荐
CompletableFuture<List<String>> recommendationsFuture = 
    CompletableFuture.supplyAsync(() -> 
        recommendationTools.suggestFollowUpTopics(query, userId)
    );

// 对话完成后追加推荐
Flux<String> mainResponse = chatClient.prompt(query).stream().content()
    .doOnComplete(() -> {
        List<String> recommendations = recommendationsFuture.get(500, TimeUnit.MILLISECONDS);
        String formatted = formatRecommendations(recommendations);
        // 追加到响应流
    });
```

#### 7.2 推荐格式

**原始AI回答**：
```
Spring AI 是一个用于构建 AI 应用的框架...
```

**追加个性引导**：
```
Spring AI 是一个用于构建 AI 应用的框架...

要不我们聊点其他的？比如 Spring Boot 的高级特性？、微服务架构设计？或者 Docker 容器化部署？
```

#### 7.3 推荐算法

1. **获取用户热门标签**
2. **AI分析当前话题相关标签**
3. **生成自然的后续建议**
4. **格式化为友好对话**

**示例提示词**：
```
基于用户当前话题"Spring AI"和兴趣标签"微服务"，生成一个自然的后续建议问题。

要求：
1. 问题要自然，像朋友间的对话
2. 要体现标签与当前话题的关联
3. 问题长度控制在20-40个字
```

---

### 8. AI 对话调度

#### 8.1 非流式对话
- **简单对话**：一次性返回完整响应
- 适用场景：简短问答、快速查询
- API 示例：
  ```java
  @GetMapping("/simple/chat")
  public String simpleChat(@RequestParam String query) {
      return aiChatService.simpleChat(query);
  }
  ```

#### 8.2 流式对话（SSE）
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

### 9. 记忆与上下文管理

#### 9.1 短期记忆（Redis）
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

#### 9.2 长期记忆（MongoDB）
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

#### 9.3 会话管理
- **自动生成会话标题**：使用 LLM 总结对话主题
- **会话列表管理**：支持多会话并发
- **消息计数统计**：追踪会话活跃度

---

### 10. LLM 增强（RAG）

#### 10.1 RAG 工作流程

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

#### 10.2 核心实现

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

### 11. 向量数据库（PgVector）

#### 11.1 技术选型
- **PostgreSQL + pgvector 扩展**
- 支持高维向量存储与相似度搜索
- 与关系型数据无缝集成

#### 11.2 文档处理流程

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

#### 11.3 智能文本分块

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

#### 11.4 支持的文档格式

| 格式 | 扩展名 | 解析器 |
|------|--------|--------|
| PDF | .pdf | Apache PDFBox |
| Word | .doc, .docx | Apache POI |
| PowerPoint | .ppt, .pptx | Apache POI |
| Markdown | .md | Tika |
| HTML | .html, .htm | Tika |
| 纯文本 | .txt, .csv | Tika |

---

### 12. AI 模型配置

#### 12.1 默认模型配置

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

#### 12.2 可调参数

- **模型选择**：qwen-turbo / qwen-plus / qwen-max
- **温度（Temperature）**：控制回答随机性
- **TopP**：控制采样范围
- **MaxTokens**：最大响应长度

---

### 13. 安全认证（JWT）

#### 13.1 架构特点

- **无状态认证**：用户信息存储在 JWT 中
- **条件化加载**：System 模块完整认证，Chat 模块简化认证
- **跨模块共享**：统一的 JWT Token

#### 13.2 工作流程

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

### 完整对话流程（含个性推荐）

```
┌──────────┐
│ 用户请求  │ (可选：携带图片URL)
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
  ┌───────────────┐
  │ 有图片？       │
  └───┬───────────┘
   是 ↓      ↓ 否
┌──────────┐  │
│提取OCR文字│  │
│增强提问   │  │
└─────┬────┘  │
      ↓       ↓
  ┌───┴────────┴───┐
  │ 模型选择器      │  根据上下文选择模型
  │ (SPI机制)      │  有图→Vision，无图→Turbo
  └───┬────────────┘
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
  │  ChatClient 调用   │  → 通义千问 API (联网搜索可选)
  └────────┬───────────┘
           ↓
  ┌────────────────────┐  并行执行 ↓
  │  Flux<String>      │  ┌──────────────────────┐
  │  (响应式流)         │  │ 异步生成个性推荐      │
  └────────┬───────────┘  │ (CompletableFuture)  │
           ↓               └──────────┬───────────┘
  ┌────────────────────┐             ↓
  │  流式返回给用户     │  ← 追加推荐内容
  └────────┬───────────┘
           ↓
  ┌────────────────────┐
  │  保存到 MongoDB    │  完整对话历史（含图片URL）
  │  更新 Redis 记忆   │  最近20轮对话
  └────────┬───────────┘
           ↓
  ┌────────────────────┐
  │  异步分析对话      │  提取标签 → 更新用户画像
  │  更新标签权重      │  重叠标签 +2，普通标签 +1
  └────────────────────┘
```

### 个性推荐流程

```
┌──────────────┐
│ 首页加载      │
└──────┬───────┘
       ↓
┌──────────────────┐
│ 获取用户热门标签  │  按totalWeight排序，Top 5
└──────┬───────────┘
       ↓
┌──────────────────┐
│ 检查Redis缓存     │  key: recommendation:user:{userId}:{weight}
└──────┬───────────┘
   命中 ↓      ↓ 未命中
┌─────────┐  ┌─────────────────┐
│返回缓存  │  │ AI生成推荐提问   │
└─────────┘  │ (基于标签)       │
             └─────┬───────────┘
                   ↓
             ┌─────────────────┐
             │ 存入Redis缓存    │  过期时间24小时
             └─────┬───────────┘
                   ↓
             ┌─────────────────┐
             │ 返回推荐列表     │
             └─────────────────┘
```

### 标签权重更新流程

```
┌──────────────┐
│ 对话完成      │
└──────┬───────┘
       ↓
┌──────────────────┐
│ AI分析对话内容    │  提取关键主题词
└──────┬───────────┘
       ↓
┌──────────────────┐
│ 生成聊天标签列表  │  ["编程", "Spring AI", "微服务"]
└──────┬───────────┘
       ↓
┌──────────────────┐
│ 查询现有标签      │
└──────┬───────────┘
       ↓
   ┌───┴────┐
   │ 标签存在？│
   └───┬────┘
    是 ↓      ↓ 否
┌──────────┐  ┌─────────────┐
│ 检查重叠  │  │ 创建新标签   │
└─────┬────┘  │ chatWeight=1│
      ↓       └─────────────┘
  ┌───┴───┐
  │ 重叠？│
  └───┬───┘
   是 ↓    ↓ 否
┌─────────────┐  ┌──────────────┐
│fusionWeight │  │chatWeight +1│
│+2 (快速上浮)│  └──────────────┘
│sourceType   │
│= FUSION     │
└─────────────┘
       ↓
┌──────────────────┐
│ 计算totalWeight   │  base + chat + fusion
└──────────────────┘
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

# MinIO 对象存储配置
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: images

# 高德地图API（天气查询）
map:
  api-key: your_amap_api_key
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

#### 基础对话

**简单对话**：
```bash
curl "http://localhost:18080/api/aiChat/simple/chat?query=你好"
```

**流式对话**：
```bash
curl "http://localhost:18080/api/aiChat/simple/streamChat?query=介绍一下Spring AI&sessionId=test-session-001"
```

**联网搜索对话**：
```bash
curl "http://localhost:18080/api/aiChat/simple/streamChat?query=2025年最新的AI技术趋势&sessionId=test-001&isWithEnableSearch=true" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### RAG 文档对话

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

#### 图片处理

**上传图片**：
```bash
curl -X POST "http://localhost:18080/api/images/upload" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@/path/to/image.jpg" \
  -F "userId=1"
```

**图文混合对话**：
```bash
curl -X POST "http://localhost:18080/api/aiChat/simple/streamChat" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "这张图片是什么？",
    "sessionId": "test-001",
    "imageUrlList": ["http://localhost:18080/api/images/2025/01/27/xxx.jpg"]
  }'
```

#### 用户画像

**创建用户画像**：
```bash
curl -X POST "http://localhost:18080/api/profile" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "gender": "男",
    "age": 28,
    "location": "上海",
    "occupation": 101,
    "hobbies": ["编程", "旅游", "摄影"]
  }'
```

**获取用户画像**：
```bash
curl "http://localhost:18080/api/profile/1" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 个性推荐

**获取首页推荐**：
```bash
curl "http://localhost:18080/api/recommendations/personalized/1?limit=5" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**获取用户兴趣分析**：
```bash
curl "http://localhost:18080/api/recommendations/interests/1" \
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
- **Tool Calling**：函数调用机制（MCP）

### 2. SPI 设计模式

- **服务提供者接口**：定义统一的模型提供者接口
- **自动发现机制**：Spring 容器自动扫描实现类
- **策略模式**：根据上下文智能选择模型
- **扩展性设计**：新增模型零配置，只需实现接口

### 3. RAG 最佳实践

- ✅ 文本分块保持语义完整性
- ✅ 合理的块大小和重叠
- ✅ 使用元数据过滤
- ✅ 相似度阈值调优
- ✅ 上下文长度控制

### 4. 记忆管理策略

- **短期记忆**：高频访问，使用 Redis
- **长期记忆**：归档存储，使用 MongoDB
- **滑动窗口**：限制上下文长度，控制 Token 消耗
- **会话隔离**：不同会话独立记忆

### 5. 用户画像与推荐

- **标签融合**：画像标签 + 聊天标签 = 综合标签
- **权重策略**：重叠标签快速上浮（×2）
- **时效性管理**：半因子衰减（每月×0.5）
- **AI驱动推荐**：基于标签生成个性化内容
- **缓存优化**：Redis缓存24小时，减少AI调用

### 6. 流式响应优化

- 使用响应式编程（Reactor）
- 异步处理（CompletableFuture）
- 及时释放资源
- 错误处理与降级
- 背压（Backpressure）管理

### 7. 多模态处理

- **图片存储**：MinIO 对象存储
- **OCR识别**：DashScope 文字提取
- **模型切换**：自动选择 Vision 模型
- **对话增强**：OCR文字增强用户提问

---

## 📝 技术亮点

### 1. SPI 模型切换机制 ⚡
- **零配置扩展**：实现接口即可自动注册
- **智能路由**：根据请求上下文自动选择最优模型
- **线程安全**：单例模式 + 双重检查锁
- **优雅降级**：模型不存在时自动回退

### 2. 个性化推荐系统 🎯
- **标签融合算法**：画像 + 聊天行为综合评分
- **权重快速上浮**：重叠标签 ×2 加权
- **半因子衰减**：每月 ×0.5，保持时效性
- **AI生成推荐**：基于标签智能生成吸引人的问题
- **智能缓存**：Redis缓存24小时，按权重区间复用

### 3. 多模态对话能力 🖼️
- **图片上传**：MinIO 对象存储
- **OCR识别**：DashScope 自动提取文字
- **模型自适应**：有图片自动切换 Vision 模型
- **对话增强**：OCR文字自动附加到提问

### 4. 响应式编程 🚀
- 基于 Project Reactor
- 非阻塞 I/O
- CompletableFuture 异步处理
- 提升并发性能

### 5. 智能分块算法 📄
- 段落级语义保持
- 句子边界识别
- 动态重叠策略

### 6. 多模态文档支持 📚
- 统一的解析接口
- 支持 10+ 文档格式
- 智能内容提取

### 7. 联网功能 🌐
- **DashScope 全网搜索**：实时获取最新信息
- **Function Calling**：天气查询、个性推荐
- **智能城市匹配**：模糊匹配 + 优先级排序

### 8. 灵活的认证机制 🔐
- 条件化用户加载
- 模块化安全配置
- 无状态 JWT

---

## 🗺️ 后续规划

### 已完成 ✅
- [x] 多 LLM 模型切换（SPI机制）
- [x] 多模态支持（图片 + OCR）
- [x] 联网功能（全网搜索 + Function Calling）
- [x] 用户画像与标签系统
- [x] 个性化推荐（首页 + 对话引导）
- [x] 标签权重管理（快速上浮 + 半因子衰减）

### 短期目标
- [ ] 添加对话导出功能（PDF/Markdown）
- [ ] 语音输入输出（ASR + TTS）
- [ ] 优化向量检索算法（混合检索）
- [ ] 添加聊天统计分析（词云、话题分析）
- [ ] 会员等级体系（基于标签活跃度）
- [ ] 更多 Function Calling 工具（新闻、翻译等）

### 长期目标
- [ ] 实现 Agent 工作流（Multi-Agent 协作）
- [ ] 添加知识图谱（标签关系网络）
- [ ] 构建 AI 应用市场
- [ ] 支持更多 AI 模型（Claude、GPT、Gemini）
- [ ] 多租户隔离（SaaS化）
- [ ] 实时协同编辑

---

## 📚 参考资料

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Spring AI Alibaba](https://github.com/alibaba/spring-ai-alibaba)
- [阿里云通义千问](https://dashscope.aliyun.com/)
- [PgVector 文档](https://github.com/pgvector/pgvector)
- [Apache Tika](https://tika.apache.org/)
- [MinIO 官方文档](https://min.io/docs/minio/linux/index.html)
- [高德地图API](https://lbs.amap.com/api/webservice/summary)

---

## 👨‍💻 开发者

**Author**: shengjie.tang  
**Version**: 2.0.0  
**Last Update**: 2025-11-02

### 版本历史

**v2.0.0** (2025-11-02)
- ✨ 新增 SPI 模型切换机制
- ✨ 新增 MinIO 图片存储与 OCR
- ✨ 新增联网功能（全网搜索 + Function Calling）
- ✨ 新增用户画像与标签系统
- ✨ 新增个性化推荐（首页 + 对话引导）
- ✨ 新增标签权重管理（快速上浮 + 半因子衰减）
- ✨ 新增 cloud-ai-membership 模块

**v1.0.0** (2025-10-01)
- 🎉 初始版本发布
- 基础 AI 对话功能
- RAG 文档问答
- 记忆管理
- JWT 认证

---

## 📄 License

本项目仅用于学习和研究，不得用于商业用途。

---

## 🙏 致谢

感谢 Spring AI 团队和阿里云提供的优秀框架和服务！

