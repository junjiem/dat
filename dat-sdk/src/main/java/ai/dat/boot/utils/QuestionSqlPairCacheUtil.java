package ai.dat.boot.utils;

import ai.dat.core.contentstore.data.QuestionSqlPair;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * @Author JunjieM
 * @Date 2025/9/28
 */
@Slf4j
public class QuestionSqlPairCacheUtil {
    private QuestionSqlPairCacheUtil() {
    }

    // Map<conversationId, List<QuestionSqlPair>>
    private final static Map<String, List<QuestionSqlPair>> CACHE = new HashMap<>();

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

    public static List<QuestionSqlPair> get(@NonNull String conversationId) {
        if (CACHE.containsKey(conversationId)) {
            return CACHE.get(conversationId);
        }
        return Collections.emptyList();
    }
}
