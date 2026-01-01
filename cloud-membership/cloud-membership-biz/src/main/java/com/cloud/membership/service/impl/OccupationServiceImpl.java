package com.cloud.membership.service.impl;


import com.cloud.membership.domain.OccupationEntity;
import com.cloud.membership.repository.OccupationRepository;
import com.cloud.membership.service.OccupationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 职业信息服务实现类
 * @date 2025/10/24 00:00
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OccupationServiceImpl implements OccupationService {

    private final OccupationRepository occupationRepository;

    @Override
    @Cacheable(value = "occupations", key = "'all_active'")
    public List<OccupationEntity> getAllActiveOccupations() {
        log.debug("查询所有启用的职业列表");
        return occupationRepository.findByStatusOrderBySortOrder(1);
    }

    @Override
    @Cacheable(value = "occupations", key = "#code")
    public Optional<OccupationEntity> getByCode(Integer code) {
        if (code == null) {
            return Optional.empty();
        }
        log.debug("根据职业代码查询职业信息: {}", code);
        return occupationRepository.findByCode(code);
    }

    @Override
    public List<String> getTagsByCode(Integer code) {
        if (code == null) {
            return Collections.singletonList("职业");
        }

        Optional<OccupationEntity> occupation = getByCode(code);
        if (occupation.isPresent() && occupation.get().getTags() != null) {
            return new ArrayList<>(occupation.get().getTags());
        }

        return Collections.singletonList("职业");
    }

    @Override
    public List<OccupationEntity> getByCodeList(List<Integer> codes) {
        if (codes == null || codes.isEmpty()) {
            return Collections.emptyList();
        }
        log.debug("根据职业代码列表查询职业信息: {}", codes);
        return occupationRepository.findByCodeIn(codes);
    }
}

