package ai.dat.boot;

import ai.dat.boot.data.FileChanges;
import ai.dat.boot.data.ModelFileState;
import ai.dat.boot.data.SchemaFileState;
import ai.dat.boot.utils.FileUtil;
import ai.dat.boot.utils.ProjectUtil;
import ai.dat.core.data.DatModel;
import ai.dat.core.data.DatSchema;
import ai.dat.core.data.project.DatProject;
import ai.dat.core.exception.ValidationException;
import ai.dat.core.semantic.data.SemanticModel;
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
import java.util.stream.Stream;

/**
 * 文件变化分析器
 *
 * @Author JunjieM
 * @Date 2025/7/17
 */
@Slf4j
public class FileChangeAnalyzer {

    private final Path modelsPath;

    private final DatProject project;

    private final List<Path> yamlFilePaths;
    private final List<Path> sqlFilePaths;
    private final List<String> sqlFileRelativePaths;

    public FileChangeAnalyzer(DatProject project,
                              Path projectPath) {
        this.project = project;
        this.modelsPath = projectPath.resolve(ProjectUtil.MODELS_DIR_NAME);
        this.yamlFilePaths = ProjectUtil.scanYamlFiles(modelsPath);
        this.sqlFilePaths = ProjectUtil.scanSqlFiles(modelsPath);
        this.sqlFileRelativePaths = sqlFilePaths.stream()
                .map(p -> modelsPath.relativize(p).toString())
                .collect(Collectors.toList());
    }

    public FileChanges analyzeChanges(List<SchemaFileState> fileStates) {
        ChangeSemanticModelsCacheUtil.remove(project.getName());

        Map<String, SchemaFileState> fileStateMap = fileStates.stream()
                .collect(Collectors.toMap(SchemaFileState::getRelativePath, Function.identity()));

        List<SchemaFileState> newFiles = new ArrayList<>();
        List<SchemaFileState> modifiedFiles = new ArrayList<>();
        List<SchemaFileState> unchangedFiles = new ArrayList<>();

        // 分析当前存在的文件
        for (Path filePath : yamlFilePaths) {
            String relativePath = modelsPath.relativize(filePath).toString();
            SchemaFileState fileState = fileStateMap.get(relativePath);
            if (fileState == null) {
                // 新文件
                long lastModified = FileUtil.lastModified(filePath);
                String md5Hash = FileUtil.md5(filePath);
                DatSchema schema = ProjectUtil.loadSchema(filePath, modelsPath);
                List<ModelFileState> dependencies = resolveDependencies(relativePath, schema);
                ChangeSemanticModelsCacheUtil.add(project.getName(), relativePath,
                        DatSchemaUtil.getSemanticModels(schema, getDatModels(dependencies)));
                List<String> semanticModelNames = schema.getSemanticModels().stream()
                        .map(SemanticModel::getName)
                        .collect(Collectors.toList());
                newFiles.add(SchemaFileState.builder()
                        .relativePath(relativePath)
                        .lastModified(lastModified)
                        .md5Hash(md5Hash)
                        .semanticModelNames(semanticModelNames)
                        .dependencies(dependencies)
                        .build());
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
                DatSchema schema = null;
                if (hasChanged || !fileState.getDependencies().isEmpty()) {
                    schema = ProjectUtil.loadSchema(filePath, modelsPath);
                }
                if (!fileState.getDependencies().isEmpty()) {
                    dependencies = resolveDependencies(relativePath, schema);
                    hasChanged = hasDependencyChanged(dependencies, fileState.getDependencies());
                }
                if (hasChanged) {
                    // 文件已修改
                    ChangeSemanticModelsCacheUtil.add(project.getName(), relativePath,
                            DatSchemaUtil.getSemanticModels(schema, getDatModels(dependencies)));
                    List<String> semanticModelNames = schema.getSemanticModels().stream()
                            .map(SemanticModel::getName)
                            .collect(Collectors.toList());
                    modifiedFiles.add(SchemaFileState.builder()
                            .relativePath(relativePath)
                            .lastModified(lastModified)
                            .md5Hash(md5Hash)
                            .semanticModelNames(semanticModelNames)
                            .dependencies(dependencies)
                            .build());
                } else {
                    // 文件未变化，保留之前的元数据
                    unchangedFiles.add(fileState);
                }
            }
        }

        // 检查语义模型名称是否有重复
        validateSemanticModelNames(newFiles, modifiedFiles, unchangedFiles);

        // 查找已删除的文件 - 直接内联处理逻辑
        List<String> relativePaths = yamlFilePaths.stream()
                .map(p -> modelsPath.relativize(p).toString()).toList();
        List<SchemaFileState> deletedFiles = fileStates.stream()
                .filter(p -> !relativePaths.contains(p.getRelativePath()))
                .collect(Collectors.toList());

        return new FileChanges(newFiles, modifiedFiles, unchangedFiles, deletedFiles);
    }

    private void validateSemanticModelNames(List<SchemaFileState> newFiles,
                                            List<SchemaFileState> modifiedFiles,
                                            List<SchemaFileState> unchangedFiles) {
        // 校验语义模型名称是否重复
        Map<String, List<String>> nameToRelativePaths = Stream.of(newFiles, modifiedFiles, unchangedFiles)
                .flatMap(List::stream)
                .filter(s -> s.getSemanticModelNames() != null && !s.getSemanticModelNames().isEmpty())
                .flatMap(s -> s.getSemanticModelNames().stream().map(n -> Map.entry(n, s.getRelativePath())))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
        Map<String, List<String>> duplicates = nameToRelativePaths.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!duplicates.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            duplicates.forEach((semanticModelName, relativePaths) -> {
                sb.append("Discover duplicate semantic model name: ").append(semanticModelName).append("\n");
                sb.append("The YAML file relative path: \n");
                relativePaths.forEach(relativePath -> sb.append("  - ").append(relativePath).append("\n"));
                sb.append("\n");
            });
            throw new ValidationException(sb.toString());
        }
    }

    private List<ModelFileState> resolveDependencies(String relativePath, DatSchema schema) {
        return DatSchemaUtil.getModelName(schema).stream()
                .map(modelName -> {
                    try {
                        return getModelFileMetadata(relativePath, modelName);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
    }

    private List<DatModel> getDatModels(List<ModelFileState> dependencies) {
        if (dependencies == null || dependencies.isEmpty()) {
            return Collections.emptyList();
        }
        return dependencies.stream()
                .filter(f -> sqlFileRelativePaths.contains(f.getRelativePath()))
                .map(f -> ProjectUtil.loadModel(modelsPath.resolve(f.getRelativePath()), modelsPath))
                .collect(Collectors.toList());
    }

    private ModelFileState getModelFileMetadata(String relativePath, String modelName) throws IOException {
        List<Path> modelFiles = sqlFilePaths.stream()
                .filter(p -> FileUtil.fileNameWithoutSuffix(p.getFileName().toString()).equals(modelName))
                .toList();
        Preconditions.checkArgument(!modelFiles.isEmpty(),
                "The model file corresponding to the model reference of the semantic model in "
                        + relativePath + " cannot be found model: " + modelName);
        if (modelFiles.size() > 1) {
            StringBuffer sb = new StringBuffer();
            sb.append("Discover duplicate model name: ").append(modelName).append("\n");
            sb.append("Then SQL file relative path: \n");
            modelFiles.stream()
                    .map(p -> modelsPath.relativize(p).toString())
                    .forEach(p -> sb.append("  - ").append(p).append("\n"));
            throw new ValidationException(sb.toString());
        }
        Path modelFilePath = modelFiles.get(0);
        String modelRelativePath = modelsPath.relativize(modelFilePath).toString();
        long lastModified = FileUtil.lastModified(modelFilePath);
        String md5Hash = FileUtil.md5(modelFilePath);
        return new ModelFileState(modelRelativePath, lastModified, md5Hash);
    }

    private boolean hasDependencyChanged(List<ModelFileState> currentDeps,
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