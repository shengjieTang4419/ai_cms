# AI 提示词管理

本目录用于存放 AI 聊天系统的各种提示词文件。

## 📁 文件说明

### system-prompt.txt
- **用途**: 系统级提示词，定义 AI 的角色、行为规则和工具使用方式
- **加载位置**: `ChatClientConfig.java`
- **修改后**: 需要重启应用生效

## 📝 如何修改提示词

1. **直接编辑文本文件**
   ```bash
   vim src/main/resources/prompts/system-prompt.txt
   ```

2. **重启应用**
   ```bash
   # 修改后需要重启 Spring Boot 应用
   mvn spring-boot:run
   ```

3. **验证加载**
   - 查看启动日志，确认提示词加载成功
   - 日志示例：`成功加载提示词文件: prompts/system-prompt.txt, 长度: XXX 字符`

## ✨ 优势

- ✅ **易于维护**: 提示词独立管理，不混入代码
- ✅ **版本控制**: 提示词修改可追踪
- ✅ **热修改**: 未来可扩展为热加载机制
- ✅ **多语言支持**: 可轻松添加多语言版本（如 system-prompt-en.txt）

## 🔧 扩展功能

如果需要添加新的提示词文件：

```java
// 在 PromptLoader.java 中添加新方法
public String loadCustomPrompt() {
    return loadPrompt("prompts/custom-prompt.txt");
}
```

## 📌 注意事项

1. 文件编码必须为 **UTF-8**
2. 提示词文件会在应用启动时加载到内存
3. 修改提示词后必须重启应用才能生效
4. 如果文件不存在或加载失败，会返回空字符串并记录错误日志

