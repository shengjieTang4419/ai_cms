package com.cloud.membership.domain;

import com.cloud.membership.domain.dto.SimpleProfile;
import com.cloud.membership.domain.enums.AgeRange;
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
    @Column(name = "user_id", nullable = false)
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
     * 职业名称
     */
    @Column(name = "occupation_name")
    private String occupationName;

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


    /**
     * 失效时间
     */
    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    /**
     * 画像版本
     */
    @Column(name = "version")
    private Integer version;

    /**
     * 熵增计数：记录标签竞争替换次数
     */
    @Column(name = "entropy_count")
    private Integer entropyCount = 0;

    /**
     * 坍缩年龄：记录坍缩次数，用于动态调整坍缩阈值
     */
    @Column(name = "collapse_age")
    private Integer collapseAge = 0;

    /**
     * 画像置信度：用于判断是否需要快速发散
     * 计算规则：collapseAge == 0 && entropyCount < 阈值 → 低置信度
     */
    @Column(name = "confidence_score")
    private Double confidenceScore = 0.0;

    @Column(name = "divergence_attempts")
    private Integer divergenceAttempts = 0;

    /**
     * 获取用户简单画像
     * 包含：性别、年龄段、居住地、职业
     * 不包含：爱好（用户可能不填写，且不可靠）
     *
     * @return 简单画像对象
     */
    public SimpleProfile getSimpleProfile() {
        AgeRange range = AgeRange.fromAge(this.age);

        return SimpleProfile.builder()
                .gender(this.getGender())
                .ageRange(range != null ? range.getLabel() : null)
                .location(this.getLocation())
                .occupationName(this.getOccupationName())
                .build();
    }

    @Transient
    private List<UserTagsDimension> userTagsDimensions = new ArrayList<>();
}
