package com.cloud.cloud.ai.chat.mcp.api;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * MCP工具统一抽象接口
 * <p>
 * 所有MCP工具必须实现此接口，提供统一的工具定义、参数验证和执行逻辑
 *
 * @author shengjie.tang
 * @version 1.0.0
 * @date 2025/11/16
 */
public interface McpTool {

    /**
     * 获取工具名称（唯一标识）
     * 例如：get_weather, plan_route
     *
     * @return 工具名称
     */
    String getName();

    /**
     * 获取工具描述
     * 用于AI模型理解工具的用途和使用场景
     *
     * @return 工具描述
     */
    String getDescription();

    /**
     * 获取输入参数的Schema定义
     * 用于参数验证，避免AI产生幻觉参数
     * 此参数就是@Tool内的 key value的集合
     *
     * @return 输入参数Schema
     */
    Schema getInputSchema();

    /**
     * 获取输出结果的Schema定义
     * 用于结果验证，确保输出格式符合预期
     *
     * @return 输出结果Schema
     */
    Schema getOutputSchema();

    /**
     * 判断是否匹配用户意图（可选）
     * 可以通过关键词、语义等方式判断工具是否适用于当前查询
     *
     * @param query 用户查询
     * @return 是否匹配
     */
    default boolean match(String query) {
        return false;
    }

    /**
     * 执行工具逻辑
     *
     * @param input 输入参数（已通过Schema验证）
     * @return 执行结果（需符合输出Schema）
     * @throws Exception 执行异常
     */
    Object execute(JsonNode input) throws Exception;

    /**
     * 获取工具分类（用于分组管理）
     * 例如：basic（基础工具）、life（生活工具）、office（办公工具）
     *
     * @return 工具分类
     */
    default String getCategory() {
        return "default";
    }

    /**
     * 获取工具版本
     *
     * @return 版本号
     */
    default String getVersion() {
        return "1.0.0";
    }

    /**
     * 工具是否可用
     * 可以根据配置、权限等判断工具是否可用
     *
     * @return 是否可用
     */
    default boolean isEnabled() {
        return true;
    }
}
