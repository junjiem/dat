package ai.dat.boot;

import ai.dat.core.contentstore.data.QuestionSqlPair;
import lombok.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Caches question-SQL pairs that have changed for a project to be processed during incremental builds.
 */
class ChangeQuestionSqlPairsCacheUtil {

    /**
     * Utility class; prevent instantiation.
     */
    private ChangeQuestionSqlPairsCacheUtil() {
    }

    // Map<projectId, Map<yamlFileRelativePath, List<QuestionSqlPair>>>
    private final static Map<String, Map<String, List<QuestionSqlPair>>> CACHE = new HashMap<>();

    /**
     * Adds a set of changed question-SQL pairs for a given project and schema file.
     *
     * @param projectId the identifier of the project
     * @param relativePath the relative path of the schema file
     * @param questionSqlPairs the question-SQL pairs that have changed
     */
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

    /**
     * Retrieves changed question-SQL pairs for the specified project and schema file.
     *
     * @param projectId the identifier of the project
     * @param relativePath the relative path of the schema file
     * @return the list of cached question-SQL pairs, or {@code null} when none exist
     */
    public static List<QuestionSqlPair> get(@NonNull String projectId,
                                            @NonNull String relativePath) {
        Map<String, List<QuestionSqlPair>> listMap = new HashMap<>();
        if (CACHE.containsKey(projectId)) {
            listMap = CACHE.get(projectId);
        }
        return listMap.get(relativePath);
    }

    /**
     * Retrieves all cached question-SQL pairs for the specified project.
     *
     * @param projectId the identifier of the project
     * @return a map keyed by schema file relative paths containing changed pairs
     */
    public static Map<String, List<QuestionSqlPair>> get(String projectId) {
        if (CACHE.containsKey(projectId)) {
            return CACHE.get(projectId);
        }
        return Collections.emptyMap();
    }

    /**
     * Removes and returns all cached question-SQL pairs for the specified project.
     *
     * @param projectId the identifier of the project
     * @return the removed cache entries, or {@code null} if nothing was cached
     */
    public static Map<String, List<QuestionSqlPair>> remove(String projectId) {
        return CACHE.remove(projectId);
    }
}
