package ai.dat.boot;

import lombok.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Caches knowledge documents that have changed for a project during incremental builds.
 */
class ChangeKnowledgeCacheUtil {

    /**
     * Utility class; prevent instantiation.
     */
    private ChangeKnowledgeCacheUtil() {
    }

    // Map<projectId, Map<yamlFileRelativePath, List<String>>>
    private final static Map<String, Map<String, List<String>>> CACHE = new HashMap<>();

    /**
     * Adds changed knowledge entries for the specified project and schema file.
     *
     * @param projectId the identifier of the project
     * @param relativePath the relative path of the schema file
     * @param knowledge the knowledge entries that have changed
     */
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

    /**
     * Retrieves changed knowledge entries for the specified project and schema file.
     *
     * @param projectId the identifier of the project
     * @param relativePath the relative path of the schema file
     * @return the list of cached knowledge entries, or {@code null} when none exist
     */
    public static List<String> get(@NonNull String projectId,
                                   @NonNull String relativePath) {
        Map<String, List<String>> listMap = new HashMap<>();
        if (CACHE.containsKey(projectId)) {
            listMap = CACHE.get(projectId);
        }
        return listMap.get(relativePath);
    }

    /**
     * Retrieves all cached knowledge entries for the specified project.
     *
     * @param projectId the identifier of the project
     * @return a map keyed by schema file relative paths containing changed knowledge entries
     */
    public static Map<String, List<String>> get(String projectId) {
        if (CACHE.containsKey(projectId)) {
            return CACHE.get(projectId);
        }
        return Collections.emptyMap();
    }

    /**
     * Removes and returns all cached knowledge entries for the specified project.
     *
     * @param projectId the identifier of the project
     * @return the removed cache entries, or {@code null} if nothing was cached
     */
    public static Map<String, List<String>> remove(String projectId) {
        return CACHE.remove(projectId);
    }
}
