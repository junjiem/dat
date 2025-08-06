package ai.dat.core.adapter.data;

import lombok.Getter;

import java.sql.Types;

/**
 * ANSI SQL标准数据类型枚举
 * 用于统一不同数据库方言的字段类型
 *
 * @Author JunjieM
 * @Date 2025/8/7
 */
@Getter
public enum AnsiSqlType {

    // 字符类型
    CHAR("CHAR"),
    VARCHAR("VARCHAR"),
    TEXT("TEXT"),

    // 数值类型
    TINYINT("TINYINT"),
    SMALLINT("SMALLINT"),
    INTEGER("INTEGER"),
    BIGINT("BIGINT"),
    DECIMAL("DECIMAL"),
    NUMERIC("NUMERIC"),
    REAL("REAL"),
    DOUBLE("DOUBLE"),
    FLOAT("FLOAT"),

    // 布尔类型
    BOOLEAN("BOOLEAN"),

    // 日期时间类型
    DATE("DATE"),
    TIME("TIME"),
    TIMESTAMP("TIMESTAMP"),

    // 二进制类型
    BINARY("BINARY"),
    VARBINARY("VARBINARY"),
    BLOB("BLOB"),

    // 其他类型
    NULL("NULL"),
    UNKNOWN("UNKNOWN");

    private final String value;

    AnsiSqlType(String value) {
        this.value = value;
    }

    /**
     * 从JDBC类型映射到ANSI SQL类型
     *
     * @param columnType JDBC类型常量 (java.sql.Types)
     * @return 对应的ANSI SQL类型
     */
    public static AnsiSqlType fromColumnType(int columnType) {
        return switch (columnType) {
            // 字符类型
            case Types.CHAR, Types.NCHAR -> CHAR;
            case Types.VARCHAR, Types.NVARCHAR -> VARCHAR;
            case Types.LONGVARCHAR, Types.LONGNVARCHAR, Types.CLOB, Types.NCLOB -> TEXT;

            // 数值类型
            case Types.TINYINT -> TINYINT;
            case Types.SMALLINT -> SMALLINT;
            case Types.INTEGER -> INTEGER;
            case Types.BIGINT -> BIGINT;
            case Types.DECIMAL -> DECIMAL;
            case Types.NUMERIC -> NUMERIC;
            case Types.REAL -> REAL;
            case Types.DOUBLE -> DOUBLE;
            case Types.FLOAT -> FLOAT;

            // 布尔类型
            case Types.BOOLEAN, Types.BIT -> BOOLEAN;

            // 日期时间类型
            case Types.DATE -> DATE;
            case Types.TIME, Types.TIME_WITH_TIMEZONE -> TIME;
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> TIMESTAMP;

            // 二进制类型
            case Types.BINARY -> BINARY;
            case Types.VARBINARY -> VARBINARY;
            case Types.LONGVARBINARY, Types.BLOB -> BLOB;

            // 特殊类型
            case Types.NULL -> NULL;
            default -> UNKNOWN;
        };
    }

    @Override
    public String toString() {
        return value;
    }
}