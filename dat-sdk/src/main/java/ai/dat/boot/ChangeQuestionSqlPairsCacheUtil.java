package ai.dat.boot;

import ai.dat.core.contentstore.data.QuestionSqlPair;
import lombok.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 变化的问答SQL对缓存工具
 *
 * @Author JunjieM
 * @Date 2025/9/26
 */
class ChangeQuestionSqlPairsCacheUtil {

    private ChangeQuestionSqlPairsCacheUtil() {
    }

    // Map<projectId, Map<yamlFileRelativePath, List<QuestionSqlPair>>>
    private final static Map<String, Map<String, List<QuestionSqlPair>>> CACHE = new HashMap<>();

    public static void add(@NonNull String projectId,
                           @NonNull String relativePath,
                           @NonNull List<QuestionSqlPair> questionSqlPairs) {
        Map<String, List<QuestionSqlPair>> listMap = new HashMap<>();
        if (CACHE.containsKey(projectId)) {
            listMap = CACHE.get(projectId);
        }
        listMap.put(relativePath, questionSqlPairs);
        CACHE.put(projectId, listMap);
    }

    public static List<QuestionSqlPair> get(@NonNull String projectId,
                                            @NonNull String relativePath) {
        Map<String, List<QuestionSqlPair>> listMap = new HashMap<>();
        if (CACHE.containsKey(projectId)) {
            listMap = CACHE.get(projectId);
        }
        return listMap.get(relativePath);
    }

    public static Map<String, List<QuestionSqlPair>> get(String projectId) {
        if (CACHE.containsKey(projectId)) {
            return CACHE.get(projectId);
        }
        return Collections.emptyMap();
    }

    public static Map<String, List<QuestionSqlPair>> remove(String projectId) {
        return CACHE.remove(projectId);
    }
}
