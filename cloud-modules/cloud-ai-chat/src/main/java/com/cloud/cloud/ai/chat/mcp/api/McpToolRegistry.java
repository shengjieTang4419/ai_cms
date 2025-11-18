package com.cloud.cloud.ai.chat.mcp.api;

import com.cloud.cloud.ai.chat.mcp.api.exception.ToolNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MCP工具注册器 - 自动发现和管理所有工具
 * 
 * @author shengjie.tang
 * @version 1.0.0
 * @date 2025/11/16
 */
@Component
@Slf4j
public class McpToolRegistry {
    
    /**
     * 工具存储 - key: 工具名称, value: 工具实例
     */
    private final Map<String, McpTool> tools = new ConcurrentHashMap<>();
    
    /**
     * 分类索引 - key: 分类名称, value: 工具名称列表
     */
    private final Map<String, List<String>> categoryIndex = new ConcurrentHashMap<>();
    
    /**
     * 自动注入所有实现了McpTool接口的Bean
     */
    @Autowired(required = false)
    private List<McpTool> mcpTools;
    
    /**
     * 初始化 - 自动注册所有工具
     */
    @PostConstruct
    public void init() {
        if (mcpTools == null || mcpTools.isEmpty()) {
            log.warn("未发现任何MCP工具");
            return;
        }
        
        log.info("开始注册MCP工具，共发现 {} 个工具", mcpTools.size());
        
        for (McpTool tool : mcpTools) {
            register(tool);
        }
        
        log.info("MCP工具注册完成，共注册 {} 个工具", tools.size());
        logToolSummary();
    }
    
    /**
     * 注册单个工具
     */
    public void register(McpTool tool) {
        if (tool == null) {
            log.warn("尝试注册null工具");
            return;
        }
        
        String name = tool.getName();
        if (name == null || name.trim().isEmpty()) {
            log.warn("工具名称为空，跳过注册: {}", tool.getClass().getName());
            return;
        }
        
        if (tools.containsKey(name)) {
            log.warn("工具名称冲突，覆盖旧工具: {}", name);
        }
        
        tools.put(name, tool);
        
        // 更新分类索引
        String category = tool.getCategory();
        categoryIndex.computeIfAbsent(category, k -> new ArrayList<>()).add(name);
        
        log.info("注册工具: {} (分类: {}, 版本: {}, 启用: {})", 
                 name, category, tool.getVersion(), tool.isEnabled());
    }
    
    /**
     * 获取指定名称的工具
     */
    public McpTool get(String toolName) {
        McpTool tool = tools.get(toolName);
        if (tool == null) {
            throw new ToolNotFoundException(toolName);
        }
        return tool;
    }
    
    /**
     * 获取所有工具
     */
    public Collection<McpTool> getAllTools() {
        return new ArrayList<>(tools.values());
    }
    
    /**
     * 获取所有可用的工具
     */
    public List<McpTool> getEnabledTools() {
        return tools.values().stream()
                .filter(McpTool::isEnabled)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据分类获取工具
     */
    public List<McpTool> getToolsByCategory(String category) {
        List<String> toolNames = categoryIndex.get(category);
        if (toolNames == null || toolNames.isEmpty()) {
            return Collections.emptyList();
        }
        
        return toolNames.stream()
                .map(tools::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * 检查工具是否存在
     */
    public boolean exists(String toolName) {
        return tools.containsKey(toolName);
    }
    
    /**
     * 获取工具数量
     */
    public int getToolCount() {
        return tools.size();
    }
    
    /**
     * 获取所有分类
     */
    public Set<String> getAllCategories() {
        return new HashSet<>(categoryIndex.keySet());
    }
    
    /**
     * 根据查询匹配工具
     */
    public List<McpTool> matchTools(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        return tools.values().stream()
                .filter(McpTool::isEnabled)
                .filter(tool -> tool.match(query))
                .collect(Collectors.toList());
    }
    
    /**
     * 注销工具
     */
    public void unregister(String toolName) {
        McpTool removed = tools.remove(toolName);
        if (removed != null) {
            // 从分类索引中移除
            String category = removed.getCategory();
            List<String> categoryTools = categoryIndex.get(category);
            if (categoryTools != null) {
                categoryTools.remove(toolName);
            }
            log.info("注销工具: {}", toolName);
        }
    }
    
    /**
     * 清空所有工具
     */
    public void clear() {
        tools.clear();
        categoryIndex.clear();
        log.info("清空所有工具");
    }
    
    /**
     * 输出工具摘要信息
     */
    private void logToolSummary() {
        log.info("=== MCP工具注册摘要 ===");
        log.info("总工具数: {}", tools.size());
        log.info("可用工具数: {}", getEnabledTools().size());
        log.info("分类数: {}", categoryIndex.size());
        
        for (Map.Entry<String, List<String>> entry : categoryIndex.entrySet()) {
            log.info("  - {}: {} 个工具 {}", entry.getKey(), entry.getValue().size(), entry.getValue());
        }
    }
    
    /**
     * 获取工具清单（用于前端展示或AI理解）
     */
    public List<Map<String, Object>> getToolManifest() {
        return getEnabledTools().stream()
                .map(tool -> {
                    Map<String, Object> manifest = new HashMap<>();
                    manifest.put("name", tool.getName());
                    manifest.put("description", tool.getDescription());
                    manifest.put("category", tool.getCategory());
                    manifest.put("version", tool.getVersion());
                    manifest.put("inputSchema", tool.getInputSchema());
                    manifest.put("outputSchema", tool.getOutputSchema());
                    return manifest;
                })
                .collect(Collectors.toList());
    }
}
