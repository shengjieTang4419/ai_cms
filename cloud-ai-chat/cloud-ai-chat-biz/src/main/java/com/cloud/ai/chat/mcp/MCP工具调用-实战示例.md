# MCP工具调用 - 实战示例

## 📝 完整示例：天气查询工具调用全过程

### 场景
用户输入：**"今天北京天气怎么样？"**

---

## 1️⃣ 请求入口

### 前端发起请求
```javascript
// 前端代码
fetch('/api/aiChat/simple/streamChat?query=今天北京天气怎么样&sessionId=session_123')
  .then(response => response.body)
  .then(stream => {
    // 处理流式响应
  });
```

### Controller接收
```java
// AiChatController.java
@GetMapping("/simple/streamChat")
public Flux<String> streamChat(
    @RequestParam("query") String query,              // "今天北京天气怎么样"
    @RequestParam("sessionId") String sessionId       // "session_123"
) {
    return aiChatService.streamChat(query, sessionId, null, false, false, null, null);
}
```

---

## 2️⃣ Service层处理

### 构建上下文
```java
// AIChatService.java - streamChat方法

// 1. 构建ChatContext
ChatContext context = ChatContext.builder()
    .query("今天北京天气怎么样")
    .sessionId("session_123")
    .ragEnhanced(false)
    .withEnableSearch(false)
    .useThinking(false)
    .build();

// 2. 选择合适的模型
ChatClient chatClient = getChatClient(context);
// → ModelSelector选择: qwen-turbo (因为不需要Vision)
// → 返回: QwenTurboProvider.getChatClient()
```

### 发起请求
```java
// 3. 构建请求流
Flux<String> contentFlux = chatClient
    .prompt("今天北京天气怎么样")
    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "session_123"))
    .stream()
    .content();
```

---

## 3️⃣ 工具已在启动时注册

### Spring容器启动流程
```java
// 启动时执行

// 1. McpToolRegistry初始化
@PostConstruct
public void init() {
    // Spring自动注入所有McpTool实现类
    // mcpTools = [WeatherMcpTool, LocationMcpTool, ...]
    
    for (McpTool tool : mcpTools) {
        register(tool);
        // 注册 WeatherMcpTool: 
        //   - name: "get_weather"
        //   - category: "basic"
        //   - version: "2.0.0"
    }
}

// 2. MCPConfig创建ToolCallbackProvider
@Bean
public ToolCallbackProvider toolCallbackProvider() {
    List<ToolCallback> callbacks = new ArrayList<>();
    
    for (McpTool mcpTool : mcpToolRegistry.getEnabledTools()) {
        // 为每个工具创建适配器
        McpToolAdapter adapter = new McpToolAdapter(mcpTool, objectMapper);
        callbacks.add(adapter);
    }
    
    return () -> callbacks.toArray(new ToolCallback[0]);
}

// 3. QwenTurboProvider创建ChatClient
@Override
public ChatClient getChatClient() {
    var builder = chatClientBuilder
        .defaultSystem(systemPrompt)
        .defaultToolCallbacks(
            // 注册所有工具：[get_weather, get_location, ...]
            toolCallbackProvider.getToolCallbacks()
        );
    
    return builder.build();
}
```

---

## 4️⃣ AI接收请求

### 发送给AI的完整数据
```json
{
  "model": "qwen-turbo",
  "messages": [
    {
      "role": "system",
      "content": "你是一个智能助手..."
    },
    {
      "role": "user",
      "content": "今天北京天气怎么样"
    }
  ],
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "get_weather",
        "description": "获取指定城市或当前位置的实时天气信息。如果提供了城市名称参数（如：北京、上海），则查询该城市的天气；如果没有提供城市名称，则自动通过IP定位获取用户所在城市的天气。",
        "parameters": {
          "type": "object",
          "properties": {
            "city": {
              "type": "string",
              "description": "城市名称，例如：北京、上海、深圳等。如果不提供此参数，系统将自动通过IP定位获取用户所在城市"
            }
          },
          "required": []
        }
      }
    }
    // ... 其他工具
  ],
  "stream": true
}
```

---

## 5️⃣ AI分析与决策

### AI内部处理流程
```
1. 分析用户query: "今天北京天气怎么样"
   ↓
2. 关键字提取: ["今天", "北京", "天气"]
   ↓
3. 查看可用工具列表
   ↓
4. 遍历tools数组:
   - get_weather: description包含"天气信息" ✅
   - get_location: 不相关 ❌
   - ...
   ↓
5. 匹配度最高: get_weather
   ↓
6. 分析需要的参数:
   - city: 必需? 否 (required=[])
   - 用户提到了"北京" → city = "北京"
   ↓
7. 生成工具调用请求
```

### AI返回的响应
```json
{
  "choices": [{
    "delta": {
      "tool_calls": [{
        "id": "call_abc123",
        "type": "function",
        "function": {
          "name": "get_weather",
          "arguments": "{\"city\":\"北京\"}"
        }
      }]
    },
    "finish_reason": "tool_calls"
  }]
}
```

---

## 6️⃣ Spring AI执行工具

### 工具调用处理
```java
// Spring AI内部处理

// 1. 接收到AI的tool_calls响应
String toolName = "get_weather";
String arguments = "{\"city\":\"北京\"}";

// 2. 根据工具名称查找ToolCallback
ToolCallback callback = findCallbackByName(toolName);
// → 找到: McpToolAdapter(WeatherMcpTool)

// 3. 执行工具
String result = callback.call(arguments);
```

### McpToolAdapter执行
```java
// McpToolAdapter.java

@Override
public String call(String functionArguments) {
    log.debug("适配器执行工具: get_weather, 输入: {\"city\":\"北京\"}");
    
    // 1. 解析JSON参数
    JsonNode inputNode = objectMapper.readTree(functionArguments);
    // inputNode = {"city": "北京"}
    
    // 2. 执行MCP工具
    Object result = mcpTool.execute(inputNode);
    // → 调用 WeatherMcpTool.execute()
    
    // 3. 返回结果
    return result instanceof String 
        ? (String) result 
        : objectMapper.writeValueAsString(result);
}
```

---

## 7️⃣ WeatherMcpTool执行

### 工具业务逻辑
```java
// WeatherMcpTool.java

@Override
public Object execute(JsonNode input) throws Exception {
    // 1. 提取参数
    String cityName = null;
    if (input != null && input.has("city")) {
        cityName = input.get("city").asText();  // "北京"
    }
    
    // 2. 获取城市编码
    String cityCode = getCityCode(cityName);
    // cityInfoService.getCityCode("北京") → "110100"
    
    // 3. 调用天气API
    WeatherResponse weatherResponse = weatherService
        .getWeather(cityCode)
        .timeout(Duration.ofSeconds(10))
        .block();
    
    // 4. 格式化结果
    return formatWeatherResponse(weatherResponse);
    // → "北京天气：晴，温度：22℃，风向：南风，风力：3级，湿度：45%，更新时间：2025-11-18 09:00"
}
```

---

## 8️⃣ 结果返回给AI

### 工具执行结果
```json
{
  "role": "tool",
  "tool_call_id": "call_abc123",
  "name": "get_weather",
  "content": "北京天气：晴，温度：22℃，风向：南风，风力：3级，湿度：45%，更新时间：2025-11-18 09:00"
}
```

### 再次发送给AI
```json
{
  "model": "qwen-turbo",
  "messages": [
    {
      "role": "system",
      "content": "你是一个智能助手..."
    },
    {
      "role": "user",
      "content": "今天北京天气怎么样"
    },
    {
      "role": "assistant",
      "tool_calls": [
        {
          "id": "call_abc123",
          "type": "function",
          "function": {
            "name": "get_weather",
            "arguments": "{\"city\":\"北京\"}"
          }
        }
      ]
    },
    {
      "role": "tool",
      "tool_call_id": "call_abc123",
      "name": "get_weather",
      "content": "北京天气：晴，温度：22℃，风向：南风，风力：3级，湿度：45%，更新时间：2025-11-18 09:00"
    }
  ],
  "stream": true
}
```

---

## 9️⃣ AI生成最终回复

### AI处理
```
1. 接收到工具返回的天气数据
   ↓
2. 理解数据: 北京/晴/22℃/南风/3级/45%
   ↓
3. 生成用户友好的自然语言回复
   ↓
4. 流式输出
```

### 流式响应
```json
// 逐个token返回
{"choices":[{"delta":{"content":"今"},"finish_reason":null}]}
{"choices":[{"delta":{"content":"天"},"finish_reason":null}]}
{"choices":[{"delta":{"content":"北"},"finish_reason":null}]}
{"choices":[{"delta":{"content":"京"},"finish_reason":null}]}
{"choices":[{"delta":{"content":"的"},"finish_reason":null}]}
{"choices":[{"delta":{"content":"天"},"finish_reason":null}]}
{"choices":[{"delta":{"content":"气"},"finish_reason":null}]}
{"choices":[{"delta":{"content":"很"},"finish_reason":null}]}
{"choices":[{"delta":{"content":"不"},"finish_reason":null}]}
{"choices":[{"delta":{"content":"错"},"finish_reason":null}]}
// ... 继续
{"choices":[{"delta":{"content":"。"},"finish_reason":"stop"}]}
```

### 最终回复
```
今天北京的天气很不错，是晴天，温度22℃，南风3级，湿度45%。
适合外出活动，记得做好防晒措施！
```

---

## 🔟 返回给前端

### Service层处理
```java
// AIChatService.java

return contentFlux
    .doOnNext(chunk -> {
        // 每个chunk: "今", "天", "北", "京", ...
        fullResponse.append(chunk);
    })
    .doOnComplete(() -> {
        // 保存消息到数据库
        saveMessages(userId, sessionId, originQuery, completeResponse);
    });
```

### 前端展示
```javascript
// 前端逐字显示
const reader = response.body.getReader();
while (true) {
  const {done, value} = await reader.read();
  if (done) break;
  
  const chunk = new TextDecoder().decode(value);
  // 显示: "今" "天" "北" ...
  appendToChat(chunk);
}
```

---

## 🎯 关键点总结

### 1. 工具如何被发现？
```
@Component注解 → Spring扫描 → 自动注入到McpToolRegistry
```

### 2. 工具如何被注册？
```
McpToolRegistry → McpToolAdapter → ToolCallbackProvider → ChatClient
```

### 3. AI如何知道调用哪个工具？
```
ToolDefinition {
  name: "get_weather",
  description: "获取...天气信息",  ← AI通过这个理解工具用途
  parameters: { ... }              ← AI通过这个知道传什么参数
}
```

### 4. 工具如何被执行？
```
AI返回tool_calls → Spring AI找到ToolCallback → McpToolAdapter.call()
→ McpTool.execute() → 实际业务逻辑
```

### 5. 结果如何返回用户？
```
工具结果 → 发送给AI → AI生成友好回复 → 流式返回前端
```

---

## 🔍 调试技巧

### 查看注册的工具
```java
// 启动日志
✅ 注册工具: get_weather (分类: basic, 版本: 2.0.0, 启用: true)
✅ 注册工具: get_location (分类: basic, 版本: 1.0.0, 启用: true)
...
✅ MCP工具系统配置完成，共注册 5 个工具
```

### 查看工具调用
```java
// 运行时日志
适配器执行工具: get_weather, 输入: {"city":"北京"}
天气查询成功：北京天气：晴，温度：22℃...
```

### 查看AI决策
```java
// Spring AI日志（开启DEBUG）
Sending request to AI with tools: [get_weather, get_location, ...]
AI returned tool_calls: get_weather with arguments: {"city":"北京"}
Executing tool: get_weather
Tool result: 北京天气：晴...
Sending tool result back to AI
```

---

## 💡 最佳实践

### 1. 工具描述要详细
```java
@Override
public String getDescription() {
    // ❌ 不好
    return "查天气";
    
    // ✅ 好
    return "获取指定城市或当前位置的实时天气信息。" +
           "如果提供了城市名称参数（如：北京、上海），则查询该城市的天气；" +
           "如果没有提供城市名称，则自动通过IP定位获取用户所在城市的天气。";
}
```

### 2. Schema要准确
```java
@Override
public Schema getInputSchema() {
    Map<String, Schema> properties = new HashMap<>();
    
    // 详细的参数描述帮助AI理解
    properties.put("city", Schema.string(
        "城市名称，例如：北京、上海、深圳等。" +
        "如果不提供此参数，系统将自动通过IP定位获取用户所在城市"
    ));
    
    // required=[] 表示city是可选的
    return Schema.object(properties, List.of());
}
```

### 3. 工具命名要语义化
```java
// ✅ 好的命名
"get_weather"      // 清晰表达意图
"plan_route"       // 动词+名词
"recommend_topics" // 描述性强

// ❌ 不好的命名
"weather"    // 不明确是查询还是设置
"tool1"      // 无意义
"func"       // 太泛化
```

---

## 🚀 扩展示例

### 添加新工具：路线规划

```java
@Component
@RequiredArgsConstructor
public class RoutePlanningMcpTool implements McpTool {
    
    @Override
    public String getName() {
        return "plan_route";
    }
    
    @Override
    public String getDescription() {
        return "规划两地之间的出行路线，支持驾车、公交、步行等多种出行方式";
    }
    
    @Override
    public Schema getInputSchema() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("origin", Schema.string("出发地"));
        properties.put("destination", Schema.string("目的地"));
        properties.put("mode", Schema.stringEnum(
            "出行方式", 
            List.of("driving", "transit", "walking")
        ));
        
        // origin和destination是必填的
        return Schema.object(properties, List.of("origin", "destination"));
    }
    
    @Override
    public Object execute(JsonNode input) {
        String origin = input.get("origin").asText();
        String destination = input.get("destination").asText();
        String mode = input.has("mode") 
            ? input.get("mode").asText() 
            : "driving";
        
        return routeService.planRoute(origin, destination, mode);
    }
}
```

**无需任何配置，重启后自动可用！**

---

这就是完整的MCP工具调用流程！从HTTP请求到工具执行，再到AI生成回复，整个链路清晰可控。
