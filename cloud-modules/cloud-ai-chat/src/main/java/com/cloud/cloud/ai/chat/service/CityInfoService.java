package com.cloud.cloud.ai.chat.service;

import com.cloud.cloud.ai.chat.domain.CityInfo;
import com.cloud.cloud.ai.chat.repository.CityInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * 城市信息服务类
 * 提供城市名称到城市编码的智能匹配功能
 *
 * @author shengjie.tang
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CityInfoService {

    private final CityInfoRepository cityInfoRepository;

    /**
     * 根据城市名称智能匹配城市编码
     * 支持多种匹配策略：精确匹配、模糊匹配、省市区组合匹配等
     *
     * @param cityName 用户输入的城市名称（如：上海、北京、浦东新区、上海市浦东新区等）
     * @return 匹配的城市信息，如果未找到返回null
     */
    public CityInfo getCityInfoByName(String cityName) {
        if (!StringUtils.hasText(cityName)) {
            log.warn("城市名称为空");
            return null;
        }

        // 清理输入：去除空格和特殊字符
        String cleanName = cityName.trim().replaceAll("\\s+", "");

        log.info("开始匹配城市：{}", cleanName);

        // 1. 精确匹配完整名称
        Optional<CityInfo> exactMatch = cityInfoRepository.findByNameAndEnabled(cleanName, 1);
        if (exactMatch.isPresent()) {
            log.info("精确匹配成功：{} -> {}", cleanName, exactMatch.get().getAmapCityCode());
            return exactMatch.get();
        }

        // 2. 模糊匹配（优先匹配完整名称）
        List<CityInfo> fuzzyMatches = cityInfoRepository.findByKeywordFuzzyMatch(cleanName, 1);
        if (!fuzzyMatches.isEmpty()) {
            CityInfo bestMatch = selectBestMatch(cleanName, fuzzyMatches);
            log.info("模糊匹配成功：{} -> {} ({})", cleanName, bestMatch.getAmapCityCode(), bestMatch.getFullName());
            return bestMatch;
        }

        // 3. 尝试分割匹配（例如："上海市浦东新区" -> 先匹配"浦东新区"，再匹配"上海市"）
        CityInfo splitMatch = trySplitMatch(cleanName);
        if (splitMatch != null) {
            log.info("分割匹配成功：{} -> {} ({})", cleanName, splitMatch.getAmapCityCode(), splitMatch.getFullName());
            return splitMatch;
        }

        // 4. 省份+城市匹配（例如："广东深圳" -> 先找广东省，再找深圳）
        CityInfo provinceCityMatch = tryProvinceCityMatch(cleanName);
        if (provinceCityMatch != null) {
            log.info("省市匹配成功：{} -> {} ({})", cleanName, provinceCityMatch.getAmapCityCode(), provinceCityMatch.getFullName());
            return provinceCityMatch;
        }

        log.warn("未找到匹配的城市：{}", cleanName);
        return null;
    }

    /**
     * 根据城市名称获取城市编码
     *
     * @param cityName 城市名称
     * @return 城市编码，如果未找到返回null
     */
    public String getCityCode(String cityName) {
        CityInfo cityInfo = getCityInfoByName(cityName);
        return cityInfo != null ? cityInfo.getAmapCityCode() : null;
    }

    /**
     * 从候选列表中选择最佳匹配
     */
    private CityInfo selectBestMatch(String input, List<CityInfo> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }

        // 优先级排序：
        // 1. 完整名称完全匹配
        // 2. 名称长度最接近输入长度
        // 3. 行政级别较低（具体城市优先于省份）

        return candidates.stream()
                .min((c1, c2) -> {
                    // 完整名称匹配优先
                    boolean c1FullMatch = c1.getFullName().equals(input) || c1.getName().equals(input);
                    boolean c2FullMatch = c2.getFullName().equals(input) || c2.getName().equals(input);
                    if (c1FullMatch && !c2FullMatch) return -1;
                    if (!c1FullMatch && c2FullMatch) return 1;

                    // 名称长度相似度
                    int c1LengthDiff = Math.abs(c1.getName().length() - input.length());
                    int c2LengthDiff = Math.abs(c2.getName().length() - input.length());
                    if (c1LengthDiff != c2LengthDiff) {
                        return Integer.compare(c1LengthDiff, c2LengthDiff);
                    }

                    // 行政级别优先（具体城市优先）
                    return Integer.compare(c1.getLevel(), c2.getLevel());
                })
                .orElse(candidates.get(0));
    }

    /**
     * 尝试分割匹配
     * 例如："上海市浦东新区" -> 先匹配"浦东新区"，再匹配"上海市"
     */
    private CityInfo trySplitMatch(String input) {
        // 尝试常见的分割模式
        String[] separators = {"新区", "区", "市", "省", "自治区", "特别行政区"};

        for (String separator : separators) {
            if (input.contains(separator)) {
                String[] parts = input.split(separator);
                for (String part : parts) {
                    if (part.length() > 1) { // 避免单个字的匹配
                        Optional<CityInfo> match = cityInfoRepository.findByNameAndEnabled(part.trim(), 1);
                        if (match.isPresent()) {
                            return match.get();
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * 尝试省份+城市匹配
     * 例如："广东深圳" -> 先找广东省，再找深圳
     */
    private CityInfo tryProvinceCityMatch(String input) {
        // 常见省份简称映射
        java.util.Map<String, String> provinceMap = java.util.Map.of(
            "广东", "广东省", "广西", "广西壮族自治区", "新疆", "新疆维吾尔自治区",
            "西藏", "西藏自治区", "内蒙古", "内蒙古自治区", "宁夏", "宁夏回族自治区"
        );

        // 尝试省份+城市模式
        for (java.util.Map.Entry<String, String> entry : provinceMap.entrySet()) {
            if (input.startsWith(entry.getKey())) {
                String province = entry.getValue();
                String cityPart = input.substring(entry.getKey().length()).trim();

                // 在该省份下查找城市
                List<CityInfo> provinceCities = cityInfoRepository.findByProvinceAndLevelAndEnabledOrderByName(province, 3, 1);
                for (CityInfo city : provinceCities) {
                    if (city.getCity().contains(cityPart) || city.getName().contains(cityPart)) {
                        return city;
                    }
                }
            }
        }

        return null;
    }

    /**
     * 获取热门城市列表（用于提示用户）
     */
    public List<CityInfo> getPopularCities() {
        return cityInfoRepository.findPopularCities(1);
    }

    /**
     * 根据城市编码获取城市信息
     */
    public CityInfo getCityInfoByCode(String cityCode) {
        return cityInfoRepository.findByAmapCityCodeAndEnabled(cityCode, 1).orElse(null);
    }

    /**
     * 获取所有城市（用于管理界面）
     */
    public List<CityInfo> getAllCities() {
        return cityInfoRepository.findByEnabledOrderByLevelAsc(1);
    }

    /**
     * 添加或更新城市信息
     */
    public CityInfo saveCityInfo(CityInfo cityInfo) {
        return cityInfoRepository.save(cityInfo);
    }
}
