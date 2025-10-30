package ai.dat.adapter.mysql;

import ai.dat.core.adapter.SemanticAdapter;
import ai.dat.core.adapter.data.AnsiSqlType;
import ai.dat.core.semantic.data.Dimension;
import org.apache.calcite.sql.SqlDialect;

/**
 * MySQL数据库语义适配器
 *
 * @Author JunjieM
 * @Date 2025/7/3
 */
public class MySqlSemanticAdapter implements SemanticAdapter {

    public static final SqlDialect DEFAULT = new DatMysqlSqlDialect(
            DatMysqlSqlDialect.DEFAULT_CONTEXT.withIdentifierQuoteString(""));

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
            case YEAR -> "DATE_FORMAT(" + dateExpr + ", '%Y-01-01')";
            case QUARTER -> "CASE QUARTER(" + dateExpr + ") " +
                    "WHEN 1 THEN CONCAT(YEAR(" + dateExpr + "), '-01-01') " +
                    "WHEN 2 THEN CONCAT(YEAR(" + dateExpr + "), '-04-01') " +
                    "WHEN 3 THEN CONCAT(YEAR(" + dateExpr + "), '-07-01') " +
                    "WHEN 4 THEN CONCAT(YEAR(" + dateExpr + "), '-10-01') " +
                    "END";
            case MONTH -> "DATE_FORMAT(" + dateExpr + ", '%Y-%m-01')";
            case WEEK -> "DATE_SUB(" + dateExpr + ", INTERVAL WEEKDAY(" + dateExpr + ") DAY)";
            case DAY -> "DATE_FORMAT(" + dateExpr + ", '%Y-%m-%d')";
            case HOUR -> "DATE_FORMAT(" + dateExpr + ", '%Y-%m-%d %H:00:00')";
            case MINUTE -> "DATE_FORMAT(" + dateExpr + ", '%Y-%m-%d %H:%i:00')";
            case SECOND -> "DATE_FORMAT(" + dateExpr + ", '%Y-%m-%d %H:%i:%s')";
        };
    }

    @Override
    public AnsiSqlType toAnsiSqlType(String columnTypeName) {
        // MySQL特定的类型映射规则（仅基于类型名称）
        return switch (columnTypeName.toUpperCase()) {
            case "TINYINT" ->
                // MySQL的TINYINT，无法确定精度时，考虑到TINYINT(1)常用作布尔类型，默认为BOOLEAN
                    AnsiSqlType.BOOLEAN;
            case "SMALLINT" -> AnsiSqlType.SMALLINT;
            case "MEDIUMINT" ->
                // MySQL的MEDIUMINT映射为INTEGER
                    AnsiSqlType.INTEGER;
            case "INT", "INTEGER" -> AnsiSqlType.INTEGER;
            case "BIGINT" -> AnsiSqlType.BIGINT;
            case "DECIMAL", "DEC", "NUMERIC" -> AnsiSqlType.DECIMAL;
            case "FLOAT" -> AnsiSqlType.FLOAT;
            case "DOUBLE", "DOUBLE PRECISION", "REAL" -> AnsiSqlType.DOUBLE;
            case "BIT" -> AnsiSqlType.BOOLEAN;
            case "BOOL", "BOOLEAN" -> AnsiSqlType.BOOLEAN;
            case "CHAR" -> AnsiSqlType.CHAR;
            case "VARCHAR" -> AnsiSqlType.VARCHAR;
            case "TEXT", "TINYTEXT", "MEDIUMTEXT", "LONGTEXT" -> AnsiSqlType.TEXT;
            case "BINARY" -> AnsiSqlType.BINARY;
            case "VARBINARY" -> AnsiSqlType.VARBINARY;
            case "BLOB", "TINYBLOB", "MEDIUMBLOB", "LONGBLOB" -> AnsiSqlType.BLOB;
            case "DATE" -> AnsiSqlType.DATE;
            case "TIME" -> AnsiSqlType.TIME;
            case "DATETIME", "TIMESTAMP" -> AnsiSqlType.TIMESTAMP;
            case "YEAR" ->
                // MySQL的YEAR类型映射为SMALLINT
                    AnsiSqlType.SMALLINT;
            case "JSON" ->
                // MySQL的JSON类型映射为TEXT
                    AnsiSqlType.TEXT;
            case "GEOMETRY", "POINT", "LINESTRING", "POLYGON", "MULTIPOINT",
                    "MULTILINESTRING", "MULTIPOLYGON", "GEOMETRYCOLLECTION" ->
                // MySQL的空间数据类型映射为VARBINARY
                    AnsiSqlType.VARBINARY;
            default ->
                // 对于未知类型，返回UNKNOWN
                    AnsiSqlType.UNKNOWN;
        };
    }
}