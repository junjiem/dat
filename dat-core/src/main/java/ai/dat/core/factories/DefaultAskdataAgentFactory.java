package ai.dat.core.factories;


import ai.dat.core.adapter.DatabaseAdapter;
import ai.dat.core.agent.AskdataAgent;
import ai.dat.core.agent.DefaultAskdataAgent;
import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.contentstore.ContentStore;
import ai.dat.core.semantic.data.SemanticModel;
import ai.dat.core.utils.FactoryUtil;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.NonNull;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/6/30
 */
public class DefaultAskdataAgentFactory implements AskdataAgentFactory {

    public static final String IDENTIFIER = "default";

    public static final ConfigOption<Boolean> INTENT_CLASSIFICATION =
            ConfigOptions.key("intent-classification")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription("Sets whether the intent classification");

    public static final ConfigOption<Boolean> SQL_GENERATION_REASONING =
            ConfigOptions.key("sql-generation-reasoning")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription("Sets whether the SQL generation reasoning");

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public AskdataAgent create(@NonNull ReadableConfig config,
                               List<SemanticModel> semanticModels,
                               @NonNull ContentStore contentStore,
                               @NonNull ChatModel chatModel,
                               @NonNull StreamingChatModel streamingChatModel,
                               @NonNull DatabaseAdapter databaseAdapter) {
        FactoryUtil.validateFactoryOptions(this, config);

        boolean intentClassification = config.get(INTENT_CLASSIFICATION);
        boolean sqlGenerationReasoning = config.get(SQL_GENERATION_REASONING);

        DefaultAskdataAgent.DefaultAskdataAgentBuilder builder = DefaultAskdataAgent.builder()
                .contentStore(contentStore)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .databaseAdapter(databaseAdapter)
                .intentClassification(intentClassification)
                .sqlGenerationReasoning(sqlGenerationReasoning);
        if (semanticModels != null && !semanticModels.isEmpty()) {
            builder.semanticModels(semanticModels);
        }
        return builder.build();
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        return Collections.emptySet();
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        return new LinkedHashSet<>(List.of(INTENT_CLASSIFICATION, SQL_GENERATION_REASONING));
    }
}