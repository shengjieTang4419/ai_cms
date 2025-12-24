package com.cloud.membership.repository;

import com.cloud.membership.domain.OccupationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 职业信息Repository
 * @date 2025/10/24 00:00
 */
@Repository
public interface OccupationRepository extends JpaRepository<OccupationEntity, Long> {

    /**
     * 根据职业代码查询
     */
    Optional<OccupationEntity> findByCode(Integer code);

    /**
     * 查询所有启用的职业，按排序顺序排列
     */
    List<OccupationEntity> findByStatusOrderBySortOrder(Integer status);

    /**
     * 根据职业代码列表查询
     */
    List<OccupationEntity> findByCodeIn(List<Integer> codes);
}

