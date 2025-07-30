package ai.dat.core.factories;


import ai.dat.core.adapter.DatabaseAdapter;
import ai.dat.core.agent.AskdataAgent;
import ai.dat.core.agent.DefaultAskdataAgent;
import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.configuration.description.Description;
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

    public static final ConfigOption<String> LANGUAGE =
            ConfigOptions.key("language")
                    .stringType()
                    .defaultValue("Simplified Chinese")
                    .withDescription("The language used in answer during conversations. " +
                            "For example: 'Simplified Chinese', 'English', '简体中文', '英语', etc.");

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

    public static final ConfigOption<String> TEXT_TO_SQL_RULES =
            ConfigOptions.key("text-to-sql-rules")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Customize the text-to-SQL rules. " +
                            "When the value is empty, use the built-in text-to-SQL rules.");

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public String factoryDescription() {
        return "The default ask data agent is implemented using the workflow mode.";
    }

    @Override
    public AskdataAgent create(@NonNull ReadableConfig config,
                               List<SemanticModel> semanticModels,
                               @NonNull ContentStore contentStore,
                               @NonNull ChatModel chatModel,
                               @NonNull StreamingChatModel streamingChatModel,
                               @NonNull DatabaseAdapter databaseAdapter) {
        FactoryUtil.validateFactoryOptions(this, config);

        String language = config.get(LANGUAGE);
        boolean intentClassification = config.get(INTENT_CLASSIFICATION);
        boolean sqlGenerationReasoning = config.get(SQL_GENERATION_REASONING);

        DefaultAskdataAgent.DefaultAskdataAgentBuilder builder = DefaultAskdataAgent.builder()
                .contentStore(contentStore)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .databaseAdapter(databaseAdapter)
                .language(language)
                .intentClassification(intentClassification)
                .sqlGenerationReasoning(sqlGenerationReasoning);
        config.getOptional(TEXT_TO_SQL_RULES).ifPresent(builder::textToSqlRules);
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
        return new LinkedHashSet<>(List.of(LANGUAGE, INTENT_CLASSIFICATION,
                SQL_GENERATION_REASONING, TEXT_TO_SQL_RULES));
    }
}