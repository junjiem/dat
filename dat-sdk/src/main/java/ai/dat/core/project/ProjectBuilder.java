package ai.dat.core.project;

import ai.dat.core.adapter.DatabaseAdapter;
import ai.dat.core.contentstore.ContentStore;
import ai.dat.core.data.DatModel;
import ai.dat.core.data.DatSchema;
import ai.dat.core.data.project.DatProject;
import ai.dat.core.exception.ValidationException;
import ai.dat.core.project.data.FileChanges;
import ai.dat.core.project.data.SchemaFileState;
import ai.dat.core.project.utils.ProjectUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 项目构建器
 *
 * @Author JunjieM
 * @Date 2025/7/17
 */
@Slf4j
public class ProjectBuilder {

    private final Path projectPath;
    private final Path modelsPath;

    private DatProject project;
    private Map<Path, DatSchema> schemas;
    private Map<Path, DatModel> models;

    private final BuildStateManager stateManager;

    public ProjectBuilder(@NonNull Path projectPath) {
        this.projectPath = projectPath;
        this.modelsPath = projectPath.resolve("models");
        this.stateManager = new BuildStateManager(projectPath);
    }

    public ProjectBuilder(@NonNull Path projectPath,
                          @NonNull DatProject project,
                          @NonNull Map<Path, DatSchema> schemas,
                          @NonNull Map<Path, DatModel> models) {
        this(projectPath);
        this.project = project;
        this.schemas = schemas;
        this.models = models;
    }

    /**
     * 构建项目
     */
    public void build() throws IOException {
        validate();

        if (project == null) {
            project = ProjectUtil.loadProject(projectPath);
        }

        String fingerprint = ProjectUtil.contentStoreFingerprint(project);

        List<SchemaFileState> fileStates = stateManager.loadBuildState(fingerprint);

        FileChangeAnalyzer fileChangeAnalyzer = new FileChangeAnalyzer(projectPath, schemas, models);
        FileChanges changes = fileChangeAnalyzer.analyzeChanges(fileStates);

        if (changes.hasChanges()) {
            DatabaseAdapter databaseAdapter = ProjectUtil.createDatabaseAdapter(project);
            ContentStore contentStore = ProjectUtil.createContentStore(project, projectPath);
            ContentStoreManager storeManager = new ContentStoreManager(projectPath,
                    databaseAdapter, contentStore, fingerprint, schemas, models);
            storeManager.updateStore(fileStates, changes);
        }
    }

    /**
     * 强制重建项目
     */
    public void forceRebuild() throws IOException {
        cleanState();
        build();
    }

    private void validate() {
        if (schemas == null) {
            schemas = ProjectUtil.loadAllSchema(projectPath);
        }
        validateYamlFiles(schemas);
        if (models == null) {
            models = ProjectUtil.loadAllModel(projectPath);
        }
        validateSqlFiles(models);
    }

    private void validateSqlFiles(Map<Path, DatModel> models) {
        Map<String, List<Path>> nameToPaths = models.entrySet().stream()
                .collect(Collectors.groupingBy(
                        e -> e.getValue().getName(),
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())
                ));
        Map<String, List<Path>> duplicates = nameToPaths.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!duplicates.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            duplicates.forEach((modelName, paths) -> {
                sb.append("Discover duplicate model name: ")
                        .append(modelName).append("\n");
                sb.append("The SQL file relative path: \n");
                paths.stream()
                        .map(p -> modelsPath.relativize(p).toString())
                        .forEach(p -> sb.append("  - ").append(p).append("\n"));
                sb.append("\n");
            });
            throw new ValidationException(sb.toString());
        }
    }

    private void validateYamlFiles(Map<Path, DatSchema> schemas) {
        Map<String, List<Path>> nameToPaths = schemas.entrySet().stream()
                .flatMap(entry -> entry.getValue().getSemanticModels().stream()
                        .map(model -> Map.entry(model.getName(), entry.getKey())))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
        Map<String, List<Path>> duplicates = nameToPaths.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!duplicates.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            duplicates.forEach((semanticModelName, paths) -> {
                sb.append("Discover duplicate semantic model name: ")
                        .append(semanticModelName).append("\n");
                sb.append("The YAML file relative path: \n");
                paths.stream()
                        .map(p -> modelsPath.relativize(p).toString())
                        .forEach(p -> sb.append("  - ").append(p).append("\n"));
                sb.append("\n");
            });
            throw new ValidationException(sb.toString());
        }
    }

    /**
     * 清理当前状态文件
     */
    public void cleanState() throws IOException {
        if (project == null) {
            project = ProjectUtil.loadProject(projectPath);
        }
        String id = ProjectUtil.contentStoreFingerprint(project);
        stateManager.cleanState(id);
    }

    /**
     * 清理所有状态文件
     */
    public void cleanAllStates() throws IOException {
        stateManager.cleanAllState();
    }

    /**
     * 清理过期的状态文件
     */
    public void cleanOldStates(int keepCount) throws IOException {
        stateManager.cleanOldStates(keepCount);
    }
}