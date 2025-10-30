package ai.dat.boot;

import ai.dat.core.semantic.data.SemanticModel;
import lombok.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Caches semantic models that have changed for a project in preparation for incremental updates.
 */
class ChangeSemanticModelsCacheUtil {

    /**
     * Utility class; prevent instantiation.
     */
    private ChangeSemanticModelsCacheUtil() {
    }

    // Map<projectId, Map<yamlFileRelativePath, List<SemanticModel>>>
    private final static Map<String, Map<String, List<SemanticModel>>> CACHE = new HashMap<>();

    /**
     * Adds changed semantic models for the specified project and schema file.
     *
     * @param projectId the identifier of the project
     * @param relativePath the relative path of the schema file
     * @param models the semantic models that have changed
     */
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

    /**
     * Retrieves changed semantic models for the specified project and schema file.
     *
     * @param projectId the identifier of the project
     * @param relativePath the relative path of the schema file
     * @return the list of cached semantic models, or {@code null} when none exist
     */
    public static List<SemanticModel> get(@NonNull String projectId,
                                          @NonNull String relativePath) {
        Map<String, List<SemanticModel>> listMap = new HashMap<>();
        if (CACHE.containsKey(projectId)) {
            listMap = CACHE.get(projectId);
        }
        return listMap.get(relativePath);
    }

    /**
     * Retrieves all cached semantic models for the specified project.
     *
     * @param projectId the identifier of the project
     * @return a map keyed by schema file relative paths containing semantic models
     */
    public static Map<String, List<SemanticModel>> get(String projectId) {
        if (CACHE.containsKey(projectId)) {
            return CACHE.get(projectId);
        }
        return Collections.emptyMap();
    }

    /**
     * Removes and returns all cached semantic models for the specified project.
     *
     * @param projectId the identifier of the project
     * @return the removed cache entries, or {@code null} if nothing was cached
     */
    public static Map<String, List<SemanticModel>> remove(String projectId) {
        return CACHE.remove(projectId);
    }
}
