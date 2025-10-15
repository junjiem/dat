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
 * @Author JunjieM
 * @Date 2025/7/21
 */
@Getter
public class ProjectRunner {

    private final AskdataAgent agent;

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

    @Deprecated
    public ProjectRunner(@NonNull Path projectPath, @NonNull String agentName) {
        this(projectPath, agentName, null);
    }

    public StreamAction ask(@NonNull String question) {
        return agent.ask(question);
    }

    public StreamAction ask(@NonNull String question, @NonNull List<QuestionSqlPair> histories) {
        return agent.ask(question, histories);
    }

    public void userResponse(@NonNull String response) {
        agent.userResponse(response);
    }

    public void userApproval(@NonNull Boolean approval) {
        agent.userApproval(approval);
    }
}