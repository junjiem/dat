package ai.dat.core.semantic;

import ai.dat.core.adapter.SemanticAdapter;
import ai.dat.core.semantic.data.SemanticModel;
import ai.dat.core.utils.SemanticModelUtil;
import lombok.NonNull;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.pretty.SqlPrettyWriter;

import java.util.*;
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

    public SemanticSqlConverter(@NonNull SemanticAdapter semanticAdapter,
                                @NonNull List<SemanticModel> semanticModels) {
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
    public String convertFormat(@NonNull String semanticSql) throws SqlParseException {
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
    public String convert(@NonNull String semanticSql) throws SqlParseException {
        semanticSql = semanticSql.trim();
        semanticSql = semanticSql.endsWith(";") ?
                semanticSql.substring(0, semanticSql.length() - 1) : semanticSql;
        SqlNode sqlNode = ansiSqlParser.parseQuery(semanticSql);
        if (sqlNode == null) {
            throw new IllegalArgumentException("SQL node cannot be null");
        }
        if (sqlNode instanceof SqlSelect sqlSelect) {
            return convertSelect(sqlSelect);
        } else if (sqlNode instanceof SqlOrderBy sqlOrderBy) {
            return convertOrderBy(sqlOrderBy);
        } else if (sqlNode instanceof SqlWith sqlWith) {
            return convertWith(sqlWith);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported SQL class: " + sqlNode.getClass().getSimpleName());
        }
    }

    private String convertSelect(SqlNode sqlNode) throws SqlParseException {
        return "WITH " + getSemanticModelSqls(sqlNode).entrySet().stream()
                .map(e -> e.getKey() + " AS (" + e.getValue() + ")")
                .collect(Collectors.joining(",")) +
                " " + sqlNode2Sql(sqlNode);
    }

    private String convertOrderBy(SqlOrderBy sqlOrderBy) throws SqlParseException {
        SqlNode query = sqlOrderBy.query;
        if (query instanceof SqlSelect) {
            return convertSelect(sqlOrderBy);
        } else if (query instanceof SqlWith) {
            return convertWith(sqlOrderBy);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported SQL class: " + sqlOrderBy.getClass().getSimpleName());
        }
    }

    private String convertWith(SqlNode sqlNode) throws SqlParseException {
        SqlWith sqlWith;
        if (sqlNode instanceof SqlWith with) {
            sqlWith = with;
        } else if (sqlNode instanceof SqlOrderBy sqlOrderBy
                && sqlOrderBy.query instanceof SqlWith with) {
            sqlWith = with;
        } else {
            throw new IllegalArgumentException(
                    "Unsupported SQL class: " + sqlNode.getClass().getSimpleName());
        }
        List<SqlNode> withSqlNodes = sqlWith.withList.getList().stream()
                .filter(Objects::nonNull)
                .map(node -> (SqlWithItem) node)
                .filter(item -> item.query instanceof SqlSelect || item.query instanceof SqlOrderBy)
                .map(item -> item.query)
                .toList();
        List<SqlNode> sqlNodes = new ArrayList<>(withSqlNodes);
        sqlNodes.add(sqlWith.body);
        return "WITH " + getSemanticModelSqls(sqlNodes).entrySet().stream()
                .map(e -> e.getKey() + " AS (" + e.getValue() + ")")
                .collect(Collectors.joining(",")) +
                ", " + sqlNode2Sql(sqlNode).trim().substring(5); // 直接截掉开头的"WITH "（5个字符）;
    }

    private Set<String> extractReferencedTables(SqlNode sqlNode) {
        Set<String> tables = new HashSet<>();

        SqlSelect select;
        if (sqlNode instanceof SqlSelect) {
            select = (SqlSelect) sqlNode;
        } else if (sqlNode instanceof SqlOrderBy orderBy
                && orderBy.query instanceof SqlSelect) {
            select = (SqlSelect) orderBy.query;
        } else {
            return tables;
        }

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
        if (condition instanceof SqlSelect || condition instanceof SqlOrderBy) {
            // 处理子查询
            tables.addAll(extractReferencedTables(condition));
        } else if (condition instanceof SqlBasicCall call) {
            // 递归处理操作数
            for (SqlNode operand : call.getOperandList()) {
                extractTablesFromCondition(operand, tables);
            }
        }
        // 对于其他类型的节点，可能不包含表引用，暂时忽略
    }

    private void extractTablesFromNode(SqlNode sqlNode, Set<String> tables) {
        if (sqlNode instanceof SqlIdentifier identifier) {
            if (identifier.isSimple()) {
                tables.add(identifier.getSimple());
            }
        } else if (sqlNode instanceof SqlJoin join) {
            // 处理JOIN节点
            extractTablesFromNode(join.getLeft(), tables);
            extractTablesFromNode(join.getRight(), tables);
        } else if (sqlNode instanceof SqlBasicCall call) {
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
        } else if (sqlNode instanceof SqlSelect || sqlNode instanceof SqlOrderBy) {
            // 处理嵌套子查询
            tables.addAll(extractReferencedTables(sqlNode));
        }
        // 可以根据需要添加更多的节点类型处理
    }

    private Map<String, String> getSemanticModelSqls(List<SqlNode> sqlNodes) throws SqlParseException {
        // 提取所有SQL中引用的表名
        Set<String> referencedTables = sqlNodes.stream()
                .flatMap(sqlNode -> extractReferencedTables(sqlNode).stream())
                .collect(Collectors.toSet());
        // 找到使用的语义模型
        Set<SemanticModel> usedModels = findUsedSemanticModels(referencedTables);
        if (usedModels.isEmpty()) {
            throw new IllegalArgumentException("No matching semantic model was found");
        }
        return getSemanticModelSqls(usedModels);
    }

    private Map<String, String> getSemanticModelSqls(SqlNode sqlNode) throws SqlParseException {
        return getSemanticModelSqls(Collections.singletonList(sqlNode));
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
            String semanticModelSql = SemanticModelUtil.semanticModelSql(semanticAdapter, semanticModel);
            semanticModelSqls.put(quoteIdentifier(semanticModel.getName()), semanticModelSql);
        }
        return semanticModelSqls;
    }

    private String sqlNode2Sql(SqlNode sqlNode) {
        return sqlNode.toSqlString(sqlDialect).getSql();
    }

    private String quoteIdentifier(String name) {
        return semanticAdapter.quoteIdentifier(name);
    }
}