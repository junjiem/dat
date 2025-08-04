package ai.dat.boot.cache;

import ai.dat.core.semantic.data.SemanticModel;
import lombok.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author JunjieM
 * @Date 2025/8/4
 */
public class SemanticModelsCacheUtil {

    private SemanticModelsCacheUtil() {
    }

    private final static Map<String, Map<String, List<SemanticModel>>> CACHE = new HashMap<>();

    public static void add(@NonNull String projectId,
                           @NonNull String relativePath,
                           @NonNull List<SemanticModel> models) {
        Map<String, List<SemanticModel>> listMap = new HashMap<>();
        if (CACHE.containsKey(projectId)) {
            listMap = CACHE.get(projectId);
        }
        listMap.put(relativePath, models);
        CACHE.put(projectId, listMap);
    }

    public static List<SemanticModel> get(@NonNull String projectId,
                                          @NonNull String relativePath) {
        Map<String, List<SemanticModel>> listMap = new HashMap<>();
        if (CACHE.containsKey(projectId)) {
            listMap = CACHE.get(projectId);
        }
        return listMap.get(relativePath);
    }

    public static Map<String, List<SemanticModel>> get(String projectId) {
        if (CACHE.containsKey(projectId)) {
            return CACHE.get(projectId);
        }
        return Collections.emptyMap();
    }

    public static Map<String, List<SemanticModel>> remove(String projectId) {
        return CACHE.remove(projectId);
    }
}
