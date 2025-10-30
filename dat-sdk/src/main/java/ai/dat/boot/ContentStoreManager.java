package ai.dat.boot;

import ai.dat.boot.data.FileChanges;
import ai.dat.boot.data.SchemaFileState;
import ai.dat.boot.utils.ProjectUtil;
import ai.dat.core.contentstore.ContentStore;
import ai.dat.core.data.project.DatProject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Manages updates to the content store based on changes detected in schema files.
 */
@Slf4j
class ContentStoreManager {

    private final DatProject project;

    private final ContentStore contentStore;
    private final String stateId;
    private final BuildStateManager stateManager;

    /**
     * Creates a content store manager for the supplied project.
     *
     * @param project the project definition containing content store configuration
     * @param projectPath the root path of the project on disk
     * @param stateId the identifier used to persist build state snapshots
     */
    public ContentStoreManager(DatProject project, Path projectPath, String stateId) {
        this.project = project;
        this.stateId = stateId;
        this.contentStore = ProjectUtil.createContentStore(project, projectPath);
        this.stateManager = new BuildStateManager(projectPath);
    }

    /**
     * Applies detected schema file changes to the content store and persists the new state.
     *
     * @param fileStates previously persisted schema file states
     * @param changes the set of detected changes for the current build cycle
     * @throws IOException if persisting the build state fails
     */
    public void updateStore(@NonNull List<SchemaFileState> fileStates,
                            @NonNull FileChanges changes) throws IOException {
        Map<String, SchemaFileState> oldFileStates = fileStates.stream()
                .collect(Collectors.toMap(SchemaFileState::getRelativePath, f -> f));
        // Files that remain unchanged
        List<SchemaFileState> newFileStates = new ArrayList<>(changes.unchangedFiles());
        // Handle deleted files
        changes.deletedFiles().forEach(fs -> remove(oldFileStates, fs));
        // Handle new files
        changes.newFiles().forEach(fs -> add(newFileStates, fs));
        // Handle modified files
        changes.modifiedFiles().forEach(fs -> {
            remove(oldFileStates, fs);
            add(newFileStates, fs);
        });
        // Persist the updated state
        stateManager.saveBuildState(stateId, newFileStates);
    }

    /**
     * Adds a schema file to the content store, indexing newly generated artifacts.
     *
     * @param newFileStates the collection that will hold the updated state entries
     * @param fileState the schema file state describing the new or modified file
     */
    private void add(List<SchemaFileState> newFileStates, SchemaFileState fileState) {
        String projectId = project.getName();
        String relativePath = fileState.getRelativePath();
        SchemaFileState.SchemaFileStateBuilder builder = SchemaFileState.builder()
                .relativePath(fileState.getRelativePath())
                .lastModified(fileState.getLastModified())
                .md5Hash(fileState.getMd5Hash())
                .semanticModelNames(fileState.getSemanticModelNames())
                .modelFileStates(fileState.getModelFileStates());
        Optional.ofNullable(ChangeSemanticModelsCacheUtil.get(projectId, relativePath))
                .filter(Predicate.not(List::isEmpty))
                .map(contentStore::addMdls)
                .ifPresent(builder::semanticModelVectorIds);
        Optional.ofNullable(ChangeQuestionSqlPairsCacheUtil.get(projectId, relativePath))
                .filter(Predicate.not(List::isEmpty))
                .map(contentStore::addSqls)
                .ifPresent(builder::questionSqlPairVectorIds);
        Optional.ofNullable(ChangeWordSynonymPairsCacheUtil.get(projectId, relativePath))
                .filter(Predicate.not(List::isEmpty))
                .map(contentStore::addSyns)
                .ifPresent(builder::wordSynonymPairVectorIds);
        Optional.ofNullable(ChangeKnowledgeCacheUtil.get(projectId, relativePath))
                .filter(Predicate.not(List::isEmpty))
                .map(contentStore::addDocs)
                .ifPresent(builder::knowledgeVectorIds);
        newFileStates.add(builder.build());
    }

    /**
     * Removes content store artifacts associated with a deleted schema file.
     *
     * @param oldFileStates the map of previously known schema file states keyed by relative path
     * @param fileState the schema file state representing the deleted resource
     */
    private void remove(Map<String, SchemaFileState> oldFileStates, SchemaFileState fileState) {
        String relativePath = fileState.getRelativePath();
        if (!oldFileStates.containsKey(relativePath)) {
            return;
        }
        SchemaFileState oldFileState = oldFileStates.get(relativePath);
        Optional.ofNullable(oldFileState.getSemanticModelVectorIds())
                .filter(Predicate.not(List::isEmpty))
                .ifPresent(contentStore::removeMdls);
        Optional.ofNullable(oldFileState.getQuestionSqlPairVectorIds())
                .filter(Predicate.not(List::isEmpty))
                .ifPresent(contentStore::removeSqls);
        Optional.ofNullable(oldFileState.getWordSynonymPairVectorIds())
                .filter(Predicate.not(List::isEmpty))
                .ifPresent(contentStore::removeSyns);
        Optional.ofNullable(oldFileState.getKnowledgeVectorIds())
                .filter(Predicate.not(List::isEmpty))
                .ifPresent(contentStore::removeDocs);
    }

}