package com.cloud.common.core.converter;

import org.springframework.beans.BeanUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 泛型 Bean 转换器，用于 PO 和 DTO 之间的转换
 * @date 2025/12/09
 */
public class BeanConverter {

    /**
     * 单个对象转换
     *
     * @param source      源对象
     * @param targetClass 目标类型
     * @param <S>         源类型
     * @param <T>         目标类型
     * @return 转换后的对象
     */
    public static <S, T> T convert(S source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }

        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("对象转换失败: " + e.getMessage(), e);
        }
    }

    /**
     * 单个对象转换（使用 Supplier）
     * 适用于目标类没有无参构造函数的情况
     *
     * @param source         源对象
     * @param targetSupplier 目标对象供应器
     * @param <S>            源类型
     * @param <T>            目标类型
     * @return 转换后的对象
     */
    public static <S, T> T convert(S source, Supplier<T> targetSupplier) {
        if (source == null) {
            return null;
        }

        T target = targetSupplier.get();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    /**
     * 列表对象转换
     *
     * @param sourceList  源对象列表
     * @param targetClass 目标类型
     * @param <S>         源类型
     * @param <T>         目标类型
     * @return 转换后的对象列表
     */
    public static <S, T> List<T> convertList(List<S> sourceList, Class<T> targetClass) {
        if (sourceList == null || sourceList.isEmpty()) {
            return Collections.emptyList();
        }

        return sourceList.stream()
                .map(source -> convert(source, targetClass))
                .collect(Collectors.toList());
    }

    /**
     * 列表对象转换（使用 Supplier）
     *
     * @param sourceList     源对象列表
     * @param targetSupplier 目标对象供应器
     * @param <S>            源类型
     * @param <T>            目标类型
     * @return 转换后的对象列表
     */
    public static <S, T> List<T> convertList(List<S> sourceList, Supplier<T> targetSupplier) {
        if (sourceList == null || sourceList.isEmpty()) {
            return Collections.emptyList();
        }

        return sourceList.stream()
                .map(source -> convert(source, targetSupplier))
                .collect(Collectors.toList());
    }

    /**
     * 带自定义映射的单个对象转换
     * 用于复杂的转换逻辑
     *
     * @param source      源对象
     * @param targetClass 目标类型
     * @param callback    自定义回调，用于处理特殊字段
     * @param <S>         源类型
     * @param <T>         目标类型
     * @return 转换后的对象
     */
    public static <S, T> T convert(S source, Class<T> targetClass, ConvertCallback<S, T> callback) {
        if (source == null) {
            return null;
        }

        T target = convert(source, targetClass);
        if (callback != null) {
            callback.customize(source, target);
        }
        return target;
    }

    /**
     * 带自定义映射的列表对象转换
     *
     * @param sourceList  源对象列表
     * @param targetClass 目标类型
     * @param callback    自定义回调
     * @param <S>         源类型
     * @param <T>         目标类型
     * @return 转换后的对象列表
     */
    public static <S, T> List<T> convertList(List<S> sourceList, Class<T> targetClass, ConvertCallback<S, T> callback) {
        if (sourceList == null || sourceList.isEmpty()) {
            return Collections.emptyList();
        }

        return sourceList.stream()
                .map(source -> convert(source, targetClass, callback))
                .collect(Collectors.toList());
    }

    /**
     * 自定义转换回调接口
     *
     * @param <S> 源类型
     * @param <T> 目标类型
     */
    @FunctionalInterface
    public interface ConvertCallback<S, T> {
        /**
         * 自定义转换逻辑
         *
         * @param source 源对象
         * @param target 目标对象
         */
        void customize(S source, T target);
    }
}
