package ai.dat.agent.agentic;

import ai.dat.agent.agentic.tools.email.EmailSender;
import ai.dat.core.adapter.DatabaseAdapter;
import ai.dat.core.agent.AbstractHitlAskdataAgent;
import ai.dat.core.agent.data.EventOption;
import ai.dat.core.agent.data.StreamAction;
import ai.dat.core.agent.data.StreamEvent;
import ai.dat.core.contentstore.ContentStore;
import ai.dat.core.contentstore.DefaultContentStore;
import ai.dat.core.contentstore.data.QuestionSqlPair;
import ai.dat.core.semantic.data.SemanticModel;
import ai.dat.core.utils.JinjaTemplateUtil;
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
import dev.langchain4j.service.tool.BeforeToolExecution;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static ai.dat.agent.agentic.AgenticEventOptions.*;
import static ai.dat.agent.agentic.Text2SqlContentInjector.HISTORIES_CONTENT_TYPE;

/**
 * @Author JunjieM
 * @Date 2025/8/11
 */
@Slf4j
class AgenticAskdataAgent extends AbstractHitlAskdataAgent {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private static final String ASK_USER_TOOL_NAME = "askUser";

    private static final String TOOL_NOT_APPROVAL_MESSAGE = "The user did not approve the execution of this tool!";
    private static final String TOOL_NOT_APPROVAL_AND_GAVE_FEEDBACK_MESSAGE =
            "The user did not approve the execution of this tool and gave feedback: ";

    private final ChatModel defaultModel;
    private final StreamingChatModel defaultStreamingModel;
    private final ChatModel text2sqlModel;

    private final List<SemanticModel> semanticModels;
    private final EmailSender emailSender;
    private final Map<String, McpTransport> mcpTransports;

    private final Integer maxToolsInvocations;
    private final String textToSqlRules;
    private final String instruction;
    private final Integer maxHistories;
    private final Integer semanticModelDataPreviewLimit;

    private final Boolean humanInTheLoop;
    private final Boolean humanInTheLoopAskUser;
    private final Boolean humanInTheLoopToolApproval;
    private final Boolean humanInTheLoopToolNotApprovalAndFeedback;

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
                               Map<String, Object> variables,
                               EmailSender emailSender,
                               Map<String, McpTransport> mcpTransports,
                               Integer maxToolsInvocations,
                               String textToSqlRules,
                               String instruction,
                               Integer maxMessages,
                               Integer maxHistories,
                               Integer semanticModelDataPreviewLimit,
                               Boolean humanInTheLoop,
                               Boolean humanInTheLoopAskUser,
                               Boolean humanInTheLoopToolApproval,
                               Boolean humanInTheLoopToolNotApprovalAndFeedback) {
        super(contentStore, databaseAdapter, variables);
        SemanticModelUtil.validateSemanticModels(semanticModels);
        this.defaultModel = defaultModel;
        this.defaultStreamingModel = defaultStreamingModel;
        this.text2sqlModel = text2sqlModel;
        this.semanticModels = semanticModels;
        this.emailSender = emailSender;
        this.mcpTransports = mcpTransports;
        this.maxToolsInvocations = Optional.ofNullable(maxToolsInvocations).orElse(10);
        Preconditions.checkArgument(
                this.maxToolsInvocations <= 100 && this.maxToolsInvocations >= 1,
                "maxIterations must be between 1 and 100");
        this.textToSqlRules = textToSqlRules;
        this.instruction = Optional.ofNullable(instruction).orElse("");
        this.maxHistories = Optional.ofNullable(maxHistories).orElse(0);
        Preconditions.checkArgument(this.maxHistories >= 0,
                "maxHistories must be greater than or equal to 0");
        this.semanticModelDataPreviewLimit = Optional.ofNullable(semanticModelDataPreviewLimit).orElse(0);
        Preconditions.checkArgument(this.semanticModelDataPreviewLimit >= 0 && this.semanticModelDataPreviewLimit <= 20,
                "semanticModelDataPreviewLimit must be between 0 and 20");
        this.humanInTheLoop = Optional.ofNullable(humanInTheLoop).orElse(true);
        this.humanInTheLoopAskUser = Optional.ofNullable(humanInTheLoopAskUser).orElse(true);
        this.humanInTheLoopToolApproval = Optional.ofNullable(humanInTheLoopToolApproval).orElse(false);
        this.humanInTheLoopToolNotApprovalAndFeedback =
                Optional.ofNullable(humanInTheLoopToolNotApprovalAndFeedback).orElse(true);
        int chatMemoryMaxMessages = Optional.ofNullable(maxMessages).orElse(100);
        Preconditions.checkArgument(chatMemoryMaxMessages > 0,
                "maxMessages must be greater than 0");
        this.chatMemory = MessageWindowChatMemory.withMaxMessages(chatMemoryMaxMessages);
        this.mainAgent = createMainAgent();
    }

    @Override
    public Set<EventOption> eventOptions() {
        return Set.of(SQL_GENERATE_EVENT, SEMANTIC_TO_SQL_EVENT, SQL_EXECUTE_EVENT,
                BEFORE_TOOL_EXECUTION, TOOL_EXECUTION, AGENT_ANSWER, HITL_AI_REQUEST);
    }

    @Override
    protected void run(@NonNull String question, @NonNull List<QuestionSqlPair> histories) {
        this.histories = histories.subList(Math.max(0, histories.size() - maxHistories), histories.size());

        TokenStream tokenStream = mainAgent.ask(instruction, question);
        CompletableFuture<Void> future = new CompletableFuture<>();
        tokenStream.onPartialResponse(s -> action.add(StreamEvent.from(AGENT_ANSWER, CONTENT, s)))
                .beforeToolExecution(this::beforeToolExecution)
                .onToolExecuted(this::onToolExecuted)
                .onCompleteResponse(r -> future.complete(null))
                .onError(e -> {
                    action.add(StreamEvent.from(AGENT_ANSWER, ERROR, e.getMessage()));
                    future.completeExceptionally(e);
                })
                .start();
        future.join();
    }

    private void beforeToolExecution(BeforeToolExecution beforeToolExecution) {
        String toolName = beforeToolExecution.request().name();
        String toolArgs = beforeToolExecution.request().arguments();
        if (!humanInTheLoop || !ASK_USER_TOOL_NAME.equals(toolName)) {
            action.add(StreamEvent.from(BEFORE_TOOL_EXECUTION, TOOL_NAME, toolName)
                    .set(TOOL_ID, beforeToolExecution.request().id())
                    .set(TOOL_ARGS, toolArgs));
        }
        if (!humanInTheLoop) {
            return;
        }
        if (humanInTheLoopAskUser && ASK_USER_TOOL_NAME.equals(toolName)) {
            Map<?, ?> arguments = Json.fromJson(toolArgs, Map.class);
            String request = arguments.get("request").toString();
            action.add(StreamEvent.from(HITL_AI_REQUEST, AI_REQUEST, request));
        } else if (humanInTheLoopToolApproval) {
            action.add(StreamEvent.from(HITL_TOOL_APPROVAL, TOOL_APPROVAL,
                    "Do you approve the execution of the '" + toolName + "' tool?"));
        }
    }

    private boolean isToolApproval() throws Exception {
        boolean approval = true;
        if (humanInTheLoop && humanInTheLoopToolApproval) {
            try {
                approval = this.waitForUserApproval();
            } catch (Exception e) {
                throw new Exception("Failed to wait for user approval: " + e.getMessage(), e);
            }
        }
        return approval;
    }

    private void onToolExecuted(ToolExecution toolExecution) {
        String toolName = toolExecution.request().name();
        if (!humanInTheLoop || !humanInTheLoopAskUser || !ASK_USER_TOOL_NAME.equals(toolName)) {
            action.add(StreamEvent.from(TOOL_EXECUTION, TOOL_NAME, toolName)
                    .set(TOOL_ID, toolExecution.request().id())
                    .set(TOOL_ARGS, toolExecution.request().arguments())
                    .set(TOOL_RESULT, toolExecution.result()));
        }
    }

    private MainAgent createMainAgent() {
        AiServices<MainAgent> aiServices = AiServices.builder(MainAgent.class)
                .streamingChatModel(defaultStreamingModel)
                .maxSequentialToolsInvocations(maxToolsInvocations)
                .tools(
                        createMisleadingAssistanceAgent(),
                        createDataAssistanceAgent(),
                        createText2SqlAgent(),
                        new Toolbox(contentStore, databaseAdapter, variables, semanticModels, action)
                )
                .inputGuardrails()
                .chatMemoryProvider(memoryId -> chatMemory);
        aiServices.tools(createTools());
        ToolProvider toolProvider = createToolProvider();
        if (toolProvider != null) {
            aiServices.toolProvider(toolProvider);
        }
        return aiServices.build();
    }

    private ToolProvider createToolProvider() {
        if (mcpTransports == null || mcpTransports.isEmpty()) {
            return null;
        }
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
        return McpToolProvider.builder()
                .mcpClients(mcpClients)
                .toolWrapper(toolExecutor -> {
                    boolean approval;
                    try {
                        approval = isToolApproval();
                    } catch (Exception e) {
                        return (toolExecutionRequest, memoryId) -> e.getMessage();
                    }
                    return approval ? toolExecutor :
                            (toolExecutionRequest, memoryId) -> TOOL_NOT_APPROVAL_MESSAGE;
                })
                .build();
    }

    private Map<ToolSpecification, ToolExecutor> createTools() {
        Map<ToolSpecification, ToolExecutor> tools = new HashMap<>();

        if (emailSender != null) {
            ToolSpecification toolSpecification = ToolSpecification.builder()
                    .name("sendEmail")
                    .description("Send email to recipients (and CC recipients) with specified subject and content")
                    .parameters(JsonObjectSchema.builder()
                            .addStringProperty("recipients", "Recipient email address(es), separated by comma")
                            .addStringProperty("ccRecipients", "CC recipient email address(es), separated by comma")
                            .addStringProperty("subject", "Email subject")
                            .addStringProperty("content", "Email content")
                            .addBooleanProperty("isHtml", "Whether the content is HTML format")
                            .required("recipients", "subject", "content", "isHtml")
                            .build())
                    .build();
            ToolExecutor toolExecutor = (toolExecutionRequest, memoryId) -> {
                String toolName = toolExecutionRequest.name();
                String toolArgs = toolExecutionRequest.arguments();
                try {
                    if (!isToolApproval()) {
                        String message = TOOL_NOT_APPROVAL_MESSAGE;
                        if (humanInTheLoopToolNotApprovalAndFeedback) {
                            long timeout = 30;
                            action.add(StreamEvent
                                    .from(HITL_AI_REQUEST, AI_REQUEST,
                                            "Please provide feedback regarding the rejection of the '"
                                                    + toolName + "' tool's execution.")
                                    .set(WAIT_TIMEOUT, timeout)
                            );
                            try {
                                String response = this.waitForUserResponse(timeout, TimeUnit.SECONDS);
                                if (response == null || response.isEmpty()) {
                                    return message;
                                }
                                return TOOL_NOT_APPROVAL_AND_GAVE_FEEDBACK_MESSAGE + response;
                            } catch (TimeoutException e) {
                                log.warn("Timeout to wait for user feedback", e);
                                return message;
                            }
                        }
                        return message;
                    }
                } catch (Exception e) {
                    return e.getMessage();
                }
                try {
                    Map<?, ?> arguments = Json.fromJson(toolArgs, Map.class);
                    String recipients = arguments.get("recipients").toString();
                    String[] to = Arrays.stream(recipients.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList()
                            .toArray(new String[]{});
                    String ccRecipients = null;
                    String[] cc = null;
                    if (arguments.containsKey("ccRecipients")) {
                        ccRecipients = arguments.get("ccRecipients").toString();
                        cc = Arrays.stream(ccRecipients.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .toList()
                                .toArray(new String[]{});
                    }
                    String subject = arguments.get("subject").toString();
                    String content = arguments.get("content").toString();
                    boolean isHtml = false;
                    if (arguments.containsKey("isHtml")) {
                        isHtml = (boolean) arguments.get("isHtml");
                    }
                    if (cc != null && cc.length > 0) {
                        emailSender.sendEmailWithCc(to, cc, subject, content, isHtml);
                        return String.format("The email has been successfully sent：%s, CC：%s", recipients, ccRecipients);
                    } else {
                        emailSender.sendEmail(to, subject, content, isHtml);
                        return String.format("The email has been successfully sent：%s", recipients);
                    }
                } catch (Exception e) {
                    log.error("Failed to send the email", e);
                    return e.getMessage();
                }
            };
            tools.put(toolSpecification, toolExecutor);
        }

        if (humanInTheLoop && humanInTheLoopAskUser) {
            ToolSpecification toolSpecification = ToolSpecification.builder()
                    .name(ASK_USER_TOOL_NAME)
                    .description("An tool that asks the user for missing information")
                    .parameters(JsonObjectSchema.builder()
                            .addStringProperty("request", "The request of AI")
                            .required("request")
                            .build())
                    .build();
            ToolExecutor toolExecutor = (toolExecutionRequest, memoryId) -> {
                try {
                    return this.waitForUserResponse();
                } catch (Exception e) {
                    log.warn("Failed to wait for user response", e);
                    return "Failed to wait for user response.";
                }
            };
            tools.put(toolSpecification, toolExecutor);
        }

        return tools;
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
                        contentStore, databaseAdapter, semanticModels, variables, textToSqlRules,
                        semanticModelDataPreviewLimit))
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

    public record Toolbox(ContentStore contentStore, DatabaseAdapter databaseAdapter, Map<String, Object> variables,
                          List<SemanticModel> semanticModels, StreamAction action) {
        @Tool("Convert the given ANSI SQL into the dialect SQL of the target database")
        public String ansiSql2dialectSql(@P("The ANSI SQL") String ansiSql) {
            log.info("semanticSql: " + ansiSql);
            action.add(StreamEvent.from(SQL_GENERATE_EVENT, SQL, ansiSql));
            List<SemanticModel> semanticModels = this.semanticModels;
            if (semanticModels == null || semanticModels.isEmpty()) {
                semanticModels = contentStore.allMdls();
                Preconditions.checkArgument(!semanticModels.isEmpty(), "Semantic models is empty");
            }
            List<SemanticModel> renderedSemanticModels = semanticModels.stream().map(m -> {
                try {
                    SemanticModel semanticModel = JSON_MAPPER.readValue(
                            JSON_MAPPER.writeValueAsString(m), SemanticModel.class);
                    semanticModel.setModel(JinjaTemplateUtil.render(semanticModel.getModel(), variables));
                    return semanticModel;
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException(ex);
                }
            }).collect(Collectors.toList());
            try {
                String dialectSql = databaseAdapter.generateSql(ansiSql, renderedSemanticModels);
                log.info("dialectSql: " + dialectSql);
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
            log.info("executeSql: " + dialectSql);
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
        public List<String> searchSemanticModels(
                @P("Keywords or question to search for semantic models") String query) {
            List<SemanticModel> semanticModels = contentStore.retrieveMdl(query);
            return semanticModels.stream()
                    .map(semanticModel -> SemanticModelUtil.toSemanticModelViewText(
                            semanticModel, databaseAdapter.semanticAdapter()))
                    .collect(Collectors.toList());
        }
    }

}
