package ai.dat.adapter.oracle;

import ai.dat.core.adapter.data.AnsiSqlType;
import ai.dat.core.adapter.GenericSqlDatabaseAdapter;

import javax.sql.DataSource;
import java.sql.Types;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Oracle数据库适配器
 * 处理Oracle特定的数据类型转换和映射
 * 
 * @Author JunjieM
 * @Date 2025/7/3
 */
public class OracleDatabaseAdapter extends GenericSqlDatabaseAdapter {

    public OracleDatabaseAdapter(DataSource dataSource) {
        super(new OracleSemanticAdapter(), dataSource);
    }

    @Override
    protected Object handleSpecificTypes(Object value, int columnType) {
        if (value == null) {
            return null;
        }
        
        switch (columnType) {
            case Types.NUMERIC:
            case Types.DECIMAL:
                // Oracle的NUMBER类型可能映射为BigDecimal
                if (value instanceof BigDecimal bd) {
                    // 如果是整数且在合理范围内，转换为Long或Integer
                    if (bd.scale() == 0) {
                        if (bd.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) <= 0 &&
                            bd.compareTo(BigDecimal.valueOf(Integer.MIN_VALUE)) >= 0) {
                            return bd.intValue();
                        } else {
                            return bd.longValue();
                        }
                    }
                    return bd.doubleValue();
                }
                break;
                
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                // Oracle的TIMESTAMP类型
                if (value instanceof Timestamp) {
                    return value;
                }
                break;
                
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.NCHAR:
            case Types.NVARCHAR:
                // Oracle的字符类型，去除末尾空格（如果是CHAR类型）
                if (value instanceof String str) {
                    return columnType == Types.CHAR ? str.trim() : str;
                }
                break;
                
            case Types.CLOB:
            case Types.NCLOB:
                // Oracle的CLOB类型
                if (value instanceof java.sql.Clob clob) {
                    try {
                        return clob.getSubString(1, (int) clob.length());
                    } catch (Exception e) {
                        return value.toString();
                    }
                }
                break;
        }
        
        return value;
    }
    
    @Override
    public AnsiSqlType toAnsiSqlType(int columnType, String columnTypeName, int precision, int scale) {
        // Oracle特定的类型映射规则
        return switch (columnTypeName.toUpperCase()) {
            case "NUMBER" -> {
                // Oracle的NUMBER类型需要根据精度和小数位数进行细分
                if (scale == 0) {
                    // 整数类型
                    if (precision <= 3) {
                        yield AnsiSqlType.TINYINT;
                    } else if (precision <= 5) {
                        yield AnsiSqlType.SMALLINT;
                    } else if (precision <= 10) {
                        yield AnsiSqlType.INTEGER;
                    } else {
                        yield AnsiSqlType.BIGINT;
                    }
                } else {
                    // 小数类型
                    yield AnsiSqlType.DECIMAL;
                }
            }
                
            case "BINARY_FLOAT" ->
                // Oracle的BINARY_FLOAT映射为REAL
                    AnsiSqlType.REAL;
            case "BINARY_DOUBLE" ->
                // Oracle的BINARY_DOUBLE映射为DOUBLE
                    AnsiSqlType.DOUBLE;
            case "FLOAT" ->
                    AnsiSqlType.FLOAT;
            case "VARCHAR2", "NVARCHAR2" ->
                // Oracle的VARCHAR2映射为VARCHAR
                    AnsiSqlType.VARCHAR;
            case "CHAR", "NCHAR" ->
                // Oracle的CHAR映射为CHAR
                    AnsiSqlType.CHAR;
            case "CLOB", "NCLOB" ->
                // Oracle的CLOB映射为TEXT
                    AnsiSqlType.TEXT;
            case "BLOB" ->
                // Oracle的BLOB映射为BLOB
                    AnsiSqlType.BLOB;
            case "RAW", "LONG RAW" ->
                // Oracle的RAW类型映射为VARBINARY
                    AnsiSqlType.VARBINARY;
            case "DATE" ->
                // Oracle的DATE类型实际包含时间信息，映射为TIMESTAMP
                    AnsiSqlType.TIMESTAMP;
            case "TIMESTAMP", "TIMESTAMP WITH TIME ZONE", "TIMESTAMP WITH LOCAL TIME ZONE" ->
                // Oracle的TIMESTAMP类型映射为TIMESTAMP
                    AnsiSqlType.TIMESTAMP;
            case "INTERVAL YEAR TO MONTH", "INTERVAL DAY TO SECOND" ->
                // Oracle的INTERVAL类型映射为VARCHAR
                    AnsiSqlType.VARCHAR;
            case "XMLTYPE" ->
                // Oracle的XMLType映射为TEXT
                    AnsiSqlType.TEXT;
            case "ROWID", "UROWID" ->
                // Oracle的ROWID类型映射为VARCHAR
                    AnsiSqlType.VARCHAR;
            case "BFILE" ->
                // Oracle的BFILE类型映射为VARCHAR（存储文件路径）
                    AnsiSqlType.VARCHAR;
            case "LONG" ->
                // Oracle的LONG类型映射为TEXT
                    AnsiSqlType.TEXT;
            default ->
                // 对于其他类型，使用默认的JDBC类型映射
                    super.toAnsiSqlType(columnType, columnTypeName, precision, scale);
        };
    }

    @Override
    protected String stringDataType() {
        return "CLOB";
    }

    @Override
    protected int toColumnType(String dataType) {
        if (dataType == null) {
            return Types.VARCHAR;
        }
        return switch (extractBaseType(dataType).toUpperCase()) {
            case "NUMBER" -> Types.NUMERIC;
            case "FLOAT", "BINARY_FLOAT" -> Types.FLOAT;
            case "BINARY_DOUBLE" -> Types.DOUBLE;
            case "CHAR", "NCHAR" -> Types.CHAR;
            case "VARCHAR2", "NVARCHAR2" -> Types.VARCHAR;
            case "CLOB", "NCLOB" -> Types.CLOB;
            case "BLOB" -> Types.BLOB;
            case "RAW", "LONG RAW" -> Types.VARBINARY;
            case "DATE" -> Types.TIMESTAMP; // Oracle的DATE包含时间
            case "TIMESTAMP", "TIMESTAMP WITH TIME ZONE", "TIMESTAMP WITH LOCAL TIME ZONE" -> Types.TIMESTAMP;
            case "INTERVAL YEAR TO MONTH", "INTERVAL DAY TO SECOND" -> Types.VARCHAR;
            case "XMLTYPE" -> Types.CLOB;
            case "ROWID", "UROWID" -> Types.VARCHAR;
            case "BFILE" -> Types.VARCHAR;
            case "LONG" -> Types.LONGVARCHAR;
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
        return String.format("BEGIN EXECUTE IMMEDIATE 'DROP TABLE %s'; EXCEPTION WHEN OTHERS THEN NULL; END;", 
                           quoteIdentifier(tableName));
    }

    @Override
    public String limitClause(int limit) {
        return "FETCH FIRST " + limit + " ROWS ONLY";
    }
}