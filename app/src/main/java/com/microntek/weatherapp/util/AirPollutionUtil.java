package com.microntek.weatherapp.util;

/**
 * 空气污染指数描述转换工具类
 * 用于将API返回的空气污染扩散条件的描述转换为更易理解的内容
 */
public class AirPollutionUtil {

    /**
     * 将原始的空气污染扩散描述转换为更易理解的文本
     * @param originalDescription 原始API返回的描述
     * @return 转换后的描述
     */
    public static String convertDescription(String originalDescription) {
        if (originalDescription == null || originalDescription.isEmpty()) {
            return "一般";
        }

        // 根据原始描述转换为更合适的文本
        switch (originalDescription) {
            case "较少开启":
                return "优良";
            case "不宜开启":
                return "较差";
            case "开启":
                return "良好";
            case "开启较少":
                return "中等";
            default:
                // 如果是其他描述，返回原始描述或替换为默认值
                if (isValidDescription(originalDescription)) {
                    return originalDescription;
                } else {
                    return "一般";
                }
        }
    }

    /**
     * 检查描述是否有效且合理
     * @param description 描述文本
     * @return 是否为有效合理的描述
     */
    private static boolean isValidDescription(String description) {
        // 这里可以添加一些合理的空气质量描述词
        String[] validDescriptions = {
            "优", "良", "优良", "轻度污染", "中度污染", 
            "重度污染", "严重污染", "良好", "较差", "中等"
        };

        for (String valid : validDescriptions) {
            if (description.contains(valid)) {
                return true;
            }
        }
        return false;
    }
} 