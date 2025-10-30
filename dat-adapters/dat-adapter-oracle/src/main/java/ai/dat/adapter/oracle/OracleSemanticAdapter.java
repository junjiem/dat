package ai.dat.adapter.oracle;

import ai.dat.core.adapter.SemanticAdapter;
import ai.dat.core.adapter.data.AnsiSqlType;
import ai.dat.core.semantic.data.Dimension;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.dialect.OracleSqlDialect;

/**
 * Oracle数据库语义适配器
 */
public class OracleSemanticAdapter implements SemanticAdapter {

    public static final SqlDialect DEFAULT = new OracleSqlDialect(
            OracleSqlDialect.DEFAULT_CONTEXT.withIdentifierQuoteString(""));

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
            case YEAR -> "TRUNC(" + dateExpr + ", 'YEAR')";
            case QUARTER -> "TRUNC(" + dateExpr + ", 'Q')";
            case MONTH -> "TRUNC(" + dateExpr + ", 'MONTH')";
            case WEEK -> "TRUNC(" + dateExpr + ", 'WW')";
            case DAY -> "TRUNC(" + dateExpr + ", 'DD')";
            case HOUR -> "TRUNC(" + dateExpr + ", 'HH24')";
            case MINUTE -> "TRUNC(" + dateExpr + ", 'MI')";
            case SECOND -> "TRUNC(" + dateExpr + ", 'MI') + TRUNC(EXTRACT(SECOND FROM " + dateExpr + ")) / 86400";
        };
    }

    @Override
    public AnsiSqlType toAnsiSqlType(String columnTypeName) {
        // Oracle特定的类型映射规则（仅基于类型名称）
        return switch (columnTypeName.toUpperCase()) {
            case "NUMBER" ->
                // Oracle的NUMBER类型，无法确定精度时，为了与带精度的方法保持一致，默认为INTEGER（中等精度）
                    AnsiSqlType.INTEGER;
            case "BINARY_FLOAT" ->
                // Oracle的BINARY_FLOAT映射为REAL
                    AnsiSqlType.REAL;
            case "BINARY_DOUBLE" ->
                // Oracle的BINARY_DOUBLE映射为DOUBLE
                    AnsiSqlType.DOUBLE;
            case "FLOAT" -> AnsiSqlType.FLOAT;
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
                // 对于未知类型，返回UNKNOWN
                    AnsiSqlType.UNKNOWN;
        };
    }
} 