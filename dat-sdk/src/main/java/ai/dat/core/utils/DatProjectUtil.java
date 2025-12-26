package ai.dat.core.utils;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.data.project.*;
import ai.dat.core.exception.ValidationException;
import ai.dat.core.factories.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.base.Preconditions;
import com.networknt.schema.Error;
import com.networknt.schema.*;
import lombok.Getter;
import lombok.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * DAT项目配置工具类
 *
 * @Author JunjieM
 * @Date 2025/1/16
 */
public class DatProjectUtil {

    private static final YAMLMapper YAML_MAPPER = new YAMLMapper();
    private static final JsonMapper JSON_MAPPER = new JsonMapper();

    private static final SchemaRegistryConfig SCHEMA_CONFIG =
            SchemaRegistryConfig.builder().locale(Locale.ENGLISH).build();
    private static final SchemaRegistry SCHEMA_REGISTRY = SchemaRegistry
            .withDefaultDialect(SpecificationVersion.DRAFT_2020_12,
                    builder -> builder.schemaRegistryConfig(SCHEMA_CONFIG));
    private static final String SCHEMA_PATH = "schemas/project_schema.json";
    private static final String TEXT_PATH = "templates/project_yaml_template.jinja";

    private static final String LLM_NAME_PREFIX = "llm_";
    private static final String AGENT_NAME_PREFIX = "agent_";

    private static final Schema SCHEMA;
    private static final String TEMPLATE;

    static {
        try {
            SCHEMA = loadProjectSchema();
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to load project schema file: " + e.getMessage());
        }
        TEMPLATE = loadText();
    }

    private static Schema loadProjectSchema() throws IOException {
        try (InputStream stream = DatProjectUtil.class.getClassLoader().getResourceAsStream(SCHEMA_PATH)) {
            if (stream == null) {
                throw new IOException("Project schema file not found in classpath: " + SCHEMA_PATH);
            }
            try {
                JsonNode schemaNode = JSON_MAPPER.readTree(stream);
                return SCHEMA_REGISTRY.getSchema(schemaNode);
            } catch (IOException e) {
                throw new IOException("Failed to parse project schema file: " + SCHEMA_PATH
                                      + " - " + e.getMessage(), e);
            }
        }
    }

    private static String loadText() {
        try (InputStream inputStream = DatProjectUtil.class.getClassLoader().getResourceAsStream(TEXT_PATH)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load text from resources: " + TEXT_PATH, e);
        }
    }

    public static final ConfigOption<Boolean> BUILDING_VERIFY_MDL_DIMENSIONS_ENUM_VALUES =
            ConfigOptions.key("building.verify-mdl-dimensions-enum-values")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription("Whether to verify the enumeration values of dimensions " +
                                     "in the semantic model during building");

    public static final ConfigOption<Boolean> BUILDING_VERIFY_MDL_DATA_TYPES =
            ConfigOptions.key("building.verify-mdl-data-types")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription("Whether to verify the data types of " +
                                     "entities, dimensions, measures in the semantic model during building");

    public static final ConfigOption<Boolean> BUILDING_AUTO_COMPLETE_MDL_DATA_TYPES =
            ConfigOptions.key("building.auto-complete-mdl-data-types")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription("Whether to automatically complete the data types of " +
                                     "entities, dimensions, measures in the semantic model during building");

    private DatProjectUtil() {
    }

    public static List<Error> validate(@NonNull String yamlContent) throws IOException {
        Preconditions.checkArgument(!yamlContent.isEmpty(), "yamlContent cannot be empty");
        try {
            JsonNode jsonNode = YAML_MAPPER.readTree(yamlContent);
            return SCHEMA.validate(jsonNode);
        } catch (IOException e) {
            throw new IOException("Failed to parse YAML content: " + e.getMessage(), e);
        }
    }

    public static DatProject datProject(@NonNull String yamlContent) throws IOException {
        List<Error> errors = DatProjectUtil.validate(yamlContent);
        if (!errors.isEmpty()) {
            throw new ValidationException("The YAML verification not pass: \n" + errors);
        }
        return YAML_MAPPER.readValue(yamlContent, DatProject.class);
    }

    public static Set<ConfigOption<?>> projectRequiredOptions() {
        return Collections.emptySet();
    }

    public static Set<ConfigOption<?>> projectOptionalOptions() {
        return new LinkedHashSet<>(List.of(
                BUILDING_VERIFY_MDL_DIMENSIONS_ENUM_VALUES,
                BUILDING_VERIFY_MDL_DATA_TYPES,
                BUILDING_AUTO_COMPLETE_MDL_DATA_TYPES
        ));
    }

    public static Set<ConfigOption<?>> fingerprintOptions() {
        return Set.of(BUILDING_AUTO_COMPLETE_MDL_DATA_TYPES);
    }

    public static Map<String, String> projectFingerprintConfigs(@NonNull ReadableConfig config) {
        List<String> keys = fingerprintOptions().stream()
                .map(ConfigOption::key)
                .toList();
        return config.toMap().entrySet().stream()
                .filter(e -> keys.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static String yamlTemplate() {
        List<SingleItemTemplate> dbs = DatabaseAdapterFactoryManager.getSupports().stream()
                .map(identifier -> {
                    DatabaseAdapterFactory factory = DatabaseAdapterFactoryManager.getFactory(identifier);
                    boolean display = DatabaseConfig.DEFAULT_PROVIDER.equals(identifier);
                    return new SingleItemTemplate(identifier, display, getConfiguration(factory));
                })
                .sorted((o1, o2) -> Boolean.compare(o2.display, o1.display))
                .collect(Collectors.toList());

        AtomicInteger llmNameAtomic = new AtomicInteger(1);
        List<MultipleItemTemplate> llms = ChatModelFactoryManager.getSupports().stream()
                .map(identifier -> {
                    ChatModelFactory factory = ChatModelFactoryManager.getFactory(identifier);
                    boolean display = LlmConfig.DEFAULT_PROVIDER.equals(identifier);
                    String name = display ? LlmConfig.DEFAULT_NAME : LLM_NAME_PREFIX + (llmNameAtomic.getAndIncrement());
                    return new MultipleItemTemplate(name, identifier, display, getConfiguration(factory));
                })
                .sorted((o1, o2) -> Boolean.compare(o2.display, o1.display))
                .collect(Collectors.toList());

        List<SingleItemTemplate> embeddings = EmbeddingModelFactoryManager.getSupports().stream()
                .map(identifier -> {
                    EmbeddingModelFactory factory = EmbeddingModelFactoryManager.getFactory(identifier);
                    boolean display = EmbeddingConfig.DEFAULT_PROVIDER.equals(identifier);
                    return new SingleItemTemplate(identifier, display, getConfiguration(factory));
                })
                .sorted((o1, o2) -> Boolean.compare(o2.display, o1.display))
                .collect(Collectors.toList());

        List<SingleItemTemplate> embeddingStores = EmbeddingStoreFactoryManager.getSupports().stream()
                .map(identifier -> {
                    EmbeddingStoreFactory factory = EmbeddingStoreFactoryManager.getFactory(identifier);
                    boolean display = EmbeddingStoreConfig.DEFAULT_PROVIDER.equals(identifier);
                    return new SingleItemTemplate(identifier, display, getConfiguration(factory));
                })
                .sorted((o1, o2) -> Boolean.compare(o2.display, o1.display))
                .collect(Collectors.toList());

        List<SingleItemTemplate> rerankings = ScoringModelFactoryManager.getSupports().stream()
                .map(identifier -> {
                    ScoringModelFactory factory = ScoringModelFactoryManager.getFactory(identifier);
                    boolean display = RerankingConfig.DEFAULT_PROVIDER.equals(identifier);
                    return new SingleItemTemplate(identifier, display, getConfiguration(factory));
                })
                .sorted((o1, o2) -> Boolean.compare(o2.display, o1.display))
                .collect(Collectors.toList());

        List<SingleItemTemplate> contentStores = ContentStoreFactoryManager.getSupports().stream()
                .map(identifier -> {
                    ContentStoreFactory factory = ContentStoreFactoryManager.getFactory(identifier);
                    boolean display = ContentStoreConfig.DEFAULT_PROVIDER.equals(identifier);
                    return new SingleItemTemplate(identifier, display, getConfiguration(factory));
                })
                .sorted((o1, o2) -> Boolean.compare(o2.display, o1.display))
                .collect(Collectors.toList());

        AtomicInteger agentNameAtomic = new AtomicInteger(1);
        List<MultipleItemContainCommentTemplate> agents = AskdataAgentFactoryManager.getSupports().stream()
                .map(identifier -> {
                    AskdataAgentFactory factory = AskdataAgentFactoryManager.getFactory(identifier);
                    boolean display = AgentConfig.DEFAULT_PROVIDER.equals(identifier);
                    String name = display ? AgentConfig.DEFAULT_NAME : AGENT_NAME_PREFIX + (agentNameAtomic.getAndIncrement());
                    return new MultipleItemContainCommentTemplate(factory.factoryDescription(), name,
                            identifier, display, getConfiguration(factory));
                })
                .sorted((o1, o2) -> Boolean.compare(o2.display, o1.display))
                .collect(Collectors.toList());

        Map<String, Object> variables = new HashMap<>();
        variables.put("project_configuration", getProjectConfiguration());
        variables.put("dbs", dbs);
        variables.put("llms", llms);
        variables.put("embeddings", embeddings);
        variables.put("rerankings", rerankings);
        variables.put("embedding_stores", embeddingStores);
        variables.put("content_stores", contentStores);
        variables.put("agents", agents);

        return JinjaTemplateUtil.render(TEMPLATE, variables);
    }

    public static String getProjectConfiguration() {
        return YamlTemplateUtil.getConfiguration(projectRequiredOptions(), projectOptionalOptions());
    }

    public static String getDefaultAgentConfiguration() {
        return getConfiguration(new DefaultAskdataAgentFactory());
    }

    private static String getConfiguration(Factory factory) {
        return YamlTemplateUtil.getConfiguration(factory);
    }

    private record SingleItemTemplate(@Getter String provider, @Getter boolean display,
                                      @Getter String configuration) {
    }

    private record MultipleItemTemplate(@Getter String name, @Getter String provider, @Getter boolean display,
                                        @Getter String configuration) {
    }

    private record MultipleItemContainCommentTemplate(@Getter String comment, @Getter String name,
                                                      @Getter String provider, @Getter boolean display,
                                                      @Getter String configuration) {
    }
}