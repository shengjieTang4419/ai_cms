package com.cloud.membership.service.impl;


import com.cloud.membership.domain.UserProfile;
import com.cloud.membership.domain.UserTagsDimension;
import com.cloud.memebership.domain.TagPath;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 可信度算法
 * @date 2026/1/11 19:16
 */
@Component
public class CredibilityService {

    /**
     * 路径前缀深度计算
     *
     * @param a
     * @param b
     * @return
     */
    public int commonPrefixDepth(List<String> a, List<String> b) {
        int depth = 0;
        for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
            if (a.get(i).equals(b.get(i))) {
                depth++;
            } else {
                break;
            }
        }
        return depth;
    }

    /**
     * 深度 → 证据强度函数
     *
     * @param depth
     * @return
     */
    public double depthWeight(int depth) {
        if (depth <= 0) return 0.5;   // 新领域，弱证据
        if (depth == 1) return 1.0;   // 同领域，正常证据
        if (depth == 2) return 2.0;   // 强相关
        return 3.0;                   // 非常强
    }

    public double getMatchGain(TagPath newPath, UserProfile profile) {
        double bestGain = 1.0; // 默认相信一次

        for (UserTagsDimension existing : profile.getUserTagsDimensions()) {
            int depth = commonPrefixDepth(newPath.getPath(), existing.getTagPath().getPath());
            double gain = depthWeight(depth);
            bestGain = Math.max(bestGain, gain);
        }
        return bestGain;
    }
}
