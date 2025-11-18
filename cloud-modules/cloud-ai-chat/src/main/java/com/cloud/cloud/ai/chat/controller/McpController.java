package com.cloud.cloud.ai.chat.controller;

import com.cloud.cloud.ai.chat.mcp.api.McpExecutor;
import com.cloud.cloud.ai.chat.mcp.api.McpToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MCP工具控制器
 * 
 * @author shengjie.tang
 * @version 1.0.0
 * @date 2025/11/16
 */
@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
@Slf4j
public class McpController {
    
    private final McpExecutor mcpExecutor;
    private final McpToolRegistry mcpToolRegistry;
    
    /**
     * 执行工具调用
     * 
     * AI会发送如下格式的请求：
     * {
     *   "tool": "get_weather",
     *   "arguments": {
     *     "city": "北京"
     *   }
     * }
     */
    @PostMapping("/execute")
    public ApiResponse<Object> executeTool(@RequestBody ToolCallRequest request) {
        try {
            log.info("收到工具调用请求: tool={}, arguments={}", request.getTool(), request.getArguments());
            
            Object result = mcpExecutor.executeToolCall(request.getTool(), request.getArguments());
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("工具执行失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 批量执行工具调用
     */
    @PostMapping("/execute/batch")
    public ApiResponse<Map<String, Object>> executeBatch(@RequestBody BatchToolCallRequest request) {
        try {
            log.info("收到批量工具调用请求，共{}个工具", request.getCalls().size());
            
            Map<String, Object> results = mcpExecutor.executeBatch(request.getCalls());
            
            return ApiResponse.success(results);
        } catch (Exception e) {
            log.error("批量工具执行失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 获取所有可用工具的清单
     * 用于前端展示或AI理解可用工具
     */
    @GetMapping("/tools")
    public ApiResponse<List<Map<String, Object>>> getToolManifest() {
        try {
            List<Map<String, Object>> manifest = mcpToolRegistry.getToolManifest();
            return ApiResponse.success(manifest);
        } catch (Exception e) {
            log.error("获取工具清单失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 获取工具统计信息
     */
    @GetMapping("/tools/stats")
    public ApiResponse<ToolStats> getToolStats() {
        try {
            ToolStats stats = new ToolStats();
            stats.setTotalTools(mcpToolRegistry.getToolCount());
            stats.setEnabledTools(mcpToolRegistry.getEnabledTools().size());
            stats.setCategories(mcpToolRegistry.getAllCategories().size());
            stats.setCategoryList(mcpToolRegistry.getAllCategories());
            
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("获取工具统计失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 根据分类获取工具
     */
    @GetMapping("/tools/category/{category}")
    public ApiResponse<List<String>> getToolsByCategory(@PathVariable String category) {
        try {
            List<String> tools = mcpToolRegistry.getToolsByCategory(category)
                    .stream()
                    .map(tool -> tool.getName())
                    .toList();
            
            return ApiResponse.success(tools);
        } catch (Exception e) {
            log.error("获取分类工具失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 工具调用请求
     */
    @Data
    public static class ToolCallRequest {
        private String tool;
        private JsonNode arguments;
    }
    
    /**
     * 批量工具调用请求
     */
    @Data
    public static class BatchToolCallRequest {
        private List<McpExecutor.ToolCall> calls;
    }
    
    /**
     * 工具统计信息
     */
    @Data
    public static class ToolStats {
        private int totalTools;
        private int enabledTools;
        private int categories;
        private java.util.Set<String> categoryList;
    }
    
    /**
     * 统一响应格式
     */
    @Data
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        
        public static <T> ApiResponse<T> success(T data) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setSuccess(true);
            response.setMessage("success");
            response.setData(data);
            return response;
        }
        
        public static <T> ApiResponse<T> error(String message) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setSuccess(false);
            response.setMessage(message);
            return response;
        }
    }
}
