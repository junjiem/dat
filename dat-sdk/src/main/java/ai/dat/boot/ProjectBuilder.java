package ai.dat.boot;

import ai.dat.boot.data.FileChanges;
import ai.dat.boot.data.SchemaFileState;
import ai.dat.boot.utils.ProjectUtil;
import ai.dat.core.data.project.DatProject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @Author JunjieM
 * @Date 2025/7/17
 */
@Slf4j
public class ProjectBuilder {

    private final Path projectPath;

    private DatProject project;

    private final BuildStateManager stateManager;

    public ProjectBuilder(@NonNull Path projectPath) {
        this.projectPath = projectPath;
        this.stateManager = new BuildStateManager(projectPath);
    }

    public ProjectBuilder(@NonNull Path projectPath,
                          @NonNull DatProject project) {
        this(projectPath);
        this.project = project;
    }

    /**
     * 构建项目
     *
     * @throws IOException
     */
    public void build(Map<String, Object> variables) throws IOException {
        log.info("Start incremental build project ...");
        if (project == null) {
            project = ProjectUtil.loadProject(projectPath);
        }

        String fingerprint = ProjectUtil.contentStoreFingerprint(project);

        List<SchemaFileState> fileStates = stateManager.loadBuildState(fingerprint);

        FileChangeAnalyzer fileChangeAnalyzer = new FileChangeAnalyzer(project, projectPath);
        FileChanges changes = fileChangeAnalyzer.analyzeChanges(fileStates);

        if (changes.hasChanges()) {
            // 校验
            new PreBuildValidator(project, projectPath, variables).validate();
            // 更新状态
            ContentStoreManager storeManager = new ContentStoreManager(project, projectPath, fingerprint);
            storeManager.updateStore(fileStates, changes);
        }
        log.info("Incremental build project completed");
    }

    /**
     * 构建项目
     *
     * @throws IOException
     */
    @Deprecated
    public void build() throws IOException {
        build(null);
    }

    /**
     * 强制重建项目
     *
     * @throws IOException
     */
    public void forceRebuild(Map<String, Object> variables) throws IOException {
        log.info("Start force rebuild project ...");
        cleanState();
        build(variables);
    }

    /**
     * 强制重建项目
     *
     * @throws IOException
     */
    @Deprecated
    public void forceRebuild() throws IOException {
        forceRebuild(Collections.emptyMap());
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