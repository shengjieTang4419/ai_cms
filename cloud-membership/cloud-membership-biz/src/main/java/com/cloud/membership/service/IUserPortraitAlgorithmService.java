package com.cloud.membership.service;

import com.cloud.membership.domain.UserProfile;
import com.cloud.memebership.domain.UserPortraitDTO;
import com.cloud.memebership.domain.UserTagDTO;
import com.cloud.memebership.domain.ValueChat;

import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 用户画像算法
 * @date 2026/1/3 12:03
 */
public interface IUserPortraitAlgorithmService {

    /**
     * 一次有效的对话
     * 计算逻辑：话题路径匹配叠加权重
     *
     * @param valueChat
     */
    void onValueChat(ValueChat valueChat);

    /**
     * 衰减机制
     * 懒加载机制 由用户有效对话触达
     * 触达内部逻辑分为 活跃度衰减 + 自然时间衰减
     *
     * @param userId
     */
    void attenuation(Long userId);

    /**
     * 价值对话竞争
     * 对话竞争 参考redis W-TinyLFU
     * 引入末级淘汰策略与滑动窗口 对topK的话题和标签重定义
     * 话题本身存在score（score会自然衰减 attenuation）
     *
     * @param userPortraitDTO
     * @param sessionId       对话对象
     */
    void competition(UserPortraitDTO userPortraitDTO, String sessionId);

    /**
     * 标签坍缩
     * 核心语义：在用户标签熵增到一定程度后 对用户标签进行相似度整合
     * <p>
     * 借鉴思想 GC分代年龄思想+CAS自适应自旋时间思想+AI分析相似度
     *
     * @param userPortraitDTO
     * @return UserTagDTO
     */
    List<UserTagDTO> collapse(UserPortraitDTO userPortraitDTO);

    /**
     * 画像重生
     * 核心语义：当用户基础画像发生较大变化（职业/爱好/地区等）时，保留画像“内核”，削弱“外壳”，并重新发散
     *
     * @param beforeProfile 更新前的画像快照
     * @param afterProfile  更新后的画像快照
     */
    void rebirth(UserProfile beforeProfile, UserProfile afterProfile);

    /**
     * 快速发散匹配
     * 在每一次进入用户新生态的时候进行触达，用于快速匹配用户感兴趣的标签
     * 新生态分为以下场景
     * - 用户首次登录
     * 发散采用 70%的用户画像 + 20%相似更好画像指导 + 10%共同圈子话题
     * 发散对象标签
     * <p>
     * 方法抽离 在状态机内进行画像置信度的拆分
     *
     * @param userId
     */
    void divergence(Long userId);


    /**
     * 基于用户画像
     * 对话题进行引导 生产N个匹配的当前用户的具有up（采用up权重，默认10%）指示的话题
     *
     * @param userPortraitDTO
     * @return
     */
    List<String> guideTopics(UserPortraitDTO userPortraitDTO);
}
