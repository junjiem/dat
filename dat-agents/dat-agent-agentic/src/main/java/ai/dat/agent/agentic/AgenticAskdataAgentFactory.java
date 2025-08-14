package ai.dat.agent.agentic;

import ai.dat.agent.agentic.mcp.McpTransportFactory;
import ai.dat.core.adapter.DatabaseAdapter;
import ai.dat.core.agent.AskdataAgent;
import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.Configuration;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.contentstore.ContentStore;
import ai.dat.core.factories.AskdataAgentFactory;
import ai.dat.core.factories.data.ChatModelInstance;
import ai.dat.core.semantic.data.SemanticModel;
import ai.dat.core.utils.FactoryUtil;
import com.google.common.base.Preconditions;
import dev.langchain4j.mcp.client.transport.McpTransport;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author JunjieM
 * @Date 2025/8/11
 */
public class AgenticAskdataAgentFactory implements AskdataAgentFactory {

    public static final String IDENTIFIER = "agentic";

    public static final ConfigOption<String> DEFAULT_LLM =
            ConfigOptions.key("default-llm")
                    .stringType()
                    .defaultValue("default")
                    .withDescription("Specify the default LLM model name. " +
                            "Note: This model needs to support tools (Function Calling)");

    public static final ConfigOption<Integer> MAX_TOOLS_INVOCATIONS =
            ConfigOptions.key("max-tools-invocations")
                    .intType()
                    .defaultValue(10)
                    .withDescription("Maximum number of tools invocations. " +
                            "Value must be between 1 and 100.");

    public static final ConfigOption<String> SQL_GENERATION_LLM =
            ConfigOptions.key("sql-generation-llm")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Specify the SQL generation LLM model name. " +
                            "If not specified, use the default llm.");

    public static final ConfigOption<Integer> MAX_MESSAGES =
            ConfigOptions.key("max-messages")
                    .intType()
                    .defaultValue(100)
                    .withDescription("Maximum number of messages. " +
                            "Messages covering the roles of user, assistant, and tool.");

    public static final ConfigOption<Integer> MAX_HISTORIES =
            ConfigOptions.key("max-histories")
                    .intType()
                    .defaultValue(20)
                    .withDescription("Maximum number of histories");

    public static final ConfigOption<String> TEXT_TO_SQL_RULES =
            ConfigOptions.key("text-to-sql-rules")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Customize the text-to-SQL rules. " +
                            "When the value is empty, use the built-in text-to-SQL rules.");

    public static final ConfigOption<String> INSTRUCTION =
            ConfigOptions.key("instruction")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("User instruction");

    public static final ConfigOption<Map<String, Object>> MCP_SERVERS =
            ConfigOptions.key("mcp-servers")
                    .mapObjectType()
                    .noDefaultValue()
                    .withDescription("""
                            MCP Servers configurations.
                            Supported transports are `stdio`, `http`.
                                                        
                            For example:
                            ```
                            mcp-servers:
                              server_name1:
                                transport: stdio
                                command: ["/usr/bin/npm", "exec", "@modelcontextprotocol/server-everything@0.6.2"]
                                log-events: true  # only if you want to see the traffic in the log
                              server_name2:
                                transport: http
                                sse-url: "http://localhost:3001/sse"
                                log-requests: true  # if you want to see the traffic in the log
                                log-responses: true
                                timeout: 60000
                            ```
                            """);

    public static final ConfigOption<Boolean> HUMAN_IN_THE_LOOP =
            ConfigOptions.key("human-in-the-loop")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription("Enable HITL (human-in-the-loop). The master switch of the human-in-the-loop.");

    public static final ConfigOption<Boolean> HUMAN_IN_THE_LOOP_ASK_USER =
            ConfigOptions.key("human-in-the-loop.ask-user")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription("""
                            Enable HITL (human-in-the-loop) Ask User. 
                            Have a human in the loop, allowing the system to ask user's input for \
                            missing information or approval before proceeding with certain actions.
                            """);

    public static final ConfigOption<Boolean> HUMAN_IN_THE_LOOP_TOOL_APPROVAL =
            ConfigOptions.key("human-in-the-loop.tool-approval")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("""
                            Enable HITL (human-in-the-loop) Tool Approval. 
                            Have a human in the loop, allowing the system to apply to the user for \
                            approval before proceeding with execution tool.
                            """);


    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        return Collections.emptySet();
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        return new LinkedHashSet<>(List.of(
                DEFAULT_LLM, MAX_MESSAGES, MAX_HISTORIES, MAX_TOOLS_INVOCATIONS, SQL_GENERATION_LLM,
                TEXT_TO_SQL_RULES, INSTRUCTION, MCP_SERVERS,
                HUMAN_IN_THE_LOOP, HUMAN_IN_THE_LOOP_ASK_USER, HUMAN_IN_THE_LOOP_TOOL_APPROVAL
        ));
    }

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public String factoryDescription() {
        return "The ask data agent is implemented using the tools (Function Calling) agent mode " +
                "and supports the addition of MCP tools.";
    }

    @Override
    public AskdataAgent create(ReadableConfig config,
                               List<SemanticModel> semanticModels,
                               ContentStore contentStore,
                               List<ChatModelInstance> chatModelInstances,
                               DatabaseAdapter databaseAdapter) {
        Preconditions.checkArgument(!chatModelInstances.isEmpty(),
                "chatModelInstances cannot be empty");
        FactoryUtil.validateFactoryOptions(this, config);
        Map<String, ChatModelInstance> instances = chatModelInstances.stream()
                .collect(Collectors.toMap(ChatModelInstance::getName, i -> i));
        validateConfigOptions(config, instances);

        ChatModelInstance defaultInstance = config.getOptional(DEFAULT_LLM)
                .map(instances::get).orElseGet(() -> chatModelInstances.get(0));
        ChatModelInstance sqlGenerationInstance = config.getOptional(SQL_GENERATION_LLM)
                .map(instances::get).orElse(defaultInstance);

        Integer maxToolsInvocations = config.get(MAX_TOOLS_INVOCATIONS);
        Integer maxMessages = config.get(MAX_MESSAGES);
        Integer maxHistories = config.get(MAX_HISTORIES);
        Boolean humanInTheLoop = config.get(HUMAN_IN_THE_LOOP);
        Boolean humanInTheLoopAskUser = config.get(HUMAN_IN_THE_LOOP_ASK_USER);
        Boolean humanInTheLoopToolApproval = config.get(HUMAN_IN_THE_LOOP_TOOL_APPROVAL);

        AgenticAskdataAgent.AgenticAskdataAgentBuilder builder = AgenticAskdataAgent.builder()
                .contentStore(contentStore)
                .databaseAdapter(databaseAdapter)
                .defaultModel(defaultInstance.getChatModel())
                .defaultStreamingModel(defaultInstance.getStreamingChatModel())
                .text2sqlModel(sqlGenerationInstance.getChatModel())
                .maxToolsInvocations(maxToolsInvocations)
                .maxMessages(maxMessages)
                .maxHistories(maxHistories)
                .humanInTheLoop(humanInTheLoop)
                .humanInTheLoopAskUser(humanInTheLoopAskUser)
                .humanInTheLoopToolApproval(humanInTheLoopToolApproval);

        config.getOptional(TEXT_TO_SQL_RULES).ifPresent(builder::textToSqlRules);
        config.getOptional(INSTRUCTION).ifPresent(builder::instruction);

        if (semanticModels != null && !semanticModels.isEmpty()) {
            builder.semanticModels(semanticModels);
        }

        config.getOptional(MCP_SERVERS).ifPresent(mcpServers -> {
            Map<String, McpTransport> mcpTransports = mcpServers.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                        Map<String, Object> map = (Map<String, Object>) e.getValue();
                        return new McpTransportFactory().create(Configuration.fromMap(map));
                    }));
            builder.mcpTransports(mcpTransports);
        });

        return builder.build();
    }

    private void validateConfigOptions(ReadableConfig config, Map<String, ChatModelInstance> instances) {
        config.getOptional(MAX_MESSAGES)
                .ifPresent(n -> Preconditions.checkArgument(n > 0,
                        "'" + MAX_MESSAGES.key() + "' value must be greater than 0"));
        config.getOptional(MAX_HISTORIES)
                .ifPresent(n -> Preconditions.checkArgument(n > 0,
                        "'" + MAX_HISTORIES.key() + "' value must be greater than 0"));
        config.getOptional(MAX_TOOLS_INVOCATIONS)
                .ifPresent(n -> Preconditions.checkArgument(n <= 100 && n >= 1,
                        "'" + MAX_TOOLS_INVOCATIONS.key() + "' value must be between 1 and 100"));
        String llmNames = String.join(", ", instances.keySet());
        String errorMessageFormat = "'%s' value must be one of [%s]";
        config.getOptional(DEFAULT_LLM)
                .ifPresent(n -> Preconditions.checkArgument(instances.containsKey(n),
                        String.format(errorMessageFormat, DEFAULT_LLM.key(), llmNames)));
        config.getOptional(SQL_GENERATION_LLM)
                .ifPresent(n -> Preconditions.checkArgument(instances.containsKey(n),
                        String.format(errorMessageFormat, SQL_GENERATION_LLM.key(), llmNames)));
    }
}
