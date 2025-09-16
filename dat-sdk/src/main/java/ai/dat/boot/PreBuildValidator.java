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
import ai.dat.core.utils.SemanticModelUtil;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.apache.calcite.sql.parser.SqlParseException;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ai.dat.core.factories.DatProjectFactory.*;

/**
 * @Author JunjieM
 * @Date 2025/8/7
 */
public class PreBuildValidator {

    private final DatProject project;
    private final  Path projectPath;

    public PreBuildValidator(DatProject project, Path projectPath) {
        this.project = project;
        this.projectPath = projectPath;
    }

    public void validate() {
        ReadableConfig config = project.getConfiguration();
        DatProjectFactory factory = new DatProjectFactory();
        Set<ConfigOption<?>> requiredOptions = factory.projectRequiredOptions();
        Set<ConfigOption<?>> optionalOptions = factory.projectOptionalOptions();
        FactoryUtil.validateFactoryOptions(requiredOptions, optionalOptions, config);

        DatabaseAdapter databaseAdapter = ProjectUtil.createDatabaseAdapter(project, projectPath);

        validateModelSqls(project.getName(), databaseAdapter); // 校验模型SQL
        validateSemanticModelSqls(project.getName(), databaseAdapter); // 校验语义模型SQL

        if (config.get(BUILDING_VERIFY_MDL_DIMENSIONS_ENUM_VALUES)) {
            validateDimensionsEnumValues(project.getName(), databaseAdapter);
        }
        if (config.get(BUILDING_VERIFY_MDL_DATA_TYPES)) {
            validateDataTypes(project.getName(), databaseAdapter);
        }
        if (config.get(BUILDING_AUTO_COMPLETE_MDL_DATA_TYPES)) {
            autoCompleteDataTypes(project.getName(), databaseAdapter);
        }
    }

    private static void validateModelSqls(@NonNull String projectId,
                                          @NonNull DatabaseAdapter databaseAdapter) {
        Map<String, List<ValidationMessage>> validations =
                ChangeSemanticModelsCacheUtil.get(projectId).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> e.getValue().stream()
                                        .map(model -> validateModelSql(databaseAdapter, model))
                                        .filter(o -> o.exception != null)
                                        .collect(Collectors.toList())
                        ))
                        .entrySet().stream().filter(e -> !e.getValue().isEmpty())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!validations.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            validations.forEach((relativePath, validationMessages) -> {
                sb.append("There has exceptions in the model SQL validation of the semantic model, " +
                        "in the YAML file relative path: ").append(relativePath).append("\n");
                validationMessages.forEach(m -> sb.append("  - ").append(m.semanticModelName)
                        .append(": ").append(m.exception.getMessage()).append("\n"));
                sb.append("\n");
            });
            throw new ValidationException(sb.toString());
        }
    }

    private static ValidationMessage validateModelSql(@NonNull DatabaseAdapter databaseAdapter,
                                                      @NonNull SemanticModel semanticModel) {
        String sql = "SELECT 1 FROM (" + semanticModel.getModel() + ") AS __dat_model WHERE 1=0";
        SQLException sqlException;
        try {
            databaseAdapter.executeQuery(sql);
            sqlException = null;
        } catch (SQLException exception) {
            sqlException = exception;
        }
        return new ValidationMessage(semanticModel.getName(), sqlException);
    }

    private static void validateSemanticModelSqls(@NonNull String projectId,
                                                  @NonNull DatabaseAdapter databaseAdapter) {
        Map<String, List<ValidationMessage>> validations =
                ChangeSemanticModelsCacheUtil.get(projectId).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> e.getValue().stream()
                                        .map(model -> validateSemanticModelSql(databaseAdapter, model))
                                        .filter(o -> o.exception != null)
                                        .collect(Collectors.toList())
                        ))
                        .entrySet().stream().filter(e -> !e.getValue().isEmpty())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!validations.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            validations.forEach((relativePath, validationMessages) -> {
                sb.append("There has exceptions in the semantic model SQL validation of the semantic model, " +
                        "in the YAML file relative path: ").append(relativePath).append("\n");
                validationMessages.forEach(m -> sb.append("  - ").append(m.semanticModelName)
                        .append(": ").append(m.exception.getMessage()).append("\n"));
                sb.append("\n");
            });
            throw new ValidationException(sb.toString());
        }
    }

    private static ValidationMessage validateSemanticModelSql(@NonNull DatabaseAdapter databaseAdapter,
                                                              @NonNull SemanticModel semanticModel) {
        SemanticAdapter semanticAdapter = databaseAdapter.semanticAdapter();
        String semanticModelSql;
        try {
            semanticModelSql = SemanticModelUtil.semanticModelSql(semanticAdapter, semanticModel);
        } catch (SqlParseException e) {
            return new ValidationMessage(semanticModel.getName(), e);
        }
        String sql = "SELECT 1 FROM (" + semanticModelSql + ") AS __dat_semantic_model WHERE 1=0";
        SQLException exception;
        try {
            databaseAdapter.executeQuery(sql);
            exception = null;
        } catch (SQLException e) {
            exception = e;
        }
        return new ValidationMessage(semanticModel.getName(), exception);
    }

    private static void validateDimensionsEnumValues(@NonNull String projectId,
                                                     @NonNull DatabaseAdapter databaseAdapter) {
        Map<String, List<ValidationMessage>> validations =
                ChangeSemanticModelsCacheUtil.get(projectId).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> e.getValue().stream()
                                        .map(model -> validateDimensionEnumValues(databaseAdapter, model))
                                        .filter(Objects::nonNull)
                                        .filter(o -> o.exception != null)
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

    private static ValidationMessage validateDimensionEnumValues(@NonNull DatabaseAdapter databaseAdapter,
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
                                + "' -> Enum values contain values that do not exist in the database";
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

    private static long dimensionDistinctCount(Dimension dimension,
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

    private static void validateDataTypes(@NonNull String projectId,
                                          @NonNull DatabaseAdapter databaseAdapter) {
        Map<String, List<ValidationMessage>> validations =
                ChangeSemanticModelsCacheUtil.get(projectId).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> e.getValue().stream()
                                        .map(model -> validateDataTypes(databaseAdapter, model))
                                        .filter(Objects::nonNull)
                                        .filter(o -> o.exception != null)
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

    private static ValidationMessage validateDataTypes(@NonNull DatabaseAdapter databaseAdapter,
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
            return new ValidationMessage(semanticModel.getName(), e);
        }
        String sql = "SELECT * FROM (" + semanticModelSql + ") AS __dat_semantic_model WHERE 1=0";
        String message = null;
        try {
            Map<String, String> columnTypes = databaseAdapter.getColumnMetadata(sql).stream()
                    .collect(Collectors.toMap(ColumnMetadata::getColumnLabel, ColumnMetadata::getColumnTypeName));
            Map<String, String> incorrectDataTypes = dataTypes.entrySet().stream()
                    .filter(e -> !e.getValue().equalsIgnoreCase(columnTypes.get(e.getKey())))
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> columnTypes.get(e.getKey())));
            if (!incorrectDataTypes.isEmpty()) {
                message = incorrectDataTypes.entrySet().stream()
                        .map(e -> "data type of '" + e.getKey() + "' should be '" + e.getValue() + "'")
                        .collect(Collectors.joining(", and "));
            }
        } catch (SQLException e) {
            message = e.getMessage();
        }
        if (message == null) {
            return null;
        }
        return new ValidationMessage(semanticModel.getName(), new ValidationException(message));
    }

    private static void autoCompleteDataTypes(@NonNull String projectId,
                                              @NonNull DatabaseAdapter databaseAdapter) {
        Map<String, List<ValidationMessage>> validations =
                ChangeSemanticModelsCacheUtil.get(projectId).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> e.getValue().stream()
                                        .map(model -> autoCompleteDataTypes(databaseAdapter, model))
                                        .filter(Objects::nonNull)
                                        .filter(o -> o.exception != null)
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

    private static ValidationMessage autoCompleteDataTypes(@NonNull DatabaseAdapter databaseAdapter,
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
            return new ValidationMessage(semanticModel.getName(), e);
        }
        String sql = "SELECT * FROM (" + semanticModelSql + ") AS __dat_semantic_model WHERE 1=0";
        String message = null;
        try {
            Map<String, AnsiSqlType> ansiSqlTypes = databaseAdapter.getColumnMetadata(sql).stream()
                    .collect(Collectors.toMap(ColumnMetadata::getColumnLabel, ColumnMetadata::getAnsiSqlType));
            elements.forEach(e -> e.setAnsiSqlType(ansiSqlTypes.get(e.getName())));
        } catch (SQLException e) {
            message = e.getMessage();
        }
        if (message == null) {
            return null;
        }
        return new ValidationMessage(semanticModel.getName(), new ValidationException(message));
    }

    @AllArgsConstructor
    private static class ValidationMessage {
        private String semanticModelName;
        private Exception exception;
    }
}
