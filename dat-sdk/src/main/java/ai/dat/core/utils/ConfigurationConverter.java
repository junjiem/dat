package ai.dat.core.utils;

import ai.dat.core.configuration.Configuration;
import ai.dat.core.configuration.ReadableConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置转换工具类
 * 将嵌套的Map结构转换为ReadableConfig，支持嵌套键的扁平化处理
 *
 * @Author JunjieM
 * @Date 2025/7/16
 */
public class ConfigurationConverter {

    private ConfigurationConverter() {
    }

    /**
     * 将嵌套的Map配置转换为ReadableConfig
     * 会自动扁平化嵌套结构，使用点分隔符连接嵌套键
     *
     * @param configurationMap 嵌套的配置Map
     * @return ReadableConfig实例
     */
    public static ReadableConfig fromMap(Map<String, Object> configurationMap) {
        if (configurationMap == null || configurationMap.isEmpty()) {
            return new Configuration();
        }
        Map<String, Object> flattened = ConfigurationFlattener.flatten(configurationMap);
        Map<String, String> stringMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : flattened.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value != null) {
                stringMap.put(key, value.toString());
            }
        }
        return Configuration.fromMap(stringMap);
    }

    /**
     * 将ReadableConfig转换为嵌套的Map结构
     *
     * @param config ReadableConfig实例
     * @return 嵌套的配置Map
     */
    public static Map<String, Object> toNestedMap(ReadableConfig config) {
        if (config == null) {
            return new HashMap<>();
        }
        Map<String, String> flatMap = config.toMap();
        Map<String, Object> objectMap = new HashMap<>();
        for (Map.Entry<String, String> entry : flatMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            objectMap.put(key, convertStringToObject(value));
        }
        return ConfigurationFlattener.unflatten(objectMap);
    }

    /**
     * 尝试将字符串值转换为合适的对象类型
     *
     * @param value 字符串值
     * @return 转换后的对象
     */
    private static Object convertStringToObject(String value) {
        if (value == null) {
            return null;
        }
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // 继续尝试其他类型
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            // 继续尝试其他类型
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            // 继续尝试其他类型
        }
        return value;
    }
} 