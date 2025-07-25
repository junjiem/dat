package ai.dat.core.agent;

import ai.dat.core.agent.data.EventOption;
import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/7/17
 */
public class DefaultEventOptions {

    public static final ConfigOption<String> CONTENT =
            ConfigOptions.key("content")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("content");

    public static final ConfigOption<String> ERROR =
            ConfigOptions.key("error")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("error message");

    // ----------------------------- exception --------------------------

    public static final ConfigOption<String> MESSAGE =
            ConfigOptions.key("message")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("message");

    public static final EventOption EXCEPTION_EVENT = EventOption.builder()
            .name("exception")
            .dataOptions(Set.of(MESSAGE))
            .build();

    // ----------------------------- intent_classification --------------------------

    public static final ConfigOption<String> REASONING =
            ConfigOptions.key("reasoning")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Brief chain-of-thought reasoning (max 20 words)");

    public static final ConfigOption<DefaultAskdataAgent.Intent> INTENT =
            ConfigOptions.key("intent")
                    .enumType(DefaultAskdataAgent.Intent.class)
                    .noDefaultValue()
                    .withDescription("The intent of intent classification");

    public static final ConfigOption<String> REPHRASED_QUESTION =
            ConfigOptions.key("rephrased_question")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Rephrased question in full standalone question if there are previous questions, " +
                            "otherwise the original question");

    public static final EventOption INTENT_CLASSIFICATION_EVENT = EventOption.builder()
            .name("intent_classification")
            .dataOptions(Set.of(REPHRASED_QUESTION, REASONING, INTENT))
            .build();

    // ----------------------------- misleading_assistance --------------------------

    public static final EventOption MISLEADING_ASSISTANCE_EVENT = EventOption.builder()
            .name("misleading_assistance")
            .incrementalOption(CONTENT)
            .dataOptions(Set.of(CONTENT, ERROR))
            .build();

    // ----------------------------- data_assistance --------------------------

    public static final EventOption DATA_ASSISTANCE_EVENT = EventOption.builder()
            .name("data_assistance")
            .incrementalOption(CONTENT)
            .dataOptions(Set.of(CONTENT, ERROR))
            .build();

    // ----------------------------- sql_generation_reasoning --------------------------

    public static final EventOption SQL_GENERATION_REASONING_EVENT = EventOption.builder()
            .name("sql_generation_reasoning")
            .incrementalOption(CONTENT)
            .dataOptions(Set.of(CONTENT, ERROR))
            .build();

    // ----------------------------- sql_generate --------------------------

    public static final ConfigOption<String> SQL =
            ConfigOptions.key("sql")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("SQL");

    public static final EventOption SQL_GENERATE_EVENT = EventOption.builder()
            .name("sql_generate")
            .semanticSqlOption(SQL)
            .dataOptions(Set.of(SQL))
            .build();

    // ----------------------------- semantic_to_sql --------------------------

    public static final EventOption SEMANTIC_TO_SQL_EVENT = EventOption.builder()
            .name("semantic_to_sql")
            .querySqlOption(SQL)
            .dataOptions(Set.of(SQL))
            .build();

    // ----------------------------- sql_execute --------------------------

    public static final ConfigOption<List<Map<String, Object>>> DATA =
            ConfigOptions.key("data")
                    .mapType((Class<Map<String, Object>>) (Class<?>) Map.class)
                    .asList()
                    .noDefaultValue()
                    .withDescription("The data queried from the database");

    public static final EventOption SQL_EXECUTE_EVENT = EventOption.builder()
            .name("sql_execute")
            .queryDataOption(DATA)
            .dataOptions(Set.of(DATA, ERROR))
            .build();
}
