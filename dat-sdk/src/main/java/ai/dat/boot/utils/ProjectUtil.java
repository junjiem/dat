package ai.dat.boot.utils;

import ai.dat.core.adapter.DatabaseAdapter;
import ai.dat.core.agent.AskdataAgent;
import ai.dat.core.contentstore.ContentStore;
import ai.dat.core.data.DatModel;
import ai.dat.core.data.DatSchema;
import ai.dat.core.data.project.*;
import ai.dat.core.exception.ValidationException;
import ai.dat.core.factories.*;
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

    public final static String MODELS_DIR_NAME = "models";
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
        Map<String, LlmConfig> llmMap = project.getLlms().stream()
                .collect(Collectors.toMap(LlmConfig::getName, o -> o));

        String llmName = project.getContentStore().getLlm();
        Preconditions.checkArgument(llmMap.containsKey(llmName),
                "The project doesn't exist llm '%s', modify the llm in the content store", llmName);
        LlmConfig llmConfig = llmMap.get(llmName);

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

        return FactoryUtil.createContentStore(project.getName(),
                project.getContentStore().getProvider(), project.getContentStore().getConfiguration(),
                project.getEmbedding().getProvider(), project.getEmbedding().getConfiguration(),
                embeddingStore.getProvider(), embeddingStore.getConfiguration(),
                llmConfig.getProvider(), llmConfig.getConfiguration());
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
        String llmName = agentConfig.getLlm();
        List<String> semanticModelNames = agentConfig.getSemanticModels();

        Map<String, LlmConfig> llmMap = project.getLlms().stream()
                .collect(Collectors.toMap(LlmConfig::getName, o -> o));
        Preconditions.checkArgument(llmMap.containsKey(llmName),
                "The project doesn't exist llm '%s', modify the llm in the agent '%s'",
                llmName, agentName);
        LlmConfig llmConfig = llmMap.get(llmName);

        List<SemanticModel> agentSemanticModels = semanticModels.stream()
                .filter(model -> semanticModelNames.contains(model.getName()))
                .collect(Collectors.toList());

        return FactoryUtil.createAskdataAgent(
                agentConfig.getProvider(), agentConfig.getConfiguration(),
                agentSemanticModels, createContentStore(project, projectPath),
                llmConfig.getProvider(), llmConfig.getConfiguration(),
                project.getDb().getProvider(), project.getDb().getConfiguration());
    }

    public static DatabaseAdapter createDatabaseAdapter(@NonNull DatProject project) {
        return FactoryUtil.createDatabaseAdapter(
                project.getDb().getProvider(), project.getDb().getConfiguration());
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
            missingNames.forEach((agentName, missings) -> {
                sb.append(String.format("There are non-existent semantic models %s in the agent '%s'",
                        missings.stream().map(n -> String.format("'%s'", n))
                                .collect(Collectors.joining(", ")),
                        agentName
                )).append("\n");
            });
            throw new ValidationException(sb.toString());
        }
    }

    public static DatProject loadProject(Path projectPath) {
        try {
            return DatProjectUtil.datProject(getProjectConfig(projectPath));
        } catch (IOException e) {
            throw new RuntimeException("The read project configuration file last modified time failed", e);
        }
    }

    public static String getProjectConfig(Path projectPath) {
        Path path = findProjectConfigFile(projectPath);
        if (path == null) {
            throw new RuntimeException("The project configuration file not found "
                    + PROJECT_CONFIG_FILE_NAME_YAML + " or " + PROJECT_CONFIG_FILE_NAME_YML
                    + ", please ensure that the project configuration file exists in the project root directory.");
        }
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException("The read project configuration file last modified time failed", e);
        }
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

    public static Map<Path, DatSchema> loadAllSchema(Path modelsPath) {
        List<Path> yamlFiles = scanYamlFiles(modelsPath);
        return yamlFiles.stream().collect(Collectors.toMap(p -> p,
                p -> ProjectUtil.loadSchema(p, modelsPath)));
    }

    private static DatSchema loadSchema(Path filePath, Path modelsPath) {
        try {
            String content = Files.readString(filePath);
            return DatSchemaUtil.datSchema(content);
        } catch (Exception e) {
            throw new RuntimeException("The " + modelsPath.relativize(filePath)
                    + " YAML file content does not meet the requirements", e);
        }
    }

    public static Map<Path, DatModel> loadAllModel(Path modelsPath) {
        List<Path> sqlFiles = scanSqlFiles(modelsPath);
        return sqlFiles.stream().collect(Collectors.toMap(p -> p,
                p -> ProjectUtil.loadModel(p, modelsPath)));
    }

    private static DatModel loadModel(Path filePath, Path modelsPath) {
        try {
            String content = Files.readString(filePath);
            String modelName = fileNamePrefix(filePath.getFileName().toString());
            return DatModel.from(modelName, content);
        } catch (Exception e) {
            throw new RuntimeException("The " + modelsPath.relativize(filePath)
                    + " SQL file content does not meet the requirements", e);
        }
    }

    private static String fileNamePrefix(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(0, lastDotIndex) : fileName;
    }

    private static List<Path> scanYamlFiles(Path modelsPath) {
        List<Path> files = new ArrayList<>();
        Preconditions.checkArgument(Files.exists(modelsPath),
                "There is no 'models' directory in the project root directory");
        try {
            Files.walkFileTree(modelsPath, new SimpleFileVisitor<>() {
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
}