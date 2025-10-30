package ai.dat.boot;

import ai.dat.boot.utils.ProjectUtil;
import ai.dat.core.agent.AskdataAgent;
import ai.dat.core.agent.data.StreamAction;
import ai.dat.core.contentstore.ContentStore;
import ai.dat.core.contentstore.data.QuestionSqlPair;
import ai.dat.core.data.project.AgentConfig;
import ai.dat.core.data.project.DatProject;
import ai.dat.core.semantic.data.SemanticModel;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Coordinates loading, building, and interacting with a DAT project through an {@link AskdataAgent}.
 */
@Getter
public class ProjectRunner {

    private final AskdataAgent agent;

    /**
     * Creates a runner that loads the project, builds it, and prepares the specified agent for interaction.
     *
     * @param projectPath the root path of the DAT project
     * @param agentName the name of the agent to activate
     * @param variables additional variables used during project build or agent creation
     * @throws IllegalArgumentException if the agent does not exist in the project definition
     */
    public ProjectRunner(@NonNull Path projectPath, @NonNull String agentName,
                         Map<String, Object> variables) {
        DatProject project = ProjectUtil.loadProject(projectPath);
        Map<String, AgentConfig> agentMap = project.getAgents().stream()
                .collect(Collectors.toMap(AgentConfig::getName, o -> o));
        Preconditions.checkArgument(agentMap.containsKey(agentName),
                "The project doesn't exist agent: " + agentName);
        ProjectBuilder builder = new ProjectBuilder(projectPath, project);
        try {
            builder.build(variables);
        } catch (IOException e) {
            throw new RuntimeException("The project build failed", e);
        }
        this.agent = ProjectUtil.createAskdataAgent(project, agentName, projectPath, variables);
    }

    /**
     * Creates a runner without additional variables. Prefer the three-argument constructor.
     *
     * @param projectPath the root path of the DAT project
     * @param agentName the name of the agent to activate
     * @deprecated use {@link #ProjectRunner(Path, String, Map)}
     */
    @Deprecated
    public ProjectRunner(@NonNull Path projectPath, @NonNull String agentName) {
        this(projectPath, agentName, null);
    }

    /**
     * Submits a user question to the underlying agent.
     *
     * @param question the natural language query to answer
     * @return a {@link StreamAction} representing the agent response stream
     */
    public StreamAction ask(@NonNull String question) {
        return agent.ask(question);
    }

    /**
     * Submits a user question along with prior question-SQL history for additional context.
     *
     * @param question the natural language query to answer
     * @param histories the historical question and SQL pairs that provide context
     * @return a {@link StreamAction} representing the agent response stream
     */
    public StreamAction ask(@NonNull String question, @NonNull List<QuestionSqlPair> histories) {
        return agent.ask(question, histories);
    }

    /**
     * Sends a follow-up response from the user to the agent conversation.
     *
     * @param response the user-provided response content
     */
    public void userResponse(@NonNull String response) {
        agent.userResponse(response);
    }

    /**
     * Communicates the user's approval decision back to the agent.
     *
     * @param approval whether the user approves the proposed action or answer
     */
    public void userApproval(@NonNull Boolean approval) {
        agent.userApproval(approval);
    }
}