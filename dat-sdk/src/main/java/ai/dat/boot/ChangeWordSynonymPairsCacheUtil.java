package ai.dat.boot;

import ai.dat.core.contentstore.data.WordSynonymPair;
import lombok.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Caches synonym pairs that have changed for a project to support incremental updates.
 */
class ChangeWordSynonymPairsCacheUtil {

    /**
     * Utility class; prevent instantiation.
     */
    private ChangeWordSynonymPairsCacheUtil() {
    }

    // Map<projectId, Map<yamlFileRelativePath, List<WordSynonymPair>>>
    private final static Map<String, Map<String, List<WordSynonymPair>>> CACHE = new HashMap<>();

    /**
     * Adds changed synonym pairs for the specified project and schema file.
     *
     * @param projectId the identifier of the project
     * @param relativePath the relative path of the schema file
     * @param wordSynonymPairs the synonym pairs that have changed
     */
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

    /**
     * Retrieves changed synonym pairs for the specified project and schema file.
     *
     * @param projectId the identifier of the project
     * @param relativePath the relative path of the schema file
     * @return the list of cached synonym pairs, or {@code null} when none exist
     */
    public static List<WordSynonymPair> get(@NonNull String projectId,
                                            @NonNull String relativePath) {
        Map<String, List<WordSynonymPair>> listMap = new HashMap<>();
        if (CACHE.containsKey(projectId)) {
            listMap = CACHE.get(projectId);
        }
        return listMap.get(relativePath);
    }

    /**
     * Retrieves all cached synonym pairs for the specified project.
     *
     * @param projectId the identifier of the project
     * @return a map keyed by schema file relative paths containing synonym pairs
     */
    public static Map<String, List<WordSynonymPair>> get(String projectId) {
        if (CACHE.containsKey(projectId)) {
            return CACHE.get(projectId);
        }
        return Collections.emptyMap();
    }

    /**
     * Removes and returns all cached synonym pairs for the specified project.
     *
     * @param projectId the identifier of the project
     * @return the removed cache entries, or {@code null} if nothing was cached
     */
    public static Map<String, List<WordSynonymPair>> remove(String projectId) {
        return CACHE.remove(projectId);
    }
}
