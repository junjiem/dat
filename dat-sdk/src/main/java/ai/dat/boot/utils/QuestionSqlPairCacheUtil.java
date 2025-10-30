package ai.dat.boot.utils;

import ai.dat.core.contentstore.data.QuestionSqlPair;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Provides an in-memory cache of question-SQL pairs keyed by conversation identifier.
 */
@Slf4j
public class QuestionSqlPairCacheUtil {
    /**
     * Utility class; prevent instantiation.
     */
    private QuestionSqlPairCacheUtil() {
    }

    // Map<conversationId, List<QuestionSqlPair>>
    private final static Map<String, List<QuestionSqlPair>> CACHE = new HashMap<>();

    /**
     * Adds a new question-SQL pair to the cache for the specified conversation.
     *
     * @param conversationId the unique conversation identifier
     * @param questionSqlPair the question-SQL pair to cache
     */
    public static void add(@NonNull String conversationId, @NonNull QuestionSqlPair questionSqlPair) {
        log.debug("conversationId: " + conversationId
                + "\nquestion: " + questionSqlPair.getQuestion()
                + "\nsql: " + questionSqlPair.getSql());
        List<QuestionSqlPair> list = new ArrayList<>();
        if (CACHE.containsKey(conversationId)) {
            list = CACHE.get(conversationId);
        }
        list.add(questionSqlPair);
        CACHE.put(conversationId, list);
    }

    /**
     * Retrieves cached question-SQL pairs for the specified conversation.
     *
     * @param conversationId the unique conversation identifier
     * @return the cached question-SQL pairs, or an empty list when none are present
     */
    public static List<QuestionSqlPair> get(@NonNull String conversationId) {
        if (CACHE.containsKey(conversationId)) {
            return CACHE.get(conversationId);
        }
        return Collections.emptyList();
    }
}
