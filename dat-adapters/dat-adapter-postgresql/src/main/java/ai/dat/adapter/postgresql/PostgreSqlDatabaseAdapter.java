package ai.dat.adapter.postgresql;

import ai.dat.core.adapter.GenericSqlDatabaseAdapter;
import ai.dat.core.adapter.data.AnsiSqlType;
import org.postgresql.util.PGobject;

import javax.sql.DataSource;
import java.sql.Types;

/**
 * PostgreSQL数据库适配器
 * 处理PostgreSQL特定的数据类型转换和映射
 *
 * @Author JunjieM
 * @Date 2025/7/2
 */
public class PostgreSqlDatabaseAdapter extends GenericSqlDatabaseAdapter {
    public PostgreSqlDatabaseAdapter(DataSource dataSource) {
        super(new PostgreSqlSemanticAdapter(), dataSource);
    }

    @Override
    protected Object handleSpecificTypes(Object value, int columnType) {
        if (value == null) {
            return null;
        }

        switch (columnType) {
            case Types.BIT:
            case Types.BOOLEAN:
                // PostgreSQL的BOOLEAN类型转换
                if (value instanceof Boolean) {
                    return value;
                } else if (value instanceof String str) {
                    return "t".equalsIgnoreCase(str) || "true".equalsIgnoreCase(str) || "1".equals(str);
                } else if (value instanceof Number num) {
                    return num.intValue() != 0;
                }
                break;

            case Types.SMALLINT:
                // PostgreSQL的SMALLINT(int2)类型
                if (value instanceof Number num) {
                    return num.shortValue();
                }
                break;

            case Types.INTEGER:
                // PostgreSQL的INTEGER(int4)类型
                if (value instanceof Number num) {
                    return num.intValue();
                }
                break;

            case Types.BIGINT:
                // PostgreSQL的BIGINT(int8)类型
                if (value instanceof Number num) {
                    return num.longValue();
                }
                break;

            case Types.REAL:
                // PostgreSQL的REAL(float4)类型
                if (value instanceof Number num) {
                    return num.floatValue();
                }
                break;

            case Types.DOUBLE:
            case Types.FLOAT:
                // PostgreSQL的DOUBLE PRECISION(float8)类型
                if (value instanceof Number num) {
                    return num.doubleValue();
                }
                break;

            case Types.NUMERIC:
            case Types.DECIMAL:
                // PostgreSQL的NUMERIC/DECIMAL类型
                if (value instanceof java.math.BigDecimal bd) {
                    // 保持BigDecimal的精度，不做强制转换
                    return bd;
                }
                break;

            case Types.CHAR:
                // PostgreSQL的CHAR(bpchar)类型，去除末尾空格
                if (value instanceof String str) {
                    return str.trim();
                }
                break;

            case Types.VARCHAR:
            case Types.LONGVARCHAR:
                // PostgreSQL的VARCHAR和TEXT类型
                if (value instanceof String) {
                    return value;
                }
                break;

            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                // PostgreSQL的BYTEA类型
                if (value instanceof byte[]) {
                    return value;
                }
                break;

            case Types.DATE:
                // PostgreSQL的DATE类型
                if (value instanceof java.sql.Date) {
                    return value;
                }
                break;

            case Types.TIME:
            case Types.TIME_WITH_TIMEZONE:
                // PostgreSQL的TIME和TIMETZ类型
                if (value instanceof java.sql.Time) {
                    return value;
                }
                break;

            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                // PostgreSQL的TIMESTAMP和TIMESTAMPTZ类型
                if (value instanceof java.sql.Timestamp) {
                    return value;
                }
                break;

            case Types.ARRAY:
                // PostgreSQL的数组类型
                if (value instanceof java.sql.Array array) {
                    try {
                        // 将SQL数组转换为Java数组
                        return array.getArray();
                    } catch (Exception e) {
                        // 如果转换失败，返回字符串表示
                        return value.toString();
                    }
                }
                break;

            case Types.OTHER:
                // PostgreSQL的特殊类型（JSON、JSONB、UUID、几何类型等）
                if (value instanceof PGobject pgObject) {
                    String type = pgObject.getType();
                    String stringValue = pgObject.getValue();

                    switch (type.toLowerCase()) {
                        case "json":
                        case "jsonb":
                            // JSON类型保持为字符串，后续可由应用层解析
                            return stringValue;

                        case "uuid":
                            // UUID类型转换为标准字符串格式
                            return stringValue;

                        case "point":
                        case "line":
                        case "lseg":
                        case "box":
                        case "path":
                        case "polygon":
                        case "circle":
                            // 几何类型保持为字符串表示
                            return stringValue;

                        case "inet":
                        case "cidr":
                        case "macaddr":
                        case "macaddr8":
                            // 网络地址类型保持为字符串
                            return stringValue;

                        case "interval":
                            // 时间间隔类型保持为字符串
                            return stringValue;

                        case "money":
                            // 货币类型转换为数值
                            try {
                                // 移除货币符号并转换为BigDecimal
                                String cleanValue = stringValue.replaceAll("[^0-9.-]", "");
                                return new java.math.BigDecimal(cleanValue);
                            } catch (NumberFormatException e) {
                                return stringValue;
                            }

                        case "bit":
                        case "varbit":
                            // 位串类型保持为字符串
                            return stringValue;

                        case "tsvector":
                        case "tsquery":
                            // 全文搜索类型保持为字符串
                            return stringValue;

                        default:
                            // 其他未知的PG类型保持为字符串
                            return stringValue;
                    }
                }
                break;
        }

        return value;
    }

    @Override
    public AnsiSqlType toAnsiSqlType(int columnType, String columnTypeName, int precision, int scale) {
        // PostgreSQL特定的类型映射规则
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
        return switch (extractBaseType(dataType).toLowerCase()) {
            case "smallint", "int2" -> Types.SMALLINT;
            case "integer", "int", "int4" -> Types.INTEGER;
            case "bigint", "int8" -> Types.BIGINT;
            case "serial", "serial4" -> Types.INTEGER;
            case "bigserial", "serial8" -> Types.BIGINT;
            case "real", "float4" -> Types.REAL;
            case "double precision", "float8" -> Types.DOUBLE;
            case "numeric", "decimal" -> Types.NUMERIC;
            case "money" -> Types.NUMERIC;
            case "boolean", "bool" -> Types.BOOLEAN;
            case "char", "\"char\"" -> Types.CHAR;
            case "varchar", "character varying" -> Types.VARCHAR;
            case "character", "bpchar" -> Types.CHAR;
            case "text" -> Types.LONGVARCHAR;
            case "date" -> Types.DATE;
            case "time", "time without time zone" -> Types.TIME;
            case "timetz", "time with time zone" -> Types.TIME;
            case "timestamp", "timestamp without time zone" -> Types.TIMESTAMP;
            case "timestamptz", "timestamp with time zone" -> Types.TIMESTAMP;
            case "interval" -> Types.VARCHAR;
            case "uuid" -> Types.VARCHAR;
            case "json", "jsonb" -> Types.LONGVARCHAR;
            case "xml" -> Types.LONGVARCHAR;
            case "bytea" -> Types.VARBINARY;
            case "bit" -> Types.BIT;
            case "varbit", "bit varying" -> Types.VARBINARY;
            case "point", "line", "lseg", "box", "path", "polygon", "circle" -> Types.LONGVARCHAR;
            case "inet", "cidr", "macaddr", "macaddr8" -> Types.VARCHAR;
            case "tsvector", "tsquery" -> Types.LONGVARCHAR;
            // 常见数组类型，统一映射为LONGVARCHAR（字符串表示）
            case "_int2", "_int4", "_int8", "_float4", "_float8", "_text", "_varchar" -> Types.LONGVARCHAR;
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
    protected String getDropTableSqlIfExists(String tableName) {
        return String.format("DROP TABLE IF EXISTS %s CASCADE", quoteIdentifier(tableName));
    }

    @Override
    public String limitClause(int limit) {
        return "LIMIT " + limit;
    }
}