package ai.dat.boot;

import lombok.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 变化的业务知识缓存工具
 *
 * @Author JunjieM
 * @Date 2025/9/26
 */
class ChangeKnowledgeCacheUtil {

    private ChangeKnowledgeCacheUtil() {
    }

    // Map<projectId, Map<yamlFileRelativePath, List<String>>>
    private final static Map<String, Map<String, List<String>>> CACHE = new HashMap<>();

    public static void add(@NonNull String projectId,
                           @NonNull String relativePath,
                           @NonNull List<String> knowledge) {
        Map<String, List<String>> listMap = new HashMap<>();
        if (CACHE.containsKey(projectId)) {
            listMap = CACHE.get(projectId);
        }
        listMap.put(relativePath, knowledge);
        CACHE.put(projectId, listMap);
    }

    public static List<String> get(@NonNull String projectId,
                                   @NonNull String relativePath) {
        Map<String, List<String>> listMap = new HashMap<>();
        if (CACHE.containsKey(projectId)) {
            listMap = CACHE.get(projectId);
        }
        return listMap.get(relativePath);
    }

    public static Map<String, List<String>> get(String projectId) {
        if (CACHE.containsKey(projectId)) {
            return CACHE.get(projectId);
        }
        return Collections.emptyMap();
    }

    public static Map<String, List<String>> remove(String projectId) {
        return CACHE.remove(projectId);
    }
}
