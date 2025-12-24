package com.cloud.ai.chat.repository;

import com.cloud.ai.chat.domain.CityInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 城市信息Repository
 *
 * @author shengjie.tang
 * @version 1.0.0
 */
@Repository
public interface CityInfoRepository extends JpaRepository<CityInfo, Long> {

    /**
     * 根据城市名称精确查询
     */
    Optional<CityInfo> findByNameAndEnabled(String name, Integer enabled);

    /**
     * 根据高德地图城市编码查询
     */
    Optional<CityInfo> findByAmapCityCodeAndEnabled(String amapCityCode, Integer enabled);

    /**
     * 根据行政区划代码查询
     */
    Optional<CityInfo> findByAdminCodeAndEnabled(String adminCode, Integer enabled);

    /**
     * 根据完整名称模糊查询（用于用户输入匹配）
     */
    List<CityInfo> findByFullNameContainingAndEnabledOrderByLevel(String fullName, Integer enabled);

    /**
     * 根据城市名称模糊查询（优先匹配完整名称）
     */
    @Query("SELECT c FROM CityInfo c WHERE c.enabled = :enabled " +
            "AND (c.fullName LIKE %:keyword% OR c.name LIKE %:keyword% " +
            "OR c.city LIKE %:keyword% OR c.district LIKE %:keyword%) " +
            "ORDER BY CASE " +
            "WHEN c.fullName LIKE %:keyword% THEN 1 " +
            "WHEN c.name LIKE %:keyword% THEN 2 " +
            "WHEN c.city LIKE %:keyword% THEN 3 " +
            "WHEN c.district LIKE %:keyword% THEN 4 " +
            "ELSE 5 END, c.level")
    List<CityInfo> findByKeywordFuzzyMatch(@Param("keyword") String keyword, @Param("enabled") Integer enabled);

    /**
     * 根据省份查询所有城市
     */
    List<CityInfo> findByProvinceAndLevelAndEnabledOrderByName(String province, Integer level, Integer enabled);

    /**
     * 根据城市查询所有区县
     */
    List<CityInfo> findByCityAndLevelAndEnabledOrderByName(String city, Integer level, Integer enabled);

    /**
     * 获取所有启用的城市（按级别排序）
     */
    List<CityInfo> findByEnabledOrderByLevelAsc(Integer enabled);

    /**
     * 检查城市名称是否存在
     */
    boolean existsByNameAndEnabled(String name, Integer enabled);

    /**
     * 根据行政级别统计城市数量
     */
    long countByLevelAndEnabled(Integer level, Integer enabled);

    /**
     * 查找热门城市（可以根据使用频率或其他条件）
     */
    @Query("SELECT c FROM CityInfo c WHERE c.enabled = :enabled AND c.level <= 3 " +
            "ORDER BY c.level, c.name")
    List<CityInfo> findPopularCities(@Param("enabled") Integer enabled);
}
