package com.cloud.gateway.utils;

import org.springframework.http.server.PathContainer;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;

/**
 * 安全服务工具类
 */
public class SecurityUtils {
    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    /**
     * 判断请求URI是否在忽略列表中
     * @param requestUri 请求URI
     * @param ignoreUrls 忽略的URL列表
     * @return 是否忽略
     */
    public static boolean isIgnoreUrl(String requestUri, List<String> ignoreUrls) {
        if (ignoreUrls == null || ignoreUrls.isEmpty()) {
            return false;
        }
        return ignoreUrls.stream().anyMatch(url -> ANT_PATH_MATCHER.match(url, requestUri));
    }
}
