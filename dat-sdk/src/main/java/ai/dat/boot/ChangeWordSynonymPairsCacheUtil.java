package ai.dat.boot;

import ai.dat.core.contentstore.data.WordSynonymPair;
import lombok.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 变化的同义词对缓存工具
 *
 * @Author JunjieM
 * @Date 2025/9/26
 */
class ChangeWordSynonymPairsCacheUtil {

    private ChangeWordSynonymPairsCacheUtil() {
    }

    // Map<projectId, Map<yamlFileRelativePath, List<WordSynonymPair>>>
    private final static Map<String, Map<String, List<WordSynonymPair>>> CACHE = new HashMap<>();

    public static void add(@NonNull String projectId,
                           @NonNull String relativePath,
                           @NonNull List<WordSynonymPair> wordSynonymPairs) {
        Map<String, List<WordSynonymPair>> listMap = new HashMap<>();
        if (CACHE.containsKey(projectId)) {
            listMap = CACHE.get(projectId);
        }
        listMap.put(relativePath, wordSynonymPairs);
        CACHE.put(projectId, listMap);
    }

    public static List<WordSynonymPair> get(@NonNull String projectId,
                                            @NonNull String relativePath) {
        Map<String, List<WordSynonymPair>> listMap = new HashMap<>();
        if (CACHE.containsKey(projectId)) {
            listMap = CACHE.get(projectId);
        }
        return listMap.get(relativePath);
    }

    public static Map<String, List<WordSynonymPair>> get(String projectId) {
        if (CACHE.containsKey(projectId)) {
            return CACHE.get(projectId);
        }
        return Collections.emptyMap();
    }

    public static Map<String, List<WordSynonymPair>> remove(String projectId) {
        return CACHE.remove(projectId);
    }
}
