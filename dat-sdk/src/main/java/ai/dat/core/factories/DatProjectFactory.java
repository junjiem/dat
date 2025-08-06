package ai.dat.core.factories;

import ai.dat.adapter.postgresql.PostgreSqlDatabaseAdapterFactory;
import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.configuration.description.HtmlFormatter;
import ai.dat.core.configuration.time.TimeUtils;
import ai.dat.core.data.project.DatProject;
import ai.dat.core.utils.DatProjectUtil;
import ai.dat.core.utils.JinjaTemplateUtil;
import ai.dat.embedder.inprocess.BgeSmallZhV15QuantizedEmbeddingModelFactory;
import ai.dat.llm.openai.OpneAiChatModelFactory;
import ai.dat.storer.weaviate.duckdb.DuckDBEmbeddingStoreFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.ValidationMessage;
import jinjava.org.jsoup.helper.ValidationException;
import lombok.Getter;
import lombok.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @Author JunjieM
 * @Date 2025/8/7
 */
public class DatProjectFactory {

    private static final YAMLMapper YAML_MAPPER = new YAMLMapper();

    private static final String TEMPLATE_PATH = "templates/project_yaml_template.jinja";
    private static final String DEFAULT_NAME = "default";
    private static final String LLM_NAME_PREFIX = "llm_";
    private static final String AGENT_NAME_PREFIX = "agent_";

    private static final String TEMPLATE_CONTENT;

    static {
        try {
            TEMPLATE_CONTENT = loadProjectTemplate();
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to load project YAML template file: " + e.getMessage());
        }
    }

    private static String loadProjectTemplate() throws IOException {
        try (InputStream stream = DatProjectUtil.class.getClassLoader().getResourceAsStream(TEMPLATE_PATH)) {
            if (stream == null) {
                throw new IOException("Project YAML template file not found in classpath: " + TEMPLATE_PATH);
            }
            return new String(stream.readAllBytes());
        }
    }

    public static final ConfigOption<Boolean> BUILDING_VERIFY_MDL_DIMENSIONS_ENUM_VALUES =
            ConfigOptions.key("building.verify-mdl-dimensions-enum-values")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether to verify the enumeration values of dimensions " +
                            "in the semantic model during building");

    public static final ConfigOption<Boolean> BUILDING_VERIFY_MDL_DATA_TYPES =
            ConfigOptions.key("building.verify-mdl-data-types")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether to verify the data types of " +
                            "entities, dimensions, measures in the semantic model during building");

    public static final ConfigOption<Boolean> BUILDING_AUTO_COMPLETE_MDL_DATA_TYPES =
            ConfigOptions.key("building.auto-complete-mdl-data-types")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription("Whether to automatically complete the data types of " +
                            "entities, dimensions, measures in the semantic model during building");

    public Set<ConfigOption<?>> projectRequiredOptions() {
        return Collections.emptySet();
    }

    public Set<ConfigOption<?>> projectOptionalOptions() {
        return new LinkedHashSet<>(List.of(
                BUILDING_VERIFY_MDL_DIMENSIONS_ENUM_VALUES,
                BUILDING_VERIFY_MDL_DATA_TYPES,
                BUILDING_AUTO_COMPLETE_MDL_DATA_TYPES
        ));
    }

    public Set<ConfigOption<?>> fingerprintOptions() {
        return Set.of(BUILDING_AUTO_COMPLETE_MDL_DATA_TYPES);
    }

    public Map<String, String> projectFingerprintConfigs(@NonNull ReadableConfig config) {
        List<String> keys = fingerprintOptions().stream()
                .map(ConfigOption::key)
                .toList();
        return config.toMap().entrySet().stream()
                .filter(e -> keys.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public DatProject create(@NonNull String yamlContent) throws IOException {
        Set<ValidationMessage> validationErrors = DatProjectUtil.validate(yamlContent);
        if (!validationErrors.isEmpty()) {
            throw new ValidationException("The YAML verification not pass: \n" + validationErrors);
        }
        return YAML_MAPPER.readValue(yamlContent, DatProject.class);
    }

    public String yamlTemplate() {
        List<SingleItemTemplate> dbs = DatabaseAdapterFactoryManager.getSupports().stream()
                .map(identifier -> {
                    DatabaseAdapterFactory factory = DatabaseAdapterFactoryManager.getFactory(identifier);
                    boolean display = PostgreSqlDatabaseAdapterFactory.IDENTIFIER.equals(identifier);
                    return new SingleItemTemplate(identifier, display, configTemplates(factory));
                })
                .sorted((o1, o2) -> Boolean.compare(o2.display, o1.display))
                .collect(Collectors.toList());

        AtomicInteger llmNameAtomic = new AtomicInteger(1);
        List<MultipleItemTemplate> llms = ChatModelFactoryManager.getSupports().stream()
                .map(identifier -> {
                    ChatModelFactory factory = ChatModelFactoryManager.getFactory(identifier);
                    boolean display = OpneAiChatModelFactory.IDENTIFIER.equals(identifier);
                    String name = display ? DEFAULT_NAME : LLM_NAME_PREFIX + (llmNameAtomic.getAndIncrement());
                    return new MultipleItemTemplate(name, identifier, display, configTemplates(factory));
                })
                .sorted((o1, o2) -> Boolean.compare(o2.display, o1.display))
                .collect(Collectors.toList());

        List<SingleItemTemplate> embeddings = EmbeddingModelFactoryManager.getSupports().stream()
                .map(identifier -> {
                    EmbeddingModelFactory factory = EmbeddingModelFactoryManager.getFactory(identifier);
                    boolean display = BgeSmallZhV15QuantizedEmbeddingModelFactory.IDENTIFIER.equals(identifier);
                    return new SingleItemTemplate(identifier, display, configTemplates(factory));
                })
                .sorted((o1, o2) -> Boolean.compare(o2.display, o1.display))
                .collect(Collectors.toList());

        List<SingleItemTemplate> embeddingStores = EmbeddingStoreFactoryManager.getSupports().stream()
                .filter(identifier -> !InMemoryEmbeddingStoreFactory.IDENTIFIER.equals(identifier))
                .map(identifier -> {
                    EmbeddingStoreFactory factory = EmbeddingStoreFactoryManager.getFactory(identifier);
                    boolean display = DuckDBEmbeddingStoreFactory.IDENTIFIER.equals(identifier);
                    return new SingleItemTemplate(identifier, display, configTemplates(factory));
                })
                .sorted((o1, o2) -> Boolean.compare(o2.display, o1.display))
                .collect(Collectors.toList());

        List<SingleItemTemplate> contentStores = ContentStoreFactoryManager.getSupports().stream()
                .map(identifier -> {
                    ContentStoreFactory factory = ContentStoreFactoryManager.getFactory(identifier);
                    boolean display = DefaultContentStoreFactory.IDENTIFIER.equals(identifier);
                    return new SingleItemTemplate(identifier, display, configTemplates(factory));
                })
                .sorted((o1, o2) -> Boolean.compare(o2.display, o1.display))
                .collect(Collectors.toList());

        AtomicInteger agentNameAtomic = new AtomicInteger(1);
        List<MultipleItemContainCommentTemplate> agents = AskdataAgentFactoryManager.getSupports().stream()
                .map(identifier -> {
                    AskdataAgentFactory factory = AskdataAgentFactoryManager.getFactory(identifier);
                    boolean display = DefaultAskdataAgentFactory.IDENTIFIER.equals(identifier);
                    String name = display ? DEFAULT_NAME : AGENT_NAME_PREFIX + (agentNameAtomic.getAndIncrement());
                    return new MultipleItemContainCommentTemplate(factory.factoryDescription(), name,
                            identifier, display, configTemplates(factory));
                })
                .sorted((o1, o2) -> Boolean.compare(o2.display, o1.display))
                .collect(Collectors.toList());

        Map<String, Object> variables = new HashMap<>();
        variables.put("projectConfigs", projectConfigTemplates());
        variables.put("dbs", dbs);
        variables.put("llms", llms);
        variables.put("embeddings", embeddings);
        variables.put("embedding_stores", embeddingStores);
        variables.put("content_stores", contentStores);
        variables.put("agents", agents);

        return JinjaTemplateUtil.render(TEMPLATE_CONTENT, variables);
    }

    public List<ConfigTemplate> projectConfigTemplates() {
        DatProjectFactory factory = new DatProjectFactory();
        return configTemplates(factory.projectRequiredOptions(), factory.projectOptionalOptions());
    }

    public List<ConfigTemplate> defaultAgentConfigTemplates() {
        return configTemplates(new DefaultAskdataAgentFactory());
    }

    private List<ConfigTemplate> configTemplates(Factory factory) {
        return configTemplates(factory.requiredOptions(), factory.optionalOptions());
    }

    private List<ConfigTemplate> configTemplates(Set<ConfigOption<?>> requiredOptions,
                                                 Set<ConfigOption<?>> optionalOptions) {
        List<ConfigTemplate> configs = new ArrayList<>();
        if (requiredOptions != null) {
            configs.addAll(requiredOptions.stream()
                    .map(o -> configTemplate(true, o))
                    .toList());
        }
        if (optionalOptions != null) {
            configs.addAll(optionalOptions.stream()
                    .map(o -> configTemplate(false, o))
                    .toList());
        }
        return configs;
    }

    private ConfigTemplate configTemplate(boolean required, ConfigOption<?> configOption) {
        return new ConfigTemplate(required, configOption.key(), toValue(configOption.defaultValue()),
                toDescription(required, configOption));
    }

    private static Object toValue(Object value) {
        if (value instanceof Duration val) {
            return TimeUtils.formatWithHighestUnit(val);
        } else {
            return value;
        }
    }

    private static String toDescription(boolean required, ConfigOption<?> configOption) {
        return "("
                + configOption.getClazz().getSimpleName() + ", "
                + (required ? "[Required]" : "[Optional]")
                + (configOption.hasDefaultValue() ? ", Default: " + toValue(configOption.defaultValue()) : "")
                + ") "
                + new HtmlFormatter().format(configOption.description());
    }

    private record SingleItemTemplate(@Getter String provider, @Getter boolean display,
                                      @Getter List<ConfigTemplate> configs) {
    }

    private record MultipleItemTemplate(@Getter String name, @Getter String provider, @Getter boolean display,
                                        @Getter List<ConfigTemplate> configs) {
    }

    private record MultipleItemContainCommentTemplate(@Getter String comment, @Getter String name,
                                                      @Getter String provider, @Getter boolean display,
                                                      @Getter List<ConfigTemplate> configs) {
    }

    private record ConfigTemplate(@Getter boolean required, @Getter String key, @Getter Object value,
                                  @Getter String description) {
    }

}
