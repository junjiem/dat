package ai.dat.server.openapi.controller;

import ai.dat.core.agent.data.StreamAction;
import ai.dat.core.agent.data.StreamEvent;
import ai.dat.core.contentstore.data.QuestionSqlPair;
import ai.dat.server.openapi.dto.AskRequest;
import ai.dat.server.openapi.dto.AskUserApproval;
import ai.dat.server.openapi.dto.AskUserResponse;
import ai.dat.server.openapi.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RestController
@RequestMapping("/api/v1/ask")
@RequiredArgsConstructor
@Tag(name = "Ask data", description = "Ask data API based on natural language")
public class AskController {

    // ------------------------------ Event name ------------------------------------
    private static final String PING_EVENT = "ping";
    private static final String SQL_GENERATE_EVENT = "sql_generate";
    private static final String SEMANTIC_TO_SQL_EVENT = "semantic_to_sql";
    private static final String SQL_EXECUTE_EVENT = "sql_execute";
    private static final String AGENT_ANSWER_EVENT = "agent_answer";
    private static final String AGENT_ANSWER_END_EVENT = "agent_answer_end";
    private static final String HITL_AI_REQUEST_EVENT = "hitl_ai_request";
    private static final String HITL_TOOL_APPROVAL_EVENT = "hitl_tool_approval";
    private static final String OTHER_EVENT = "other";
    private static final String ERROR_EVENT = "error";
    private static final String FINISHED_EVENT = "finished";

    // ------------------------------ Parameter key ------------------------------------
    private static final String CONVERSATION_ID = "conversation_id";
    private static final String TIMESTAMP = "timestamp";
    private static final String SEMANTIC_SQL = "semantic_sql";
    private static final String QUERY_SQL = "query_sql";
    private static final String QUERY_DATA = "query_data";
    private static final String ANSWER = "answer";
    private static final String ANSWER_ID = "answer_id";
    private static final String AI_REQUEST = "ai_request";
    private static final String TOOL_APPROVAL = "tool_approval";
    private static final String WAIT_TIMEOUT = "wait_timeout";
    private static final String ERROR = "error";
    private static final String STATUS = "status";
    private static final String SUB_EVENT = "sub_event";

    // ------------------------------ Parameter value ------------------------------------
    private static final String STATUS_SUCCESS = "succeeded";
    private static final String STATUS_FAILURE = "failed";

    private final ProjectService runnerService;

    // 用于定时发送ping事件的线程池
    private final ScheduledExecutorService pingScheduler = Executors.newScheduledThreadPool(1);

    private static final Map<String, List<QuestionSqlPair>> historiesPool = new HashMap<>();
    private static final String NOT_GENERATE = "<not generate>";

    @Operation(summary = "Ask data (Streaming)",
            description = "Ask data using natural language and return the SSE (Server-Sent Events) stream")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful",
                    content = @Content(mediaType = "text/event-stream",
                            examples = {
                                    @ExampleObject(name = PING_EVENT,
                                            summary = "Ping event",
                                            description = "Ping event every 10 seconds to keep the connection alive.",
                                            value = "event: " + PING_EVENT + "\n" +
                                                    "data: {\"" + TIMESTAMP + "\":1756051200000,\""
                                                    + CONVERSATION_ID + ":\"<id>\"}\n\n"),
                                    @ExampleObject(name = SQL_GENERATE_EVENT,
                                            summary = "Semantic SQL generation event",
                                            description = "an intermediate product from semantics to statements (" + SEMANTIC_SQL + ")",
                                            value = "event: " + SQL_GENERATE_EVENT + "\n" +
                                                    "data: {\"" + CONVERSATION_ID + "\":\"<id>\",\""
                                                    + TIMESTAMP + "\":1756051200000,\""
                                                    + SEMANTIC_SQL + "\":\"SELECT * FROM orders WHERE ...\"}\n\n"),
                                    @ExampleObject(name = SEMANTIC_TO_SQL_EVENT,
                                            summary = "Semantic SQL to dialect SQL event",
                                            description = "The dialect SQL ultimately used for execution (" + QUERY_SQL + ")",
                                            value = "event: " + SEMANTIC_TO_SQL_EVENT + "\n" +
                                                    "data: {\"" + CONVERSATION_ID + "\":\"<id>\",\""
                                                    + TIMESTAMP + "\":1756051200000,\""
                                                    + QUERY_SQL + "\":\"SELECT * FROM orders LIMIT 10\"}\n\n"),
                                    @ExampleObject(name = SQL_EXECUTE_EVENT,
                                            summary = "SQL execution result event",
                                            description = "Query result data (" + QUERY_DATA + ")",
                                            value = "event: " + SQL_EXECUTE_EVENT + "\n" +
                                                    "data: {\"" + CONVERSATION_ID + "\":\"<id>\",\""
                                                    + TIMESTAMP + "\":1756051200000,\""
                                                    + QUERY_DATA + "\":[{\"order_id\":1,\"amount\":100.0}]}\n\n"),
                                    @ExampleObject(name = AGENT_ANSWER_EVENT,
                                            summary = "Agent incremental answer event",
                                            description = "Returned text chunk content (" + ANSWER + ")",
                                            value = "event: " + AGENT_ANSWER_EVENT + "\n" +
                                                    "data: {\"" + CONVERSATION_ID + "\":\"<id>\",\""
                                                    + TIMESTAMP + "\":1756051200000,\""
                                                    + ANSWER_ID + "\":\"<id>\",\""
                                                    + ANSWER + "\":\"We are analyzing the data for you ...\"}\n\n"),
                                    @ExampleObject(name = AGENT_ANSWER_END_EVENT,
                                            summary = "Agent incremental answer end event",
                                            description = "Receiving this event means agent incremental answer streaming has ended.",
                                            value = "event: " + AGENT_ANSWER_EVENT + "\n" +
                                                    "data: {\"" + CONVERSATION_ID + "\":\"<id>\",\""
                                                    + TIMESTAMP + "\":1756051200000,\""
                                                    + ANSWER_ID + "\":\"<id>\",\""
                                                    + ANSWER + "\":\"We are analyzing the data for you ...\"}\n\n"),
                                    @ExampleObject(name = HITL_AI_REQUEST_EVENT,
                                            summary = "Human-in-the-loop AI request event",
                                            description = "Requests that require additional input from the user (" + AI_REQUEST + "). " +
                                                    "Human-in-the-loop waiting timeout time seconds (" + WAIT_TIMEOUT + " [optional]).",
                                            value = "event: " + HITL_AI_REQUEST_EVENT + "\n" +
                                                    "data: {\"" + CONVERSATION_ID + "\":\"<id>\",\""
                                                    + TIMESTAMP + "\":1756051200000,\""
                                                    + AI_REQUEST + "\":\"Please provide the time range for screening\",\""
                                                    + WAIT_TIMEOUT + "\":30}\n\n"),
                                    @ExampleObject(name = HITL_TOOL_APPROVAL_EVENT,
                                            summary = "Human-in-the-loop tool approval event",
                                            description = "Requests that tool approval from the user (" + TOOL_APPROVAL + "). " +
                                                    "Human-in-the-loop waiting timeout time seconds (" + WAIT_TIMEOUT + " [optional]).",
                                            value = "event: " + HITL_TOOL_APPROVAL_EVENT + "\n" +
                                                    "data: {\"" + CONVERSATION_ID + "\":\"<id>\",\""
                                                    + TIMESTAMP + "\":1756051200000,\""
                                                    + TOOL_APPROVAL + "\":\"Is it allowed to perform the operation of sending emails?\",\""
                                                    + WAIT_TIMEOUT + "\":30}\n\n"),
                                    @ExampleObject(name = ERROR_EVENT,
                                            summary = "Error event",
                                            description = "Exceptions that occur during the streaming process " +
                                                    "will be output in the form of stream events",
                                            value = "event: " + ERROR_EVENT + "\n" +
                                                    "data: {\"" + ERROR + "\":\"Stream processing failed: ...\",\""
                                                    + TIMESTAMP + "\":1756051200000,\""
                                                    + CONVERSATION_ID + "\":\"<id>\"}\n\n"),
                                    @ExampleObject(name = FINISHED_EVENT,
                                            summary = "Finished event",
                                            description = "execution ends, succeeded or failed in different states in the same event",
                                            value = "event: " + FINISHED_EVENT + "\n" +
                                                    "data: {\"" + STATUS + ":\"succeeded\",\""
                                                    + TIMESTAMP + "\":1756051200000,\""
                                                    + CONVERSATION_ID + "\":\"<id>\"}\n\n"),
                                    @ExampleObject(name = OTHER_EVENT,
                                            summary = "Other event",
                                            description = "Other supplementary message.",
                                            value = "event: " + OTHER_EVENT + "\n" +
                                                    "data: {\"" + SUB_EVENT + "\":\"<sub_event>\",\""
                                                    + TIMESTAMP + "\":1756051200000," +
                                                    "\"" + CONVERSATION_ID + "\":\"<id>\", ...}\n\n")
                            }
                    )),
            @ApiResponse(responseCode = "400", description = "Request parameter error"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(@Valid @RequestBody AskRequest request) {
        String conversationId = request.getConversationId() == null || request.getConversationId().isBlank() ?
                UUID.randomUUID().toString() : request.getConversationId();

        List<QuestionSqlPair> histories = historiesPool.getOrDefault(conversationId, Collections.emptyList());

        SseEmitter emitter = new SseEmitter();

        // 启动定时ping任务，每10秒发送一次ping事件
        ScheduledFuture<?> pingTask = pingScheduler.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(PING_EVENT)
                        .data(Map.of(TIMESTAMP, System.currentTimeMillis(),
                                CONVERSATION_ID, conversationId)));
            } catch (IOException e) {
                log.debug("Failed to send ping event for request [{}]: {}", conversationId, e.getMessage());
                // ping失败通常表示连接已断开，不需要特殊处理
            }
        }, 0, 10, TimeUnit.SECONDS);

        // 异步处理流式响应
        new Thread(() -> {
            String sql = NOT_GENERATE;
            boolean isAccurateSql = false;

            try {
                StreamAction action = runnerService.ask(conversationId,
                        request.getAgentName(), request.getQuestion(), histories);

                String lastEvent = "";
                boolean lastIncremental = false;
                String eventId = null;
                // 处理流式事件
                for (StreamEvent event : action) {
                    if (event == null) break;

                    if (event.getSemanticSql().isPresent()) {
                        sql = event.getSemanticSql().get();
                    }
                    if (event.getQueryData().isPresent()) {
                        isAccurateSql = true;
                    }

                    String eventName = event.name();
                    if (!lastEvent.equals(eventName)) {
                        if (lastIncremental) {
                            emitter.send(SseEmitter.event()
                                    .name(AGENT_ANSWER_END_EVENT)
                                    .data(Map.of(ANSWER_ID, eventId,
                                            TIMESTAMP, System.currentTimeMillis(),
                                            CONVERSATION_ID, conversationId)));
                        }
                        lastEvent = eventName;
                        lastIncremental = event.getIncrementalContent().isPresent();
                        eventId = UUID.randomUUID().toString();
                    }

                    // 转换并发送事件
                    String finalEventId = eventId;
                    AtomicReference<String> reference = new AtomicReference<>(OTHER_EVENT);
                    Map<String, Object> eventData = new HashMap<>();
                    eventData.put(CONVERSATION_ID, conversationId);
                    eventData.put(TIMESTAMP, System.currentTimeMillis());
                    event.getIncrementalContent().ifPresent(content -> {
                        reference.set(AGENT_ANSWER_EVENT);
                        eventData.put(ANSWER_ID, finalEventId);
                        eventData.put(ANSWER, content);
                    });
                    event.getSemanticSql().ifPresent(semanticSql -> {
                        reference.set(SQL_GENERATE_EVENT);
                        eventData.put(SEMANTIC_SQL, semanticSql);
                    });
                    event.getQuerySql().ifPresent(querySql -> {
                        reference.set(SEMANTIC_TO_SQL_EVENT);
                        eventData.put(QUERY_SQL, querySql);
                    });
                    event.getQueryData().ifPresent(data -> {
                        reference.set(SQL_EXECUTE_EVENT);
                        eventData.put(QUERY_DATA, data);
                    });
                    event.getHitlAiRequest().ifPresent(aiRequest -> {
                        reference.set(HITL_AI_REQUEST_EVENT);
                        eventData.put(AI_REQUEST, aiRequest);
                        event.getHitlWaitTimeout().ifPresent(timeout -> eventData.put(WAIT_TIMEOUT, timeout));
                    });
                    event.getHitlToolApproval().ifPresent(approval -> {
                        reference.set(HITL_TOOL_APPROVAL_EVENT);
                        eventData.put(TOOL_APPROVAL, approval);
                        event.getHitlWaitTimeout().ifPresent(timeout -> eventData.put(WAIT_TIMEOUT, timeout));
                    });
                    Map<String, Object> messages = event.getMessages();
                    if (OTHER_EVENT.equals(reference.get())) {
                        eventData.put(SUB_EVENT, event.name());
                    }
                    eventData.putAll(messages);
                    emitter.send(SseEmitter.event().name(reference.get()).data(eventData));

                    // 如果有错误，发送错误事件并结束
                    if (eventData.containsKey("error") || eventData.containsKey("exception")) {
                        Object errorMsg = eventData.get("error");
                        if (errorMsg == null) {
                            errorMsg = eventData.get("exception");
                        }
                        emitter.send(SseEmitter.event()
                                .name(ERROR_EVENT)
                                .data(Map.of(ERROR, errorMsg,
                                        TIMESTAMP, System.currentTimeMillis(),
                                        CONVERSATION_ID, conversationId))
                        );
                        break;
                    }
                }

                if (lastIncremental) {
                    emitter.send(SseEmitter.event()
                            .name(AGENT_ANSWER_END_EVENT)
                            .data(Map.of(ANSWER_ID, eventId,
                                    TIMESTAMP, System.currentTimeMillis(),
                                    CONVERSATION_ID, conversationId)));
                }

                // 发送完成事件
                emitter.send(SseEmitter.event()
                        .name(FINISHED_EVENT)
                        .data(Map.of(STATUS, STATUS_SUCCESS,
                                TIMESTAMP, System.currentTimeMillis(),
                                CONVERSATION_ID, conversationId))
                );

                emitter.complete();
                log.info("Stream ask data request completed [{}]", conversationId);
            } catch (Exception e) {
                log.error("Error during stream processing [{}]: {}", conversationId, e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event()
                            .name(ERROR_EVENT)
                            .data(Map.of(ERROR, e.getMessage(),
                                    TIMESTAMP, System.currentTimeMillis(),
                                    CONVERSATION_ID, conversationId))
                    );
                    emitter.send(SseEmitter.event()
                            .name(FINISHED_EVENT)
                            .data(Map.of(STATUS, STATUS_FAILURE,
                                    TIMESTAMP, System.currentTimeMillis(),
                                    ERROR, e.getMessage(),
                                    CONVERSATION_ID, conversationId))
                    );
                } catch (IOException ex) {
                    log.error("Error sending error event: {}", ex.getMessage());
                }
                emitter.completeWithError(e);
            } finally {
                // 取消ping任务
                pingTask.cancel(false);
                // 添加历史记录
                if (!isAccurateSql && !NOT_GENERATE.equals(sql)) {
                    sql = "/* Incorrect SQL */ " + sql;
                }
                List<QuestionSqlPair> copyHistories = new ArrayList<>(histories);
                copyHistories.add(QuestionSqlPair.from(request.getQuestion(), sql));
                historiesPool.put(conversationId, copyHistories);
            }
        }).start();

        emitter.onCompletion(() -> pingTask.cancel(false));// 添加完成回调，确保ping任务被取消
        emitter.onTimeout(() -> pingTask.cancel(false)); // 添加超时回调，确保ping任务被取消
        emitter.onError((ex) -> pingTask.cancel(false)); // 添加错误回调，确保ping任务被取消

        return emitter;
    }

    @Operation(summary = "User response",
            description = "Handle the user's response to AI request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful"),
            @ApiResponse(responseCode = "400", description = "Request parameter error"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/user-response")
    public ResponseEntity<Map<String, String>> userResponse(
            @Valid @RequestBody AskUserResponse response) {
        try {
            runnerService.userResponse(response.getConversationId(), response.getUserResponse());
            return ResponseEntity.ok(Map.of("status", "success",
                    "message", "User response received"));
        } catch (Exception e) {
            log.error("Error processing user response: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @Operation(summary = "User approval",
            description = "Handle the user's approval for tool call")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful"),
            @ApiResponse(responseCode = "400", description = "Request parameter error"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/user-approval")
    public ResponseEntity<Map<String, String>> userApproval(
            @Valid @RequestBody AskUserApproval approval) {
        try {
            runnerService.userApproval(approval.getConversationId(), approval.getUserApproval());
            return ResponseEntity.ok(Map.of("status", "success",
                    "message", "User approval received"));
        } catch (Exception e) {
            log.error("Error processing user approval: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
