package ai.dat.server.openapi.service;

import ai.dat.boot.ProjectRunner;
import ai.dat.boot.utils.ProjectUtil;
import ai.dat.core.agent.data.StreamAction;
import ai.dat.core.contentstore.data.QuestionSqlPair;
import ai.dat.core.data.project.DatProject;
import ai.dat.server.openapi.config.ServerConfig;
import com.google.common.base.Preconditions;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ServerConfig serverConfig;

    private static final Map<String, ProjectRunner> projectRunnerPool = new HashMap<>();

    private ProjectRunner getProjectRunner(@NonNull String conversationId, String agentName) {
        ProjectRunner projectRunner = projectRunnerPool.get(conversationId);
        if (projectRunner == null) {
            Preconditions.checkArgument(agentName != null && !agentName.isEmpty(),
                    "The agent name cannot be empty");
            Path projectPath = serverConfig.getAbsoluteProjectPath();
            Map<String, Object> variables = serverConfig.getVariables();
            try {
                projectRunner = new ProjectRunner(projectPath, agentName, variables);
                projectRunnerPool.put(conversationId, projectRunner);
            } catch (Exception e) {
                log.error("Failed to initialize project runner", e);
                throw new RuntimeException("Failed to initialize project runner: " + e.getMessage(), e);
            }
        }
        return projectRunner;
    }

    public DatProject getProject() {
        Path projectPath = serverConfig.getAbsoluteProjectPath();
        return ProjectUtil.loadProject(projectPath);
    }

    public StreamAction ask(@NonNull String conversationId, @NonNull String agentName,
                            @NonNull String question, @NonNull List<QuestionSqlPair> histories) {
        return getProjectRunner(conversationId, agentName).ask(question, histories);
    }

    public void userResponse(@NonNull String conversationId, @NonNull String response) {
        getProjectRunner(conversationId, null).userResponse(response);
    }

    public void userApproval(@NonNull String conversationId, @NonNull Boolean approval) {
        getProjectRunner(conversationId, null).userApproval(approval);
    }
}