package ai.dat.adapter.postgresql;

import ai.dat.core.adapter.SemanticAdapter;
import ai.dat.core.adapter.data.AnsiSqlType;
import ai.dat.core.semantic.data.Dimension;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.dialect.PostgresqlSqlDialect;

/**
 * PostgreSQL数据库语义适配器
 *
 * @Author JunjieM
 * @Date 2025/7/3
 */
class PostgreSqlSemanticAdapter implements SemanticAdapter {

    public static final SqlDialect DEFAULT = new PostgresqlSqlDialect(
            PostgresqlSqlDialect.DEFAULT_CONTEXT.withIdentifierQuoteString(""));

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
        // PostgreSQL特定的类型映射规则（仅基于类型名称）
        return switch (columnTypeName.toLowerCase()) {
            case "int2", "smallint", "smallserial" ->
                // PostgreSQL的int2映射为SMALLINT
                    AnsiSqlType.SMALLINT;
            case "int4", "int", "integer", "serial" ->
                // PostgreSQL的int4映射为INTEGER
                    AnsiSqlType.INTEGER;
            case "int8", "bigint", "bigserial" ->
                // PostgreSQL的int8映射为BIGINT
                    AnsiSqlType.BIGINT;
            case "float4", "real" ->
                // PostgreSQL的float4映射为REAL
                    AnsiSqlType.REAL;
            case "float8", "double precision", "float" ->
                // PostgreSQL的float8映射为DOUBLE
                    AnsiSqlType.DOUBLE;
            case "numeric", "decimal" ->
                // PostgreSQL的numeric映射为DECIMAL
                    AnsiSqlType.DECIMAL;
            case "money" ->
                // PostgreSQL的money类型映射为DECIMAL
                    AnsiSqlType.DECIMAL;
            case "bpchar", "char" ->
                // PostgreSQL的bpchar(blank-padded char)映射为CHAR
                    AnsiSqlType.CHAR;
            case "varchar", "character varying" ->
                // PostgreSQL的varchar映射为VARCHAR
                    AnsiSqlType.VARCHAR;
            case "text" ->
                // PostgreSQL的text映射为TEXT
                    AnsiSqlType.TEXT;
            case "bytea" ->
                // PostgreSQL的bytea映射为VARBINARY
                    AnsiSqlType.VARBINARY;
            case "bool", "boolean" ->
                // PostgreSQL的bool映射为BOOLEAN
                    AnsiSqlType.BOOLEAN;
            case "date" ->
                // PostgreSQL的date映射为DATE
                    AnsiSqlType.DATE;
            case "time", "time without time zone" ->
                // PostgreSQL的time类型映射为TIME
                    AnsiSqlType.TIME;
            case "timetz", "time with time zone" ->
                // PostgreSQL的timetz类型映射为TIME
                    AnsiSqlType.TIME;
            case "timestamp", "timestamp without time zone" ->
                // PostgreSQL的timestamp类型映射为TIMESTAMP
                    AnsiSqlType.TIMESTAMP;
            case "timestamptz", "timestamp with time zone" ->
                // PostgreSQL的timestamptz类型映射为TIMESTAMP
                    AnsiSqlType.TIMESTAMP;
            case "interval" ->
                // PostgreSQL的interval类型映射为VARCHAR
                    AnsiSqlType.VARCHAR;
            case "uuid" ->
                // PostgreSQL的UUID类型映射为VARCHAR
                    AnsiSqlType.VARCHAR;
            case "json", "jsonb" ->
                // PostgreSQL的JSON类型映射为TEXT
                    AnsiSqlType.TEXT;
            case "xml" ->
                // PostgreSQL的XML类型映射为TEXT
                    AnsiSqlType.TEXT;
            case "point", "line", "lseg", "box", "path", "polygon", "circle" ->
                // PostgreSQL的几何类型映射为TEXT
                    AnsiSqlType.TEXT;
            case "inet", "cidr", "macaddr", "macaddr8" ->
                // PostgreSQL的网络地址类型映射为VARCHAR
                    AnsiSqlType.VARCHAR;
            case "bit", "varbit" ->
                // PostgreSQL的位串类型映射为VARBINARY
                    AnsiSqlType.VARBINARY;
            case "tsvector", "tsquery" ->
                // PostgreSQL的全文搜索类型映射为TEXT
                    AnsiSqlType.TEXT;
            case "_int2", "_int4", "_int8", "_float4", "_float8", "_text", "_varchar" ->
                // PostgreSQL的数组类型，映射为TEXT（以字符串形式表示）
                    AnsiSqlType.TEXT;
            default ->
                // 对于未知类型，返回UNKNOWN
                    AnsiSqlType.UNKNOWN;
        };
    }
} 