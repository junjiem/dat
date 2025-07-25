package ai.dat.adapter.postgresql;

import ai.dat.core.adapter.SemanticAdapter;
import ai.dat.core.semantic.data.Dimension;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.dialect.PostgresqlSqlDialect;

/**
 * PostgreSQL数据库语义适配器
 *
 * @Author JunjieM
 * @Date 2025/7/3
 */
class PostgreSqlSemanticAdapter implements SemanticAdapter {

    @Override
    public SqlDialect getSqlDialect() {
        return PostgresqlSqlDialect.DEFAULT;
    }

    @Override
    public String applyTimeGranularity(String dateExpr, Dimension.TypeParams.TimeGranularity granularity) {
        if (dateExpr == null || dateExpr.trim().isEmpty()) {
            throw new IllegalArgumentException("Date expression cannot be null or empty");
        }
        return switch (granularity) {
            case YEAR -> "DATE_TRUNC('year', " + dateExpr + ")";
            case QUARTER -> "DATE_TRUNC('quarter', " + dateExpr + ")";
            case MONTH -> "DATE_TRUNC('month', " + dateExpr + ")";
            case WEEK -> "DATE_TRUNC('week', " + dateExpr + ")";
            case DAY -> "DATE_TRUNC('day', " + dateExpr + ")";
            case HOUR -> "DATE_TRUNC('hour', " + dateExpr + ")";
            case MINUTE -> "DATE_TRUNC('minute', " + dateExpr + ")";
            case SECOND -> "DATE_TRUNC('second', " + dateExpr + ")";
        };
    }
} 