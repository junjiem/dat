package ai.dat.server.mcp.service.tools;

import ai.dat.boot.utils.QuestionSqlPairCacheUtil;
import ai.dat.core.agent.data.StreamAction;
import ai.dat.core.agent.data.StreamEvent;
import ai.dat.core.contentstore.data.QuestionSqlPair;
import ai.dat.core.data.project.DatProject;
import ai.dat.server.mcp.service.ProjectService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @Author JunjieM
 * @Date 2025/9/8
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolsService {

    private static final String NOT_GENERATE = "<not generate>";

    private final static ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final ProjectService runnerService;

    @Tool(name = "dat_agents", description = "List agents information of ask data.")
    public String agents() throws JsonProcessingException {
        DatProject project = runnerService.getProject();
        List<Map<String, String>> agentList = project.getAgents().stream()
                .map(agent -> Map.of(
                        "name", agent.getName(),
                        "description", agent.getDescription() == null ? "<none>" : agent.getDescription()
                )).collect(Collectors.toList());
        return JSON_MAPPER.writeValueAsString(agentList);
    }

    @Tool(name = "dat_ask_data", description = "Ask data using natural language " +
            "(It is best to call the \"dat_agents\" tool before calling to check which available agents are available).")
    public String ask(
            @ToolParam(description = "Conversation ID, to continue the conversation based on previous chat records, " +
                    "it is necessary to pass the previous message's conversation_id.", required = false) String conversationId,
            @ToolParam(description = "Ask data agent name. " +
                    "If not specified, it defaults to \"default\"", required = false) String agentName,
            @ToolParam(description = "User question") String question) {
        conversationId = (conversationId == null || conversationId.isBlank() ?
                UUID.randomUUID().toString() : conversationId);

        agentName = (agentName == null || agentName.isBlank() ? "default" : agentName);

        List<QuestionSqlPair> histories = QuestionSqlPairCacheUtil.get(conversationId);

        StreamAction action = runnerService.ask(conversationId, agentName, question, histories);

        StringBuilder result = new StringBuilder();

        String sql = NOT_GENERATE;
        String lastEvent = "";
        boolean lastIncremental = false;
        boolean isAccurateSql = false;
        for (StreamEvent event : action) {
            if (event == null) break;
            String eventName = event.name();
            if (event.getSemanticSql().isPresent()) {
                sql = event.getSemanticSql().get();
            }
            if (event.getQueryData().isPresent()) {
                isAccurateSql = true;
            }
            if (!lastEvent.equals(eventName)) {
                if (lastIncremental) result.append("\n");
                lastEvent = eventName;
                lastIncremental = event.getIncrementalContent().isPresent();
                result.append("--------------------- ").append(eventName).append(" ---------------------\n");
            }
            append(event, result);
        }

        if (lastIncremental) result.append("\n");
        if (!isAccurateSql && !NOT_GENERATE.equals(sql)) {
            sql = "/* Incorrect SQL */ " + sql;
        }
        QuestionSqlPairCacheUtil.add(conversationId, QuestionSqlPair.from(question, sql));

        return result.toString();
    }

    private void append(StreamEvent event, StringBuilder result) {
        event.getHitlAiRequest().ifPresent(request -> {
            throw new RuntimeException("HITL (Human-in-the-loop) is not supported.");
        });
        event.getHitlToolApproval().ifPresent(prompt -> {
            throw new RuntimeException("HITL (Human-in-the-loop) is not supported.");
        });
        event.getIncrementalContent().ifPresent(result::append);
        event.getSemanticSql().ifPresent(content ->
                result.append("Semantic SQL: ").append(content).append("\n"));
        event.getQuerySql().ifPresent(content ->
                result.append("Query SQL: ").append(content).append("\n"));
        event.getQueryData().ifPresent(data -> {
            try {
                String queryResults = JSON_MAPPER.writeValueAsString(data);
                result.append("Query Results: ").append(queryResults).append("\n");
            } catch (JsonProcessingException e) {
                result.append("Failed to serialize query results to JSON: ")
                        .append(e.getMessage()).append("\n");
            }
        });
        event.getToolExecutionRequest().ifPresent(request ->
                result.append("id: ").append(request.id())
                        .append("\nname: ").append(request.name())
                        .append("\narguments: ").append(request.arguments()).append("\n"));
        event.getToolExecutionResult().ifPresent(toolResult ->
                result.append("result: ").append(toolResult).append("\n"));
        Map<String, Object> messages = event.getMessages();
        if (messages != null && !messages.isEmpty()) {
            try {
                result.append(JSON_MAPPER.writeValueAsString(messages)).append("\n");
            } catch (JsonProcessingException e) {
                result.append("Failed to serialize messages to JSON: ")
                        .append(e.getMessage()).append("\n");
            }
        }
    }
}
