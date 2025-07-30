package ai.dat.core.project;

import ai.dat.core.adapter.DatabaseAdapter;
import ai.dat.core.contentstore.ContentStore;
import ai.dat.core.data.DatModel;
import ai.dat.core.data.DatSchema;
import ai.dat.core.exception.ValidationException;
import ai.dat.core.project.data.FileChanges;
import ai.dat.core.project.data.ModelFileState;
import ai.dat.core.project.data.SchemaFileState;
import ai.dat.core.semantic.data.SemanticModel;
import ai.dat.core.utils.DatSchemaUtil;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 内容存储管理器
 *
 * @Author JunjieM
 * @Date 2025/7/17
 */
@Slf4j
public class ContentStoreManager {

    private final Path projectPath;
    private final Path modelsPath;

    private final Map<String, DatSchema> schemas;
    private final Map<String, DatModel> models;

    private final ContentStore contentStore;
    private final DatabaseAdapter databaseAdapter;
    private final String stateId;
    private final BuildStateManager stateManager;

    private Map<String, List<SemanticModel>> semanticModelsCache;

    public ContentStoreManager(Path projectPath,
                               DatabaseAdapter databaseAdapter,
                               ContentStore contentStore,
                               String stateId,
                               Map<Path, DatSchema> schemas,
                               Map<Path, DatModel> models) {
        this.projectPath = projectPath;
        this.modelsPath = projectPath.resolve("models");
        this.databaseAdapter = databaseAdapter;
        this.contentStore = contentStore;
        this.stateId = stateId;
        this.schemas = schemas.entrySet().stream()
                .collect(Collectors.toMap(e -> modelsPath.relativize(e.getKey()).toString(),
                        Map.Entry::getValue));
        this.models = models.entrySet().stream()
                .collect(Collectors.toMap(e -> modelsPath.relativize(e.getKey()).toString(),
                        Map.Entry::getValue));
        this.stateManager = new BuildStateManager(projectPath);
    }

    private void validateModelSqls(@NonNull FileChanges changes) {
        semanticModelsCache = Stream.of(changes.newFiles(), changes.modifiedFiles())
                .flatMap(List::stream)
                .collect(Collectors.toMap(SchemaFileState::getRelativePath, fs -> {
                    DatSchema schema = schemas.get(fs.getRelativePath());
                    List<DatModel> models = getDatModels(fs.getDependencies());
                    return DatSchemaUtil.getSemanticModels(schema, models);
                })).entrySet().stream().filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String, List<ValidationMessage>> validations = semanticModelsCache.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue().stream().map(m -> {
                                    SQLException sqlException;
                                    String sql = "SELECT 1 FROM (" + m.getModel() + ") AS __dat_model WHERE 1=0";
                                    try {
                                        databaseAdapter.executeQuery(sql);
                                        sqlException = null;
                                    } catch (SQLException exception) {
                                        sqlException = exception;
                                    }
                                    return new ValidationMessage(m.getName(), sqlException);
                                })
                                .filter(vm -> vm.sqlException != null)
                                .collect(Collectors.toList())
                ))
                .entrySet().stream().filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!validations.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            validations.forEach((relativePath, validationMessages) -> {
                sb.append("There has exceptions in the model SQL validation of the semantic model, " +
                                "in the YAML file relative path: ").append(relativePath).append("\n");
                validationMessages.forEach(m -> sb.append("  - ").append(m.semanticModelName)
                        .append(": ").append(m.sqlException.getMessage()).append("\n"));
                sb.append("\n");
            });
            throw new ValidationException(sb.toString());
        }
    }

    public void updateStore(@NonNull List<SchemaFileState> fileStates,
                            @NonNull FileChanges changes) throws IOException {
        // 校验模型SQL
        validateModelSqls(changes);
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
            List<SemanticModel> semanticModels = semanticModelsCache.get(fs.getRelativePath());
            if (semanticModels != null && !semanticModels.isEmpty()) {
                List<String> vectorIds = contentStore.addMdls(semanticModels);
                newFileStates.add(SchemaFileState.builder()
                        .relativePath(fs.getRelativePath())
                        .lastModified(fs.getLastModified())
                        .md5Hash(fs.getMd5Hash())
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
            List<SemanticModel> semanticModels = semanticModelsCache.get(fs.getRelativePath());
            if (semanticModels != null && !semanticModels.isEmpty()) {
                List<String> vectorIds = contentStore.addMdls(semanticModels);
                newFileStates.add(SchemaFileState.builder()
                        .relativePath(fs.getRelativePath())
                        .lastModified(fs.getLastModified())
                        .md5Hash(fs.getMd5Hash())
                        .vectorIds(vectorIds)
                        .dependencies(fs.getDependencies())
                        .build());
            }
        }
        stateManager.saveBuildState(stateId, newFileStates);
    }

    private List<DatModel> getDatModels(List<ModelFileState> dependencies) {
        if (dependencies == null || dependencies.isEmpty()) {
            return Collections.emptyList();
        }
        return dependencies.stream().filter(f -> models.containsKey(f.getRelativePath()))
                .map(f -> models.get(f.getRelativePath()))
                .collect(Collectors.toList());
    }

    @AllArgsConstructor
    static class ValidationMessage {
        private String semanticModelName;
        private SQLException sqlException;
    }

}