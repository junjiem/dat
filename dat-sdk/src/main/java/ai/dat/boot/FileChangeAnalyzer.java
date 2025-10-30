package ai.dat.boot;

import ai.dat.boot.data.FileChanges;
import ai.dat.boot.data.RelevantFileState;
import ai.dat.boot.data.SchemaFileState;
import ai.dat.boot.utils.FileUtil;
import ai.dat.boot.utils.ProjectUtil;
import ai.dat.core.data.DatModel;
import ai.dat.core.data.DatSchema;
import ai.dat.core.data.example.Example;
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
 * Analyzes schema and model files to detect additions, deletions, and modifications for incremental builds.
 */
@Slf4j
class FileChangeAnalyzer {

    private final Path modelsPath;

    private final DatProject project;

    private final List<Path> yamlFilePaths;
    private final List<Path> sqlFilePaths;
    private final List<String> sqlFileRelativePaths;

    /**
     * Creates a file change analyzer for the given project and root path.
     *
     * @param project the project definition containing model references
     * @param projectPath the root directory containing project files
     */
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

    /**
     * Analyzes schema files and determines which ones are new, modified, unchanged, or deleted.
     *
     * @param fileStates previously persisted schema file states
     * @return a collection describing detected file changes
     */
    public FileChanges analyzeChanges(List<SchemaFileState> fileStates) {
        ChangeSemanticModelsCacheUtil.remove(project.getName());

        Map<String, SchemaFileState> fileStateMap = fileStates.stream()
                .collect(Collectors.toMap(SchemaFileState::getRelativePath, Function.identity()));

        List<SchemaFileState> newFiles = new ArrayList<>();
        List<SchemaFileState> modifiedFiles = new ArrayList<>();
        List<SchemaFileState> unchangedFiles = new ArrayList<>();

        // Analyze existing files
        for (Path filePath : yamlFilePaths) {
            String relativePath = modelsPath.relativize(filePath).toString();
            SchemaFileState fileState = fileStateMap.get(relativePath);
            if (fileState == null) {
                // New YAML file
                long lastModified = FileUtil.lastModified(filePath);
                String md5Hash = FileUtil.md5(filePath);
                DatSchema schema = ProjectUtil.loadSchema(filePath, modelsPath);
                List<RelevantFileState> modelFileStates = resolveModelFileStates(relativePath, schema);
                newFiles.add(createSchemaFileState(
                        relativePath, lastModified, md5Hash, schema, modelFileStates));
            } else {
                // Existing YAML file, check for changes
                boolean hasChanged = false;
                String md5Hash = null;
                long lastModified = FileUtil.lastModified(filePath);
                if (lastModified - fileState.getLastModified() > 0) {
                    md5Hash = FileUtil.md5(filePath);
                    hasChanged = !md5Hash.equals(fileState.getMd5Hash());
                }
                List<RelevantFileState> modelFileStates = Collections.emptyList();
                DatSchema schema = null;
                boolean hasModelFiles = !fileState.getModelFileStates().isEmpty();
                if (hasChanged || hasModelFiles) {
                    schema = ProjectUtil.loadSchema(filePath, modelsPath);
                }
                if (hasModelFiles) {
                    modelFileStates = resolveModelFileStates(relativePath, schema);
                    hasChanged = hasChanged || hasModelFileChanged(modelFileStates, fileState.getModelFileStates());
                }
                if (hasChanged) {
                    // YAML file modified
                    modifiedFiles.add(createSchemaFileState(
                            relativePath, lastModified, md5Hash, schema, modelFileStates));
                } else {
                    // YAML file unchanged; retain previous metadata
                    unchangedFiles.add(fileState);
                }
            }
        }

        // Verify that semantic model names do not conflict
        validateSemanticModelNames(newFiles, modifiedFiles, unchangedFiles);

        // Identify deleted YAML files
        List<String> relativePaths = yamlFilePaths.stream()
                .map(p -> modelsPath.relativize(p).toString()).toList();
        List<SchemaFileState> deletedFiles = fileStates.stream()
                .filter(p -> !relativePaths.contains(p.getRelativePath()))
                .collect(Collectors.toList());

        return new FileChanges(newFiles, modifiedFiles, unchangedFiles, deletedFiles);
    }

    /**
     * Builds a {@link SchemaFileState} for the specified schema file and caches related artifacts.
     *
     * @param relativePath the schema file path relative to the models directory
     * @param lastModified the last modified timestamp of the file
     * @param md5Hash the MD5 hash of the file contents
     * @param schema the parsed schema definition
     * @param modelFileStates metadata about SQL model files referenced by the schema
     * @return a populated {@link SchemaFileState}
     */
    private SchemaFileState createSchemaFileState(String relativePath, long lastModified, String md5Hash,
                                                  DatSchema schema, List<RelevantFileState> modelFileStates) {
        ChangeSemanticModelsCacheUtil.add(project.getName(), relativePath,
                DatSchemaUtil.getSemanticModels(schema, getDatModels(modelFileStates)));
        Example example = schema.getExample();
        if (example != null) {
            ChangeQuestionSqlPairsCacheUtil.add(project.getName(), relativePath,
                    example.getQuestionSqlPairs());
            ChangeWordSynonymPairsCacheUtil.add(project.getName(), relativePath,
                    example.getWordSynonymPairs());
            ChangeKnowledgeCacheUtil.add(project.getName(), relativePath,
                    example.getKnowledge());
        }
        List<String> semanticModelNames = schema.getSemanticModels().stream()
                .map(SemanticModel::getName)
                .collect(Collectors.toList());
        return SchemaFileState.builder()
                .relativePath(relativePath)
                .lastModified(lastModified)
                .md5Hash(md5Hash)
                .semanticModelNames(semanticModelNames)
                .modelFileStates(modelFileStates)
                .build();
    }

    /**
     * Ensures that semantic model names remain unique across all schema files.
     *
     * @param newFiles newly detected schema files
     * @param modifiedFiles schema files that have been modified
     * @param unchangedFiles schema files whose metadata remained unchanged
     */
    private void validateSemanticModelNames(List<SchemaFileState> newFiles,
                                            List<SchemaFileState> modifiedFiles,
                                            List<SchemaFileState> unchangedFiles) {
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

    /**
     * Resolves metadata for SQL model files referenced by the schema.
     *
     * @param relativePath the schema file path relative to the models directory
     * @param schema the parsed schema definition
     * @return a list of metadata for referenced model files
     */
    private List<RelevantFileState> resolveModelFileStates(String relativePath, DatSchema schema) {
        return DatSchemaUtil.getModelName(schema).stream()
                .map(modelName -> {
                    try {
                        return getModelFileMetadata(relativePath, modelName);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
    }

    /**
     * Loads DAT models associated with the supplied file metadata.
     *
     * @param modelFileStates metadata describing SQL model files
     * @return loaded DAT models corresponding to the metadata
     */
    private List<DatModel> getDatModels(List<RelevantFileState> modelFileStates) {
        if (modelFileStates == null || modelFileStates.isEmpty()) {
            return Collections.emptyList();
        }
        return modelFileStates.stream()
                .filter(f -> sqlFileRelativePaths.contains(f.getRelativePath()))
                .map(f -> ProjectUtil.loadModel(modelsPath.resolve(f.getRelativePath()), modelsPath))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves metadata for the model file referenced by the semantic model.
     *
     * @param relativePath the schema file path relative to the models directory
     * @param modelName the referenced model name
     * @return metadata for the corresponding SQL model file
     * @throws IOException if reading file metadata fails
     */
    private RelevantFileState getModelFileMetadata(String relativePath, String modelName) throws IOException {
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
        return new RelevantFileState(modelRelativePath, lastModified, md5Hash);
    }

    /**
     * Determines whether any referenced SQL model files have changed since the previous build.
     *
     * @param currentDeps metadata for current model dependencies
     * @param previousDeps metadata for previously tracked model dependencies
     * @return {@code true} if the dependency set has changed, otherwise {@code false}
     */
    private boolean hasModelFileChanged(List<RelevantFileState> currentDeps,
                                        List<RelevantFileState> previousDeps) {
        if (previousDeps == null || previousDeps.isEmpty()) {
            return !currentDeps.isEmpty(); // Previously no dependencies, now present
        }
        if (currentDeps.isEmpty()) {
            return true; // Previously had dependencies, now none
        }
        // Check if the number of dependencies has changed
        if (currentDeps.size() != previousDeps.size()) {
            return true;
        }
        Map<String, RelevantFileState> currentMap = currentDeps.stream()
                .collect(Collectors.toMap(RelevantFileState::getRelativePath, d -> d));
        Map<String, RelevantFileState> previousMap = previousDeps.stream()
                .collect(Collectors.toMap(RelevantFileState::getRelativePath, d -> d));
        // Verify each dependency for changes
        for (Map.Entry<String, RelevantFileState> entry : currentMap.entrySet()) {
            String relativePath = entry.getKey();
            RelevantFileState current = entry.getValue();
            RelevantFileState previous = previousMap.get(relativePath);
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
        // Determine if any dependencies were removed
        return previousMap.keySet().stream().anyMatch(key -> !currentMap.containsKey(key));
    }
}