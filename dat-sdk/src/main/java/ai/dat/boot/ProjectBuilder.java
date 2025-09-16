package ai.dat.boot;

import ai.dat.boot.data.FileChanges;
import ai.dat.boot.data.SchemaFileState;
import ai.dat.boot.utils.ProjectUtil;
import ai.dat.core.data.DatModel;
import ai.dat.core.data.DatSchema;
import ai.dat.core.data.project.DatProject;
import ai.dat.core.exception.ValidationException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
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
        this.modelsPath = projectPath.resolve(ProjectUtil.MODELS_DIR_NAME);
        this.stateManager = new BuildStateManager(projectPath);
    }

    public ProjectBuilder(@NonNull Path projectPath,
                          @NonNull Map<Path, DatSchema> schemas,
                          @NonNull Map<Path, DatModel> models) {
        this(projectPath, ProjectUtil.loadProject(projectPath), schemas, models);
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
        log.info("Start incremental build project ...");
        if (project == null) {
            project = ProjectUtil.loadProject(projectPath);
        }
        if (schemas == null) {
            schemas = ProjectUtil.loadAllSchema(modelsPath);
        }
        if (models == null) {
            models = ProjectUtil.loadAllModel(modelsPath);
        }

        String fingerprint = ProjectUtil.contentStoreFingerprint(project);

        List<SchemaFileState> fileStates = stateManager.loadBuildState(fingerprint);

        FileChangeAnalyzer fileChangeAnalyzer = new FileChangeAnalyzer(project, projectPath, schemas, models);
        FileChanges changes = fileChangeAnalyzer.analyzeChanges(fileStates);

        if (changes.hasChanges()) {
            // 校验
            new PreBuildValidator(project).validate();
            // 更新状态
            ContentStoreManager storeManager = new ContentStoreManager(project, projectPath, fingerprint);
            storeManager.updateStore(fileStates, changes);
        }
        log.info("Incremental build project completed");
    }

    /**
     * 强制重建项目
     */
    public void forceRebuild() throws IOException {
        log.info("Start force rebuild project ...");
        cleanState();
        build();
    }



    /**
     * 清理当前状态文件
     */
    public void cleanState() throws IOException {
        log.info("Start clean current states ...");
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
        log.info("Start clean all states ...");
        stateManager.cleanAllState();
    }

    /**
     * 清理过期的状态文件
     */
    public void cleanOldStates(int keepCount) throws IOException {
        log.info("Start clean the expired states ..., keep count: {}", keepCount);
        stateManager.cleanOldStates(keepCount);
    }
}