package ai.dat.core.agent;

import ai.dat.core.adapter.DatabaseAdapter;
import ai.dat.core.agent.data.StreamAction;
import ai.dat.core.agent.data.StreamEvent;
import ai.dat.core.contentstore.ContentStore;
import ai.dat.core.contentstore.data.QuestionSqlPair;
import ai.dat.core.semantic.data.SemanticModel;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static ai.dat.core.agent.DefaultEventOptions.*;

/**
 * @Author JunjieM
 * @Date 2025/6/25
 */
@Slf4j
public abstract class AbstractAskdataAgent implements AskdataAgent {

    private static final ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger id = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("dat-agent-" + id.addAndGet(1));
            return thread;
        }
    });

    protected final ContentStore contentStore;
    protected final DatabaseAdapter databaseAdapter;

    public AbstractAskdataAgent(@NonNull ContentStore contentStore,
                                @NonNull DatabaseAdapter databaseAdapter) {
        this.contentStore = contentStore;
        this.databaseAdapter = databaseAdapter;
    }

    @Override
    public ContentStore contentStore() {
        return contentStore;
    }

    @Override
    public StreamAction ask(String question) {
        return ask(question, Collections.emptyList());
    }

    public StreamAction ask(String question, List<QuestionSqlPair> histories) {
        StreamAction action = new StreamAction();
        executor.execute(() -> {
            try {
                ask(question, action, histories);
            } catch (Exception e) {
                log.error("Ask data exception", e);
                action.add(StreamEvent.from(EXCEPTION_EVENT, MESSAGE, e.getMessage()));
            } finally {
                action.finished();
            }
        });
        return action;
    }

    protected abstract void ask(String question, StreamAction action, List<QuestionSqlPair> histories);

    protected List<Map<String, Object>> executeQuery(String semanticSql,
                                                     List<SemanticModel> semanticModels,
                                                     StreamAction action) throws SQLException {
        String sql = databaseAdapter.generateSql(semanticSql, semanticModels);
        action.add(StreamEvent.from(SEMANTIC_TO_SQL_EVENT, SQL, sql));
        try {
            List<Map<String, Object>> results = databaseAdapter.executeQuery(sql);
            action.add(StreamEvent.from(SQL_EXECUTE_EVENT, DATA, results));
            return results;
        } catch (SQLException e) {
            action.add(StreamEvent.from(SQL_EXECUTE_EVENT, ERROR, e.getMessage()));
            throw new SQLException(e);
        }
    }
}
