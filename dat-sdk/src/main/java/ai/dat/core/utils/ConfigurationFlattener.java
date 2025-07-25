package ai.dat.core.utils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 配置扁平化工具类
 * 将嵌套的Map结构转换为点分隔的扁平键值对结构
 *
 * 例如：
 * {
 *   "timeout": 600,
 *   "kwargs": {
 *     "n": 1,
 *     "temperature": 0.6,
 *     "response_format": {
 *       "type": "text"
 *     }
 *   }
 * }
 *
 * 转换为：
 * {
 *   "timeout": 600,
 *   "kwargs.n": 1,
 *   "kwargs.temperature": 0.6,
 *   "kwargs.response_format.type": "text"
 * }
 *
 * @Author JunjieM
 * @Date 2025/7/16
 */
public class ConfigurationFlattener {
    private ConfigurationFlattener() {
    }

    private static final String SEPARATOR = ".";

    /**
     * 将嵌套的配置Map展平为点分隔的结构
     *
     * @param configuration 原始嵌套配置
     * @return 展平后的配置Map
     */
    public static Map<String, Object> flatten(Map<String, Object> configuration) {
        if (configuration == null || configuration.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Object> flattened = new HashMap<>();
        flattenRecursive(configuration, "", flattened);
        return flattened;
    }

    /**
     * 递归展平嵌套结构
     *
     * @param source 源配置Map
     * @param prefix 键前缀
     * @param target 目标展平Map
     */
    private static void flattenRecursive(Map<String, Object> source, String prefix, Map<String, Object> target) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            String fullKey = prefix.isEmpty() ? key : prefix + SEPARATOR + key;

            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                flattenRecursive(nestedMap, fullKey, target);
            } else if (value instanceof java.util.List) {
                flattenList((java.util.List<?>) value, fullKey, target);
            } else {
                target.put(fullKey, value);
            }
        }
    }

    /**
     * 扁平化列表结构
     *
     * @param list 列表对象
     * @param prefix 键前缀
     * @param target 目标展平Map
     */
    private static void flattenList(java.util.List<?> list, String prefix, Map<String, Object> target) {
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            String indexKey = prefix + SEPARATOR + i;

            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> itemMap = (Map<String, Object>) item;
                flattenRecursive(itemMap, indexKey, target);
            } else if (item instanceof java.util.List) {
                flattenList((java.util.List<?>) item, indexKey, target);
            } else {
                target.put(indexKey, item);
            }
        }
    }

    /**
     * 将展平的配置转换回嵌套结构
     *
     * @param flattenedConfiguration 展平的配置
     * @return 嵌套结构的配置Map
     */
    public static Map<String, Object> unflatten(Map<String, Object> flattenedConfiguration) {
        if (flattenedConfiguration == null || flattenedConfiguration.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Object> nested = new HashMap<>();

        for (Map.Entry<String, Object> entry : flattenedConfiguration.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            setNestedValue(nested, key, value);
        }

        return nested;
    }

    /**
     * 在嵌套Map中设置值
     *
     * @param target 目标Map
     * @param key 点分隔的键
     * @param value 值
     */
    @SuppressWarnings("unchecked")
    private static void setNestedValue(Map<String, Object> target, String key, Object value) {
        String[] keyParts = key.split("\\.");
        Object current = target;

        for (int i = 0; i < keyParts.length - 1; i++) {
            String part = keyParts[i];

            if (current instanceof Map) {
                Map<String, Object> currentMap = (Map<String, Object>) current;

                // 检查下一个部分是否为数字索引
                String nextPart = keyParts[i + 1];
                if (isNumeric(nextPart)) {
                    // 需要创建或获取列表
                    currentMap.computeIfAbsent(part, k -> new java.util.ArrayList<>());
                    current = currentMap.get(part);
                } else {
                    // 创建或获取嵌套Map
                    currentMap.computeIfAbsent(part, k -> new HashMap<String, Object>());
                    current = currentMap.get(part);
                }
            } else if (current instanceof java.util.List) {
                java.util.List<Object> currentList = (java.util.List<Object>) current;
                int index = Integer.parseInt(part);

                // 确保列表足够大
                while (currentList.size() <= index) {
                    currentList.add(null);
                }

                // 检查下一个部分确定需要创建什么类型的对象
                String nextPart = keyParts[i + 1];
                if (isNumeric(nextPart)) {
                    if (currentList.get(index) == null) {
                        currentList.set(index, new java.util.ArrayList<>());
                    }
                } else {
                    if (currentList.get(index) == null) {
                        currentList.set(index, new HashMap<String, Object>());
                    }
                }
                current = currentList.get(index);
            }
        }

        // 设置最终值
        String lastPart = keyParts[keyParts.length - 1];
        if (current instanceof Map) {
            ((Map<String, Object>) current).put(lastPart, value);
        } else if (current instanceof java.util.List) {
            java.util.List<Object> currentList = (java.util.List<Object>) current;
            int index = Integer.parseInt(lastPart);

            // 确保列表足够大
            while (currentList.size() <= index) {
                currentList.add(null);
            }
            currentList.set(index, value);
        }
    }

    /**
     * 检查字符串是否为数字
     *
     * @param str 字符串
     * @return 是否为数字
     */
    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 检查键是否为嵌套键（包含点分隔符）
     *
     * @param key 键名
     * @return 是否为嵌套键
     */
    public static boolean isNestedKey(String key) {
        return key != null && key.contains(SEPARATOR);
    }

    /**
     * 获取所有嵌套键的前缀集合
     *
     * @param configuration 配置Map
     * @return 前缀集合
     */
    public static Set<String> getNestedPrefixes(Map<String, Object> configuration) {
        if (configuration == null) {
            return Collections.emptySet();
        }
        return configuration.keySet().stream()
                .filter(ConfigurationFlattener::isNestedKey)
                .map(key -> key.substring(0, key.indexOf(SEPARATOR)))
                .collect(Collectors.toSet());
    }
} 