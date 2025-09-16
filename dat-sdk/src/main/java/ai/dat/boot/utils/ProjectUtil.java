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
import ai.dat.storer.weaviate.duckdb.DuckDBEmbeddingStoreFactory;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    public final static String STORE_FILE_PREFIX = "embeddings_";

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
        EmbeddingModelFactory embeddingModelFactory = EmbeddingModelFactoryManager
                .getFactory(embedding.getProvider());
        Map<String, String> embeddingModelFingerprintConfigs = embeddingModelFactory
                .fingerprintConfigs(embedding.getConfiguration());
        EmbeddingStoreFactory embeddingStoreFactory = EmbeddingStoreFactoryManager
                .getFactory(embeddingStore.getProvider());
        Map<String, String> embeddingStoreFingerprintConfigs = embeddingStoreFactory
                .fingerprintConfigs(embeddingStore.getConfiguration());
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
            return DigestUtils.md5Hex(configStr);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Calculate the content store fingerprint failed", e);
        }
    }

    public static ContentStore createContentStore(@NonNull DatProject project) {
        return createContentStore(project, null);
    }

    public static ContentStore createContentStore(@NonNull Path projectPath) {
        DatProject project = loadProject(projectPath);
        return createContentStore(project, projectPath);
    }

    public static ContentStore createContentStore(@NonNull DatProject project, Path projectPath) {
        EmbeddingStoreConfig embeddingStore = project.getEmbeddingStore();

        if (projectPath != null
                && DuckDBEmbeddingStoreFactory.IDENTIFIER.equals(embeddingStore.getProvider())
                && embeddingStore.getConfiguration().getOptional(DuckDBEmbeddingStoreFactory.FILE_PATH).isEmpty()) {
            Path datDirPath = projectPath.resolve(DAT_DIR_NAME);
            if (!Files.exists(datDirPath)) {
                try {
                    Files.createDirectories(datDirPath);
                } catch (IOException e) {
                    throw new RuntimeException(
                            "The creation of the .dat directory under the project root directory failed", e);
                }
            }
            String storeFileName = STORE_FILE_PREFIX + contentStoreFingerprint(project);
            Path filePath = projectPath.resolve(DAT_DIR_NAME + File.separator + storeFileName);
            embeddingStore.setConfiguration(Map.of("file-path", filePath.toAbsolutePath().toString()));
        }

        FactoryDescriptor contentStoreFactoryDescriptor = FactoryDescriptor.from(
                project.getContentStore().getProvider(), project.getContentStore().getConfiguration());
        FactoryDescriptor embeddingFactoryDescriptor = FactoryDescriptor.from(
                project.getEmbedding().getProvider(), project.getEmbedding().getConfiguration());
        FactoryDescriptor embeddingStoreFactoryDescriptor = FactoryDescriptor.from(
                embeddingStore.getProvider(), embeddingStore.getConfiguration());

        Map<String, FactoryDescriptor> chatModelFactoryDescriptors = project.getLlms().stream()
                .collect(Collectors.toMap(LlmConfig::getName,
                        o -> FactoryDescriptor.from(o.getProvider(), o.getConfiguration())));

        return FactoryUtil.createContentStore(project.getName(),
                contentStoreFactoryDescriptor, embeddingFactoryDescriptor,
                embeddingStoreFactoryDescriptor, chatModelFactoryDescriptors);
    }

    public static AskdataAgent createAskdataAgent(@NonNull DatProject project,
                                                  @NonNull String agentName,
                                                  @NonNull List<SemanticModel> semanticModels) {
        return createAskdataAgent(project, agentName, semanticModels, null);
    }

    public static AskdataAgent createAskdataAgent(@NonNull Path projectPath,
                                                  @NonNull String agentName,
                                                  @NonNull List<SemanticModel> semanticModels) {
        DatProject project = loadProject(projectPath);
        return createAskdataAgent(project, agentName, semanticModels, projectPath);
    }

    public static AskdataAgent createAskdataAgent(@NonNull DatProject project,
                                                  @NonNull String agentName,
                                                  @NonNull List<SemanticModel> semanticModels,
                                                  Path projectPath) {
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
        FactoryDescriptor databaseAdapterFactoryDescriptor = FactoryDescriptor.from(
                project.getDb().getProvider(), project.getDb().getConfiguration());

        return FactoryUtil.createAskdataAgent(agentFactoryDescriptor,
                agentSemanticModels, createContentStore(project, projectPath),
                chatModelFactoryDescriptors, databaseAdapterFactoryDescriptor);
    }

    public static DatabaseAdapter createDatabaseAdapter(@NonNull DatProject project) {
        return FactoryUtil.createDatabaseAdapter(FactoryDescriptor.from(
                project.getDb().getProvider(), project.getDb().getConfiguration()));
    }

    private static void validateAgents(List<AgentConfig> agents, List<SemanticModel> semanticModels) {
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
            StringBuffer sb = new StringBuffer();
            missingNames.forEach((agentName, missings) ->
                    sb.append(String.format("There are non-existent semantic models %s in the agent '%s'",
                            missings.stream().map(n -> String.format("'%s'", n))
                                    .collect(Collectors.joining(", ")),
                            agentName
                    )).append("\n"));
            throw new ValidationException(sb.toString());
        }
    }

    public static DatProject loadProject(Path projectPath) {
        try {
            return DatProjectUtil.datProject(getProjectConfig(projectPath));
        } catch (IOException e) {
            throw new RuntimeException("The project YAML file content does not meet the requirements", e);
        }
    }

    private static String getProjectConfig(Path projectPath) throws IOException {
        Path filePath = findProjectConfigFile(projectPath);
        if (filePath == null) {
            throw new RuntimeException("The project configuration file not found "
                    + PROJECT_CONFIG_FILE_NAME_YAML + " or " + PROJECT_CONFIG_FILE_NAME_YML
                    + ", please ensure that the project configuration file exists in the project root directory.");
        }
        return Files.readString(filePath);
    }

    private static Path findProjectConfigFile(Path projectPath) {
        Path projectYaml = projectPath.resolve(PROJECT_CONFIG_FILE_NAME_YAML);
        Path projectYml = projectPath.resolve(PROJECT_CONFIG_FILE_NAME_YML);
        if (Files.exists(projectYaml)) {
            return projectYaml;
        } else if (Files.exists(projectYml)) {
            return projectYml;
        }
        return null;
    }

    public static Map<Path, DatSchema> loadAllSchema(Path dirPath) {
        Map<Path, DatSchema> schemas = scanYamlFiles(dirPath).stream()
                .collect(Collectors.toMap(p -> p, p -> loadSchema(p, dirPath)));
        validateYamlFiles(dirPath, schemas);
        return schemas;
    }

    private static DatSchema loadSchema(Path filePath, Path dirPath) {
        try {
            String content = Files.readString(filePath);
            return DatSchemaUtil.datSchema(content);
        } catch (Exception e) {
            throw new RuntimeException("The " + dirPath.relativize(filePath)
                    + " YAML file content does not meet the requirements: \n" + e.getMessage(), e);
        }
    }

    private static void validateYamlFiles(Path dirPath, Map<Path, DatSchema> schemas) {
        // 校验语义模型名称是否重复
        Map<String, List<Path>> nameToPaths = schemas.entrySet().stream()
                .flatMap(entry -> entry.getValue().getSemanticModels().stream()
                        .map(model -> Map.entry(model.getName(), entry.getKey())))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
        Map<String, List<Path>> duplicates = nameToPaths.entrySet().stream()
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
    }

    public static Map<Path, DatModel> loadAllModel(Path modelsPath) {
        Map<Path, DatModel> models = scanSqlFiles(modelsPath).stream()
                .collect(Collectors.toMap(p -> p, p -> loadModel(p, modelsPath)));
        validateModelFiles(modelsPath, models);
        return models;
    }

    private static DatModel loadModel(Path filePath, Path modelsPath) {
        try {
            String content = Files.readString(filePath);
            String name = fileNamePrefix(filePath.getFileName().toString());
            return DatModel.from(name, content);
        } catch (Exception e) {
            throw new RuntimeException("The " + modelsPath.relativize(filePath)
                    + " SQL file content does not meet the requirements: \n" + e.getMessage(), e);
        }
    }

    private static void validateModelFiles(Path modelsPath, Map<Path, DatModel> models) {
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

    public static Map<Path, DatSeed> loadAllSeed(Path seedsPath) {
        Map<Path, DatSeed> seeds = scanCsvFiles(seedsPath).stream()
                .collect(Collectors.toMap(p -> p, p -> loadSeed(p, seedsPath)));
        validateSeedFiles(seedsPath, seeds);
        return seeds;
    }

    private static DatSeed loadSeed(Path filePath, Path seedsPath) {
        try {
            String content = Files.readString(filePath);
            String name = fileNamePrefix(filePath.getFileName().toString());
            return DatSeed.from(name, content);
        } catch (Exception e) {
            throw new RuntimeException("The " + seedsPath.relativize(filePath)
                    + " CSV file content does not meet the requirements: \n" + e.getMessage(), e);
        }
    }

    private static void validateSeedFiles(Path seedsPath, Map<Path, DatSeed> seeds) {
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

    private static String fileNamePrefix(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(0, lastDotIndex) : fileName;
    }

    private static List<Path> scanYamlFiles(Path dirPath) {
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

    private static boolean isYamlFile(String fileName) {
        return YAML_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    private static List<Path> scanSqlFiles(Path modelsPath) {
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

    private static boolean isSqlFile(String fileName) {
        return SQL_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    private static List<Path> scanCsvFiles(Path seedsPath) {
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

    private static boolean isCsvFile(String fileName) {
        return CSV_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }
}