package com.cloud.membership.repository;

import com.cloud.membership.domain.StereotypeTemplate;
import com.cloud.membership.domain.enums.AgeRange;
import com.cloud.membership.domain.enums.Gender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 刻板印象模板Repository
 * @date 2026/2/5 16:00
 */
@Repository
public interface StereotypeTemplateRepository extends JpaRepository<StereotypeTemplate, Long> {

    /**
     * 根据模板Key查询
     */
    Optional<StereotypeTemplate> findByTemplateKey(String templateKey);

    /**
     * 精确匹配：性别 + 年龄段 + 职业
     */
    Optional<StereotypeTemplate> findByGenderAndAgeRangeAndOccupationCodeAndStatus(
            Gender gender, AgeRange ageRange, Integer occupationCode, Integer status);

    /**
     * 模糊匹配：按优先级降序，支持部分字段为空
     */
    @Query("SELECT t FROM StereotypeTemplate t WHERE t.status = 1 " +
            "AND (t.gender = :gender OR t.gender IS NULL) " +
            "AND (t.ageRange = :ageRange OR t.ageRange IS NULL) " +
            "AND (t.occupationCode = :occupationCode OR t.occupationCode IS NULL) " +
            "ORDER BY t.priority DESC")
    List<StereotypeTemplate> findMatchingTemplates(
            @Param("gender") Gender gender,
            @Param("ageRange") AgeRange ageRange,
            @Param("occupationCode") Integer occupationCode);

    /**
     * 查询所有启用的模板
     */
    List<StereotypeTemplate> findByStatusOrderByPriorityDesc(Integer status);

    /**
     * 根据职业代码查询模板
     */
    List<StereotypeTemplate> findByOccupationCodeAndStatus(Integer occupationCode, Integer status);

    /**
     * 检查模板是否存在
     */
    boolean existsByTemplateKey(String templateKey);
}
