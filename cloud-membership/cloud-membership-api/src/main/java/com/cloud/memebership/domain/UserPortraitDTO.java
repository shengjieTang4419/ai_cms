package com.cloud.memebership.domain;


import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 用户画像
 * @date 2026/1/3 11:57
 */
@Data
public class UserPortraitDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;

    private Long userId;

    private String userName;

    private Date createdAt;

    //过期时间 默认一个月过期
    private Date expireAt;

    // 热点标签
    private List<UserTagDTO> hotTags = new ArrayList<>();

    // 价值对话 top 一个月内top30 激烈竞争
    private List<ValueChat> valueChats = new ArrayList<>();

}
