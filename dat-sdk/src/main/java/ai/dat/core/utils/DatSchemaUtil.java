package ai.dat.core.utils;

import ai.dat.core.data.DatModel;
import ai.dat.core.data.DatSchema;
import ai.dat.core.semantic.data.SemanticModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.base.Preconditions;
import com.networknt.schema.*;
import jinjava.org.jsoup.helper.ValidationException;
import lombok.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility methods for validating schema YAML files and transforming semantic models.
 */
public class DatSchemaUtil {

    private static final YAMLMapper YAML_MAPPER = new YAMLMapper();
    private static final JsonSchemaFactory SCHEMA_FACTORY = JsonSchemaFactory
            .getInstance(SpecVersion.VersionFlag.V202012);
    private static final SchemaValidatorsConfig SCHEMA_CONFIG =
            SchemaValidatorsConfig.builder().locale(Locale.ENGLISH).build();
    private static final String SCHEMA_PATH = "schemas/schema.json";

    private static final Pattern MODEL_REF_PATTERN = Pattern.compile("ref\\(['\"]([^'\"]+)['\"]\\)");
    private static final String NAME_PART = "[a-zA-Z_][a-zA-Z0-9_]*";
    private static final Pattern SIMPLE_TABLE_NAME_PATTERN = Pattern.compile("^" + NAME_PART + "$");
    private static final Pattern QUALIFIED_TABLE_NAME_PATTERN = Pattern.compile(
            "^" + NAME_PART + "\\." + NAME_PART + "$");

    private static final JsonSchema JSON_SCHEMA;

    static {
        try {
            JSON_SCHEMA = loadSchema();
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to load schema: " + e.getMessage());
        }
    }

    /**
     * Loads and compiles the JSON schema used to validate DAT schema files.
     *
     * @return the compiled {@link JsonSchema}
     * @throws IOException if the schema resource is missing or cannot be parsed
     */
    private static JsonSchema loadSchema() throws IOException {
        try (InputStream schemaStream = DatSchemaUtil.class.getClassLoader().getResourceAsStream(SCHEMA_PATH)) {
            if (schemaStream == null) {
                throw new IOException("Schema file not found in classpath: " + SCHEMA_PATH);
            }
            try {
                JsonNode schemaNode = new JsonMapper().readTree(schemaStream);
                return SCHEMA_FACTORY.getSchema(schemaNode, SCHEMA_CONFIG);
            } catch (IOException e) {
                throw new IOException("Failed to parse schema file: " + SCHEMA_PATH + " - " + e.getMessage(), e);
            }
        }
    }

    /**
     * Prevents instantiation of this utility class.
     */
    private DatSchemaUtil() {
    }

    /**
     * Validates schema YAML content using the JSON schema definition.
     *
     * @param yamlContent the YAML content to validate
     * @return the set of validation messages produced by the schema validator
     * @throws IOException if the YAML cannot be parsed
     */
    public static Set<ValidationMessage> validate(@NonNull String yamlContent) throws IOException {
        Preconditions.checkArgument(!yamlContent.isEmpty(), "yamlContent cannot be empty");
        JsonNode jsonNode = YAML_MAPPER.readTree(yamlContent);
        return JSON_SCHEMA.validate(jsonNode);
    }

    /**
     * Parses YAML content into a {@link DatSchema} after validation.
     *
     * @param yamlContent the YAML content describing the schema
     * @return the deserialized {@link DatSchema}
     * @throws IOException if the YAML cannot be parsed
     */
    public static DatSchema datSchema(@NonNull String yamlContent) throws IOException {
        Set<ValidationMessage> validationErrors = validate(yamlContent);
        if (!validationErrors.isEmpty()) {
            throw new ValidationException("The YAML verification not pass: \n" + validationErrors);
        }
        return YAML_MAPPER.readValue(yamlContent, DatSchema.class);
    }

    /**
     * Parses semantic models from YAML content while resolving model references.
     *
     * @param yamlContent the schema YAML content
     * @param models the available DAT models referenced by semantic models
     * @return the list of semantic models with SQL resolved
     * @throws IOException if the YAML cannot be parsed
     */
    public static List<SemanticModel> getSemanticModels(@NonNull String yamlContent,
                                                        List<DatModel> models) throws IOException {
        return getSemanticModels(datSchema(yamlContent), models);
    }

    /**
     * Resolves semantic models from a {@link DatSchema} instance using the supplied DAT models.
     *
     * @param schema the schema definition
     * @param models the available DAT models referenced by semantic models
     * @return the list of semantic models with SQL resolved
     */
    public static List<SemanticModel> getSemanticModels(@NonNull DatSchema schema, @NonNull List<DatModel> models) {
        Map<String, DatModel> modelMap = models.stream().collect(Collectors.toMap(DatModel::getName, m -> m));
        return schema.getSemanticModels().stream()
                .map(m -> toSemanticModel(m, modelMap)).collect(Collectors.toList());
    }

    /**
     * Extracts referenced model names from the schema.
     *
     * @param schema the schema definition
     * @return the list of referenced model names
     */
    public static List<String> getModelName(@NonNull DatSchema schema) {
        return schema.getSemanticModels().stream()
                .map(m -> extractModelName(m.getModel()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Resolves the SQL backing a semantic model by expanding references and normalizing SQL.
     */
    private static SemanticModel toSemanticModel(@NonNull SemanticModel semanticModel,
                                                 @NonNull Map<String, DatModel> models) {
        String name = semanticModel.getName();
        String model = semanticModel.getModel();
        String modelName = extractModelName(model);
        if (modelName != null) {
            Preconditions.checkArgument(models.containsKey(modelName),
                    String.format("There are non-existent model '%s' in the semantic model '%s'",
                            modelName, name));
            String sql = models.get(modelName).getSql();
            model = convertSql(sql);
        } else if (isSelectSql(model)) {
            model = convertSql(model);
        } else if (isTableName(model)) {
            model = "SELECT * FROM " + model.trim();
        } else {
            throw new RuntimeException(
                    String.format("The model value of the semantic model '%s' is incorrect. model: %s",
                            name, model));
        }
        semanticModel.setModel(model);
        return semanticModel;
    }

    /**
     * Extracts a referenced model name from a {@code ref('model_name')} expression.
     */
    private static String extractModelName(String ref) {
        Matcher matcher = MODEL_REF_PATTERN.matcher(ref);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Determines whether the provided text represents a SELECT SQL statement.
     */
    public static boolean isSelectSql(String str) {
        String sql = removeSqlComments(str).trim();
        return Pattern.compile("^\\s*select\\b", Pattern.CASE_INSENSITIVE).matcher(sql).find();
    }

    /**
     * Removes both single-line and multi-line SQL comments from the supplied statement.
     */
    public static String removeSqlComments(String sql) {
        return sql.replaceAll("--.*", "") // Remove single-line comments (-- comment)
                .replaceAll("/\\*[\\s\\S]*?\\*/", ""); // Remove block comments (/* ... */)
    }

    /**
     * Normalizes SQL by removing comments and trailing semicolons.
     */
    private static String convertSql(String sql) {
        return removeSqlComments(sql).trim().replaceAll(";\\s*$", "").trim();
    }

    /**
     * Determines whether the input represents a table name (qualified or unqualified).
     */
    private static boolean isTableName(String str) {
        return isSimpleTableName(str) || isQualifiedTableName(str);
    }

    /**
     * Checks whether the input is a simple (unqualified) table name.
     */
    private static boolean isSimpleTableName(String str) {
        return SIMPLE_TABLE_NAME_PATTERN.matcher(str.trim()).matches();
    }

    /**
     * Checks whether the input is a schema-qualified table name.
     */
    private static boolean isQualifiedTableName(String str) {
        return QUALIFIED_TABLE_NAME_PATTERN.matcher(str.trim()).matches();
    }
}