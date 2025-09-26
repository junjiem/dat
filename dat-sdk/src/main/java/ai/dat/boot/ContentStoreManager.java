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
 * 内容存储管理器
 *
 * @Author JunjieM
 * @Date 2025/7/17
 */
@Slf4j
class ContentStoreManager {

    private final DatProject project;

    private final ContentStore contentStore;
    private final String stateId;
    private final BuildStateManager stateManager;

    public ContentStoreManager(DatProject project, Path projectPath, String stateId) {
        this.project = project;
        this.stateId = stateId;
        this.contentStore = ProjectUtil.createContentStore(project, projectPath);
        this.stateManager = new BuildStateManager(projectPath);
    }

    public void updateStore(@NonNull List<SchemaFileState> fileStates,
                            @NonNull FileChanges changes) throws IOException {
        Map<String, SchemaFileState> oldFileStates = fileStates.stream()
                .collect(Collectors.toMap(SchemaFileState::getRelativePath, f -> f));
        // 未变化的文件
        List<SchemaFileState> newFileStates = new ArrayList<>(changes.unchangedFiles());
        // 处理删除的文件
        changes.deletedFiles().forEach(fs -> remove(oldFileStates, fs));
        // 处理新增的文件
        changes.newFiles().forEach(fs -> add(newFileStates, fs));
        // 处理修改的文件
        changes.modifiedFiles().forEach(fs -> {
            remove(oldFileStates, fs);
            add(newFileStates, fs);
        });
        // 保存状态
        stateManager.saveBuildState(stateId, newFileStates);
    }

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