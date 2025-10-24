package com.cloud.cloud.ai.chat.service;

import com.cloud.cloud.ai.chat.domain.OccupationEntity;

import java.util.List;
import java.util.Optional;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 职业信息服务接口
 * @date 2025/10/24 00:00
 */
public interface OccupationService {

    /**
     * 获取所有启用的职业列表
     */
    List<OccupationEntity> getAllActiveOccupations();

    /**
     * 根据职业代码获取职业信息
     */
    Optional<OccupationEntity> getByCode(Integer code);

    /**
     * 根据职业代码获取标签列表
     */
    List<String> getTagsByCode(Integer code);

    /**
     * 根据职业代码列表获取职业信息列表
     */
    List<OccupationEntity> getByCodeList(List<Integer> codes);
}

