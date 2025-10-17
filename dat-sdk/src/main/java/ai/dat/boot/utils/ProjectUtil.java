package ai.dat.boot.utils;

import ai.dat.core.adapter.DatabaseAdapter;
import ai.dat.core.agent.AskdataAgent;
import ai.dat.core.contentstore.ContentStore;
import ai.dat.core.data.DatModel;
import ai.dat.core.data.DatSchema;
import ai.dat.core.data.DatSeed;
import ai.dat.core.data.project.*;
import ai.dat.core.exception.ValidationException;
import ai.dat.core.factories.*;
import ai.dat.core.factories.data.FactoryDescriptor;
import ai.dat.core.semantic.data.SemanticModel;
import ai.dat.core.utils.DatProjectUtil;
import ai.dat.core.utils.DatSchemaUtil;
import ai.dat.core.utils.FactoryUtil;
import ai.dat.core.utils.SemanticModelUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

@Slf4j
public class ProjectUtil {

    public final static String PROJECT_CONFIG_FILE_NAME_PREFIX = "dat_project";
    public final static String PROJECT_CONFIG_FILE_NAME_YAML = PROJECT_CONFIG_FILE_NAME_PREFIX + ".yaml";
    public final static String PROJECT_CONFIG_FILE_NAME_YML = PROJECT_CONFIG_FILE_NAME_PREFIX + ".yml";

    public final static Set<String> YAML_EXTENSIONS = Set.of(".yaml", ".yml");
    public final static Set<String> SQL_EXTENSIONS = Set.of(".sql");
    public final static Set<String> CSV_EXTENSIONS = Set.of(".csv");

    public final static String MODELS_DIR_NAME = "models";
    public final static String SEEDS_DIR_NAME = "seeds";
    public final static String DAT_DIR_NAME = ".dat";

    public final static String DUCKDB_EMBEDDING_STORE_FILE_PREFIX = "embeddings_";
    public final static String DUCKDB_DATABASE_FILE_NAME = "duckdb";

    private final static ObjectMapper JSON_MAPPER = new ObjectMapper();

    private ProjectUtil() {
    }

    public static String contentStoreFingerprint(@NonNull Path projectPath) {
        DatProject project = loadProject(projectPath);
        return contentStoreFingerprint(project);
    }

    public static String contentStoreFingerprint(@NonNull DatProject project) {
        DatProjectFactory projectFactory = new DatProjectFactory();
        Map<String, String> projectFingerprintConfigs = projectFactory
                .projectFingerprintConfigs(project.getConfiguration());
        EmbeddingConfig embedding = project.getEmbedding();
        EmbeddingStoreConfig embeddingStore = project.getEmbeddingStore();
        ContentStoreConfig contentStore = project.getContentStore();
        EmbeddingModelFactory embeddingModelFactory = EmbeddingModelFactoryManager
                .getFactory(embedding.getProvider());
        Map<String, String> embeddingModelFingerprintConfigs = embeddingModelFactory
                .fingerprintConfigs(embedding.getConfiguration());
        EmbeddingStoreFactory embeddingStoreFactory = EmbeddingStoreFactoryManager
                .getFactory(embeddingStore.getProvider());
        Map<String, String> embeddingStoreFingerprintConfigs = embeddingStoreFactory
                .fingerprintConfigs(embeddingStore.getConfiguration());
        ContentStoreFactory contentStoreFactory = ContentStoreFactoryManager
                .getFactory(contentStore.getProvider());
        Map<String, String> contentStoreFingerprintConfigs = contentStoreFactory
                .fingerprintConfigs(contentStore.getConfiguration());
        try {
            String configStr = String.format("project:name=%s;" +
                            "project:configuration=%s;" +
                            "embedding:provider=%s;" +
                            "embedding:configuration=%s;" +
                            "embeddingStore:provider=%s;" +
                            "embeddingStore:configuration=%s;",
                    project.getName(),
                    JSON_MAPPER.writeValueAsString(projectFingerprintConfigs),
                    embedding.getProvider(),
                    JSON_MAPPER.writeValueAsString(embeddingModelFingerprintConfigs),
                    embeddingStore.getProvider(),
                    JSON_MAPPER.writeValueAsString(embeddingStoreFingerprintConfigs)
            );
            // For backward compatibility
            if (!contentStoreFingerprintConfigs.isEmpty()) {
                configStr += String.format("contentStore:provider=%s;" +
                                "contentStore:configuration=%s;",
                        contentStore.getProvider(),
                        JSON_MAPPER.writeValueAsString(contentStoreFingerprintConfigs)
                );
            }
            return DigestUtils.md5Hex(configStr);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Calculate the content store fingerprint failed", e);
        }
    }

    public static ContentStore createContentStore(@NonNull Path projectPath) {
        DatProject project = loadProject(projectPath);
        return createContentStore(project, projectPath);
    }

    public static ContentStore createContentStore(@NonNull DatProject project, @NonNull Path projectPath) {
        FactoryDescriptor contentStoreFactoryDescriptor = FactoryDescriptor.from(
                project.getContentStore().getProvider(), project.getContentStore().getConfiguration());
        FactoryDescriptor embeddingFactoryDescriptor = FactoryDescriptor.from(
                project.getEmbedding().getProvider(), project.getEmbedding().getConfiguration());

        FactoryDescriptor embeddingStoreFactoryDescriptor =
                createEmbeddingStoreFactoryDescriptor(project, projectPath);

        Map<String, FactoryDescriptor> chatModelFactoryDescriptors = project.getLlms().stream()
                .collect(Collectors.toMap(LlmConfig::getName,
                        o -> FactoryDescriptor.from(o.getProvider(), o.getConfiguration())));

        return FactoryUtil.createContentStore(project.getName(),
                contentStoreFactoryDescriptor, embeddingFactoryDescriptor,
                embeddingStoreFactoryDescriptor, chatModelFactoryDescriptors);
    }

    private static void adjustEmbeddingStoreConfig(@NonNull DatProject project, @NonNull Path projectPath) {
        EmbeddingStoreConfig embeddingStore = project.getEmbeddingStore();
        if (EmbeddingStoreConfig.DUCKDB_PROVIDER.equals(embeddingStore.getProvider())
                && embeddingStore.getConfiguration().getOptional(EmbeddingStoreConfig.DUCKDB_FILE_PATH).isEmpty()) {
            Path datDirPath = projectPath.resolve(DAT_DIR_NAME);
            if (!Files.exists(datDirPath)) {
                try {
                    Files.createDirectories(datDirPath);
                } catch (IOException e) {
                    throw new RuntimeException(
                            "The creation of the .dat directory under the project root directory failed", e);
                }
            }
            String storeFileName = DUCKDB_EMBEDDING_STORE_FILE_PREFIX + contentStoreFingerprint(project);
            Path filePath = projectPath.resolve(DAT_DIR_NAME + File.separator + storeFileName);
            embeddingStore.setConfiguration(
                    Map.of(EmbeddingStoreConfig.DUCKDB_FILE_PATH.key(), filePath.toAbsolutePath().toString())
            );
        }
    }

    private static FactoryDescriptor createEmbeddingStoreFactoryDescriptor(@NonNull DatProject project,
                                                                           @NonNull Path projectPath) {
        adjustEmbeddingStoreConfig(project, projectPath); // 调整Embedding存储配置
        return FactoryDescriptor.from(project.getEmbeddingStore().getProvider(),
                project.getEmbeddingStore().getConfiguration());
    }

    public static AskdataAgent createAskdataAgent(@NonNull Path projectPath,
                                                  @NonNull String agentName,
                                                  Map<String, Object> variables) {
        DatProject project = loadProject(projectPath);
        return createAskdataAgent(project, agentName, projectPath, variables);
    }

    public static AskdataAgent createAskdataAgent(@NonNull DatProject project,
                                                  @NonNull String agentName,
                                                  @NonNull Path projectPath,
                                                  Map<String, Object> variables) {
        Preconditions.checkArgument(!agentName.isBlank(),
                "agentName cannot be empty");
        Map<String, AgentConfig> agentMap = project.getAgents().stream()
                .collect(Collectors.toMap(AgentConfig::getName, o -> o));
        Preconditions.checkArgument(agentMap.containsKey(agentName),
                "The project doesn't exist agent: " + agentName);

        List<SemanticModel> semanticModels = null;
        AgentConfig agentConfig = agentMap.get(agentName);
        List<String> semanticModelNames = agentConfig.getSemanticModels();
        List<String> semanticModelTags = agentConfig.getSemanticModelTags();
        // When the corresponding list of semantic_models or semantic_model_tags is manually specified in the agent
        if (!semanticModelNames.isEmpty() || !semanticModelTags.isEmpty()) {
            ContentStore contentStore = ProjectUtil.createContentStore(project, projectPath);
            List<SemanticModel> allSemanticModels = contentStore.allMdls();
            validateAgent(agentConfig, allSemanticModels);
            semanticModels = allSemanticModels.stream()
                    .filter(model -> semanticModelNames.contains(model.getName())
                            || model.getTags().stream().anyMatch(semanticModelTags::contains))
                    .collect(Collectors.toList());
        }

        Map<String, FactoryDescriptor> chatModelFactoryDescriptors = project.getLlms().stream()
                .collect(Collectors.toMap(LlmConfig::getName,
                        o -> FactoryDescriptor.from(o.getProvider(), o.getConfiguration())));

        FactoryDescriptor agentFactoryDescriptor = FactoryDescriptor.from(
                agentConfig.getProvider(), agentConfig.getConfiguration());

        FactoryDescriptor databaseAdapterFactoryDescriptor =
                createDatabaseAdapterFactoryDescriptor(project, projectPath);

        return FactoryUtil.createAskdataAgent(agentFactoryDescriptor,
                semanticModels, createContentStore(project, projectPath),
                chatModelFactoryDescriptors, databaseAdapterFactoryDescriptor,
                variables);
    }

    @Deprecated
    public static AskdataAgent createAskdataAgent(@NonNull Path projectPath,
                                                  @NonNull String agentName,
                                                  @NonNull List<SemanticModel> semanticModels,
                                                  Map<String, Object> variables) {
        DatProject project = loadProject(projectPath);
        return createAskdataAgent(project, agentName, semanticModels, projectPath, variables);
    }

    @Deprecated
    public static AskdataAgent createAskdataAgent(@NonNull DatProject project,
                                                  @NonNull String agentName,
                                                  @NonNull List<SemanticModel> semanticModels,
                                                  @NonNull Path projectPath,
                                                  Map<String, Object> variables) {
        Preconditions.checkArgument(!agentName.isBlank(),
                "agentName cannot be empty");
        Map<String, AgentConfig> agentMap = project.getAgents().stream()
                .collect(Collectors.toMap(AgentConfig::getName, o -> o));
        Preconditions.checkArgument(agentMap.containsKey(agentName),
                "The project doesn't exist agent: " + agentName);
        SemanticModelUtil.validateSemanticModels(semanticModels);
        validateAgents(project.getAgents(), semanticModels);

        AgentConfig agentConfig = agentMap.get(agentName);
        List<String> semanticModelNames = agentConfig.getSemanticModels();

        Map<String, FactoryDescriptor> chatModelFactoryDescriptors = project.getLlms().stream()
                .collect(Collectors.toMap(LlmConfig::getName,
                        o -> FactoryDescriptor.from(o.getProvider(), o.getConfiguration())));

        List<SemanticModel> agentSemanticModels = semanticModels.stream()
                .filter(model -> semanticModelNames.contains(model.getName()))
                .collect(Collectors.toList());

        FactoryDescriptor agentFactoryDescriptor = FactoryDescriptor.from(
                agentConfig.getProvider(), agentConfig.getConfiguration());

        FactoryDescriptor databaseAdapterFactoryDescriptor =
                createDatabaseAdapterFactoryDescriptor(project, projectPath);

        return FactoryUtil.createAskdataAgent(agentFactoryDescriptor,
                agentSemanticModels, createContentStore(project, projectPath),
                chatModelFactoryDescriptors, databaseAdapterFactoryDescriptor,
                variables);
    }

    @Deprecated
    public static AskdataAgent createAskdataAgent(@NonNull Path projectPath,
                                                  @NonNull String agentName,
                                                  @NonNull List<SemanticModel> semanticModels) {
        return createAskdataAgent(projectPath, agentName, semanticModels, null);
    }

    @Deprecated
    public static AskdataAgent createAskdataAgent(@NonNull DatProject project,
                                                  @NonNull String agentName,
                                                  @NonNull List<SemanticModel> semanticModels,
                                                  @NonNull Path projectPath) {
        return createAskdataAgent(project, agentName, semanticModels, projectPath, null);
    }

    public static DatabaseAdapter createDatabaseAdapter(@NonNull DatProject project, @NonNull Path projectPath) {
        return FactoryUtil.createDatabaseAdapter(createDatabaseAdapterFactoryDescriptor(project, projectPath));
    }

    private static void adjustDatabaseConfig(@NonNull DatProject project, @NonNull Path projectPath) {
        DatabaseConfig databaseConfig = project.getDb();
        if (DatabaseConfig.DUCKDB_PROVIDER.equals(databaseConfig.getProvider())
                && databaseConfig.getConfiguration().getOptional(DatabaseConfig.DUCKDB_FILE_PATH).isEmpty()) {
            Path datDirPath = projectPath.resolve(DAT_DIR_NAME);
            if (!Files.exists(datDirPath)) {
                try {
                    Files.createDirectories(datDirPath);
                } catch (IOException e) {
                    throw new RuntimeException(
                            "The creation of the .dat directory under the project root directory failed", e);
                }
            }
            Path filePath = projectPath.resolve(DAT_DIR_NAME + File.separator + DUCKDB_DATABASE_FILE_NAME);
            databaseConfig.setConfiguration(
                    Map.of(DatabaseConfig.DUCKDB_FILE_PATH.key(), filePath.toAbsolutePath().toString())
            );
        }
    }

    private static FactoryDescriptor createDatabaseAdapterFactoryDescriptor(@NonNull DatProject project,
                                                                            @NonNull Path projectPath) {
        adjustDatabaseConfig(project, projectPath); // 调整数据库配置
        return FactoryDescriptor.from(project.getDb().getProvider(), project.getDb().getConfiguration());
    }

    @Deprecated
    private static void validateAgents(@NonNull List<AgentConfig> agents,
                                       @NonNull List<SemanticModel> semanticModels) {
        Set<String> existingNames = semanticModels.stream()
                .map(SemanticModel::getName)
                .collect(Collectors.toSet());
        Map<String, List<String>> missingNames = agents.stream()
                .collect(Collectors.toMap(AgentConfig::getName,
                        a -> a.getSemanticModels().stream()
                                .filter(name -> !existingNames.contains(name))
                                .toList()))
                .entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!missingNames.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            missingNames.forEach((agentName, missings) ->
                    sb.append(String.format("There are non-existent semantic models %s in the agent '%s'",
                            missings.stream().map(n -> String.format("'%s'", n))
                                    .collect(joining(", ")),
                            agentName
                    )).append("\n"));
            throw new ValidationException(sb.toString());
        }
    }

    private static void validateAgent(@NonNull AgentConfig agentConfig,
                                      @NonNull List<SemanticModel> semanticModels) {
        Set<String> existingNames = semanticModels.stream()
                .map(SemanticModel::getName)
                .collect(Collectors.toSet());
        List<String> missingNames = agentConfig.getSemanticModels().stream()
                .filter(name -> !existingNames.contains(name))
                .toList();
        Set<String> existingTags = semanticModels.stream()
                .map(SemanticModel::getTags)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        List<String> missingTags = agentConfig.getSemanticModelTags().stream()
                .filter(tag -> !existingTags.contains(tag))
                .toList();
        if (!missingNames.isEmpty() || !missingTags.isEmpty()) {
            String message = Stream.of(
                    !missingNames.isEmpty() ?
                            String.format("There are non-existent semantic model names %s in the agent '%s'. " +
                                            "Please check the semantic models YAML in your project!",
                                    missingNames.stream().map(n -> String.format("'%s'", n)).collect(joining(", ")),
                                    agentConfig.getName())
                            : null,
                    !missingTags.isEmpty() ?
                            String.format("There are non-existent semantic model tags %s in the agent '%s'. " +
                                            "Please check the semantic models YAML in your project!",
                                    missingTags.stream().map(n -> String.format("'%s'", n)).collect(joining(", ")),
                                    agentConfig.getName())
                            : null
            ).filter(Objects::nonNull).collect(joining("\n"));
            throw new ValidationException(message);
        }
    }

    public static DatProject loadProject(@NonNull Path projectPath) {
        Path filePath = findProjectConfigFile(projectPath);
        if (filePath == null) {
            throw new RuntimeException("The project configuration file not found "
                    + PROJECT_CONFIG_FILE_NAME_YAML + " or " + PROJECT_CONFIG_FILE_NAME_YML
                    + ", please ensure that the project configuration file exists in the project root directory.");
        }
        try {
            String yamlContent = Files.readString(filePath);
            return DatProjectUtil.datProject(yamlContent);
        } catch (Exception e) {
            throw new RuntimeException("The " + projectPath.relativize(filePath)
                    + " YAML file content does not meet the requirements: \n" + e.getMessage(), e);
        }
    }

    private static Path findProjectConfigFile(@NonNull Path projectPath) {
        Path projectYaml = projectPath.resolve(PROJECT_CONFIG_FILE_NAME_YAML);
        Path projectYml = projectPath.resolve(PROJECT_CONFIG_FILE_NAME_YML);
        if (Files.exists(projectYaml)) {
            return projectYaml;
        } else if (Files.exists(projectYml)) {
            return projectYml;
        }
        return null;
    }

    public static Map<Path, DatSchema> loadAllSchema(@NonNull Path dirPath) {
        Map<Path, DatSchema> schemas = scanYamlFiles(dirPath).stream()
                .collect(Collectors.toMap(p -> p, p -> loadSchema(p, dirPath)));
        validateYamlFiles(dirPath, schemas);
        return schemas;
    }

    public static DatSchema loadSchema(@NonNull Path filePath, @NonNull Path dirPath) {
        try {
            String content = Files.readString(filePath);
            return DatSchemaUtil.datSchema(content);
        } catch (Exception e) {
            throw new RuntimeException("The " + dirPath.relativize(filePath)
                    + " YAML file content does not meet the requirements: \n" + e.getMessage(), e);
        }
    }

    private static void validateYamlFiles(@NonNull Path dirPath, @NonNull Map<Path, DatSchema> schemas) {
        Map<String, List<Path>> nameToPaths;
        Map<String, List<Path>> duplicates;
        // 校验种子名称是否重复
        nameToPaths = schemas.entrySet().stream()
                .flatMap(entry -> entry.getValue().getSeeds().stream()
                        .map(seed -> Map.entry(seed.getName(), entry.getKey())))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
        duplicates = nameToPaths.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!duplicates.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            duplicates.forEach((seedName, paths) -> {
                sb.append("Discover duplicate seed name: ").append(seedName).append("\n");
                sb.append("The YAML file relative path: \n");
                paths.stream()
                        .map(p -> dirPath.relativize(p).toString())
                        .forEach(p -> sb.append("  - ").append(p).append("\n"));
                sb.append("\n");
            });
            throw new ValidationException(sb.toString());
        }
        // 校验语义模型名称是否重复
        nameToPaths = schemas.entrySet().stream()
                .flatMap(entry -> entry.getValue().getSemanticModels().stream()
                        .map(model -> Map.entry(model.getName(), entry.getKey())))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
        duplicates = nameToPaths.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!duplicates.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            duplicates.forEach((semanticModelName, paths) -> {
                sb.append("Discover duplicate semantic model name: ").append(semanticModelName).append("\n");
                sb.append("The YAML file relative path: \n");
                paths.stream()
                        .map(p -> dirPath.relativize(p).toString())
                        .forEach(p -> sb.append("  - ").append(p).append("\n"));
                sb.append("\n");
            });
            throw new ValidationException(sb.toString());
        }
    }

    public static Map<Path, DatModel> loadAllModel(@NonNull Path modelsPath) {
        Map<Path, DatModel> models = scanSqlFiles(modelsPath).stream()
                .collect(Collectors.toMap(p -> p, p -> loadModel(p, modelsPath)));
        validateModelFiles(modelsPath, models);
        return models;
    }

    public static DatModel loadModel(@NonNull Path filePath, @NonNull Path modelsPath) {
        try {
            String content = Files.readString(filePath);
            String name = FileUtil.fileNameWithoutSuffix(filePath.getFileName().toString());
            return DatModel.from(name, content);
        } catch (Exception e) {
            throw new RuntimeException("The " + modelsPath.relativize(filePath)
                    + " SQL file content does not meet the requirements: \n" + e.getMessage(), e);
        }
    }

    private static void validateModelFiles(@NonNull Path modelsPath, @NonNull Map<Path, DatModel> models) {
        Map<String, List<Path>> nameToPaths = models.entrySet().stream()
                .collect(Collectors.groupingBy(
                        e -> e.getValue().getName(),
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())
                ));
        Map<String, List<Path>> duplicates = nameToPaths.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!duplicates.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            duplicates.forEach((modelName, paths) -> {
                sb.append("Discover duplicate model name: ").append(modelName).append("\n");
                sb.append("The SQL file relative path: \n");
                paths.stream()
                        .map(p -> modelsPath.relativize(p).toString())
                        .forEach(p -> sb.append("  - ").append(p).append("\n"));
                sb.append("\n");
            });
            throw new ValidationException(sb.toString());
        }
    }

    public static Map<Path, DatSeed> loadAllSeed(@NonNull Path seedsPath) {
        Map<Path, DatSeed> seeds = scanCsvFiles(seedsPath).stream()
                .collect(Collectors.toMap(p -> p, p -> loadSeed(p, seedsPath)));
        validateSeedFiles(seedsPath, seeds);
        return seeds;
    }

    private static DatSeed loadSeed(@NonNull Path filePath, @NonNull Path seedsPath) {
        try {
            String content = Files.readString(filePath);
            String name = FileUtil.fileNameWithoutSuffix(filePath.getFileName().toString());
            return DatSeed.from(name, content);
        } catch (Exception e) {
            throw new RuntimeException("The " + seedsPath.relativize(filePath)
                    + " CSV file content does not meet the requirements: \n" + e.getMessage(), e);
        }
    }

    private static void validateSeedFiles(@NonNull Path seedsPath, @NonNull Map<Path, DatSeed> seeds) {
        Map<String, List<Path>> nameToPaths = seeds.entrySet().stream()
                .collect(Collectors.groupingBy(
                        e -> e.getValue().getName(),
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())
                ));
        Map<String, List<Path>> duplicates = nameToPaths.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!duplicates.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            duplicates.forEach((seedName, paths) -> {
                sb.append("Discover duplicate seed name: ").append(seedName).append("\n");
                sb.append("The CSV file relative path: \n");
                paths.stream()
                        .map(p -> seedsPath.relativize(p).toString())
                        .forEach(p -> sb.append("  - ").append(p).append("\n"));
                sb.append("\n");
            });
            throw new ValidationException(sb.toString());
        }
    }

    public static List<Path> scanYamlFiles(@NonNull Path dirPath) {
        List<Path> files = new ArrayList<>();
        Preconditions.checkArgument(Files.exists(dirPath),
                "There is no '" + dirPath.getFileName() + "' directory in the project root directory");
        try {
            Files.walkFileTree(dirPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String fileName = file.getFileName().toString();
                    if (isYamlFile(fileName)) { // 检查是否为YAML文件
                        files.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("The scan for the YAML file in the 'models' directory failed", e);
        }
        return files;
    }

    private static boolean isYamlFile(@NonNull String fileName) {
        return YAML_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    public static List<Path> scanSqlFiles(@NonNull Path modelsPath) {
        List<Path> files = new ArrayList<>();
        Preconditions.checkArgument(Files.exists(modelsPath),
                "There is no 'models' directory in the project root directory");
        try {
            Files.walkFileTree(modelsPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String fileName = file.getFileName().toString();
                    if (isSqlFile(fileName)) { // 检查是否为SQL文件
                        files.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("The scan for the SQL file in the 'models' directory failed", e);
        }
        return files;
    }

    private static boolean isSqlFile(@NonNull String fileName) {
        return SQL_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    private static List<Path> scanCsvFiles(@NonNull Path seedsPath) {
        List<Path> files = new ArrayList<>();
        Preconditions.checkArgument(Files.exists(seedsPath),
                "There is no 'seeds' directory in the project root directory");
        try {
            Files.walkFileTree(seedsPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String fileName = file.getFileName().toString();
                    if (isCsvFile(fileName)) { // 检查是否为CSV文件
                        files.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("The scan for the CSV file in the 'seeds' directory failed", e);
        }
        return files;
    }

    private static boolean isCsvFile(@NonNull String fileName) {
        return CSV_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }
}