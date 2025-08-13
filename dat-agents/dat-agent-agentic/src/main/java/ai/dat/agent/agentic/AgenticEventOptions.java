package ai.dat.agent.agentic;

import ai.dat.core.agent.data.EventOption;
import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Author JunjieM
 * @Date 2025/8/11
 */
public class AgenticEventOptions {

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
                    .mapObjectType()
                    .asList()
                    .noDefaultValue()
                    .withDescription("The data queried from the database");

    public static final EventOption SQL_EXECUTE_EVENT = EventOption.builder()
            .name("sql_execute")
            .queryDataOption(DATA)
            .dataOptions(Set.of(DATA, ERROR))
            .build();

    // ----------------------------- before_tool_execution --------------------------

    public static final ConfigOption<String> TOOL_NAME =
            ConfigOptions.key("name")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("tool execution request name");

    public static final ConfigOption<String> TOOL_ARGS =
            ConfigOptions.key("arguments")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("tool execution request arguments");

    public static final EventOption BEFORE_TOOL_EXECUTION = EventOption.builder()
            .name("before_tool_execution")
            .dataOptions(Set.of(TOOL_NAME, TOOL_ARGS))
            .build();

    // ----------------------------- tool_execution --------------------------

    public static final ConfigOption<String> TOOL_RESULT =
            ConfigOptions.key("result")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("tool execution result");

    public static final EventOption TOOL_EXECUTION = EventOption.builder()
            .name("tool_execution")
            .dataOptions(Set.of(TOOL_NAME, TOOL_ARGS, TOOL_RESULT))
            .build();

    // ----------------------------- agent_answer --------------------------

    public static final EventOption AGENT_ANSWER = EventOption.builder()
            .name("agent_answer")
            .incrementalOption(CONTENT)
            .dataOptions(Set.of(CONTENT, ERROR))
            .build();

    // ----------------------------- agent_answer --------------------------

    public static final ConfigOption<String> AI_REQUEST =
            ConfigOptions.key("ai_request")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("AI request");

    // ----------------------------- Human-in-the-loop ask_user --------------------------

    public static final EventOption HITL_ASK_USER = EventOption.builder()
            .name("hitl_ask_user")
            .hitlAiRequestOption(AI_REQUEST)
            .dataOptions(Set.of(AI_REQUEST))
            .build();
}
