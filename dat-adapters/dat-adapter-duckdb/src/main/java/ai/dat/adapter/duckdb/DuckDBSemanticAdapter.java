package ai.dat.adapter.duckdb;

import ai.dat.core.adapter.SemanticAdapter;
import ai.dat.core.adapter.data.AnsiSqlType;
import ai.dat.core.semantic.data.Dimension;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.dialect.DuckDBSqlDialect;

/**
 * @Author JunjieM
 * @Date 2025/9/16
 */
public class DuckDBSemanticAdapter implements SemanticAdapter {

    public static final SqlDialect DEFAULT = new DuckDBSqlDialect(
            DuckDBSqlDialect.DEFAULT_CONTEXT.withIdentifierQuoteString(""));

    @Override
    public SqlDialect getSqlDialect() {
        return DEFAULT;
    }

    @Override
    public String applyTimeGranularity(String dateExpr, Dimension.TypeParams.TimeGranularity granularity) {
        if (dateExpr == null || dateExpr.trim().isEmpty()) {
            throw new IllegalArgumentException("Date expression cannot be null or empty");
        }
        return switch (granularity) {
            case YEAR -> "DATE_TRUNC('year', " + dateExpr + ")";
            case QUARTER -> "DATE_TRUNC('quarter', " + dateExpr + ")";
            case MONTH -> "DATE_TRUNC('month', " + dateExpr + ")";
            case WEEK -> "DATE_TRUNC('week', " + dateExpr + ")";
            case DAY -> "DATE_TRUNC('day', " + dateExpr + ")";
            case HOUR -> "DATE_TRUNC('hour', " + dateExpr + ")";
            case MINUTE -> "DATE_TRUNC('minute', " + dateExpr + ")";
            case SECOND -> "DATE_TRUNC('second', " + dateExpr + ")";
        };
    }

    @Override
    public AnsiSqlType toAnsiSqlType(String columnTypeName) {
        // DuckDB特定的类型映射规则
        return switch (columnTypeName.toUpperCase()) {
            case "TINYINT" -> AnsiSqlType.TINYINT;
            case "SMALLINT" -> AnsiSqlType.SMALLINT;
            case "INTEGER", "INT" -> AnsiSqlType.INTEGER;
            case "BIGINT", "INT8", "LONG" -> AnsiSqlType.BIGINT;
            case "HUGEINT" -> AnsiSqlType.BIGINT; // DuckDB的128位整数映射为BIGINT
            case "UTINYINT" -> AnsiSqlType.TINYINT;
            case "USMALLINT" -> AnsiSqlType.SMALLINT;
            case "UINTEGER" -> AnsiSqlType.INTEGER;
            case "UBIGINT" -> AnsiSqlType.BIGINT;
            case "DECIMAL", "NUMERIC" -> AnsiSqlType.DECIMAL;
            case "REAL", "FLOAT", "FLOAT4" -> AnsiSqlType.FLOAT;
            case "DOUBLE", "FLOAT8" -> AnsiSqlType.DOUBLE;
            case "BOOLEAN", "BOOL", "LOGICAL" -> AnsiSqlType.BOOLEAN;
            case "VARCHAR", "CHAR", "BPCHAR", "STRING", "TEXT" -> AnsiSqlType.VARCHAR;
            case "BLOB", "BYTEA", "BINARY", "VARBINARY" -> AnsiSqlType.BLOB;
            case "DATE" -> AnsiSqlType.DATE;
            case "TIME" -> AnsiSqlType.TIME;
            case "TIMESTAMP", "DATETIME" -> AnsiSqlType.TIMESTAMP;
            case "TIMESTAMPTZ" -> AnsiSqlType.TIMESTAMP; // 带时区的时间戳
            case "INTERVAL" -> AnsiSqlType.VARCHAR; // 间隔类型映射为VARCHAR
            case "UUID" -> AnsiSqlType.VARCHAR; // UUID映射为VARCHAR
            case "JSON" -> AnsiSqlType.TEXT; // JSON映射为TEXT
            case "BIT", "BITSTRING" -> AnsiSqlType.BINARY;
            case "ARRAY", "LIST" -> AnsiSqlType.TEXT; // 数组和列表映射为TEXT
            case "STRUCT" -> AnsiSqlType.TEXT; // 结构体映射为TEXT
            case "MAP" -> AnsiSqlType.TEXT; // 映射类型映射为TEXT
            case "UNION" -> AnsiSqlType.TEXT; // 联合类型映射为TEXT
            case "ENUM" -> AnsiSqlType.VARCHAR; // 枚举映射为VARCHAR
            default ->
                // 对于未知类型，返回UNKNOWN
                    AnsiSqlType.UNKNOWN;
        };
    }
}
