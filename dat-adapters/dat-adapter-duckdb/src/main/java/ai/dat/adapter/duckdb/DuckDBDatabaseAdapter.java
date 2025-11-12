package ai.dat.adapter.duckdb;

import ai.dat.core.adapter.data.AnsiSqlType;
import ai.dat.core.adapter.GenericSqlDatabaseAdapter;

import javax.sql.DataSource;
import java.sql.Types;

/**
 * @Author JunjieM
 * @Date 2025/9/16
 */
public class DuckDBDatabaseAdapter extends GenericSqlDatabaseAdapter {

    public DuckDBDatabaseAdapter(DataSource dataSource) {
        super(new DuckDBSemanticAdapter(), dataSource);
    }

    @Override
    protected Object handleSpecificTypes(Object value, int columnType) {
        if (value == null) {
            return null;
        }
        switch (columnType) {
            case Types.BOOLEAN:
                if (value instanceof Boolean) {
                    return value;
                } else if (value instanceof Number) {
                    return ((Number) value).intValue() != 0;
                }
                break;
            case Types.TIMESTAMP:
                // DuckDB时间戳处理
                if (value instanceof java.sql.Timestamp) {
                    return value;
                }
                break;
            case Types.ARRAY:
                // DuckDB数组类型处理
                if (value instanceof java.sql.Array) {
                    return value;
                }
                break;
            case Types.BLOB:
                // DuckDB二进制数据处理
                if (value instanceof byte[]) {
                    return value;
                }
                break;
        }
        return value;
    }

    @Override
    protected int toColumnType(String dataType) {
        if (dataType == null) {
            return Types.VARCHAR;
        }
        return switch (extractBaseType(dataType).toUpperCase()) {
            case "TINYINT", "UTINYINT" -> Types.TINYINT;
            case "SMALLINT", "USMALLINT" -> Types.SMALLINT;
            case "INTEGER", "INT", "UINTEGER" -> Types.INTEGER;
            case "BIGINT", "INT8", "LONG", "UBIGINT", "HUGEINT" -> Types.BIGINT;
            case "DECIMAL", "NUMERIC" -> Types.DECIMAL;
            case "REAL", "FLOAT", "FLOAT4" -> Types.FLOAT;
            case "DOUBLE", "FLOAT8" -> Types.DOUBLE;
            case "BOOLEAN", "BOOL", "LOGICAL" -> Types.BOOLEAN;
            case "VARCHAR", "CHAR", "BPCHAR", "STRING", "TEXT" -> Types.VARCHAR;
            case "BLOB", "BYTEA", "BINARY", "VARBINARY" -> Types.BLOB;
            case "DATE" -> Types.DATE;
            case "TIME" -> Types.TIME;
            case "TIMESTAMP", "DATETIME", "TIMESTAMPTZ" -> Types.TIMESTAMP;
            case "INTERVAL" -> Types.VARCHAR;
            case "UUID" -> Types.VARCHAR;
            case "JSON" -> Types.LONGVARCHAR;
            case "BIT", "BITSTRING" -> Types.BIT;
            case "ARRAY", "LIST" -> Types.ARRAY;
            case "STRUCT", "MAP", "UNION" -> Types.STRUCT;
            case "ENUM" -> Types.VARCHAR;
            default -> Types.VARCHAR;
        };
    }

    @Override
    public AnsiSqlType toAnsiSqlType(int columnType, String columnTypeName, int precision, int scale) {
        // DuckDB特定的类型映射规则
        return switch (columnTypeName.toUpperCase()) {
            case "TINYINT", "UTINYINT" -> AnsiSqlType.TINYINT;
            case "SMALLINT", "USMALLINT" -> AnsiSqlType.SMALLINT;
            case "INTEGER", "INT", "UINTEGER" -> AnsiSqlType.INTEGER;
            case "BIGINT", "INT8", "LONG", "UBIGINT", "HUGEINT" -> AnsiSqlType.BIGINT;
            case "DECIMAL", "NUMERIC" -> AnsiSqlType.DECIMAL;
            case "REAL", "FLOAT", "FLOAT4" -> AnsiSqlType.FLOAT;
            case "DOUBLE", "FLOAT8" -> AnsiSqlType.DOUBLE;
            case "BOOLEAN", "BOOL", "LOGICAL" -> AnsiSqlType.BOOLEAN;
            case "CHAR" -> AnsiSqlType.CHAR;
            case "VARCHAR", "BPCHAR", "STRING" -> AnsiSqlType.VARCHAR;
            case "TEXT" -> AnsiSqlType.TEXT;
            case "BINARY" -> AnsiSqlType.BINARY;
            case "VARBINARY" -> AnsiSqlType.VARBINARY;
            case "BLOB", "BYTEA" -> AnsiSqlType.BLOB;
            case "DATE" -> AnsiSqlType.DATE;
            case "TIME" -> AnsiSqlType.TIME;
            case "TIMESTAMP", "DATETIME", "TIMESTAMPTZ" -> AnsiSqlType.TIMESTAMP;
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
                // 对于其他类型，使用默认的JDBC类型映射
                    super.toAnsiSqlType(columnType, columnTypeName, precision, scale);
        };
    }

    @Override
    protected String stringDataType() {
        return "TEXT";
    }

    private String extractBaseType(String dataType) {
        int parenIndex = dataType.indexOf('(');
        if (parenIndex == -1) {
            return dataType;
        }
        return dataType.substring(0, parenIndex).trim();
    }

    @Override
    public String limitClause(int limit) {
        return "LIMIT " + limit;
    }
}
