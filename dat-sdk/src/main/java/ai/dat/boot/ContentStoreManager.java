package ai.dat.boot;

import ai.dat.boot.data.FileChanges;
import ai.dat.boot.data.SchemaFileState;
import ai.dat.boot.utils.ProjectUtil;
import ai.dat.core.contentstore.ContentStore;
import ai.dat.core.data.project.DatProject;
import ai.dat.core.semantic.data.SemanticModel;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 内容存储管理器
 *
 * @Author JunjieM
 * @Date 2025/7/17
 */
@Slf4j
public class ContentStoreManager {

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
        Map<String, SchemaFileState> fileStateMap = fileStates.stream()
                .collect(Collectors.toMap(SchemaFileState::getRelativePath, f -> f));
        // 未变化的文件
        List<SchemaFileState> newFileStates = new ArrayList<>(changes.unchangedFiles());
        // 处理删除的文件
        for (SchemaFileState fs : changes.deletedFiles()) {
            String relativePath = fs.getRelativePath();
            if (!fileStateMap.containsKey(relativePath)) {
                continue;
            }
            SchemaFileState fileState = fileStateMap.get(relativePath);
            List<String> oldVectorIds = fileState.getVectorIds();
            if (oldVectorIds != null && !oldVectorIds.isEmpty()) {
                try {
                    contentStore.removeMdls(oldVectorIds);
                } catch (Exception e) {
                    log.warn("Failed to delete the state: " + relativePath);
                    newFileStates.add(fileState);
                }
            }
        }
        // 处理新增的文件
        for (SchemaFileState fs : changes.newFiles()) {
            List<SemanticModel> semanticModels = ChangeSemanticModelsCacheUtil
                    .get(project.getName(), fs.getRelativePath());
            if (semanticModels != null && !semanticModels.isEmpty()) {
                List<String> vectorIds = contentStore.addMdls(semanticModels);
                newFileStates.add(SchemaFileState.builder()
                        .relativePath(fs.getRelativePath())
                        .lastModified(fs.getLastModified())
                        .md5Hash(fs.getMd5Hash())
                        .semanticModelNames(fs.getSemanticModelNames())
                        .vectorIds(vectorIds)
                        .dependencies(fs.getDependencies())
                        .build());
            }
        }
        // 处理修改的文件
        for (SchemaFileState fs : changes.modifiedFiles()) {
            String relativePath = fs.getRelativePath();
            if (!fileStateMap.containsKey(relativePath)) {
                continue;
            }
            SchemaFileState fileState = fileStateMap.get(relativePath);
            List<String> oldVectorIds = fileState.getVectorIds();
            if (oldVectorIds != null && !oldVectorIds.isEmpty()) {
                try {
                    contentStore.removeMdls(oldVectorIds);
                } catch (Exception e) {
                    log.warn("Failed to delete the state: " + relativePath);
                    newFileStates.add(fileState);
                    continue;
                }
            }
            List<SemanticModel> semanticModels = ChangeSemanticModelsCacheUtil
                    .get(project.getName(), fs.getRelativePath());
            if (semanticModels != null && !semanticModels.isEmpty()) {
                List<String> vectorIds = contentStore.addMdls(semanticModels);
                newFileStates.add(SchemaFileState.builder()
                        .relativePath(fs.getRelativePath())
                        .lastModified(fs.getLastModified())
                        .md5Hash(fs.getMd5Hash())
                        .semanticModelNames(fs.getSemanticModelNames())
                        .vectorIds(vectorIds)
                        .dependencies(fs.getDependencies())
                        .build());
            }
        }
        stateManager.saveBuildState(stateId, newFileStates);
    }

}