package ai.dat.core.utils;

import ai.dat.core.adapter.SemanticAdapter;
import ai.dat.core.semantic.SqlParserWrapper;
import ai.dat.core.semantic.data.Dimension;
import ai.dat.core.semantic.data.Entity;
import ai.dat.core.semantic.data.Measure;
import ai.dat.core.semantic.data.SemanticModel;
import ai.dat.core.semantic.view.SemanticModelView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import lombok.NonNull;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *
 */
public class SemanticModelUtil {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private SemanticModelUtil() {
    }

    public static void validateSemanticModels(List<SemanticModel> semanticModels) {
        if (semanticModels == null || semanticModels.isEmpty()) {
            return;
        }
        List<String> duplicates = semanticModels.stream()
                .map(SemanticModel::getName)
                .collect(Collectors.groupingBy(name -> name, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        Preconditions.checkArgument(duplicates.isEmpty(),
                String.format("There are duplicate semantic model names: %s",
                        String.join(", ", duplicates)));
        semanticModels.forEach(SemanticModelUtil::validateSemanticModel);
    }

    public static void validateSemanticModel(@NonNull SemanticModel semanticModel) {
        String name = semanticModel.getName();
        String sql = semanticModel.getModel();
        Preconditions.checkArgument(sql.trim().toUpperCase().startsWith("SELECT"),
                String.format("The model of the semantic model '%s' must be a SELECT statement", name));
        Preconditions.checkArgument(!Pattern.compile(".*\\s*;\\s*$", Pattern.DOTALL)
                        .matcher(sql).matches(),
                String.format("The model of the semantic model '%s' is and can only be a SELECT statement " +
                        "(The end of an statement cannot contain ';')", name));
    }

    public static SemanticModelView toSemanticModelView(@NonNull SemanticModel semanticModel) {
        return SemanticModelView.from(semanticModel, null);
    }

    public static SemanticModelView toSemanticModelView(@NonNull SemanticModel semanticModel,
                                                        SemanticAdapter semanticAdapter) {
        return SemanticModelView.from(semanticModel, semanticAdapter);
    }

    public static String toSemanticModelViewText(@NonNull SemanticModel semanticModel) {
        return toSemanticModelViewText(semanticModel, null);
    }

    public static String toSemanticModelViewText(@NonNull SemanticModel semanticModel,
                                                   SemanticAdapter semanticAdapter) {
        try {
            return JSON_MAPPER.writeValueAsString(toSemanticModelView(semanticModel, semanticAdapter));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize semantic model view to JSON: "
                    + e.getMessage(), e);
        }
    }

    /**
     * 获取语义模型的数据集SQL
     *
     * @param semanticAdapter
     * @param semanticModel
     * @return
     * @throws SqlParseException
     */
    public static String semanticModelSql(@NonNull SemanticAdapter semanticAdapter,
                                          @NonNull SemanticModel semanticModel) throws SqlParseException {
        SqlDialect sqlDialect = semanticAdapter.getSqlDialect();
        SqlParserWrapper sqlParser = SqlParserWrapper.forDialect(sqlDialect);
        String semanticModelName = semanticModel.getName();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        List<String> selectFields = new ArrayList<>();
        for (Entity entity : semanticModel.getEntities()) {
            String name = entity.getName();
            String expr = entity.getExpr() != null && !entity.getExpr().isEmpty() ?
                    entity.getExpr() : name;
            String selectField = expr + " AS " + semanticAdapter.quoteIdentifier(name);
//            String selectField = expr2Sql(expr, sqlDialect) + " AS " + quoteIdentifier(name);
            selectFields.add(selectField);
        }
        for (Dimension dimension : semanticModel.getDimensions()) {
            String name = dimension.getName();
            String expr = dimension.getExpr() != null && !dimension.getExpr().isEmpty() ?
                    dimension.getExpr() : name;
            if (Dimension.DimensionType.TIME == dimension.getType()) {
                expr = semanticAdapter.applyTimeGranularity(expr,
                        dimension.getTypeParams().getTimeGranularity());
            }
            String selectField = expr + " AS " + semanticAdapter.quoteIdentifier(name);
//            String selectField = expr2Sql(expr, sqlDialect) + " AS " + quoteIdentifier(name);
            selectFields.add(selectField);
        }
        for (Measure measure : semanticModel.getMeasures()) {
            String name = measure.getName();
            String expr = measure.getExpr() != null && !measure.getExpr().isEmpty() ?
                    measure.getExpr() : name;
            String selectField = expr + " AS " + semanticAdapter.quoteIdentifier(name);
//            String selectField = expr2Sql(expr, sqlDialect) + " AS " + quoteIdentifier(name);
            selectFields.add(selectField);
        }
        sql.append(String.join(", ", selectFields));
        String modelSql = semanticModel.getModel();
        Preconditions.checkArgument(modelSql.trim().toUpperCase().startsWith("SELECT"),
                String.format("The model of the semantic model '%s' must be a SELECT statement",
                        semanticModelName));
        Preconditions.checkArgument(!Pattern.compile(".*\\s*;\\s*$", Pattern.DOTALL)
                        .matcher(modelSql).matches(),
                String.format("The model of the semantic model '%s' is and can only be a SELECT statement " +
                        "(The end of an statement cannot contain ';')", semanticModelName));
        SqlNode sqlNode = sqlParser.parseQuery(modelSql);
        sql.append(" FROM (").append(sqlNode2Sql(sqlNode, sqlDialect)).append(") AS ")
                .append(semanticAdapter.quoteIdentifier("__dat_model"));
        return sql.toString();
    }

    private static String sqlNode2Sql(SqlNode sqlNode, SqlDialect sqlDialect) {
        return sqlNode.toSqlString(sqlDialect).getSql();
    }

    private static String expr2Sql(String expr, SqlDialect sqlDialect) throws SqlParseException {
        SqlParserWrapper sqlParser = SqlParserWrapper.forDialect(sqlDialect);
        SqlNode sqlNode = sqlParser.parseExpression(expr);
        return sqlNode2Sql(sqlNode, sqlDialect);
    }

}
