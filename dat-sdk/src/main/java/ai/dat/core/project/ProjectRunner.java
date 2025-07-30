package ai.dat.core.project;

import ai.dat.core.agent.AskdataAgent;
import ai.dat.core.agent.data.StreamAction;
import ai.dat.core.contentstore.data.QuestionSqlPair;
import ai.dat.core.data.DatModel;
import ai.dat.core.data.DatSchema;
import ai.dat.core.data.project.DatProject;
import ai.dat.core.project.utils.ProjectUtil;
import lombok.Getter;
import lombok.NonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * @Author JunjieM
 * @Date 2025/7/21
 */
@Getter
public class ProjectRunner {

    private final AskdataAgent agent;

    public ProjectRunner(@NonNull Path projectPath, @NonNull String agentName) {
        DatProject project = ProjectUtil.loadProject(projectPath);
        Map<Path, DatSchema> schemas = ProjectUtil.loadAllSchema(projectPath);
        Map<Path, DatModel> models = ProjectUtil.loadAllModel(projectPath);
        ProjectBuilder builder = new ProjectBuilder(projectPath, project, schemas, models);
        try {
            builder.build();
        } catch (IOException e) {
            throw new RuntimeException("The project build failed", e);
        }
        this.agent = ProjectUtil.createAskdataAgent(project, agentName,
                schemas.values().stream().toList(), models.values().stream().toList(), projectPath);
    }

    public StreamAction ask(String question) {
        return agent.ask(question);
    }

    public StreamAction ask(String question, List<QuestionSqlPair> histories) {
        return agent.ask(question, histories);
    }
}