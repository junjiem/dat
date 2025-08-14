package ai.dat.agent.agentic;

import ai.dat.core.adapter.DatabaseAdapter;
import ai.dat.core.agent.AbstractHitlAskdataAgent;
import ai.dat.core.agent.data.EventOption;
import ai.dat.core.agent.data.StreamAction;
import ai.dat.core.agent.data.StreamEvent;
import ai.dat.core.contentstore.ContentStore;
import ai.dat.core.contentstore.DefaultContentStore;
import ai.dat.core.contentstore.data.QuestionSqlPair;
import ai.dat.core.semantic.data.SemanticModel;
import ai.dat.core.utils.SemanticModelUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.internal.Json;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.DefaultContent;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.service.*;
import dev.langchain4j.service.memory.ChatMemoryAccess;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.Builder;
import lombok.NonNull;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static ai.dat.agent.agentic.AgenticEventOptions.*;
import static ai.dat.agent.agentic.Text2SqlContentInjector.HISTORIES_CONTENT_TYPE;

/**
 * @Author JunjieM
 * @Date 2025/8/11
 */
class AgenticAskdataAgent extends AbstractHitlAskdataAgent {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private static final String ASK_USER_TOOL_NAME = "askUser";

    private final ChatModel defaultModel;
    private final StreamingChatModel defaultStreamingModel;
    private final ChatModel text2sqlModel;

    private final List<SemanticModel> semanticModels;
    private final Map<String, McpTransport> mcpTransports;

    private final Integer maxToolsInvocations;
    private final String textToSqlRules;
    private final String instruction;
    private final Integer maxHistories;

    private final Boolean humanInTheLoop;
    private final Boolean humanInTheLoopAskUser;
    private final Boolean humanInTheLoopToolApproval;

    private List<QuestionSqlPair> histories = Collections.emptyList();
    private final MessageWindowChatMemory chatMemory;
    private final MainAgent mainAgent;

    @Builder
    public AgenticAskdataAgent(@NonNull ContentStore contentStore,
                               @NonNull DatabaseAdapter databaseAdapter,
                               @NonNull ChatModel defaultModel,
                               @NonNull StreamingChatModel defaultStreamingModel,
                               @NonNull ChatModel text2sqlModel,
                               List<SemanticModel> semanticModels,
                               Map<String, McpTransport> mcpTransports,
                               Integer maxToolsInvocations,
                               String textToSqlRules,
                               String instruction,
                               Integer maxMessages,
                               Integer maxHistories,
                               Boolean humanInTheLoop,
                               Boolean humanInTheLoopAskUser,
                               Boolean humanInTheLoopToolApproval) {
        super(contentStore, databaseAdapter);
        SemanticModelUtil.validateSemanticModels(semanticModels);
        this.defaultModel = defaultModel;
        this.defaultStreamingModel = defaultStreamingModel;
        this.text2sqlModel = text2sqlModel;
        this.semanticModels = semanticModels;
        this.mcpTransports = mcpTransports;
        this.maxToolsInvocations = Optional.ofNullable(maxToolsInvocations).orElse(10);
        Preconditions.checkArgument(
                this.maxToolsInvocations <= 100 && this.maxToolsInvocations >= 1,
                "maxIterations must be between 1 and 100");
        this.textToSqlRules = textToSqlRules;
        this.instruction = Optional.ofNullable(instruction).orElse("");
        this.maxHistories = Optional.ofNullable(maxHistories).orElse(20);
        Preconditions.checkArgument(this.maxHistories > 0,
                "maxHistories must be greater than 0");
        this.humanInTheLoop = Optional.ofNullable(humanInTheLoop).orElse(true);
        this.humanInTheLoopAskUser = Optional.ofNullable(humanInTheLoopAskUser).orElse(true);
        this.humanInTheLoopToolApproval = Optional.ofNullable(humanInTheLoopToolApproval).orElse(false);
        int chatMemoryMaxMessages = Optional.ofNullable(maxMessages).orElse(100);
        Preconditions.checkArgument(chatMemoryMaxMessages > 0,
                "maxMessages must be greater than 0");
        this.chatMemory = MessageWindowChatMemory.withMaxMessages(chatMemoryMaxMessages);
        this.mainAgent = createMainAgent();
    }

    @Override
    public Set<EventOption> eventOptions() {
        return Set.of(SQL_GENERATE_EVENT, SEMANTIC_TO_SQL_EVENT, SQL_EXECUTE_EVENT,
                BEFORE_TOOL_EXECUTION, TOOL_EXECUTION, AGENT_ANSWER, HITL_ASK_USER);
    }

    @Override
    protected void run(@NonNull String question, @NonNull List<QuestionSqlPair> histories) {
        this.histories = histories.subList(Math.max(0, histories.size() - maxHistories), histories.size());

        TokenStream tokenStream = mainAgent.ask(instruction, question);
        CompletableFuture<Void> future = new CompletableFuture<>();
        tokenStream.onPartialResponse(s -> action.add(StreamEvent.from(AGENT_ANSWER, CONTENT, s)))
                .beforeToolExecution(te -> {
                    String toolName = te.request().name();
                    String toolArgs = te.request().arguments();
                    if (!humanInTheLoop || !ASK_USER_TOOL_NAME.equals(toolName)) {
                        action.add(StreamEvent.from(BEFORE_TOOL_EXECUTION, TOOL_NAME, toolName)
                                .set(TOOL_ARGS, toolArgs));
                    }
                    if (!humanInTheLoop) {
                        return;
                    }
                    if (humanInTheLoopAskUser && ASK_USER_TOOL_NAME.equals(toolName)) {
                        Map<?, ?> arguments = Json.fromJson(toolArgs, Map.class);
                        String request = arguments.get("request").toString();
                        action.add(StreamEvent.from(HITL_ASK_USER, AI_REQUEST, request));
                    } else if (humanInTheLoopToolApproval) {
                        action.add(StreamEvent.from(HITL_TOOL_APPROVAL, ACTION_PROMPT,
                                "Do you allow the execution of the '" + toolName + "' tool? " +
                                        "(y/n) [User input/press Enter to use the y]"));
                        String response;
                        try {
                            response = this.waitForUserResponse();
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to wait for user response", e);
                        }
                        boolean allow = response.equalsIgnoreCase("y") || response.isEmpty();
                        if (!allow) {
                            throw new RuntimeException("The user is not allowed to execute the '"
                                    + toolName + "' tool!");
                        }
                    }
                })
                .onToolExecuted(te -> {
                    String toolName = te.request().name();
                    if (!humanInTheLoop || !humanInTheLoopAskUser || !ASK_USER_TOOL_NAME.equals(toolName)) {
                        action.add(StreamEvent.from(TOOL_EXECUTION, TOOL_NAME, toolName)
                                .set(TOOL_ARGS, te.request().arguments())
                                .set(TOOL_RESULT, te.result()));
                    }
                })
                .onCompleteResponse(r -> future.complete(null))
                .onError(e -> {
                    action.add(StreamEvent.from(AGENT_ANSWER, ERROR, e.getMessage()));
                    future.completeExceptionally(e);
                })
                .start();
        future.join();
    }

    private MainAgent createMainAgent() {
        AiServices<MainAgent> aiServices = AiServices.builder(MainAgent.class)
                .streamingChatModel(defaultStreamingModel)
                .maxSequentialToolsInvocations(maxToolsInvocations)
                .tools(
                        createMisleadingAssistanceAgent(),
                        createDataAssistanceAgent(),
                        createText2SqlAgent(),
                        new Toolbox(contentStore, databaseAdapter, semanticModels, action, this)
                )
                .inputGuardrails()
                .chatMemoryProvider(memoryId -> chatMemory);

        if (mcpTransports != null && !mcpTransports.isEmpty()) {
            List<McpClient> mcpClients = mcpTransports.entrySet().stream()
                    .map(e -> {
                                try {
                                    return new DefaultMcpClient.Builder()
                                            .key(e.getKey())
                                            .transport(e.getValue())
                                            .build();
                                } catch (Exception ex) {
                                    throw new RuntimeException("Create '" + e.getKey()
                                            + "' MCP client failed: " + ex.getMessage(), ex);
                                }
                            }
                    ).collect(Collectors.toList());
            McpToolProvider toolProvider = McpToolProvider.builder()
                    .mcpClients(mcpClients)
                    .build();
            aiServices.toolProvider(toolProvider);
        }

        Map<ToolSpecification, ToolExecutor> tools = new HashMap<>();
        if (humanInTheLoop && humanInTheLoopAskUser) {
            ToolSpecification toolSpecification = ToolSpecification.builder()
                    .name(ASK_USER_TOOL_NAME)
                    .description("An tool that asks the user for missing information")
                    .parameters(JsonObjectSchema.builder()
                            .addStringProperty("request", "The request of AI")
                            .build())
                    .build();
            ToolExecutor toolExecutor = (toolExecutionRequest, memoryId) -> {
//                Map<?, ?> arguments = Json.fromJson(toolExecutionRequest.arguments(), Map.class);
//                String request = arguments.get("request").toString();
//                action.add(StreamEvent.from(HITL_ASK_USER, AI_REQUEST, request));
                try {
                    return this.waitForUserResponse();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to wait for user response", e);
                }
            };
            tools.put(toolSpecification, toolExecutor);
        }
        if (!tools.isEmpty()) {
            aiServices.tools(tools);
        }

        return aiServices.build();
    }

    private Text2SqlAgent createText2SqlAgent() {
        RetrievalAugmentor text2SqlRetrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(new DefaultQueryRouter(
                        contentStore.getMdlContentRetriever(),
                        contentStore.getSqlContentRetriever(),
                        contentStore.getSynContentRetriever(),
                        contentStore.getDocContentRetriever(),
                        query -> histories.stream().map(pair -> {
                            String json;
                            try {
                                json = JSON_MAPPER.writeValueAsString(pair);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(
                                        "Failed to serialize historical question sql pair to JSON: "
                                                + e.getMessage(), e);
                            }
                            Metadata metadata = Metadata.from(
                                    DefaultContentStore.METADATA_CONTENT_TYPE, HISTORIES_CONTENT_TYPE);
                            return new DefaultContent(TextSegment.from(json, metadata));
                        }).collect(Collectors.toList())
                ))
                .contentInjector(new Text2SqlContentInjector(
                        contentStore, databaseAdapter, semanticModels, textToSqlRules))
                .build();
        return AiServices.builder(Text2SqlAgent.class)
                .chatModel(text2sqlModel)
                .retrievalAugmentor(text2SqlRetrievalAugmentor)
                .build();
    }

    private DataAssistanceAgent createDataAssistanceAgent() {
        RetrievalAugmentor dataAssistanceRetrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentStore.getMdlContentRetriever())
                .contentInjector(new DataAssistanceContentInjector(
                        contentStore, databaseAdapter, semanticModels))
                .build();
        return AiServices.builder(DataAssistanceAgent.class)
                .chatModel(defaultModel)
                .retrievalAugmentor(dataAssistanceRetrievalAugmentor)
                .build();
    }

    private MisleadingAssistanceAgent createMisleadingAssistanceAgent() {
        RetrievalAugmentor misleadingAssistanceRetrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentStore.getMdlContentRetriever())
                .contentInjector(new MisleadingAssistanceContentInjector(
                        contentStore, databaseAdapter, semanticModels))
                .build();
        return AiServices.builder(MisleadingAssistanceAgent.class)
                .chatModel(defaultModel)
                .retrievalAugmentor(misleadingAssistanceRetrievalAugmentor)
                .build();
    }

    private interface MainAgent extends ChatMemoryAccess {
        @SystemMessage(fromResource = "prompts/agentic/main_agent_system_prompt.txt")
        @UserMessage(fromResource = "prompts/agentic/main_agent_user_prompt.txt")
        TokenStream ask(@V("instruction") String instruction, @V("query") String query);
    }

    public interface MisleadingAssistanceAgent {
        @UserMessage("{{query}}")
        @Tool("A helpful assistant that can help users understand their data better. " +
                "Currently, you are given a user's question that is potentially misleading.")
        String misleadingAssistance(@P("The potentially misleading question") @V("query") String query);
    }

    public interface DataAssistanceAgent {
        @UserMessage("{{query}}")
        @Tool("A data analyst great at answering user's questions about given database schema")
        String dataAssistance(@P("The question") @V("query") String query);
    }

    public interface Text2SqlAgent {
        @UserMessage("{{query}}")
        @Tool("A helpful assistant that converts natural language queries into ANSI SQL queries")
        String text2Sql(@P("The question") @V("query") String query);
    }

    public record Toolbox(ContentStore contentStore, DatabaseAdapter databaseAdapter,
                          List<SemanticModel> semanticModels, StreamAction action,
                          AgenticAskdataAgent askdataAgent) {
        @Tool("Convert the given ANSI SQL into the dialect SQL of the target database")
        public String ansiSql2dialectSql(@P("The ANSI SQL") String ansiSql) {
            action.add(StreamEvent.from(SQL_GENERATE_EVENT, SQL, ansiSql));
            List<SemanticModel> semanticModels = this.semanticModels;
            if (semanticModels == null || semanticModels.isEmpty()) {
                semanticModels = contentStore.allMdls();
                Preconditions.checkArgument(!semanticModels.isEmpty(), "Semantic models is empty");
            }
            try {
                String dialectSql = databaseAdapter.generateSql(ansiSql, semanticModels);
                action.add(StreamEvent.from(SEMANTIC_TO_SQL_EVENT, SQL, dialectSql));
                return dialectSql;
            } catch (Exception e) {
                action.add(StreamEvent.from(SEMANTIC_TO_SQL_EVENT, ERROR, e.getMessage()));
                throw new RuntimeException(e);
            }
        }

        @Tool("The execute database dialect SQL query return the dataset")
        public List<Map<String, Object>> executeSql(
                @P("The database dialect SQL") String dialectSql) throws SQLException {
            try {
                List<Map<String, Object>> results = databaseAdapter.executeQuery(dialectSql);
                action.add(StreamEvent.from(SQL_EXECUTE_EVENT, DATA, results));
                return results;
            } catch (SQLException e) {
                action.add(StreamEvent.from(SQL_EXECUTE_EVENT, ERROR, e.getMessage()));
                throw new SQLException(e);
            }
        }

        @Tool("Search semantic models by keywords or question to find relevant data schema")
        public List<SemanticModel> searchSemanticModels(
                @P("Keywords or question to search for semantic models") String query) {
            return contentStore.retrieveMdl(query);
        }
    }
}
