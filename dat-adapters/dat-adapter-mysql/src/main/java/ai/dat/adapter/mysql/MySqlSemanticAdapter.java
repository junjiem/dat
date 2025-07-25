package ai.dat.adapter.mysql;

import ai.dat.core.adapter.SemanticAdapter;
import ai.dat.core.semantic.data.Dimension;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.dialect.MysqlSqlDialect;

/**
 * MySQL数据库语义适配器
 *
 * @Author JunjieM
 * @Date 2025/7/3
 */
public class MySqlSemanticAdapter implements SemanticAdapter {

    @Override
    public SqlDialect getSqlDialect() {
        return MysqlSqlDialect.DEFAULT;
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
}