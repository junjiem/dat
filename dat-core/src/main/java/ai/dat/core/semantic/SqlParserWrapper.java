package ai.dat.core.semantic;

import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.fun.SqlLibrary;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.impl.SqlParserImpl;

/**
 * SQL解析器，基于Apache Calcite实现
 * 用于解析语义SQL并构建AST
 */
public class SqlParserWrapper {

    private final SqlParser.Config config;

    public SqlParserWrapper() {
        this.config = SqlParser.config()
                .withLex(Lex.MYSQL_ANSI)
                .withParserFactory(SqlParserImpl.FACTORY);
    }

    public SqlParserWrapper(Lex lex) {
        this.config = SqlParser.config()
                .withLex(lex)
                .withParserFactory(SqlParserImpl.FACTORY);
    }

    public SqlParserWrapper(SqlParser.Config config) {
        this.config = config;
    }

    public static SqlParserWrapper forDialect(SqlDialect sqlDialect) {
        Lex lex = switch (sqlDialect.getConformance().semantics()) {
            case MYSQL -> Lex.MYSQL;
            case POSTGRESQL -> // 使用MySQL兼容模式
                    Lex.MYSQL;
            case ORACLE -> Lex.ORACLE;
            case MSSQL -> Lex.SQL_SERVER;
            case BIG_QUERY -> Lex.BIG_QUERY;
            default -> Lex.MYSQL_ANSI;
        };
        return new SqlParserWrapper(lex);
    }

    /**
     * 解析SQL查询语句
     *
     * @param sqlQuery SQL查询语句
     * @return SqlNode AST节点
     * @throws SqlParseException 解析异常
     */
    public SqlNode parseQuery(String sqlQuery) throws SqlParseException {
        SqlParser parser = SqlParser.create(sqlQuery, config);
        return parser.parseQuery();
    }

    /**
     * 解析SQL表达式
     *
     * @param sqlExpr
     * @return
     * @throws SqlParseException
     */
    public SqlNode parseExpression(String sqlExpr) throws SqlParseException {
        SqlParser parser = SqlParser.create(sqlExpr, config);
        return parser.parseExpression();
    }
} 