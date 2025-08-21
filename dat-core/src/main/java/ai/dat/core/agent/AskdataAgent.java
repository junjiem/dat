package ai.dat.core.agent;

import ai.dat.core.agent.data.EventOption;
import ai.dat.core.agent.data.StreamAction;
import ai.dat.core.contentstore.ContentStore;
import ai.dat.core.contentstore.data.QuestionSqlPair;
import dev.langchain4j.exception.UnsupportedFeatureException;

import java.util.List;
import java.util.Set;

/**
 * 问数Agent接口类
 *
 * @Author JunjieM
 * @Date 2025/6/25
 */
public interface AskdataAgent {

    ContentStore contentStore();

    Set<EventOption> eventOptions();

    StreamAction ask(String question);

    StreamAction ask(String question, List<QuestionSqlPair> histories);

    /**
     * Human-in-the-loop user (human) response
     *
     * @param response
     */
    default void userResponse(String response) {
        throw new UnsupportedFeatureException("Not supported yet.");
    }

    /**
     * Human-in-the-loop user (human) approval
     *
     * @param approval
     */
    default void userApproval(Boolean approval) {
        throw new UnsupportedFeatureException("Not supported yet.");
    }
}