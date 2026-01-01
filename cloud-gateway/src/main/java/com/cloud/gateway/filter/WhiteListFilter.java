package com.cloud.gateway.filter;

import com.cloud.gateway.config.properties.IgnoreWhiteProperties;
import com.cloud.gateway.utils.SecurityUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 白名单过滤器
 * 用于放行白名单中的请求
 */
@Component
public class WhiteListFilter implements GlobalFilter, Ordered {
    
    private final IgnoreWhiteProperties ignoreWhiteProperties;
    
    public WhiteListFilter(IgnoreWhiteProperties ignoreWhiteProperties) {
        this.ignoreWhiteProperties = ignoreWhiteProperties;
    }
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestPath = request.getPath().pathWithinApplication().value();
        
        // 检查是否是白名单请求
        if (SecurityUtils.isIgnoreUrl(requestPath, ignoreWhiteProperties.getWhites())) {
            // 设置请求头，标记为白名单请求
            ServerHttpRequest newRequest = request.mutate()
                    .header("is-white", "true")
                    .build();
            return chain.filter(exchange.mutate().request(newRequest).build());
        }
        
        return chain.filter(exchange);
    }
    
    @Override
    public int getOrder() {
        // 设置过滤器顺序，值越小优先级越高
        return -200;
    }
}
