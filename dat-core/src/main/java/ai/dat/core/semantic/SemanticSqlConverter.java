package ai.dat.core.semantic;

import ai.dat.core.adapter.SemanticAdapter;
import ai.dat.core.semantic.data.Dimension;
import ai.dat.core.semantic.data.Entity;
import ai.dat.core.semantic.data.Measure;
import ai.dat.core.semantic.data.SemanticModel;
import com.google.common.base.Preconditions;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.pretty.SqlPrettyWriter;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 语义SQL转换器
 */
public class SemanticSqlConverter {

    private final SemanticAdapter semanticAdapter;
    private final Map<String, SemanticModel> semanticModels;

    private final SqlDialect sqlDialect;

    private final SqlParserWrapper ansiSqlParser;
    private final SqlParserWrapper dialectSqlParser;

    public SemanticSqlConverter(SemanticAdapter semanticAdapter, List<SemanticModel> semanticModels) {
        this.semanticAdapter = semanticAdapter;
        this.semanticModels = semanticModels.stream()
                .collect(Collectors.toMap(SemanticModel::getName, model -> model));
        this.sqlDialect = semanticAdapter.getSqlDialect();
        this.ansiSqlParser = new SqlParserWrapper();
        this.dialectSqlParser = SqlParserWrapper.forDialect(sqlDialect);
    }

    /**
     * 转换语义SQL为真实SQL，并格式化
     *
     * @param semanticSql
     * @return
     * @throws SqlParseException
     */
    public String convertFormat(String semanticSql) throws SqlParseException {
        String dialectSql = convert(semanticSql);
        SqlNode sqlNode = dialectSqlParser.parseQuery(dialectSql);
        SqlWriterConfig config = SqlPrettyWriter.config()
                .withDialect(sqlDialect)

                // 核心换行配置
                .withSelectListItemsOnSeparateLines(true)  // SELECT 列表项分行显示
                .withClauseStartsLine(true)                // 子句（FROM/WHERE/GROUP BY等）从新行开始
                .withIndentation(2)                        // 缩进空格数（推荐2或4）

                // 增强可读性配置
                .withLineLength(100)                        // 最大行宽（触发换行）
                .withSelectListExtraIndentFlag(true)       // SELECT 列表额外缩进
                .withClauseEndsLine(true)                  // 子句结束后换行（如WHERE后换行）
                .withWindowNewline(true)                   // WINDOW 子句换行
                .withValuesListNewline(true)               // VALUES 列表换行

                // 可选风格配置
                .withKeywordsLowerCase(false)              // 关键字大小写（false=大写）
                .withQuoteAllIdentifiers(false)           // 是否引用所有标识符
                ;

        SqlPrettyWriter writer = new SqlPrettyWriter(config);
        sqlNode.unparse(writer, 0, 0);

        return writer.toString();
    }

    /**
     * 转换语义SQL为真实SQL
     *
     * @param semanticSql 语义SQL
     * @return 真实SQL
     * @throws SqlParseException SQL解析异常
     */
    public String convert(String semanticSql) throws SqlParseException {
        SqlNode sqlNode = ansiSqlParser.parseQuery(semanticSql);
        if (sqlNode == null) {
            throw new IllegalArgumentException("SQL node cannot be null");
        } else if (sqlNode instanceof SqlSelect select) {
            return convertSelect(select);
        } else if (sqlNode instanceof SqlOrderBy orderBy) {
            // Apache Calcite新版本将ORDER BY作为单独的节点
            if (orderBy.query instanceof SqlSelect select) {
                return convertSelect(select) + " ORDER BY " + sqlNode2Sql(orderBy.orderList);
            } else {
                throw new IllegalArgumentException("Unsupported ORDER BY query class: "
                        + orderBy.query.getClass().getSimpleName());
            }
        } else if (sqlNode instanceof SqlWith sqlWith) {
            return convertWith(sqlWith);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported SQL class: " + sqlNode.getClass().getSimpleName());
        }
    }

    private String convertSelect(SqlSelect select) throws SqlParseException {
        Map<String, String> semanticModelSqls = getSemanticModelSqls(select);
        return "WITH " + semanticModelSqls.entrySet().stream()
                .map(e -> e.getKey() + " AS (" + e.getValue() + ")")
                .collect(Collectors.joining(",")) +
                " " + sqlNode2Sql(select);
    }

    private String convertWith(SqlWith sqlWith) throws SqlParseException {
        // CTE部分
        List<SqlNode> withSqlNodes = sqlWith.withList.getList();
        List<SqlSelect> withSqlSelects = withSqlNodes.stream()
                .map(node -> (SqlWithItem) node)
                .filter(item -> item.query instanceof SqlSelect)
                .map(item -> (SqlSelect) item.query)
                .collect(Collectors.toList());
        Map<String, String> semanticModelSqls = getSemanticModelSqls(withSqlSelects);
        Map<String, String> withSqls = withSqlNodes.stream()
                .map(node -> (SqlWithItem) node)
                .collect(Collectors.toMap(item -> sqlNode2Sql(item.name), item -> sqlNode2Sql(item.query)));
        return "WITH " + semanticModelSqls.entrySet().stream()
                .map(e -> e.getKey() + " AS (" + e.getValue() + ")")
                .collect(Collectors.joining(",")) +
                ", " + withSqls.entrySet().stream()
                .map(e -> e.getKey() + " AS (" + e.getValue() + ")")
                .collect(Collectors.joining(",")) +
                " " + sqlNode2Sql(sqlWith.body);
    }

    private Set<String> extractReferencedTables(SqlSelect select) {
        Set<String> tables = new HashSet<>();

        // 从FROM子句中提取表名
        if (select.getFrom() != null) {
            extractTablesFromNode(select.getFrom(), tables);
        }

        // 从WHERE子句中提取表名（子查询）
        if (select.getWhere() != null) {
            extractTablesFromCondition(select.getWhere(), tables);
        }

        // 从HAVING子句中提取表名（子查询）
        if (select.getHaving() != null) {
            extractTablesFromCondition(select.getHaving(), tables);
        }

        // 从SELECT列表中提取表名（子查询）
        if (select.getSelectList() != null) {
            for (SqlNode selectItem : select.getSelectList()) {
                extractTablesFromCondition(selectItem, tables);
            }
        }

        return tables;
    }

    private void extractTablesFromCondition(SqlNode condition, Set<String> tables) {
        if (condition instanceof SqlSelect select) {
            // 处理子查询
            tables.addAll(extractReferencedTables(select));
        } else if (condition instanceof SqlBasicCall call) {
            // 递归处理操作数
            for (SqlNode operand : call.getOperandList()) {
                extractTablesFromCondition(operand, tables);
            }
        }
        // 对于其他类型的节点，可能不包含表引用，暂时忽略
    }

    private void extractTablesFromNode(SqlNode node, Set<String> tables) {
        if (node instanceof SqlIdentifier identifier) {
            if (identifier.isSimple()) {
                tables.add(identifier.getSimple());
            }
        } else if (node instanceof SqlJoin join) {
            // 处理JOIN节点
            extractTablesFromNode(join.getLeft(), tables);
            extractTablesFromNode(join.getRight(), tables);
        } else if (node instanceof SqlBasicCall call) {
            String operatorName = call.getOperator().getName().toUpperCase();
            // 处理JOIN操作
            if (operatorName.contains("JOIN")) {
                for (SqlNode operand : call.getOperandList()) {
                    extractTablesFromNode(operand, tables);
                }
            }
            // 处理AS别名操作
            else if (operatorName.equals("AS")) {
                // AS操作的第一个操作数是表或子查询，第二个是别名
                SqlNode tableOrSubquery = call.getOperandList().get(0);
                extractTablesFromNode(tableOrSubquery, tables);
            }
            // 处理UNION等集合操作
            else if (operatorName.equals("UNION") || operatorName.equals("UNION ALL") ||
                    operatorName.equals("INTERSECT") || operatorName.equals("EXCEPT")) {
                for (SqlNode operand : call.getOperandList()) {
                    extractTablesFromNode(operand, tables);
                }
            }
            // 处理其他可能包含表引用的操作
            else {
                for (SqlNode operand : call.getOperandList()) {
                    extractTablesFromNode(operand, tables);
                }
            }
        } else if (node instanceof SqlSelect select) {
            // 处理嵌套子查询
            tables.addAll(extractReferencedTables(select));
        }
        // 可以根据需要添加更多的节点类型处理
    }

    private Map<String, String> getSemanticModelSqls(List<SqlSelect> selects) throws SqlParseException {
        // 提取所有SQL中引用的表名
        Set<String> referencedTables = selects.stream()
                .flatMap(select -> extractReferencedTables(select).stream())
                .collect(Collectors.toSet());
        // 找到使用的语义模型
        Set<SemanticModel> usedModels = findUsedSemanticModels(referencedTables);
        if (usedModels.isEmpty()) {
            throw new IllegalArgumentException("No matching semantic model was found");
        }
        return getSemanticModelSqls(usedModels);
    }

    private Map<String, String> getSemanticModelSqls(SqlSelect select) throws SqlParseException {
        return getSemanticModelSqls(Collections.singletonList(select));
    }

    private Set<SemanticModel> findUsedSemanticModels(Set<String> referencedTables) {
        Set<SemanticModel> usedModels = new HashSet<>();
        for (String tableName : referencedTables) {
            SemanticModel model = semanticModels.get(tableName);
            if (model != null) {
                usedModels.add(model);
            }
        }
        return usedModels;
    }

    private Map<String, String> getSemanticModelSqls(Set<SemanticModel> semanticModels) throws SqlParseException {
        Map<String, String> semanticModelSqls = new HashMap<>();
        for (SemanticModel semanticModel : semanticModels) {
            String semanticModelSql = getSemanticModelSql(semanticModel);
            semanticModelSqls.put(quoteIdentifier(semanticModel.getName()), semanticModelSql);
        }
        return semanticModelSqls;
    }

    /**
     * 获取语义模型的数据集SQL
     *
     * @param semanticModel
     * @return
     * @throws SqlParseException
     */
    public String getSemanticModelSql(SemanticModel semanticModel) throws SqlParseException {
        String semanticModelName = semanticModel.getName();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        List<String> selectFields = new ArrayList<>();
        for (Entity entity : semanticModel.getEntities()) {
            String name = entity.getName();
            String expr = entity.getExpr() != null && !entity.getExpr().isEmpty() ?
                    entity.getExpr() : name;
            String selectField = expr + " AS " + quoteIdentifier(name);
//            String selectField = expr2Sql(expr) + " AS " + quoteIdentifier(name);
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
            String selectField = expr + " AS " + quoteIdentifier(name);
//            String selectField = expr2Sql(expr) + " AS " + quoteIdentifier(name);
            selectFields.add(selectField);
        }
        for (Measure measure : semanticModel.getMeasures()) {
            String name = measure.getName();
            String expr = measure.getExpr() != null && !measure.getExpr().isEmpty() ?
                    measure.getExpr() : name;
            String selectField = expr + " AS " + quoteIdentifier(name);
//            String selectField = expr2Sql(expr) + " AS " + quoteIdentifier(name);
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
        SqlNode sqlNode = dialectSqlParser.parseQuery(modelSql);
        sql.append(" FROM (").append(sqlNode2Sql(sqlNode)).append(") AS ")
                .append(quoteIdentifier("__dat_model"));
        return sql.toString();
    }

    private String sqlNode2Sql(SqlNode sqlNode) {
        return sqlNode.toSqlString(sqlDialect).getSql();
    }

//    private String expr2Sql(String expr) throws SqlParseException {
//        SqlNode sqlNode = dialectSqlParser.parseExpression(expr);
//        return sqlNode2Sql(sqlNode);
//    }

    private String quoteIdentifier(String name) {
        return semanticAdapter.quoteIdentifier(name);
    }
} 