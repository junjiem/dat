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
 * Coordinates incremental and full builds for DAT projects, including validation and content store updates.
 */
@Slf4j
public class ProjectBuilder {

    private final Path projectPath;

    private DatProject project;

    private final BuildStateManager stateManager;

    /**
     * Creates a builder for the project located at the provided path.
     *
     * @param projectPath the root directory of the project
     */
    public ProjectBuilder(@NonNull Path projectPath) {
        this.projectPath = projectPath;
        this.stateManager = new BuildStateManager(projectPath);
    }

    /**
     * Creates a builder with a preloaded project definition.
     *
     * @param projectPath the root directory of the project
     * @param project the project definition used during the build
     */
    public ProjectBuilder(@NonNull Path projectPath,
                          @NonNull DatProject project) {
        this(projectPath);
        this.project = project;
    }

    /**
     * Executes an incremental build of the project. The build validates the project definition and updates
     * the content store only when changes are detected.
     *
     * @param variables optional template variables used during the build
     * @throws IOException if the build state cannot be read or written
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
            // Perform validations
            new PreBuildValidator(project, projectPath, variables).validate();
            // Update persistent state
            ContentStoreManager storeManager = new ContentStoreManager(project, projectPath, fingerprint);
            storeManager.updateStore(fileStates, changes);
        }
        log.info("Incremental build project completed");
    }

    /**
     * Executes an incremental build without additional variables. Prefer {@link #build(Map)}.
     *
     * @throws IOException if the build state cannot be read or written
     */
    @Deprecated
    public void build() throws IOException {
        build(null);
    }

    /**
     * Forces a full rebuild of the project by clearing the current state before building.
     *
     * @param variables optional template variables used during the build
     * @throws IOException if the build state cannot be cleaned or updated
     */
    public void forceRebuild(@NonNull Map<String, Object> variables) throws IOException {
        log.info("Start force rebuild project ...");
        cleanState();
        build(variables);
    }

    /**
     * Forces a full rebuild without additional variables. Prefer {@link #forceRebuild(Map)}.
     *
     * @throws IOException if the build state cannot be cleaned or updated
     */
    @Deprecated
    public void forceRebuild() throws IOException {
        forceRebuild(Collections.emptyMap());
    }

    /**
     * Removes the stored state for the current project fingerprint.
     *
     * @throws IOException if state cleanup fails
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
     * Removes all stored build state snapshots for the project.
     *
     * @throws IOException if state cleanup fails
     */
    public void cleanAllStates() throws IOException {
        log.info("Start clean all states ...");
        stateManager.cleanAllState();
    }

    /**
     * Removes outdated build state snapshots while retaining a specified number of the most recent entries.
     *
     * @param keepCount the number of recent state snapshots to retain
     * @throws IOException if state cleanup fails
     */
    public void cleanOldStates(int keepCount) throws IOException {
        log.info("Start clean the expired states ..., keep count: {}", keepCount);
        stateManager.cleanOldStates(keepCount);
    }
}