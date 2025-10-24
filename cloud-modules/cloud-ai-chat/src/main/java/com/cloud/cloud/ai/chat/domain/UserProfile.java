package com.cloud.cloud.ai.chat.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 用户基础画像表
 * @date 2025/10/14 14:01
 */
@Data
@Entity
@Table(name = "user_profiles")
public class UserProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /**
     * 性别：男、女、其他
     */
    @Column(name = "gender", length = 10)
    private String gender;

    /**
     * 年龄
     */
    @Column(name = "age")
    private Integer age;

    /**
     * 居住地
     */
    @Column(name = "location")
    private String location;

    /**
     * 职业（职业代码）
     */
    @Column(name = "occupation")
    private Integer occupation;

    /**
     * 爱好（JSON格式存储为List）
     */
    @Column(name = "hobbies")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> hobbies;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @Transient
    private List<UserTagsDimension> userTagsDimensions = new ArrayList<>();
}
