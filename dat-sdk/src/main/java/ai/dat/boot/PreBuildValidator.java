package ai.dat.boot;

import ai.dat.boot.utils.ProjectUtil;
import ai.dat.core.adapter.DatabaseAdapter;
import ai.dat.core.adapter.SemanticAdapter;
import ai.dat.core.adapter.data.AnsiSqlType;
import ai.dat.core.adapter.data.ColumnMetadata;
import ai.dat.core.configuration.ConfigOption;
import ai.dat.core.configuration.ReadableConfig;
import ai.dat.core.data.project.DatProject;
import ai.dat.core.exception.ValidationException;
import ai.dat.core.factories.DatProjectFactory;
import ai.dat.core.semantic.data.Dimension;
import ai.dat.core.semantic.data.Element;
import ai.dat.core.semantic.data.SemanticModel;
import ai.dat.core.utils.FactoryUtil;
import ai.dat.core.utils.JinjaTemplateUtil;
import ai.dat.core.utils.SemanticModelUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.parser.SqlParseException;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ai.dat.core.factories.DatProjectFactory.*;

/**
 * Performs validation tasks that must succeed before a DAT project can be built, including
 * schema verification, SQL validation, and data type checks for semantic models.
 */
@Slf4j
class PreBuildValidator {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final DatProject project;
    private final Path projectPath;
    private final Map<String, Object> variables;

    /**
     * Creates a validator instance tied to a specific project and source path.
     *
     * @param project the DAT project definition to validate
     * @param projectPath the file system path containing the project
     * @param variables template variables used when rendering semantic model definitions
     */
    public PreBuildValidator(@NonNull DatProject project, @NonNull Path projectPath,
                             Map<String, Object> variables) {
        this.project = project;
        this.projectPath = projectPath;
        this.variables = Optional.ofNullable(variables).orElse(Collections.emptyMap());
    }

    /**
     * Executes all configured validation steps for the current project.
     */
    public void validate() {
        ReadableConfig config = project.getConfiguration();
        DatProjectFactory factory = new DatProjectFactory();
        Set<ConfigOption<?>> requiredOptions = factory.projectRequiredOptions();
        Set<ConfigOption<?>> optionalOptions = factory.projectOptionalOptions();
        FactoryUtil.validateFactoryOptions(requiredOptions, optionalOptions, config);

        Map<String, List<SemanticModel>> semanticModels = ChangeSemanticModelsCacheUtil.get(project.getName())
                .entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .map(m -> {
                                    try {
                                        SemanticModel semanticModel = JSON_MAPPER.readValue(
                                                JSON_MAPPER.writeValueAsString(m), SemanticModel.class);
                                        semanticModel.setModel(JinjaTemplateUtil.render(semanticModel.getModel(), variables));
                                        return semanticModel;
                                    } catch (JsonProcessingException ex) {
                                        throw new RuntimeException(ex);
                                    }
                                })
                                .collect(Collectors.toList()))
                );
        DatabaseAdapter databaseAdapter = ProjectUtil.createDatabaseAdapter(project, projectPath);

        validateModelSqls(semanticModels, databaseAdapter); // Validate raw model SQL
        validateSemanticModelSqls(semanticModels, databaseAdapter); // Validate generated semantic SQL

        if (config.get(BUILDING_VERIFY_MDL_DIMENSIONS_ENUM_VALUES)) {
            validateDimensionsEnumValues(semanticModels, databaseAdapter);
        }
        if (config.get(BUILDING_VERIFY_MDL_DATA_TYPES)) {
            validateDataTypes(semanticModels, databaseAdapter);
        }
        if (config.get(BUILDING_AUTO_COMPLETE_MDL_DATA_TYPES)) {
            autoCompleteDataTypes(semanticModels, databaseAdapter);
        }
    }

    /**
     * Validates that the raw model SQL statements compile against the project's database adapter.
     *
     * @param semanticModels semantic models grouped by their source file path
     * @param databaseAdapter the database adapter used to execute validation queries
     */
    private void validateModelSqls(@NonNull Map<String, List<SemanticModel>> semanticModels,
                                   @NonNull DatabaseAdapter databaseAdapter) {
        Map<String, List<ValidationMessage>> validations =
                semanticModels.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> e.getValue().stream()
                                        .map(model -> validateModelSql(databaseAdapter, model))
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList())
                        ))
                        .entrySet().stream().filter(e -> !e.getValue().isEmpty())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!validations.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            validations.forEach((relativePath, validationMessages) -> {
                sb.append("There has exceptions in the model SQL syntax validation of the semantic model, " +
                        "in the YAML file relative path: ").append(relativePath).append("\n");
                validationMessages.forEach(m -> sb.append("  - ").append(m.semanticModelName)
                        .append(": ").append(m.exception.getMessage()).append("\n"));
                sb.append("\n");
            });
            throw new ValidationException(sb.toString());
        }
    }

    /**
     * Validates a single semantic model's raw SQL by executing a minimal query.
     *
     * @param databaseAdapter the database adapter used to run the validation query
     * @param semanticModel the semantic model whose SQL should be validated
     * @return a validation message if validation fails, or {@code null} when successful
     */
    private ValidationMessage validateModelSql(@NonNull DatabaseAdapter databaseAdapter,
                                               @NonNull SemanticModel semanticModel) {
        String sql = "SELECT 1 FROM (" + semanticModel.getModel() + ") AS __dat_model WHERE 1=0";
        try {
            databaseAdapter.executeQuery(sql);
        } catch (SQLException e) {
            log.warn("SQL: " + sql + "\nException: " + e.getMessage());
            return new ValidationMessage(semanticModel.getName(), e);
        }
        return null;
    }

    /**
     * Validates the SQL generated from semantic models to ensure the semantic adapter can compile them.
     *
     * @param semanticModels semantic models grouped by their source file path
     * @param databaseAdapter the database adapter used to execute validation queries
     */
    private void validateSemanticModelSqls(@NonNull Map<String, List<SemanticModel>> semanticModels,
                                           @NonNull DatabaseAdapter databaseAdapter) {
        Map<String, List<ValidationMessage>> validations =
                semanticModels.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> e.getValue().stream()
                                        .map(model -> validateSemanticModelSql(databaseAdapter, model))
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList())
                        ))
                        .entrySet().stream().filter(e -> !e.getValue().isEmpty())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!validations.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            validations.forEach((relativePath, validationMessages) -> {
                sb.append("There has exceptions in the semantic model SQL syntax validation of the semantic model, " +
                        "in the YAML file relative path: ").append(relativePath).append("\n");
                validationMessages.forEach(m -> sb.append("  - ").append(m.semanticModelName)
                        .append(": ").append(m.exception.getMessage()).append("\n"));
                sb.append("\n");
            });
            throw new ValidationException(sb.toString());
        }
    }

    /**
     * Validates a single semantic model by generating semantic SQL and executing a minimal query.
     *
     * @param databaseAdapter the database adapter used to execute the validation query
     * @param semanticModel the semantic model whose semantic SQL should be validated
     * @return a validation message if validation fails, or {@code null} when successful
     */
    private ValidationMessage validateSemanticModelSql(@NonNull DatabaseAdapter databaseAdapter,
                                                       @NonNull SemanticModel semanticModel) {
        SemanticAdapter semanticAdapter = databaseAdapter.semanticAdapter();
        String semanticModelSql;
        try {
            semanticModelSql = SemanticModelUtil.semanticModelSql(semanticAdapter, semanticModel);
        } catch (SqlParseException e) {
            log.warn("Semantic model sql parse exception, Model SQL: " + semanticModel.getModel(), e);
            return new ValidationMessage(semanticModel.getName(), e);
        }
        String sql = "SELECT 1 FROM (" + semanticModelSql + ") AS __dat_semantic_model WHERE 1=0";
        try {
            databaseAdapter.executeQuery(sql);
        } catch (SQLException e) {
            log.warn("SQL: " + sql + "\nException: " + e.getMessage());
            return new ValidationMessage(semanticModel.getName(), e);
        }
        return null;
    }

    /**
     * Validates that configured dimension enumerations align with actual data values.
     *
     * @param semanticModels semantic models grouped by their source file path
     * @param databaseAdapter the database adapter used to execute validation queries
     */
    private void validateDimensionsEnumValues(@NonNull Map<String, List<SemanticModel>> semanticModels,
                                              @NonNull DatabaseAdapter databaseAdapter) {
        Map<String, List<ValidationMessage>> validations =
                semanticModels.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> e.getValue().stream()
                                        .map(model -> validateDimensionEnumValues(databaseAdapter, model))
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList())
                        ))
                        .entrySet().stream().filter(e -> !e.getValue().isEmpty())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!validations.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            validations.forEach((relativePath, validationMessages) -> {
                sb.append("There has exceptions in the dimension enum values validation of the semantic model, " +
                        "in the YAML file relative path: ").append(relativePath).append("\n");
                validationMessages.forEach(m -> sb.append("  - ").append(m.semanticModelName)
                        .append(": ").append(m.exception.getMessage()).append("\n"));
                sb.append("\n");
            });
            throw new ValidationException(sb.toString());
        }
    }

    /**
     * Validates enum values for a single semantic model dimension set.
     *
     * @param databaseAdapter the database adapter used to retrieve distinct values
     * @param semanticModel the semantic model whose dimensions are validated
     * @return a validation message if mismatches are found, otherwise {@code null}
     */
    private ValidationMessage validateDimensionEnumValues(@NonNull DatabaseAdapter databaseAdapter,
                                                          @NonNull SemanticModel semanticModel) {
        List<Dimension> dimensions = semanticModel.getDimensions().stream()
                .filter(d -> d.getEnumValues() != null && !d.getEnumValues().isEmpty())
                .toList();
        if (dimensions.isEmpty()) {
            return null;
        }
        SemanticAdapter semanticAdapter = databaseAdapter.semanticAdapter();
        String semanticModelSql;
        try {
            semanticModelSql = SemanticModelUtil.semanticModelSql(semanticAdapter, semanticModel);
        } catch (SqlParseException e) {
            log.warn("Semantic model sql parse exception, Model SQL: " + semanticModel.getModel(), e);
            return new ValidationMessage(semanticModel.getName(), e);
        }
        List<String> messages = dimensions.stream()
                .map(d -> {
                    List<String> enumValues = d.getEnumValues().stream()
                            .map(enumValue -> {
                                Object value = enumValue.getValue();
                                if (value instanceof Number val) {
                                    return val.toString();
                                } else {
                                    return value.toString();
                                }
                            }).toList();
                    try {
                        if (dimensionDistinctCount(d, databaseAdapter, semanticModelSql) > 1000) {
                            return "Dimension '" + d.getName()
                                    + "' -> The number of COUNT DISTINCT in this dimension field " +
                                    "in the database exceeds 1000, and not recommended to set enum values";
                        }
                        String sql = "SELECT DISTINCT " + d.getName()
                                + " FROM (" + semanticModelSql + ") AS __dat_semantic_model";
                        Set<String> values = databaseAdapter.executeQuery(sql).stream()
                                .map(map -> map.entrySet().iterator().next().getValue())
                                .filter(Objects::nonNull).map(Object::toString).collect(Collectors.toSet());
                        if (values.containsAll(enumValues)) {
                            return null;
                        }
                        return "Dimension '" + d.getName()
                                + "' -> Enum values contain values that do not exist in the database. " +
                                "\n  \t\tvalues: [" + String.join(", ", values) + "], " +
                                "\n  \t\tenum_values: [" + String.join(", ", enumValues) + "]";
                    } catch (SQLException e) {
                        return "Dimension '" + d.getName() + "' -> " + e.getMessage();
                    }
                })
                .filter(Objects::nonNull).toList();
        if (messages.isEmpty()) {
            return null;
        }
        return new ValidationMessage(semanticModel.getName(),
                new ValidationException(String.join("\n", messages)));
    }

    /**
     * Counts distinct values for a dimension using the semantic model SQL.
     *
     * @param dimension the dimension whose distinct values should be counted
     * @param databaseAdapter the database adapter executing the query
     * @param semanticModelSql the semantic SQL representing the semantic model
     * @return the count of distinct values for the dimension
     * @throws SQLException if the query execution fails
     */
    private long dimensionDistinctCount(Dimension dimension,
                                        DatabaseAdapter databaseAdapter,
                                        String semanticModelSql) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT " + dimension.getName() + ") AS distinct_count"
                + " FROM (" + semanticModelSql + ") AS __dat_semantic_model";
        Object value = databaseAdapter.executeQuery(sql).get(0)
                .entrySet().iterator().next().getValue();
        if (value instanceof Number number) {
            return number.longValue();
        } else {
            throw new ValidationException("The type " + value.getClass().getSimpleName()
                    + " cannot be converted to a numeric type");
        }
    }

    /**
     * Validates that semantic model elements annotated with data types match the database schema.
     *
     * @param semanticModels semantic models grouped by their source file path
     * @param databaseAdapter the database adapter used to retrieve column metadata
     */
    private void validateDataTypes(@NonNull Map<String, List<SemanticModel>> semanticModels,
                                   @NonNull DatabaseAdapter databaseAdapter) {
        Map<String, List<ValidationMessage>> validations =
                semanticModels.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> e.getValue().stream()
                                        .map(model -> validateDataTypes(databaseAdapter, model))
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList())
                        ))
                        .entrySet().stream().filter(e -> !e.getValue().isEmpty())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!validations.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            validations.forEach((relativePath, validationMessages) -> {
                sb.append("There has exceptions in the data types validation of the semantic model, " +
                        "in the YAML file relative path: ").append(relativePath).append("\n");
                validationMessages.forEach(m -> sb.append("  - ").append(m.semanticModelName)
                        .append(": ").append(m.exception.getMessage()).append("\n"));
                sb.append("\n");
            });
            throw new ValidationException(sb.toString());
        }
    }

    /**
     * Validates data types for a single semantic model.
     *
     * @param databaseAdapter the database adapter used to fetch column metadata
     * @param semanticModel the semantic model whose data types should be validated
     * @return a validation message if mismatches occur, otherwise {@code null}
     */
    private ValidationMessage validateDataTypes(@NonNull DatabaseAdapter databaseAdapter,
                                                @NonNull SemanticModel semanticModel) {
        Map<String, String> dataTypes = Stream.of(
                        semanticModel.getEntities().stream(),
                        semanticModel.getDimensions().stream(),
                        semanticModel.getMeasures().stream()
                )
                .flatMap(Function.identity())
                .map(o -> (Element) o)
                .filter(o -> o.getDataType() != null)
                .collect(Collectors.toMap(Element::getName, Element::getDataType));
        if (dataTypes.isEmpty()) {
            return null;
        }
        SemanticAdapter semanticAdapter = databaseAdapter.semanticAdapter();
        String semanticModelSql;
        try {
            semanticModelSql = SemanticModelUtil.semanticModelSql(semanticAdapter, semanticModel);
        } catch (SqlParseException e) {
            log.warn("Semantic model sql parse exception, Model SQL: " + semanticModel.getModel(), e);
            return new ValidationMessage(semanticModel.getName(), e);
        }
        String sql = "SELECT * FROM (" + semanticModelSql + ") AS __dat_semantic_model WHERE 1=0";
        try {
            Map<String, String> columnTypes = databaseAdapter.getColumnMetadata(sql).stream()
                    .collect(Collectors.toMap(ColumnMetadata::getColumnLabel, ColumnMetadata::getColumnTypeName));
            Map<String, String> incorrectDataTypes = dataTypes.entrySet().stream()
                    .filter(e -> !e.getValue().equalsIgnoreCase(columnTypes.get(e.getKey())))
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> columnTypes.get(e.getKey())));
            if (!incorrectDataTypes.isEmpty()) {
                String message = incorrectDataTypes.entrySet().stream()
                        .map(e -> "data type of '" + e.getKey() + "' should be '" + e.getValue() + "'")
                        .collect(Collectors.joining(", and "));
                log.warn(message);
                return new ValidationMessage(semanticModel.getName(), new ValidationException(message));
            }
        } catch (SQLException e) {
            log.warn("SQL: " + sql + "\nException: " + e.getMessage());
            return new ValidationMessage(semanticModel.getName(), e);
        }
        return null;
    }

    /**
     * Attempts to populate missing data type information by inspecting database metadata.
     *
     * @param semanticModels semantic models grouped by their source file path
     * @param databaseAdapter the database adapter used to fetch column metadata
     */
    private void autoCompleteDataTypes(@NonNull Map<String, List<SemanticModel>> semanticModels,
                                       @NonNull DatabaseAdapter databaseAdapter) {
        Map<String, List<ValidationMessage>> validations =
                semanticModels.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> e.getValue().stream()
                                        .map(model -> autoCompleteDataTypes(databaseAdapter, model))
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList())
                        ))
                        .entrySet().stream().filter(e -> !e.getValue().isEmpty())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!validations.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            validations.forEach((relativePath, validationMessages) -> {
                sb.append("There has exceptions in the data types validation of the semantic model, " +
                        "in the YAML file relative path: ").append(relativePath).append("\n");
                validationMessages.forEach(m -> sb.append("  - ").append(m.semanticModelName)
                        .append(": ").append(m.exception.getMessage()).append("\n"));
                sb.append("\n");
            });
            throw new ValidationException(sb.toString());
        }
    }

    /**
     * Populates missing data types for a single semantic model by consulting database metadata.
     *
     * @param databaseAdapter the database adapter used to fetch column metadata
     * @param semanticModel the semantic model whose missing data types should be populated
     * @return a validation message if the lookup fails, otherwise {@code null}
     */
    private ValidationMessage autoCompleteDataTypes(@NonNull DatabaseAdapter databaseAdapter,
                                                    @NonNull SemanticModel semanticModel) {
        List<Element> elements = Stream.of(
                        semanticModel.getEntities().stream(),
                        semanticModel.getDimensions().stream(),
                        semanticModel.getMeasures().stream()
                )
                .flatMap(Function.identity())
                .map(o -> (Element) o)
                .filter(o -> o.getDataType() == null)
                .toList();
        if (elements.isEmpty()) {
            return null;
        }
        SemanticAdapter semanticAdapter = databaseAdapter.semanticAdapter();
        String semanticModelSql;
        try {
            semanticModelSql = SemanticModelUtil.semanticModelSql(semanticAdapter, semanticModel);
        } catch (SqlParseException e) {
            log.warn("Semantic model sql parse exception, Model SQL: " + semanticModel.getModel(), e);
            return new ValidationMessage(semanticModel.getName(), e);
        }
        String sql = "SELECT * FROM (" + semanticModelSql + ") AS __dat_semantic_model WHERE 1=0";
        try {
            Map<String, AnsiSqlType> ansiSqlTypes = databaseAdapter.getColumnMetadata(sql).stream()
                    .collect(Collectors.toMap(ColumnMetadata::getColumnLabel, ColumnMetadata::getAnsiSqlType));
            elements.forEach(e -> e.setAnsiSqlType(ansiSqlTypes.get(e.getName())));
        } catch (SQLException e) {
            log.warn("SQL: " + sql + "\nException: " + e.getMessage());
            return new ValidationMessage(semanticModel.getName(), e);
        }
        return null;
    }

    /**
     * Captures the outcome of a validation step for a semantic model.
     *
     * @param semanticModelName the name of the semantic model being validated
     * @param exception the exception raised during validation, if any
     */
    private record ValidationMessage(@NonNull String semanticModelName, @NonNull Exception exception) {
    }
}
