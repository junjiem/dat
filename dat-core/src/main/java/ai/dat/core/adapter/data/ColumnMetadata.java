package ai.dat.core.adapter.data;

import lombok.Builder;
import lombok.Data;

/**
 * 统一的列元数据信息
 * 提供标准化的字段信息描述
 *
 * @Author JunjieM
 * @Date 2025/8/7
 */
@Data
@Builder
public class ColumnMetadata {

    /**
     * 列名
     */
    private String columnName;

    /**
     * 列标签（通常用于显示）
     */
    private String columnLabel;

    /**
     * 原始JDBC类型
     */
    private int columnType;

    /**
     * 原始数据库类型名称
     */
    private String columnTypeName;

    /**
     * 统一后的ANSI SQL类型
     */
    private AnsiSqlType ansiSqlType;

    /**
     * 列长度/精度
     */
    private int precision;

    /**
     * 小数位数
     */
    private int scale;

    /**
     * 是否可为空
     */
    private boolean nullable;

    /**
     * 是否自动递增
     */
    private boolean autoIncrement;

    /**
     * 列的显示大小
     */
    private int displaySize;

    /**
     * 列在结果集中的位置（1-based）
     */
    private int columnIndex;

}