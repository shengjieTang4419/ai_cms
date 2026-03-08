package com.cloud.membership.service;


import com.cloud.memebership.domain.UserPortraitDTO;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 用户画像服务
 * @date 2026/1/3 11:45
 */
public interface IUserPortraitService {

    UserPortraitDTO getUserPortrait(Long userId);

    /**
     * 创建用户画像
     * 1.从爱好职业 快速匹配一个预存的用户画像
     * 2.如果第一步失败 则调用AI模型创建对应的用户画像
     *
     * @param userId
     * @return
     */
    UserPortraitDTO createUserPortrait(Long userId);


}
