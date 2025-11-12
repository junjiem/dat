package ai.dat.adapter.mysql;

import ai.dat.core.adapter.data.AnsiSqlType;
import ai.dat.core.adapter.GenericSqlDatabaseAdapter;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.sql.Types;

/**
 * MySQL数据库适配器
 * 处理MySQL特定的数据类型转换和映射
 *
 * @Author JunjieM
 * @Date 2025/7/1
 */
public class MySqlDatabaseAdapter extends GenericSqlDatabaseAdapter {

    public MySqlDatabaseAdapter(DataSource dataSource) {
        super(new MySqlSemanticAdapter(), dataSource);
    }

    @Override
    protected Object handleSpecificTypes(Object value, int columnType) {
        if (value == null) {
            return null;
        }
        switch (columnType) {
            case Types.BIT:
                if (value instanceof Boolean) {
                    return value;
                } else if (value instanceof byte[] bytes) {
                    return bytes.length > 0 && bytes[0] != 0;
                }
                break;
            case Types.TINYINT:
                if (value instanceof Number) {
                    int intValue = ((Number) value).intValue();
                    return intValue != 0;
                }
                break;
            case Types.TIMESTAMP:
                if (value instanceof Timestamp) {
                    return value;
                }
                break;
        }
        return value;
    }

    @Override
    public AnsiSqlType toAnsiSqlType(int columnType, String columnTypeName, int precision, int scale) {
        // MySQL特定的类型映射规则
        return switch (columnTypeName.toUpperCase()) {
            case "TINYINT" -> {
                if (precision == 1) {
                    yield AnsiSqlType.BOOLEAN;
                }
                yield AnsiSqlType.TINYINT;
                // MySQL的TINYINT(1)通常用作布尔类型
            }
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
            case "TEXT", "TINYTEXT", "MEDIUMTEXT", "LONGTEXT" ->
                // MySQL的TEXT变体都映射为TEXT
                    AnsiSqlType.TEXT;
            case "BINARY" -> AnsiSqlType.BINARY;
            case "VARBINARY" -> AnsiSqlType.VARBINARY;
            case "BLOB", "TINYBLOB", "MEDIUMBLOB", "LONGBLOB" ->
                // MySQL的BLOB变体都映射为BLOB
                    AnsiSqlType.BLOB;
            case "DATE" -> AnsiSqlType.DATE;
            case "TIME" -> AnsiSqlType.TIME;
            case "DATETIME", "TIMESTAMP" ->
                // MySQL的DATETIME映射为TIMESTAMP
                    AnsiSqlType.TIMESTAMP;
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
                // 对于其他类型，使用默认的JDBC类型映射
                    super.toAnsiSqlType(columnType, columnTypeName, precision, scale);
        };
    }

    @Override
    protected String stringDataType() {
        return "TEXT";
    }

    @Override
    protected int toColumnType(String dataType) {
        if (dataType == null) {
            return Types.VARCHAR;
        }
        return switch (extractBaseType(dataType).toUpperCase()) {
            case "TINYINT" -> Types.TINYINT;
            case "SMALLINT", "YEAR" -> Types.SMALLINT;
            case "MEDIUMINT", "INT", "INTEGER" -> Types.INTEGER;
            case "BIGINT" -> Types.BIGINT;
            case "DECIMAL", "DEC", "NUMERIC", "FIXED" -> Types.DECIMAL;
            case "FLOAT" -> Types.FLOAT;
            case "DOUBLE", "DOUBLE PRECISION", "REAL" -> Types.DOUBLE;
            case "BIT", "BOOL", "BOOLEAN" -> Types.BOOLEAN;
            case "CHAR" -> Types.CHAR;
            case "VARCHAR" -> Types.VARCHAR;
            case "TINYTEXT", "TEXT", "MEDIUMTEXT", "LONGTEXT", "JSON" -> Types.LONGVARCHAR;
            case "BINARY" -> Types.BINARY;
            case "VARBINARY" -> Types.VARBINARY;
            case "TINYBLOB", "BLOB", "MEDIUMBLOB", "LONGBLOB" -> Types.BLOB;
            case "DATE" -> Types.DATE;
            case "TIME" -> Types.TIME;
            case "DATETIME", "TIMESTAMP" -> Types.TIMESTAMP;
            case "ENUM", "SET" -> Types.VARCHAR;
            case "GEOMETRY", "POINT", "LINESTRING", "POLYGON", "MULTIPOINT",
                 "MULTILINESTRING", "MULTIPOLYGON", "GEOMETRYCOLLECTION" -> Types.VARBINARY;
            default -> Types.VARCHAR;
        };
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