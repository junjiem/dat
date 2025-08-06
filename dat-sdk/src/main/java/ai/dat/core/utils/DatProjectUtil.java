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
 * DAT项目配置工具类
 *
 * @Author JunjieM
 * @Date 2025/1/16
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
        return new DatProjectFactory().create(yamlContent);
    }
}