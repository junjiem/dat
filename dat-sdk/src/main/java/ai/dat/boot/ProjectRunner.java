package ai.dat.boot;

import ai.dat.boot.utils.ProjectUtil;
import ai.dat.core.agent.AskdataAgent;
import ai.dat.core.agent.data.StreamAction;
import ai.dat.core.contentstore.ContentStore;
import ai.dat.core.contentstore.data.QuestionSqlPair;
import ai.dat.core.data.DatModel;
import ai.dat.core.data.DatSchema;
import ai.dat.core.data.project.DatProject;
import ai.dat.core.semantic.data.SemanticModel;
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
        Path modelsPath = projectPath.resolve(ProjectUtil.MODELS_DIR_NAME);
        Map<Path, DatSchema> schemas = ProjectUtil.loadAllSchema(modelsPath);
        Map<Path, DatModel> models = ProjectUtil.loadAllModel(modelsPath);
        ProjectBuilder builder = new ProjectBuilder(projectPath, project, schemas, models);
        try {
            builder.build();
        } catch (IOException e) {
            throw new RuntimeException("The project build failed", e);
        }
        ContentStore contentStore = ProjectUtil.createContentStore(project, projectPath);
        List<SemanticModel> allSemanticModels = contentStore.allMdls();
        this.agent = ProjectUtil.createAskdataAgent(project, agentName, allSemanticModels, projectPath);
    }

    public StreamAction ask(String question) {
        return agent.ask(question);
    }

    public StreamAction ask(String question, List<QuestionSqlPair> histories) {
        return agent.ask(question, histories);
    }
}