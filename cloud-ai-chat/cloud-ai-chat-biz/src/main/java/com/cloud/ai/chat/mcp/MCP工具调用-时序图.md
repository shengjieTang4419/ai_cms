# MCP工具调用时序图

## 📊 完整时序图

```mermaid
sequenceDiagram
    participant 前端
    participant Controller
    participant Service
    participant ModelProvider
    participant ChatClient
    participant AI模型
    participant ToolCallback
    participant McpTool
    
    前端->>Controller: GET /streamChat?query=今天天气如何
    Controller->>Service: streamChat(query, sessionId)
    
    Note over Service: 1. 构建ChatContext
    Service->>Service: buildChatContext()
    
    Note over Service: 2. 选择模型
    Service->>ModelProvider: getChatClient()
    ModelProvider-->>Service: ChatClient实例
    
    Note over Service: 3. 发起流式请求
    Service->>ChatClient: prompt(query).stream().content()
    
    Note over ChatClient: 包含已注册的工具列表
    ChatClient->>AI模型: 发送请求 + 工具定义
    
    Note over AI模型: 分析query，识别需要调用get_weather
    AI模型-->>ChatClient: 返回tool_calls请求
    
    Note over ChatClient: 根据工具名查找ToolCallback
    ChatClient->>ToolCallback: call(arguments)
    
    Note over ToolCallback: McpToolAdapter适配
    ToolCallback->>McpTool: execute(jsonNode)
    
    Note over McpTool: 执行实际业务逻辑
    McpTool->>McpTool: weatherService.getWeather()
    McpTool-->>ToolCallback: 返回天气数据
    
    ToolCallback-->>ChatClient: 工具执行结果
    ChatClient->>AI模型: 发送工具结果
    
    Note over AI模型: 基于结果生成回复
    AI模型-->>ChatClient: 流式返回内容
    ChatClient-->>Service: Flux<String>
    Service-->>Controller: Flux<String>
    Controller-->>前端: 流式响应
```

---

## 🔧 启动时的注册流程

```mermaid
sequenceDiagram
    participant Spring容器
    participant McpToolRegistry
    participant MCPConfig
    participant ModelProvider
    participant ChatClient
    
    Note over Spring容器: 应用启动
    
    Spring容器->>Spring容器: 扫描@Component
    Spring容器->>McpToolRegistry: 注入所有McpTool实现
    
    Note over McpToolRegistry: @PostConstruct
    McpToolRegistry->>McpToolRegistry: init()
    loop 遍历所有工具
        McpToolRegistry->>McpToolRegistry: register(tool)
    end
    
    Note over MCPConfig: 创建Bean
    Spring容器->>MCPConfig: toolCallbackProvider()
    MCPConfig->>McpToolRegistry: getEnabledTools()
    McpToolRegistry-->>MCPConfig: List<McpTool>
    
    loop 转换为ToolCallback
        MCPConfig->>MCPConfig: new McpToolAdapter(tool)
    end
    MCPConfig-->>Spring容器: ToolCallbackProvider
    
    Note over ModelProvider: 创建ChatClient
    Spring容器->>ModelProvider: getChatClient()
    ModelProvider->>ModelProvider: chatClientBuilder
    ModelProvider->>ToolCallbackProvider: getToolCallbacks()
    ToolCallbackProvider-->>ModelProvider: ToolCallback[]
    ModelProvider->>ModelProvider: defaultToolCallbacks(callbacks)
    ModelProvider-->>Spring容器: ChatClient
    
    Note over Spring容器: 启动完成
```

---

## 🎯 AI决策工具调用流程

```mermaid
sequenceDiagram
    participant 用户Query
    participant AI模型
    participant 工具列表
    participant 匹配引擎
    participant 参数生成器
    
    用户Query->>AI模型: "今天北京天气怎么样"
    
    Note over AI模型: 分析用户意图
    AI模型->>AI模型: 提取关键字: [天气, 北京]
    
    AI模型->>工具列表: 查询可用工具
    工具列表-->>AI模型: [get_weather, get_location, ...]
    
    AI模型->>匹配引擎: 匹配最合适的工具
    
    Note over 匹配引擎: 计算匹配度
    匹配引擎->>匹配引擎: get_weather: 95分
    匹配引擎->>匹配引擎: get_location: 30分
    匹配引擎->>匹配引擎: 其他: 10分
    
    匹配引擎-->>AI模型: 选择: get_weather
    
    AI模型->>参数生成器: 生成工具参数
    
    Note over 参数生成器: 根据inputSchema
    参数生成器->>参数生成器: 识别到"北京"
    参数生成器->>参数生成器: 参数: {city: "北京"}
    
    参数生成器-->>AI模型: 参数JSON
    
    AI模型->>AI模型: 构建tool_calls请求
    AI模型-->>用户Query: 返回工具调用请求
```

---

## 🔄 工具执行详细流程

```mermaid
sequenceDiagram
    participant Spring AI
    participant McpToolAdapter
    participant WeatherMcpTool
    participant CityInfoService
    participant WeatherService
    participant 高德API
    
    Spring AI->>McpToolAdapter: call('{"city":"北京"}')
    
    Note over McpToolAdapter: 适配器处理
    McpToolAdapter->>McpToolAdapter: 解析JSON参数
    McpToolAdapter->>WeatherMcpTool: execute(jsonNode)
    
    Note over WeatherMcpTool: 提取参数
    WeatherMcpTool->>WeatherMcpTool: cityName = "北京"
    
    Note over WeatherMcpTool: 获取城市编码
    WeatherMcpTool->>CityInfoService: getCityCode("北京")
    CityInfoService-->>WeatherMcpTool: "110100"
    
    Note over WeatherMcpTool: 查询天气
    WeatherMcpTool->>WeatherService: getWeather("110100")
    WeatherService->>高德API: HTTP GET /weather/weatherInfo
    高德API-->>WeatherService: WeatherResponse JSON
    WeatherService-->>WeatherMcpTool: WeatherResponse对象
    
    Note over WeatherMcpTool: 格式化结果
    WeatherMcpTool->>WeatherMcpTool: formatWeatherResponse()
    WeatherMcpTool-->>McpToolAdapter: "北京天气：晴，温度：22℃..."
    
    McpToolAdapter-->>Spring AI: 工具执行结果
```

---

## 📱 前端到后端完整链路

```mermaid
graph TB
    A[前端发起请求] --> B[Controller接收]
    B --> C[Service处理]
    C --> D{需要选择模型}
    
    D --> E[ModelSelector]
    E --> F{有图片?}
    F -->|是| G[VisionProvider]
    F -->|否| H[TurboProvider]
    
    G --> I[获取ChatClient]
    H --> I
    
    I --> J[ChatClient已注册工具]
    
    J --> K[发送到AI模型]
    K --> L{AI决策}
    
    L -->|需要工具| M[返回tool_calls]
    L -->|不需要| N[直接回复]
    
    M --> O[查找ToolCallback]
    O --> P[McpToolAdapter]
    P --> Q[McpTool.execute]
    
    Q --> R[执行业务逻辑]
    R --> S[返回结果给AI]
    S --> T[AI生成最终回复]
    
    N --> T
    T --> U[流式返回前端]
    
    style J fill:#90EE90
    style Q fill:#FFB6C1
    style T fill:#87CEEB
```

---

## 🏗️ 系统架构层次图

```mermaid
graph TD
    subgraph 表层调用层
        A1[AiChatController]
        A2[AIChatService]
    end
    
    subgraph 模型层
        B1[ModelSelector]
        B2[ModelProvider接口]
        B3[QwenTurboProvider]
        B4[QwenVisionProvider]
        B5[QwenThinkingProvider]
    end
    
    subgraph 工具注册层
        C1[McpToolRegistry]
        C2[MCPConfig]
        C3[ToolCallbackProvider]
        C4[McpToolAdapter]
    end
    
    subgraph 工具实现层
        D1[McpTool接口]
        D2[WeatherMcpTool]
        D3[LocationMcpTool]
        D4[RoutePlanningMcpTool]
    end
    
    subgraph Spring AI层
        E1[ChatClient]
        E2[ToolCallback]
    end
    
    subgraph AI模型层
        F1[通义千问API]
    end
    
    A1 --> A2
    A2 --> B1
    B1 --> B2
    B2 --> B3
    B2 --> B4
    B2 --> B5
    
    B3 --> E1
    B4 --> E1
    B5 --> E1
    
    C1 --> C2
    C2 --> C3
    C3 --> C4
    
    D1 --> D2
    D1 --> D3
    D1 --> D4
    
    D2 --> C4
    D3 --> C4
    D4 --> C4
    
    C4 --> E2
    E2 --> E1
    
    E1 --> F1
    
    style C1 fill:#90EE90
    style C4 fill:#FFB6C1
    style E1 fill:#87CEEB
    style F1 fill:#FFD700
```

---

## 🔐 工具注册与生命周期

```mermaid
stateDiagram-v2
    [*] --> 定义工具: 实现McpTool接口
    定义工具 --> Spring扫描: @Component注解
    Spring扫描 --> 自动注册: McpToolRegistry.init()
    自动注册 --> 适配转换: MCPConfig转换
    适配转换 --> 提供回调: ToolCallbackProvider
    提供回调 --> 注入ChatClient: defaultToolCallbacks
    注入ChatClient --> 工具就绪: 可被AI调用
    
    工具就绪 --> AI调用: 用户请求匹配
    AI调用 --> 执行工具: ToolCallback.call()
    执行工具 --> 返回结果: McpTool.execute()
    返回结果 --> 工具就绪: 等待下次调用
    
    工具就绪 --> [*]: 应用关闭
```

---

## 🎭 工具匹配决策树

```mermaid
graph TD
    A[用户Query] --> B{包含关键字?}
    
    B -->|天气| C[get_weather]
    B -->|路线/导航| D[plan_route]
    B -->|位置/定位| E[get_location]
    B -->|推荐/建议| F[recommend_topics]
    B -->|其他| G[不调用工具]
    
    C --> H{需要参数?}
    H -->|城市名| I[提取: 北京]
    H -->|无| J[使用默认]
    
    I --> K[调用工具]
    J --> K
    
    K --> L[返回结果]
    L --> M[AI整合回复]
    
    style C fill:#90EE90
    style K fill:#FFB6C1
    style M fill:#87CEEB
```

---

## 🧩 适配器模式详解

```mermaid
classDiagram
    class ToolCallback {
        <<interface>>
        +getToolDefinition() ToolDefinition
        +call(arguments) String
    }
    
    class McpTool {
        <<interface>>
        +getName() String
        +getDescription() String
        +getInputSchema() Schema
        +getOutputSchema() Schema
        +execute(input) Object
    }
    
    class McpToolAdapter {
        -McpTool mcpTool
        -ObjectMapper objectMapper
        -ToolDefinition toolDefinition
        +McpToolAdapter(mcpTool, objectMapper)
        +getToolDefinition() ToolDefinition
        +call(arguments) String
        -buildToolDefinition() ToolDefinition
    }
    
    class WeatherMcpTool {
        -WeatherService weatherService
        -LocationService locationService
        +getName() String
        +getDescription() String
        +getInputSchema() Schema
        +execute(input) Object
    }
    
    ToolCallback <|.. McpToolAdapter : implements
    McpTool <|.. WeatherMcpTool : implements
    McpToolAdapter o-- McpTool : adapts
    
    note for McpToolAdapter "适配器模式\n将McpTool适配为\nSpring AI的ToolCallback"
```

---

## 📈 性能与并发

```mermaid
sequenceDiagram
    participant 用户1
    participant 用户2
    participant Service
    participant ChatClient
    participant 工具池
    participant AI
    
    par 并发请求
        用户1->>Service: 查询天气
        and
        用户2->>Service: 规划路线
    end
    
    par ChatClient处理
        Service->>ChatClient: prompt1
        and
        Service->>ChatClient: prompt2
    end
    
    par AI处理
        ChatClient->>AI: 请求1 + 工具列表
        and
        ChatClient->>AI: 请求2 + 工具列表
    end
    
    par 工具调用
        AI-->>工具池: 调用get_weather
        and
        AI-->>工具池: 调用plan_route
    end
    
    par 返回结果
        工具池-->>用户1: 天气数据
        and
        工具池-->>用户2: 路线数据
    end
    
    note over Service,工具池: 工具是单例，支持并发调用
```

---

## 🎯 关键时序点

### 1. 启动阶段（只执行一次）
```
Spring容器启动 
→ 扫描McpTool实现 (0.1s)
→ McpToolRegistry注册 (0.05s)
→ MCPConfig转换 (0.05s)
→ ChatClient创建 (0.5s)
→ 工具就绪 (总耗时: ~0.7s)
```

### 2. 请求阶段（每次请求）
```
接收请求 (0ms)
→ Service处理 (1ms)
→ 发送到AI (50ms)
→ AI分析决策 (200ms)
→ 工具调用 (500ms) ← 主要耗时
→ AI生成回复 (300ms)
→ 流式返回 (实时)
总耗时: ~1s
```

### 3. 工具执行阶段
```
接收参数 (1ms)
→ 参数解析 (2ms)
→ 业务逻辑 (400ms) ← 主要耗时（API调用）
→ 结果格式化 (5ms)
→ 返回结果 (1ms)
```

---

## 💡 优化建议

### 1. 工具描述优化
```java
// 让AI更容易理解
@Override
public String getDescription() {
    return "【天气查询】获取指定城市的实时天气。" +
           "支持：北京、上海等中国主要城市。" +
           "返回：温度、天气状况、风向风力等。";
}
```

### 2. Schema优化
```java
// 提供更多约束帮助AI生成正确参数
properties.put("city", Schema.builder()
    .type("string")
    .description("城市名称")
    .pattern("^[\u4e00-\u9fa5]{2,10}$")  // 中文2-10字
    .examples(List.of("北京", "上海", "深圳"))
    .build()
);
```

### 3. 并发优化
```java
// 工具内部使用异步处理
@Override
public Object execute(JsonNode input) {
    return CompletableFuture
        .supplyAsync(() -> weatherService.getWeather(city))
        .thenApply(this::formatResponse)
        .get(5, TimeUnit.SECONDS);  // 超时保护
}
```

---

这些时序图清晰展示了MCP工具系统的完整调用流程！
