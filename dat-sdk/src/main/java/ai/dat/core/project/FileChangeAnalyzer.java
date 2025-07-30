package ai.dat.core.project;

import ai.dat.core.data.DatModel;
import ai.dat.core.data.DatSchema;
import ai.dat.core.project.data.FileChanges;
import ai.dat.core.project.data.SchemaFileState;
import ai.dat.core.project.data.ModelFileState;
import ai.dat.core.project.utils.FileUtil;
import ai.dat.core.utils.DatSchemaUtil;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 文件变化分析器
 *
 * @Author JunjieM
 * @Date 2025/7/17
 */
@Slf4j
public class FileChangeAnalyzer {

    private final Path modelsPath;

    private final Map<Path, DatSchema> schemas;
    private final Map<Path, DatModel> models;

    public FileChangeAnalyzer(Path projectPath,
                              Map<Path, DatSchema> schemas,
                              Map<Path, DatModel> models) {
        this.modelsPath = projectPath.resolve("models");
        this.schemas = schemas;
        this.models = models;
    }

    public FileChanges analyzeChanges(List<SchemaFileState> fileStates) {
        Map<String, SchemaFileState> fileStateMap = fileStates.stream()
                .collect(Collectors.toMap(SchemaFileState::getRelativePath, Function.identity()));

        List<SchemaFileState> newFiles = new ArrayList<>();
        List<SchemaFileState> modifiedFiles = new ArrayList<>();
        List<SchemaFileState> unchangedFiles = new ArrayList<>();

        // 分析当前存在的文件
        for (Map.Entry<Path, DatSchema> entry : schemas.entrySet()) {
            Path filePath = entry.getKey();
            DatSchema schema = entry.getValue();
            String relativePath = modelsPath.relativize(filePath).toString();
            SchemaFileState fileState = fileStateMap.get(relativePath);
            if (fileState == null) {
                // 新文件
                newFiles.add(createFileMetadata(filePath, schema));
            } else {
                // 已存在的文件，检查是否发生变化
                boolean hasChanged = false;
                String md5Hash = null;
                long lastModified = FileUtil.lastModified(filePath);
                if (lastModified - fileState.getLastModified() > 0) {
                    md5Hash = FileUtil.md5(filePath);
                    hasChanged = !md5Hash.equals(fileState.getMd5Hash());
                }
                List<ModelFileState> dependencies = Collections.emptyList();
                if (hasChanged || !fileState.getDependencies().isEmpty()) {
                    dependencies = resolveDependencies(relativePath, schema);
                    hasChanged = hasDependencyChanged(dependencies, fileState.getDependencies());
                }
                if (hasChanged) {
                    // 文件已修改
                    modifiedFiles.add(SchemaFileState.builder()
                            .relativePath(relativePath)
                            .lastModified(lastModified)
                            .md5Hash(md5Hash)
                            .dependencies(dependencies)
                            .build());
                } else {
                    // 文件未变化，保留之前的元数据
                    unchangedFiles.add(fileState);
                }
            }
        }

        // 查找已删除的文件 - 直接内联处理逻辑
        List<String> relativePaths = schemas.keySet().stream()
                .map(p -> modelsPath.relativize(p).toString()).toList();
        List<SchemaFileState> deletedFiles = fileStates.stream()
                .filter(p -> !relativePaths.contains(p.getRelativePath()))
                .collect(Collectors.toList());

        return new FileChanges(newFiles, modifiedFiles, unchangedFiles, deletedFiles);
    }

    public SchemaFileState createFileMetadata(Path filePath, DatSchema schema) {
        String relativePath = modelsPath.relativize(filePath).toString();
        long lastModified = FileUtil.lastModified(filePath);
        String md5Hash = FileUtil.md5(filePath);
        List<ModelFileState> dependencies = resolveDependencies(relativePath, schema);
        return SchemaFileState.builder()
                .relativePath(relativePath)
                .lastModified(lastModified)
                .md5Hash(md5Hash)
                .dependencies(dependencies)
                .build();
    }

    public List<ModelFileState> resolveDependencies(String relativePath, DatSchema schema) {
        return DatSchemaUtil.getModelName(schema).stream()
                .map(modelName -> {
                    try {
                        return getModelFileMetadata(relativePath, modelName);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
    }

    private ModelFileState getModelFileMetadata(String relativePath, String modelName) throws IOException {
        List<Path> modelFiles = models.entrySet().stream()
                .filter(e -> e.getValue().getName().equals(modelName))
                .map(Map.Entry::getKey)
                .toList();
        Preconditions.checkArgument(!modelFiles.isEmpty(),
                "The model file corresponding to the model reference of the semantic model in "
                        + relativePath + " cannot be found model: " + modelName);
        Path modelFilePath = modelFiles.get(0);
        String modelRelativePath = modelsPath.relativize(modelFilePath).toString();
        long lastModified = FileUtil.lastModified(modelFilePath);
        String md5Hash = FileUtil.md5(modelFilePath);
        return new ModelFileState(modelRelativePath, lastModified, md5Hash);
    }

    public boolean hasDependencyChanged(List<ModelFileState> currentDeps,
                                        List<ModelFileState> previousDeps) {
        if (previousDeps == null || previousDeps.isEmpty()) {
            return !currentDeps.isEmpty(); // 之前没有依赖，现在有依赖
        }
        if (currentDeps.isEmpty()) {
            return true; // 之前有依赖，现在没有依赖
        }
        // 检查依赖文件数量是否变化
        if (currentDeps.size() != previousDeps.size()) {
            return true;
        }
        Map<String, ModelFileState> currentMap = currentDeps.stream()
                .collect(Collectors.toMap(ModelFileState::getRelativePath, d -> d));
        Map<String, ModelFileState> previousMap = previousDeps.stream()
                .collect(Collectors.toMap(ModelFileState::getRelativePath, d -> d));
        // 检查每个依赖文件是否变化
        for (Map.Entry<String, ModelFileState> entry : currentMap.entrySet()) {
            String relativePath = entry.getKey();
            ModelFileState current = entry.getValue();
            ModelFileState previous = previousMap.get(relativePath);
            if (previous == null) {
                return true;
            }
            if (current.getLastModified() - previous.getLastModified() > 0) {
                return true;
            }
            if (!current.getMd5Hash().equals(previous.getMd5Hash())) {
                return true;
            }
        }
        // 检查是否有依赖文件被删除
        return previousMap.keySet().stream().anyMatch(key -> !currentMap.containsKey(key));
    }
}