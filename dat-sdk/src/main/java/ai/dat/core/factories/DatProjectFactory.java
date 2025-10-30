package ai.dat.core.factories;

import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ConfigOptions;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.data.project.*;
import ai.dat.core.exception.ValidationException;
import ai.dat.core.utils.DatProjectUtil;
import ai.dat.core.utils.JinjaTemplateUtil;
import ai.dat.core.utils.YamlTemplateUtil;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.ValidationMessage;
import lombok.Getter;
import lombok.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Factory responsible for creating {@link DatProject} instances from YAML definitions and
 * providing helper methods to build templated project configurations.
 */
public class DatProjectFactory {

    private static final YAMLMapper YAML_MAPPER = new YAMLMapper();

    private static final String LLM_NAME_PREFIX = "llm_";
    private static final String AGENT_NAME_PREFIX = "agent_";

    private static final String PROJECT_YAML_TEMPLATE;

    static {
        PROJECT_YAML_TEMPLATE = loadText("templates/project_yaml_template.jinja");
    }

    /**
     * Reads a text resource from the classpath using UTF-8 encoding.
     *
     * @param fromResource the resource path to load
     * @return the resource contents as a string
     */
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

    /**
     * Provides the set of configuration options that are mandatory for a DAT project.
     *
     * @return a set containing the required project options
     */
    public Set<ConfigOption<?>> projectRequiredOptions() {
        return Collections.emptySet();
    }

    /**
     * Provides optional configuration options that may be applied to a DAT project.
     *
     * @return a linked set containing optional project options in display order
     */
    public Set<ConfigOption<?>> projectOptionalOptions() {
        return new LinkedHashSet<>(List.of(
                BUILDING_VERIFY_MDL_DIMENSIONS_ENUM_VALUES,
                BUILDING_VERIFY_MDL_DATA_TYPES,
                BUILDING_AUTO_COMPLETE_MDL_DATA_TYPES
        ));
    }

    /**
     * Returns configuration options that affect the project fingerprint.
     *
     * @return a set containing fingerprint-related options
     */
    public static Set<ConfigOption<?>> fingerprintOptions() {
        return Set.of(BUILDING_AUTO_COMPLETE_MDL_DATA_TYPES);
    }

    /**
     * Extracts fingerprint-relevant configuration values from the supplied configuration.
     *
     * @param config the readable configuration source
     * @return a map of configuration keys to their string values contributing to the fingerprint
     */
    public Map<String, String> projectFingerprintConfigs(@NonNull ReadableConfig config) {
        List<String> keys = fingerprintOptions().stream()
                .map(ConfigOption::key)
                .toList();
        return config.toMap().entrySet().stream()
                .filter(e -> keys.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Builds a {@link DatProject} instance from the given YAML string after validating it against the schema.
     *
     * @param yamlContent the YAML project definition
     * @return a deserialized {@link DatProject}
     * @throws IOException if the YAML cannot be parsed
     * @throws ValidationException if schema validation fails
     */
    public DatProject create(@NonNull String yamlContent) throws IOException {
        Set<ValidationMessage> validationErrors = DatProjectUtil.validate(yamlContent);
        if (!validationErrors.isEmpty()) {
            throw new ValidationException("The YAML verification not pass: \n" + validationErrors);
        }
        return YAML_MAPPER.readValue(yamlContent, DatProject.class);
    }

    /**
     * Renders the project YAML template with the currently supported providers and configurations.
     *
     * @return a fully rendered project configuration template
     */
    public String yamlTemplate() {
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

        return JinjaTemplateUtil.render(PROJECT_YAML_TEMPLATE, variables);
    }

    /**
     * Generates the configuration block for the project itself without provider selections.
     *
     * @return the project configuration snippet
     */
    public String getProjectConfiguration() {
        return YamlTemplateUtil.getConfiguration(projectRequiredOptions(), projectOptionalOptions());
    }

    /**
     * Generates the configuration snippet for the default agent provider.
     *
     * @return YAML configuration for the default agent
     */
    public String getDefaultAgentConfiguration() {
        return getConfiguration(new DefaultAskdataAgentFactory());
    }

    /**
     * Delegates to {@link YamlTemplateUtil} to produce configuration values for a provider factory.
     *
     * @param factory the factory whose configuration is requested
     * @return the configuration snippet for the factory
     */
    private String getConfiguration(Factory factory) {
        return YamlTemplateUtil.getConfiguration(factory);
    }


    /**
     * Template representation for providers that appear at most once in the rendered YAML.
     */
    private record SingleItemTemplate(@Getter String provider, @Getter boolean display,
                                      @Getter String configuration) {
    }

    /**
     * Template representation for providers that may appear multiple times with distinct names.
     */
    private record MultipleItemTemplate(@Getter String name, @Getter String provider, @Getter boolean display,
                                        @Getter String configuration) {
    }

    /**
     * Template representation for agents that include descriptive comments alongside configuration.
     */
    private record MultipleItemContainCommentTemplate(@Getter String comment, @Getter String name,
                                                      @Getter String provider, @Getter boolean display,
                                                      @Getter String configuration) {
    }
}
