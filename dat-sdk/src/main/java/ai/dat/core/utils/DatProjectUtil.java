package ai.dat.core.utils;

import ai.dat.core.data.project.DatProject;
import ai.dat.core.factories.DatProjectFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.base.Preconditions;
import com.networknt.schema.*;
import lombok.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

/**
 * Utility methods for loading, validating, and instantiating DAT project definitions.
 */
public class DatProjectUtil {

    private static final YAMLMapper YAML_MAPPER = new YAMLMapper();
    private static final JsonSchemaFactory SCHEMA_FACTORY = JsonSchemaFactory
            .getInstance(SpecVersion.VersionFlag.V202012);
    private static final SchemaValidatorsConfig SCHEMA_CONFIG =
            SchemaValidatorsConfig.builder().locale(Locale.ENGLISH).build();
    private static final String SCHEMA_PATH = "schemas/project_schema.json";

    private static final JsonSchema JSON_SCHEMA;

    static {
        try {
            JSON_SCHEMA = loadProjectSchema();
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to load project schema file: " + e.getMessage());
        }
    }

    /**
     * Loads the JSON schema that validates DAT project configuration files.
     *
     * @return the compiled {@link JsonSchema} for project validation
     * @throws IOException if the schema resource is missing or cannot be parsed
     */
    private static JsonSchema loadProjectSchema() throws IOException {
        try (InputStream stream = DatProjectUtil.class.getClassLoader().getResourceAsStream(SCHEMA_PATH)) {
            if (stream == null) {
                throw new IOException("Project schema file not found in classpath: " + SCHEMA_PATH);
            }
            try {
                JsonNode schemaNode = new JsonMapper().readTree(stream);
                return SCHEMA_FACTORY.getSchema(schemaNode, SCHEMA_CONFIG);
            } catch (IOException e) {
                throw new IOException("Failed to parse project schema file: " + SCHEMA_PATH
                        + " - " + e.getMessage(), e);
            }
        }
    }

    /**
     * Prevents instantiation of this utility class.
     */
    private DatProjectUtil() {
    }

    /**
     * Validates a DAT project YAML configuration against the project JSON schema.
     *
     * @param yamlContent the YAML content to validate
     * @return the set of validation messages returned by the JSON schema validator
     * @throws IOException if the YAML payload cannot be parsed
     */
    public static Set<ValidationMessage> validate(@NonNull String yamlContent) throws IOException {
        Preconditions.checkArgument(!yamlContent.isEmpty(), "yamlContent cannot be empty");
        try {
            JsonNode jsonNode = YAML_MAPPER.readTree(yamlContent);
            return JSON_SCHEMA.validate(jsonNode);
        } catch (IOException e) {
            throw new IOException("Failed to parse YAML content: " + e.getMessage(), e);
        }
    }

    /**
     * Deserializes the provided YAML content into a {@link DatProject} instance.
     *
     * @param yamlContent the YAML configuration of a DAT project
     * @return a fully populated {@link DatProject}
     * @throws IOException if the YAML cannot be parsed or mapped to the project model
     */
    public static DatProject datProject(@NonNull String yamlContent) throws IOException {
        return new DatProjectFactory().create(yamlContent);
    }
}