package ai.dat.core.utils;

import ai.dat.adapter.postgresql.PostgreSqlDatabaseAdapterFactory;
import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.description.HtmlFormatter;
import ai.dat.core.configuration.time.TimeUtils;
import ai.dat.core.data.project.DatProject;
import ai.dat.core.factories.*;
import ai.dat.embedder.inprocess.BgeSmallZhV15QuantizedEmbeddingModelFactory;
import ai.dat.llm.openai.OpneAiChatModelFactory;
import ai.dat.storer.weaviate.duckdb.DuckDBEmbeddingStoreFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.base.Preconditions;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
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
 * DAT项目配置工具类
 *
 * @Author JunjieM
 * @Date 2025/1/16
 */
public class DatProjectUtil {

    private static final YAMLMapper YAML_MAPPER = new YAMLMapper();
    private static final JsonSchemaFactory SCHEMA_FACTORY = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    private static final String SCHEMA_PATH = "schemas/project_schema.json";
    private static final String TEMPLATE_PATH = "templates/project_yaml_template.jinja";

    private static final String DEFAULT_NAME = "default";

    private static final JsonSchema JSON_SCHEMA;
    private static final String TEMPLATE_CONTENT;

    static {
        try {
            JSON_SCHEMA = loadProjectSchema();
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to load project schema file: " + e.getMessage());
        }
        try {
            TEMPLATE_CONTENT = loadProjectTemplate();
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to load project YAML template file: " + e.getMessage());
        }
    }

    private static JsonSchema loadProjectSchema() throws IOException {
        try (InputStream stream = DatProjectUtil.class.getClassLoader().getResourceAsStream(SCHEMA_PATH)) {
            if (stream == null) {
                throw new IOException("Project schema file not found in classpath: " + SCHEMA_PATH);
            }
            try {
                JsonNode schemaNode = new JsonMapper().readTree(stream);
                return SCHEMA_FACTORY.getSchema(schemaNode);
            } catch (IOException e) {
                throw new IOException("Failed to parse project schema file: " + SCHEMA_PATH
                        + " - " + e.getMessage(), e);
            }
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

    private DatProjectUtil() {
    }

    public static Set<ValidationMessage> validate(@NonNull String yamlContent) throws IOException {
        Preconditions.checkArgument(!yamlContent.isEmpty(), "yamlContent cannot be empty");
        try {
            JsonNode jsonNode = YAML_MAPPER.readTree(yamlContent);
            return JSON_SCHEMA.validate(jsonNode);
        } catch (IOException e) {
            throw new IOException("Failed to parse YAML content: " + e.getMessage(), e);
        }
    }

    public static DatProject datProject(@NonNull String yamlContent) throws IOException {
        Set<ValidationMessage> validationErrors = validate(yamlContent);
        if (!validationErrors.isEmpty()) {
            throw new ValidationException("The YAML verification not pass: \n" + validationErrors);
        }
        return YAML_MAPPER.readValue(yamlContent, DatProject.class);
    }

    public static String projectYamlTemplate() {
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
                    String name = display ? DEFAULT_NAME : DEFAULT_NAME + (llmNameAtomic.getAndIncrement());
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
        List<MultipleItemTemplate> agents = AskdataAgentFactoryManager.getSupports().stream()
                .map(identifier -> {
                    AskdataAgentFactory factory = AskdataAgentFactoryManager.getFactory(identifier);
                    boolean display = DefaultAskdataAgentFactory.IDENTIFIER.equals(identifier);
                    String name = display ? DEFAULT_NAME : DEFAULT_NAME + (agentNameAtomic.getAndIncrement());
                    return new MultipleItemTemplate(name, identifier, display, configTemplates(factory));
                })
                .sorted((o1, o2) -> Boolean.compare(o2.display, o1.display))
                .collect(Collectors.toList());

        Map<String, Object> variables = new HashMap<>();
        variables.put("dbs", dbs);
        variables.put("llms", llms);
        variables.put("embeddings", embeddings);
        variables.put("embedding_stores", embeddingStores);
        variables.put("content_stores", contentStores);
        variables.put("agents", agents);

        return JinjaTemplateUtil.render(TEMPLATE_CONTENT, variables);
    }

    public static List<ConfigTemplate> configTemplates(Factory factory) {
        List<ConfigTemplate> configs = new ArrayList<>();
        if (factory.requiredOptions() != null) {
            configs.addAll(factory.requiredOptions().stream()
                    .map(o -> configTemplate(true, o))
                    .toList());
        }
        if (factory.optionalOptions() != null) {
            configs.addAll(factory.optionalOptions().stream()
                    .map(o -> configTemplate(false, o))
                    .toList());
        }
        return configs;
    }

    private static ConfigTemplate configTemplate(boolean required, ConfigOption<?> configOption) {
        return new ConfigTemplate(required, configOption.key(), toValue(configOption.defaultValue()),
                toDescription(configOption));
    }

    private static Object toValue(Object value) {
        if (value instanceof Duration val) {
            return TimeUtils.formatWithHighestUnit(val);
        } else {
            return value;
        }
    }

    private static String toDescription(ConfigOption<?> configOption) {
        return new HtmlFormatter().format(configOption.description());
    }

    private record SingleItemTemplate(@Getter String provider, @Getter boolean display,
                                      @Getter List<ConfigTemplate> configs) {
    }

    private record MultipleItemTemplate(@Getter String name, @Getter String provider, @Getter boolean display,
                                        @Getter List<ConfigTemplate> configs) {
    }

    private record ConfigTemplate(@Getter boolean required, @Getter String key, @Getter Object value,
                                  @Getter String description) {
    }

}