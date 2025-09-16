package ai.dat.core.adapter;

import ai.dat.core.adapter.data.AnsiSqlType;
import ai.dat.core.semantic.data.Dimension;
import org.apache.calcite.sql.SqlDialect;

/**
 * 语义适配器接口类
 *
 * @Author JunjieM
 * @Date 2025/7/2
 */
public interface SemanticAdapter {

    /**
     * 获取数据库SQL方言
     *
     * @return SQL方言
     */
    SqlDialect getSqlDialect();

    /**
     * 获取数据库方言名称
     *
     * @return 方言名称
     */
    default String getDialectName() {
        return getSqlDialect().getConformance().semantics().name();
    }

    /**
     * 引用表名或列名
     *
     * @param identifier 表名或列名
     * @return 带引用符号的名称
     */
    default String quoteIdentifier(String identifier) {
        return getSqlDialect().quoteIdentifier(identifier);
    }

    /**
     * 应用时间粒度函数
     *
     * @param dateExpr    日期表达式
     * @param granularity 时间粒度
     * @return 应用时间粒度后的表达式
     */
    String applyTimeGranularity(String dateExpr, Dimension.TypeParams.TimeGranularity granularity);

    /**
     * 从数据库类型名映射到ANSI SQL类型
     *
     * @param columnTypeName
     * @return
     */
    AnsiSqlType toAnsiSqlType(String columnTypeName);
}
