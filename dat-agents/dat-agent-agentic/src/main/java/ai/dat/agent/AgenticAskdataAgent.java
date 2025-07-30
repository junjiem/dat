package ai.dat.agent;

import ai.dat.core.adapter.DatabaseAdapter;
import ai.dat.core.agent.AbstractAskdataAgent;
import ai.dat.core.agent.data.EventOption;
import ai.dat.core.agent.data.StreamAction;
import ai.dat.core.contentstore.ContentStore;
import ai.dat.core.contentstore.data.QuestionSqlPair;
import ai.dat.core.semantic.data.SemanticModel;
import ai.dat.core.utils.SemanticModelUtil;
import com.google.common.base.Preconditions;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.Builder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/7/29
 */
public class AgenticAskdataAgent extends AbstractAskdataAgent {

    private final List<SemanticModel> semanticModels;

    private final Integer maxIterations;

    @Builder
    public AgenticAskdataAgent(ContentStore contentStore,
                               DatabaseAdapter databaseAdapter,
                               ChatModel chatModel,
                               StreamingChatModel streamingChatModel,
                               List<SemanticModel> semanticModels,
                               Integer maxIterations) {
        super(contentStore, databaseAdapter);
        SemanticModelUtil.validateSemanticModels(semanticModels);
        this.semanticModels = semanticModels;
        this.maxIterations = Optional.ofNullable(maxIterations).orElse(5);
        Preconditions.checkArgument(this.maxIterations <= 30 && this.maxIterations >= 1,
                "maxIterations must be between 1 and 30");
        // TODO ...
    }

    @Override
    public Set<EventOption> eventOptions() {
        return null;
    }

    @Override
    protected void ask(String question, StreamAction action, List<QuestionSqlPair> histories) {
        int iteration = 1;
        boolean functionCallState = true;
        while (functionCallState && iteration <= maxIterations) {
            // TODO ...
        }
    }

}
