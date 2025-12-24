# MCP工具调用流程详解

## 📋 目录
1. [整体架构](#整体架构)
2. [调用流程](#调用流程)
3. [工具注册机制](#工具注册机制)
4. [Spring AI调度原理](#spring-ai调度原理)
5. [从@Tool到Schema的演变](#从tool到schema的演变)
6. [完整调用链路图](#完整调用链路图)

---

## 🏗️ 整体架构

### 架构层次
```
┌─────────────────────────────────────────────────────────────┐
│                    表层调用层                                 │
│   Controller (AiChatController.streamChat)                   │
│          ↓                                                    │
│   Service (AIChatService.streamChat)                         │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                  模型选择与配置层                              │
│   ModelSelector → ModelProvider → ChatClient                │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                    工具注册层                                 │
│   MCPConfig → ToolCallbackProvider                          │
│        ↓                                                     │
│   McpToolRegistry → McpTool实现类                            │
│        ↓                                                     │
│   McpToolAdapter (适配器模式)                                │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                  Spring AI调度层                             │
│   ChatClient → AI Model (通义千问) → ToolCallback           │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                    工具执行层                                 │
│   McpTool.execute() → 实际业务逻辑                           │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔄 调用流程

### 1. 前端发起请求
```javascript
// 前端调用
GET /api/aiChat/simple/streamChat?query=今天天气如何&sessionId=xxx
```

### 2. Controller接收请求
```java
@GetMapping("/simple/streamChat")
public Flux<String> streamChat(
    @RequestParam("query") String query,
    @RequestParam("sessionId") String sessionId
) {
    return aiChatService.streamChat(query, sessionId, ...);
}
```

### 3. Service层处理
```java
// 构建上下文
ChatContext context = buildChatContext(...);

// 选择模型
ChatClient chatClient = getChatClient(context);

// 发起流式请求
return chatClient.prompt(enhancedQuery)
    .stream()
    .content();
```

### 4. AI自动调用工具
当用户查询"今天天气如何"时，AI模型会：
1. 分析query，识别"天气"关键字
2. 查看已注册的工具列表
3. 找到`get_weather`工具
4. 根据Schema生成参数
5. 调用工具执行
6. 基于结果生成回复

---

## 🔧 工具注册机制

### 完整流程

#### Step 1: 实现McpTool接口
```java
@Component
public class WeatherMcpTool implements McpTool {
    
    @Override
    public String getName() {
        return "get_weather";
    }
    
    @Override
    public String getDescription() {
        return "获取指定城市或当前位置的实时天气信息";
    }
    
    @Override
    public Schema getInputSchema() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("city", Schema.string("城市名称"));
        return Schema.object(properties, List.of());
    }
    
    @Override
    public Object execute(JsonNode input) {
        String cityName = input.has("city") 
            ? input.get("city").asText() 
            : null;
        return weatherService.getWeather(cityName);
    }
}
```

#### Step 2: 自动注册到Registry
```java
@Component
public class McpToolRegistry {
    
    @Autowired(required = false)
    private List<McpTool> mcpTools;
    
    @PostConstruct
    public void init() {
        for (McpTool tool : mcpTools) {
            register(tool);
        }
    }
}
```

#### Step 3: 适配为Spring AI格式
```java
@Configuration
public class MCPConfig {
    
    @Bean
    public ToolCallbackProvider toolCallbackProvider() {
        List<ToolCallback> callbacks = new ArrayList<>();
        
        for (McpTool mcpTool : mcpToolRegistry.getEnabledTools()) {
            callbacks.add(new McpToolAdapter(mcpTool, objectMapper));
        }
        
        return () -> callbacks.toArray(new ToolCallback[0]);
    }
}
```

#### Step 4: 注册到ChatClient
```java
@Component
public class QwenTurboProvider implements ModelProvider {
    
    @Override
    public ChatClient getChatClient() {
        var builder = chatClientBuilder
            .defaultSystem(systemPrompt)
            .defaultToolCallbacks(
                toolCallbackProvider.getToolCallbacks()
            );
        
        return builder.build();
    }
}
```

---

## 🤖 Spring AI调度原理

### AI是如何调用工具的？

#### 1. Function Calling机制
AI使用OpenAI的Function Calling标准：

```
用户query → AI分析 → 决定调用工具 → 生成工具调用请求
    ↓
Spring AI接收请求 → 找到ToolCallback → 执行工具
    ↓
结果返回AI → AI生成最终回复
```

#### 2. 工具选择依据
AI根据以下信息选择工具：
- **description**: 理解工具用途
- **name**: 工具名称暗示
- **用户query**: 关键字匹配

示例：
```
用户: "今天北京天气怎么样？"

AI分析:
1. 关键字: "天气"、"北京"
2. 查看工具: get_weather
3. 描述匹配: "获取...天气信息"
4. 决定调用
5. 生成参数: {"city": "北京"}
```

#### 3. Schema的作用

```java
{
  "name": "get_weather",
  "description": "获取指定城市或当前位置的实时天气信息",
  "parameters": {
    "type": "object",
    "properties": {
      "city": {
        "type": "string",
        "description": "城市名称，例如：北京、上海"
      }
    },
    "required": []
  }
}
```

AI通过Schema理解：
- 工具需要什么参数
- 参数类型和含义
- 哪些参数必填
- 如何生成合理的参数值

---

## 🔄 从@Tool到Schema的演变

### 旧方式：@Tool注解

```java
@Component
public class WeatherTool {
    
    @Tool(name = "get_weather", description = "获取天气")
    public String getWeather(@P("城市") String city) {
        return weatherService.getWeather(city);
    }
}
```

**问题**：
- ❌ 强依赖框架注解
- ❌ 参数验证能力弱
- ❌ 难以统一管理
- ❌ 无法动态注册

### 新方式：Schema定义

```java
@Component
public class WeatherMcpTool implements McpTool {
    // 实现接口方法
}
```

**优势**：
- ✅ 框架无关，解耦
- ✅ 强大的Schema验证
- ✅ 统一管理和扩展
- ✅ 支持动态注册

### 对比表

| 特性 | @Tool | Schema |
|------|-------|--------|
| 解耦性 | ❌ | ✅ |
| 灵活性 | ❌ | ✅ |
| 管理性 | ❌ | ✅ |
| 可扩展 | ❌ | ✅ |
| 参数验证 | ❌ | ✅ |
| 动态注册 | ❌ | ✅ |

---

## 📊 完整调用链路

### 详细步骤

1. **HTTP请求** → Controller
2. **Controller** → Service.streamChat()
3. **Service** → 选择ModelProvider
4. **ModelProvider** → 创建ChatClient（已注册工具）
5. **ChatClient** → 发送请求到AI（带工具列表）
6. **AI分析** → 决定调用get_weather
7. **AI返回** → tool_calls请求
8. **Spring AI** → 找到McpToolAdapter
9. **Adapter** → 调用McpTool.execute()
10. **execute()** → 执行业务逻辑
11. **返回结果** → 发送回AI
12. **AI** → 生成最终回复
13. **流式返回** → 前端

### 关键代码位置

| 组件 | 文件 | 行号 |
|------|------|------|
| Controller | AiChatController.java | 37-46 |
| Service | AIChatService.java | 103-133 |
| 模型选择 | AIChatService.java | 60-68 |
| 工具注册 | QwenTurboProvider.java | 75-76 |
| 工具适配 | McpToolAdapter.java | 42-62 |
| 工具执行 | WeatherMcpTool.java | 75-96 |
| Registry | McpToolRegistry.java | 44-87 |
| Config | MCPConfig.java | 39-69 |

---

## 🔑 关键要点

### 1. 注册流程
```
@Component → Spring扫描 → McpToolRegistry 
→ McpToolAdapter → ToolCallbackProvider → ChatClient
```

### 2. AI调用机制
- AI通过description理解工具
- 根据inputSchema生成参数
- Spring AI负责执行和结果传递

### 3. Schema核心作用
- 定义工具能力
- 约束参数格式
- 指导AI调用

### 4. 演进原因
从注解到Schema，实现了更好的解耦、灵活性和可管理性

---

## 📚 总结

现在你的MCP工具系统是这样工作的：

1. **定义工具**：实现McpTool接口，定义Schema
2. **自动注册**：Spring容器启动时自动发现和注册
3. **适配转换**：McpToolAdapter转为Spring AI格式
4. **注入ChatClient**：所有模型Provider都注册工具
5. **AI智能调用**：根据Schema自动匹配和调用
6. **执行返回**：工具执行后结果返回AI生成回复

这个架构实现了工具的标准化、可管理性和可扩展性！
