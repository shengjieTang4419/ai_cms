package com.cloud.cloud.ai.chat.domain;

import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import java.util.List;

/**
 * @author shengjie.tang
 * @version 1.0.0
 * @description: 地理编码响应 - 地址转坐标
 * @date 2025/01/17
 */
@Data
public class GeocodeResponse {

    private String status;
    private String info;
    private String infocode;
    private String count;
    private List<Geocode> geocodes;

    @Data
    public static class Geocode {
        private String formatted_address;  // 格式化地址
        private String country;             // 国家
        private String province;            // 省份
        private String city;               // 城市
        private String district;            // 区县
        private Object township;            // 街道（可能是字符串或数组）
        private Neighborhood neighborhood;  // 社区（对象，包含name和type字段）
        private Building building;          // 建筑（对象，包含name和type字段）
        private String adcode;              // 区域编码
        private String citycode;           // 城市编码
        private Object street;             // 街道（可能是字符串或数组）
        private Object number;              // 门牌号（可能是字符串或数组）
        private String location;            // 坐标（经度,纬度）
        private String level;               // 匹配级别

        /**
         * 获取township的字符串值
         * 如果township是数组，返回第一个元素；如果是字符串，直接返回
         */
        public String getTownshipAsString() {
            return extractStringFromObject(township);
        }

        /**
         * 获取street的字符串值
         */
        public String getStreetAsString() {
            return extractStringFromObject(street);
        }

        /**
         * 获取number的字符串值
         */
        public String getNumberAsString() {
            return extractStringFromObject(number);
        }

        /**
         * 从Object中提取字符串值（处理字符串或数组的情况）
         */
        private String extractStringFromObject(Object obj) {
            if (obj == null) {
                return null;
            }
            if (obj instanceof String) {
                return (String) obj;
            }
            if (obj instanceof List) {
                List<?> list = (List<?>) obj;
                if (!list.isEmpty() && list.get(0) != null) {
                    return list.get(0).toString();
                }
                return null; // 空数组返回null
            }
            return obj.toString();
        }

        /**
         * 自定义setter，处理township可能是数组的情况
         */
        @JsonSetter("township")
        public void setTownship(Object township) {
            this.township = township;
        }

        /**
         * 自定义setter，处理street可能是数组的情况
         */
        @JsonSetter("street")
        public void setStreet(Object street) {
            this.street = street;
        }

        /**
         * 自定义setter，处理number可能是数组的情况
         */
        @JsonSetter("number")
        public void setNumber(Object number) {
            this.number = number;
        }
    }

    /**
     * 社区信息（neighborhood字段）
     */
    @Data
    public static class Neighborhood {
        private Object name;  // 社区名称（可能是字符串或数组）
        private Object type;  // 社区类型（可能是字符串或数组）

        /**
         * 获取name的字符串值
         */
        public String getNameAsString() {
            return extractStringFromObject(name);
        }

        /**
         * 获取type的字符串值
         */
        public String getTypeAsString() {
            return extractStringFromObject(type);
        }

        private String extractStringFromObject(Object obj) {
            if (obj == null) {
                return null;
            }
            if (obj instanceof String) {
                return (String) obj;
            }
            if (obj instanceof List) {
                List<?> list = (List<?>) obj;
                if (!list.isEmpty() && list.get(0) != null) {
                    return list.get(0).toString();
                }
                return null;
            }
            return obj.toString();
        }

        @JsonSetter("name")
        public void setName(Object name) {
            this.name = name;
        }

        @JsonSetter("type")
        public void setType(Object type) {
            this.type = type;
        }
    }

    /**
     * 建筑信息（building字段）
     */
    @Data
    public static class Building {
        private Object name;  // 建筑名称（可能是字符串或数组）
        private Object type;  // 建筑类型（可能是字符串或数组）

        /**
         * 获取name的字符串值
         */
        public String getNameAsString() {
            return extractStringFromObject(name);
        }

        /**
         * 获取type的字符串值
         */
        public String getTypeAsString() {
            return extractStringFromObject(type);
        }

        private String extractStringFromObject(Object obj) {
            if (obj == null) {
                return null;
            }
            if (obj instanceof String) {
                return (String) obj;
            }
            if (obj instanceof List) {
                List<?> list = (List<?>) obj;
                if (!list.isEmpty() && list.get(0) != null) {
                    return list.get(0).toString();
                }
                return null;
            }
            return obj.toString();
        }

        @JsonSetter("name")
        public void setName(Object name) {
            this.name = name;
        }

        @JsonSetter("type")
        public void setType(Object type) {
            this.type = type;
        }
    }
}

