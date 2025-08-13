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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.ValidationMessage;
import jinjava.org.jsoup.helper.ValidationException;
import lombok.Getter;
import lombok.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

    private static final String DEFAULT_NAME = "default";
    private static final String LLM_NAME_PREFIX = "llm_";
    private static final String AGENT_NAME_PREFIX = "agent_";

    private static final String PROJECT_YAML_TEMPLATE;

    static {
        YAML_MAPPER
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER) // 禁用 ---
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES) // 最小化引号
                .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE) // 多行字符串用 |
                .enable(YAMLGenerator.Feature.INDENT_ARRAYS) // 美化数组缩进
                .setSerializationInclusion(JsonInclude.Include.NON_NULL) // 忽略 null 字段
        ;
        PROJECT_YAML_TEMPLATE = loadText("templates/project_yaml_template.jinja");
    }

    private static String loadText(String fromResource) {
        try (InputStream inputStream = DatProjectFactory.class.getClassLoader()
                .getResourceAsStream(fromResource)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load text from resources: " + fromResource, e);
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
                    return new SingleItemTemplate(identifier, display, getConfiguration(factory));
                })
                .sorted((o1, o2) -> Boolean.compare(o2.display, o1.display))
                .collect(Collectors.toList());

        AtomicInteger llmNameAtomic = new AtomicInteger(1);
        List<MultipleItemTemplate> llms = ChatModelFactoryManager.getSupports().stream()
                .map(identifier -> {
                    ChatModelFactory factory = ChatModelFactoryManager.getFactory(identifier);
                    boolean display = OpneAiChatModelFactory.IDENTIFIER.equals(identifier);
                    String name = display ? DEFAULT_NAME : LLM_NAME_PREFIX + (llmNameAtomic.getAndIncrement());
                    return new MultipleItemTemplate(name, identifier, display, getConfiguration(factory));
                })
                .sorted((o1, o2) -> Boolean.compare(o2.display, o1.display))
                .collect(Collectors.toList());

        List<SingleItemTemplate> embeddings = EmbeddingModelFactoryManager.getSupports().stream()
                .map(identifier -> {
                    EmbeddingModelFactory factory = EmbeddingModelFactoryManager.getFactory(identifier);
                    boolean display = BgeSmallZhV15QuantizedEmbeddingModelFactory.IDENTIFIER.equals(identifier);
                    return new SingleItemTemplate(identifier, display, getConfiguration(factory));
                })
                .sorted((o1, o2) -> Boolean.compare(o2.display, o1.display))
                .collect(Collectors.toList());

        List<SingleItemTemplate> embeddingStores = EmbeddingStoreFactoryManager.getSupports().stream()
                .filter(identifier -> !InMemoryEmbeddingStoreFactory.IDENTIFIER.equals(identifier))
                .map(identifier -> {
                    EmbeddingStoreFactory factory = EmbeddingStoreFactoryManager.getFactory(identifier);
                    boolean display = DuckDBEmbeddingStoreFactory.IDENTIFIER.equals(identifier);
                    return new SingleItemTemplate(identifier, display, getConfiguration(factory));
                })
                .sorted((o1, o2) -> Boolean.compare(o2.display, o1.display))
                .collect(Collectors.toList());

        List<SingleItemTemplate> contentStores = ContentStoreFactoryManager.getSupports().stream()
                .map(identifier -> {
                    ContentStoreFactory factory = ContentStoreFactoryManager.getFactory(identifier);
                    boolean display = DefaultContentStoreFactory.IDENTIFIER.equals(identifier);
                    return new SingleItemTemplate(identifier, display, getConfiguration(factory));
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
                            identifier, display, getConfiguration(factory));
                })
                .sorted((o1, o2) -> Boolean.compare(o2.display, o1.display))
                .collect(Collectors.toList());

        Map<String, Object> variables = new HashMap<>();
        variables.put("project_configuration", getProjectConfiguration());
        variables.put("dbs", dbs);
        variables.put("llms", llms);
        variables.put("embeddings", embeddings);
        variables.put("embedding_stores", embeddingStores);
        variables.put("content_stores", contentStores);
        variables.put("agents", agents);

        return JinjaTemplateUtil.render(PROJECT_YAML_TEMPLATE, variables);
    }

    public String getProjectConfiguration() {
        return configurationTemplate(projectConfigTemplates());
    }

    private List<ConfigTemplate> projectConfigTemplates() {
        return configTemplates(projectRequiredOptions(), projectOptionalOptions());
    }

    public String getDefaultAgentConfiguration() {
        return configurationTemplate(defaultAgentConfigTemplates());
    }

    private List<ConfigTemplate> defaultAgentConfigTemplates() {
        return configTemplates(new DefaultAskdataAgentFactory());
    }

    private String getConfiguration(Factory factory) {
        return configurationTemplate(configTemplates(factory));
    }

    private String configurationTemplate(List<ConfigTemplate> configs) {
        StringBuilder sb = new StringBuilder();
        for (ConfigTemplate config : configs) {
            String description = config.getDescription();
            boolean multiLineDescription = description.contains("\n");
            if (multiLineDescription) {
                sb.append("## ------------------------------\n");
                for (String str : description.split("\n")) {
                    sb.append("## ").append(str).append("\n");
                }
                sb.append("## ------------------------------\n");
            }
            if (!config.isRequired()) {
                sb.append("#");
            }
            sb.append(config.getKey()).append(": ");
            String value = config.getValue();
            if (value != null) {
                if (value.contains("\n")) {
                    sb.append("\n");
                    for (String str : value.split("\n")) {
                        if (!config.isRequired()) {
                            sb.append("#");
                        }
                        sb.append("  ").append(str).append("\n");
                    }
                } else {
                    sb.append(value);
                }
            }
            if (!multiLineDescription) {
                sb.append("  # ").append(description);
            }
            sb.append("\n");
        }
        return sb.toString().trim();
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

    private String toValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Duration val) {
            return TimeUtils.formatWithHighestUnit(val);
        } else {
            try {
                return YAML_MAPPER.writeValueAsString(value).trim();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String toDescription(boolean required, ConfigOption<?> configOption) {
        String description = new HtmlFormatter().format(configOption.description());
        String defaultValueDescription = "";
        if (configOption.hasDefaultValue()) {
            String defaultValue = toValue(configOption.defaultValue());
            if (defaultValue.contains("\n")) {
                defaultValue = "\n```\n" + defaultValue + "\n```";
            }
            defaultValueDescription = ", Default: " + defaultValue;
        }
        String prefix = "("
                + configOption.getClazz().getSimpleName() + ", "
                + (required ? "[Required]" : "[Optional]")
                + defaultValueDescription
                + ")";
        if (description.contains("\n")) {
            return prefix + "\n\n" + description;
        }
        return prefix + " " + description;
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

    private record ConfigTemplate(@Getter boolean required, @Getter String key, @Getter String value,
                                  @Getter String description) {
    }

}
