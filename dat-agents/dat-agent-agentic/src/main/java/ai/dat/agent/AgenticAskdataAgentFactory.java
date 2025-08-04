package ai.dat.agent;

import ai.dat.core.adapter.DatabaseAdapter;
import ai.dat.core.agent.AskdataAgent;
import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.contentstore.ContentStore;
import ai.dat.core.factories.AskdataAgentFactory;
import ai.dat.core.semantic.data.SemanticModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/7/29
 */
public class AgenticAskdataAgentFactory implements AskdataAgentFactory {

    public static final String IDENTIFIER = "agentic";

    public static final ConfigOption<String> INSTRUCTION =
            ConfigOptions.key("instruction")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The instruction of the agent");

    public static final ConfigOption<Integer> MAX_ITERATIONS =
            ConfigOptions.key("max-iterations")
                    .intType()
                    .defaultValue(5)
                    .withDescription("Maximum Iterations, value must be between 1 and 30");

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public String factoryDescription() {
        return "The ask data agent is implemented using the agentic workflow mode " +
                "and supports the addition of MCP tools.";
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        return Collections.emptySet();
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        return new LinkedHashSet<>(List.of(INSTRUCTION, MAX_ITERATIONS));
    }

    @Override
    public AskdataAgent create(ReadableConfig config,
                               List<SemanticModel> semanticModels,
                               ContentStore contentStore,
                               ChatModel chatModel,
                               StreamingChatModel streamingChatModel,
                               DatabaseAdapter databaseAdapter) {
        // TODO ...
        return null;
    }
}
